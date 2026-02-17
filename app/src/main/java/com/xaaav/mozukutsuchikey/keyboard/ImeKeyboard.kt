package com.xaaav.mozukutsuchikey.keyboard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.KeyEvent as AndroidKeyEvent
import android.view.inputmethod.InputConnection
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Abc
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.xaaav.mozukutsuchikey.PermissionActivity
import com.xaaav.mozukutsuchikey.clipboard.ClipboardBar
import com.xaaav.mozukutsuchikey.clipboard.ClipboardHistory
import com.xaaav.mozukutsuchikey.mozc.MozcCandidateBar
import com.xaaav.mozukutsuchikey.mozc.MozcInputController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.KeyEvent.SpecialKey
import java.util.Locale

@Composable
fun ImeKeyboard(
    inputConnection: () -> InputConnection?,
    mozcController: MozcInputController,
    clipboardHistory: ClipboardHistory,
    inputActive: StateFlow<Boolean>,
    modifier: Modifier = Modifier,
    onKeyboardBoundsChanged: ((androidx.compose.ui.geometry.Rect) -> Unit)? = null,
) {
    val dims = getQwertyDimensions()
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val gridKeys = if (screenWidthDp > 600) GRID_KEYS_SPLIT else GRID_KEYS
    val context = LocalContext.current

    var isJapaneseMode by remember { mutableStateOf(false) }
    var symbolMode by remember { mutableStateOf(false) }
    var showClipboard by remember { mutableStateOf(false) }
    val clipboardItems by clipboardHistory.items.collectAsStateWithLifecycle()
    var isListening by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    val voiceScope = rememberCoroutineScope()
    var idleTimeoutJob by remember { mutableStateOf<Job?>(null) }

    var speechRecognizer by remember {
        mutableStateOf(SpeechRecognizer.createSpeechRecognizer(context))
    }
    DisposableEffect(Unit) {
        onDispose {
            idleTimeoutJob?.cancel()
            speechRecognizer.destroy()
        }
    }

    fun stopVoiceInput() {
        idleTimeoutJob?.cancel()
        idleTimeoutJob = null
        speechRecognizer.cancel()
        isListening = false
        isSpeaking = false
    }

    // inputActive が false になったら（アプリ切替時など）マイクを停止
    LaunchedEffect(Unit) {
        inputActive.collect { active ->
            if (!active && isListening) {
                stopVoiceInput()
            }
        }
    }

    fun resetIdleTimeout() {
        idleTimeoutJob?.cancel()
        idleTimeoutJob = voiceScope.launch {
            delay(10_000)
            stopVoiceInput()
        }
    }

    fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        isListening = true
        speechRecognizer.startListening(intent)
    }

    val currentInputConnection by rememberUpdatedState(inputConnection)

    fun recreateSpeechRecognizer() {
        speechRecognizer.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    }

    val recognitionListener = remember {
        object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                if (text != null) {
                    currentInputConnection()?.commitText(text, 1)
                    resetIdleTimeout()
                }
                if (isListening) startListening()
            }
            override fun onError(error: Int) {
                Log.w("VoiceInput", "SpeechRecognizer error: $error")
                recreateSpeechRecognizer()
                if (isListening && (error == SpeechRecognizer.ERROR_NO_MATCH
                            || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)) {
                    startListening()
                } else {
                    stopVoiceInput()
                }
            }
            override fun onEndOfSpeech() { isSpeaking = false }
            override fun onReadyForSpeech(params: Bundle?) { resetIdleTimeout() }
            override fun onBeginningOfSpeech() { isSpeaking = true; resetIdleTimeout() }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }
    DisposableEffect(speechRecognizer) {
        speechRecognizer.setRecognitionListener(recognitionListener)
        onDispose {}
    }

    // Modifier state
    var ctrlState by remember { mutableStateOf(ModifierLevel.OFF) }
    var altState by remember { mutableStateOf(ModifierLevel.OFF) }
    var shiftState by remember { mutableStateOf(ModifierLevel.OFF) }

    val isShiftActive = shiftState != ModifierLevel.OFF
    val isCtrlActive = ctrlState != ModifierLevel.OFF
    val isAltActive = altState != ModifierLevel.OFF

    fun clearTransientModifiers() {
        if (ctrlState == ModifierLevel.TRANSIENT) ctrlState = ModifierLevel.OFF
        if (altState == ModifierLevel.TRANSIENT) altState = ModifierLevel.OFF
    }

    fun toggleModifier(type: ModifierType) {
        when (type) {
            ModifierType.SHIFT -> shiftState =
                if (shiftState == ModifierLevel.OFF) ModifierLevel.LOCKED else ModifierLevel.OFF
            ModifierType.CTRL -> ctrlState =
                if (ctrlState == ModifierLevel.OFF) ModifierLevel.TRANSIENT else ModifierLevel.OFF
            ModifierType.ALT -> altState =
                if (altState == ModifierLevel.OFF) ModifierLevel.TRANSIENT else ModifierLevel.OFF
        }
    }

    fun sendKeyEvent(keyCode: Int, metaState: Int = 0) {
        val ic = currentInputConnection() ?: return
        val now = android.os.SystemClock.uptimeMillis()
        ic.sendKeyEvent(AndroidKeyEvent(now, now, AndroidKeyEvent.ACTION_DOWN, keyCode, 0, metaState))
        ic.sendKeyEvent(AndroidKeyEvent(now, now, AndroidKeyEvent.ACTION_UP, keyCode, 0, metaState))
    }

    fun buildMetaState(): Int {
        var meta = 0
        if (isCtrlActive) meta = meta or AndroidKeyEvent.META_CTRL_ON or AndroidKeyEvent.META_CTRL_LEFT_ON
        if (isAltActive) meta = meta or AndroidKeyEvent.META_ALT_ON or AndroidKeyEvent.META_ALT_LEFT_ON
        if (isShiftActive) meta = meta or AndroidKeyEvent.META_SHIFT_ON or AndroidKeyEvent.META_SHIFT_LEFT_ON
        return meta
    }

    fun routeToMozc(key: Key): Boolean {
        if (!mozcController.isComposing) return false
        val specialKey = when {
            key is Key.Repeatable && key.keyCode == AndroidKeyEvent.KEYCODE_DEL -> SpecialKey.BACKSPACE
            key is Key.Action && key.keyCode == AndroidKeyEvent.KEYCODE_ENTER -> SpecialKey.ENTER
            key is Key.Action && key.keyCode == AndroidKeyEvent.KEYCODE_ESCAPE -> SpecialKey.ESCAPE
            key is Key.Repeatable && key.keyCode == AndroidKeyEvent.KEYCODE_DPAD_UP -> SpecialKey.UP
            key is Key.Repeatable && key.keyCode == AndroidKeyEvent.KEYCODE_DPAD_DOWN -> SpecialKey.DOWN
            key is Key.Repeatable && key.keyCode == AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> SpecialKey.RIGHT
            key is Key.Repeatable && key.keyCode == AndroidKeyEvent.KEYCODE_DPAD_LEFT -> SpecialKey.LEFT
            else -> null
        } ?: return false
        return mozcController.handleSpecialKey(specialKey)
    }

    fun handleKeyPress(key: Key) {
        when (key) {
            is Key.Char -> {
                if (symbolMode) {
                    // Symbol mode: send symbolKeyCode or commit symbol char
                    if (key.symbolKeyCode != null) {
                        sendKeyEvent(key.symbolKeyCode, buildMetaState())
                    } else if (key.symbol != null) {
                        currentInputConnection()?.commitText(key.symbol.toString(), 1)
                    } else {
                        // No symbol defined, fall through to normal
                        val ch = if (isShiftActive) key.shifted else key.normal
                        currentInputConnection()?.commitText(ch.toString(), 1)
                    }
                    clearTransientModifiers()
                    return
                }

                val ch = if (isShiftActive) key.shifted else key.normal
                val ic = currentInputConnection()
                when {
                    isCtrlActive || isAltActive -> {
                        val androidKeyCode = when {
                            ch in 'a'..'z' -> AndroidKeyEvent.KEYCODE_A + (ch - 'a')
                            ch in 'A'..'Z' -> AndroidKeyEvent.KEYCODE_A + (ch - 'A')
                            ch == ' ' -> AndroidKeyEvent.KEYCODE_SPACE
                            else -> -1
                        }
                        if (androidKeyCode >= 0) {
                            sendKeyEvent(androidKeyCode, buildMetaState())
                        }
                    }
                    isJapaneseMode -> {
                        if (ch == ' ' && mozcController.isComposing) {
                            mozcController.handleSpecialKey(SpecialKey.SPACE)
                        } else if (ch == ' ') {
                            ic?.commitText(" ", 1)
                        } else {
                            mozcController.handleCodePoint(ch.code)
                        }
                    }
                    else -> ic?.commitText(ch.toString(), 1)
                }
                clearTransientModifiers()
            }

            is Key.Action -> {
                if (isJapaneseMode && routeToMozc(key)) {
                    // consumed by Mozc
                } else {
                    val meta = buildMetaState()
                    if (key.keyCode == AndroidKeyEvent.KEYCODE_TAB && isShiftActive) {
                        sendKeyEvent(AndroidKeyEvent.KEYCODE_TAB,
                            AndroidKeyEvent.META_SHIFT_ON or AndroidKeyEvent.META_SHIFT_LEFT_ON)
                    } else {
                        sendKeyEvent(key.keyCode, meta)
                    }
                }
                clearTransientModifiers()
            }

            is Key.Repeatable -> {
                if (isJapaneseMode && routeToMozc(key)) {
                    // consumed by Mozc
                } else {
                    sendKeyEvent(key.keyCode, buildMetaState())
                }
                clearTransientModifiers()
            }

            is Key.Modifier -> {
                if (symbolMode && key.type == ModifierType.SHIFT) return
                toggleModifier(key.type)
            }

            is Key.JpToggle -> {
                if (symbolMode) return
                isJapaneseMode = !isJapaneseMode
                if (!isJapaneseMode) mozcController.reset()
            }

            is Key.SymbolSwitch -> {
                symbolMode = !symbolMode
                if (symbolMode) {
                    shiftState = ModifierLevel.OFF
                }
            }

            is Key.VoiceInput -> {
                if (isListening) {
                    stopVoiceInput()
                } else {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasPermission) {
                        startListening()
                    } else {
                        val intent = Intent(context, PermissionActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                }
            }

            is Key.Clipboard -> {
                showClipboard = !showClipboard
            }
        }
    }

    // Connect Mozc commit to InputConnection
    mozcController.onCommit = { text ->
        val ic = currentInputConnection()
        ic?.commitText(text, 1)
        ic?.finishComposingText()
    }

    // Update composing text in InputConnection
    val composingText = mozcController.composingText
    val prevComposingText = remember { mutableStateOf("") }
    if (composingText != prevComposingText.value) {
        prevComposingText.value = composingText
        val ic = currentInputConnection()
        if (composingText.isNotEmpty()) {
            ic?.setComposingText(composingText, 1)
        } else {
            ic?.finishComposingText()
        }
    }

    val isSplitLayout = screenWidthDp > 600
    val isFloating = true // Keys always use transparent/border style

    val keyboardContent: @Composable () -> Unit = {
        Column {
            // Mozc candidate bar (priority over clipboard bar)
            if (mozcController.isComposing) {
                MozcCandidateBar(
                    composingText = mozcController.composingText,
                    candidates = mozcController.candidates,
                    onCandidateSelected = { mozcController.selectCandidate(it) },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else if (showClipboard && clipboardItems.isNotEmpty()) {
                ClipboardBar(
                    items = clipboardItems,
                    onItemSelected = { text ->
                        currentInputConnection()?.commitText(text, 1)
                        showClipboard = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Keyboard grid
            KeyboardGrid(
                gridKeys = gridKeys,
                cols = GRID_COLS,
                rows = GRID_ROWS,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dims.totalHeight)
                    .padding(horizontal = 2.dp, vertical = 2.dp),
            ) { gridKey ->
                RenderGridKey(
                    gridKey = gridKey,
                    dims = dims,
                    isShiftActive = isShiftActive,
                    isJapaneseMode = isJapaneseMode,
                    symbolMode = symbolMode,
                    ctrlState = ctrlState,
                    altState = altState,
                    shiftState = shiftState,
                    isListening = isListening,
                    isSpeaking = isSpeaking,
                    isFloating = isFloating,
                    onKeyPress = { handleKeyPress(it) },
                    onToggleModifier = { toggleModifier(it) },
                )
            }
        }
    }

    if (isSplitLayout) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.97f)
                    .onGloballyPositioned { coords ->
                        onKeyboardBoundsChanged?.invoke(coords.boundsInWindow())
                    },
                shape = RoundedCornerShape(12.dp),
                color = FloatingKeyboardBackground,
                shadowElevation = 8.dp,
            ) {
                keyboardContent()
            }
        }
    } else {
        Surface(
            modifier = modifier.windowInsetsPadding(WindowInsets.navigationBars),
            color = KeyboardBackground,
        ) {
            keyboardContent()
        }
    }
}

