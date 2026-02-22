# 音声入力安定化計画 — MozukuTsuchiKey

## Context

MozukuTsuchiKey の音声入力が不安定。報告されている症状:
1. ボタンを押しても入力が始まらない
2. オンオフで状態が切り替わる（トグルが逆転する）
3. 10秒の idle timeout 前に不自然に切れる

**根本原因**: `SpeechRecognizer` の `onResults()` から即座に `startListening()` を呼ぶとサイレント失敗する（Android の既知問題）。RecognitionService の内部クリーンアップが完了する前に再開しようとすると、コールバックが一切来なくなる。

## 修正対象ファイル

- `app/src/main/java/com/xaaav/mozukutsuchikey/keyboard/ImeKeyboard.kt` — 音声入力ロジック本体

## 修正内容

### 1. destroy-and-recreate パターンを全リスタートに適用

**現在**: `onResults()` は同じインスタンスで `startListening()` を呼ぶ。`recreateSpeechRecognizer()` は `onError` のみ。

**修正**: リスタート時は常に destroy → recreate → delay(150ms) → startListening() にする。

```kotlin
// 新しいリスタート関数
fun restartListening() {
    recreateSpeechRecognizer()
    voiceScope.launch {
        delay(150)
        if (isListening) startListening()
    }
}
```

`onResults()` と `onError()` の両方から `restartListening()` を呼ぶ。

### 2. エラーハンドリングの3段階分類

**現在**: `ERROR_NO_MATCH` / `ERROR_SPEECH_TIMEOUT` → リトライ、それ以外 → 停止

**修正**:

| 分類 | エラーコード | アクション |
|------|-------------|-----------|
| benign | `ERROR_NO_MATCH`(7), `ERROR_SPEECH_TIMEOUT`(6) | `restartListening()` (recreate + delay) |
| recoverable | `ERROR_AUDIO`(3), `ERROR_CLIENT`(5), `ERROR_RECOGNIZER_BUSY`(8), `ERROR_SERVER`(4), `ERROR_NETWORK`(2), `ERROR_NETWORK_TIMEOUT`(1) | `restartListening()` (recreate + delay、ネットワーク系はやや長め 500ms) |
| fatal | `ERROR_INSUFFICIENT_PERMISSIONS`(9), `ERROR_LANGUAGE_NOT_SUPPORTED`(12), `ERROR_LANGUAGE_UNAVAILABLE`(13) | `stopVoiceInput()` |

### 3. `EXTRA_PARTIAL_RESULTS = true` に変更

- `onPartialResults()` でも `resetIdleTimeout()` を呼ぶことで、認識処理中のタイムアウト切れを防止
- （テキスト表示はこのフェーズでは追加しない — まず安定化が優先）

### 4. `onReadyForSpeech` で状態確認を強化

`onReadyForSpeech` が呼ばれない場合 = `startListening()` がサイレント失敗した可能性。
startListening 後に 3 秒タイムアウトを設け、`onReadyForSpeech` が来なければ recreate + retry する。

```kotlin
var readyTimeoutJob: Job? = null

fun startListening() {
    // ... existing intent setup ...
    speechRecognizer.startListening(intent)

    readyTimeoutJob?.cancel()
    readyTimeoutJob = voiceScope.launch {
        delay(3_000)
        if (isListening) {
            Log.w("VoiceInput", "onReadyForSpeech not received, restarting")
            restartListening()
        }
    }
}

// onReadyForSpeech で:
override fun onReadyForSpeech(params: Bundle?) {
    readyTimeoutJob?.cancel()
    resetIdleTimeout()
}
```

### 5. silence timeout の延長ヒント（デバイス依存）

効果はデバイスに依存するが、Intent に追加しておく:
```kotlin
putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
```

## 修正しないこと

- 状態管理の ViewModel 移行（IME は Composable 中心のアーキテクチャで妥当、このフェーズでは不要）
- partial results のリアルタイムテキスト表示（安定化が優先）
- Vosk 等の代替エンジンへの移行

## 検証方法

1. `/mozukutsuchikey-dev build` → install → enable-ime
2. テキスト入力フィールドで音声入力ボタンを押す
3. 以下を確認:
   - 連続して複数回話しかけて、毎回テキストが入力されること
   - 10秒黙った後に自動停止すること
   - ボタンのオンオフが正しくトグルすること
   - 話し続けている最中に不自然に切れないこと
4. logcat で `VoiceInput` タグのログを確認（エラーリカバリが動いていること）