@Composable
private fun RenderGridKey(
    gridKey: GridKey,
    dims: QwertyDimensions,
    isShiftActive: Boolean,
    isJapaneseMode: Boolean,
    symbolMode: Boolean,
    ctrlState: ModifierLevel,
    altState: ModifierLevel,
    shiftState: ModifierLevel,
    isListening: Boolean,
    isSpeaking: Boolean,
    isFloating: Boolean,
    onKeyPress: (Key) -> Unit,
    onToggleModifier: (ModifierType) -> Unit,
) {
    val key = gridKey.key
    val keyModifier = Modifier.fillMaxSize()
    when (key) {
        is Key.Char -> {
            val isSpace = key.normal == ' '
            val displayLabel = when {
                isSpace -> ""
                symbolMode && key.symbolLabel != null -> key.symbolLabel
                symbolMode && key.symbol != null -> key.symbol.toString()
                isShiftActive -> key.shifted.toString()
                else -> key.normal.toString()
            }
            QwertyKeyButton(
                label = displayLabel,
                onClick = { onKeyPress(key) },
                modifier = keyModifier,
                fontSize = dims.fontSize,
                cornerRadius = dims.cornerRadius,
                backgroundColor = if (isSpace) SpaceBarBackground else CharKeyBackground,
                showPreview = !isSpace,
                repeatable = true,
                isFloating = isFloating,
            )
        }

        is Key.Action -> QwertyKeyButton(
            label = key.label,
            onClick = { onKeyPress(key) },
            modifier = keyModifier,
            fontSize = dims.fontSize,
            cornerRadius = dims.cornerRadius,
            backgroundColor = ActionKeyBackground,
            showPreview = false,
            isFloating = isFloating,
        )

        is Key.Repeatable -> QwertyRepeatableButton(
            label = key.label,
            icon = key.icon,
            onPress = { onKeyPress(key) },
            modifier = keyModifier,
            fontSize = dims.fontSize,
            cornerRadius = dims.cornerRadius,
            backgroundColor = RepeatKeyBackground,
            showPreview = true,
            isFloating = isFloating,
        )

        is Key.Modifier -> {
            if (symbolMode && key.type == ModifierType.SHIFT) {
                // Disabled appearance in symbol mode
                QwertyKeyButton(
                    label = key.type.label,
                    onClick = {},
                    modifier = keyModifier,
                    fontSize = dims.fontSize,
                    cornerRadius = dims.cornerRadius,
                    backgroundColor = ModifierOffBackground,
                    showPreview = false,
                    isFloating = isFloating,
                )
            } else {
                val level = when (key.type) {
                    ModifierType.CTRL -> ctrlState
                    ModifierType.ALT -> altState
                    ModifierType.SHIFT -> shiftState
                }
                QwertyModifierButton(
                    label = key.type.label,
                    level = level,
                    onClick = { onToggleModifier(key.type) },
                    modifier = keyModifier,
                    fontSize = dims.fontSize,
                    cornerRadius = dims.cornerRadius,
                    isFloating = isFloating,
                )
            }
        }

        is Key.JpToggle -> {
            if (symbolMode) {
                // Empty/disabled in symbol mode
                QwertyKeyButton(
                    label = "",
                    onClick = {},
                    modifier = keyModifier,
                    fontSize = dims.fontSize,
                    cornerRadius = dims.cornerRadius,
                    backgroundColor = ActionKeyBackground,
                    showPreview = false,
                    isFloating = isFloating,
                )
            } else {
                QwertyKeyButton(
                    label = if (isJapaneseMode) "JP" else "EN",
                    onClick = { onKeyPress(key) },
                    modifier = keyModifier,
                    fontSize = dims.fontSize,
                    cornerRadius = dims.cornerRadius,
                    backgroundColor = (if (isJapaneseMode) JpModeBackground else EnModeBackground).let {
                        if (isFloating) it.copy(alpha = 0.3f) else it
                    },
                    pressedBackgroundColor = if (isJapaneseMode) JpModePressedBackground else EnModePressedBackground,
                    showPreview = false,
                    isFloating = isFloating,
                )
            }
        }

        is Key.SymbolSwitch -> QwertyKeyButton(
            icon = if (symbolMode) Icons.Default.Abc else Icons.Default.Tag,
            onClick = { onKeyPress(key) },
            modifier = keyModifier,
            fontSize = dims.fontSize,
            cornerRadius = dims.cornerRadius,
            backgroundColor = if (symbolMode) {
                if (isFloating) SymbolSwitchBackground.copy(alpha = 0.3f) else SymbolSwitchBackground
            } else ActionKeyBackground,
            pressedBackgroundColor = if (symbolMode) SymbolSwitchPressedBackground else KeyPressedBackground,
            showPreview = false,
            isFloating = isFloating,
        )

        is Key.VoiceInput -> {
            val voiceBg = if (isListening) {
                if (isFloating) VoiceActiveBackground.copy(alpha = 0.3f) else VoiceActiveBackground
            } else ActionKeyBackground
            Box(modifier = keyModifier) {
                QwertyKeyButton(
                    icon = if (isSpeaking) Icons.Default.GraphicEq else Icons.Default.Mic,
                    onClick = { onKeyPress(key) },
                    modifier = Modifier.fillMaxSize(),
                    fontSize = dims.fontSize,
                    cornerRadius = dims.cornerRadius,
                    backgroundColor = voiceBg,
                    showPreview = false,
                    isFloating = isFloating,
                )
                if (isListening && isSpeaking) {
                    VoicePulseRings(modifier = Modifier.fillMaxSize())
                }
            }
        }

        is Key.Clipboard -> QwertyKeyButton(
            icon = Icons.Default.ContentPaste,
            onClick = { onKeyPress(key) },
            modifier = keyModifier,
            fontSize = dims.fontSize,
            cornerRadius = dims.cornerRadius,
            backgroundColor = ActionKeyBackground,
            showPreview = false,
            isFloating = isFloating,
        )
    }
}
