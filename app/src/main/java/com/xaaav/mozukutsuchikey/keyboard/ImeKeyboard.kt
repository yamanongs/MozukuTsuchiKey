package com.xaaav.mozukutsuchikey.keyboard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.KeyCharacterMap
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
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.VerticalAlignBottom
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
import com.xaaav.mozukutsuchikey.flick.FlickDirection
import com.xaaav.mozukutsuchikey.flick.FlickEvent
import com.xaaav.mozukutsuchikey.flick.FlickInputMode
import com.xaaav.mozukutsuchikey.flick.FlickKeyboard
import com.xaaav.mozukutsuchikey.flick.getDakuten
import com.xaaav.mozukutsuchikey.flick.getDakutenSmall
import com.xaaav.mozukutsuchikey.flick.getHandakuten
import com.xaaav.mozukutsuchikey.flick.getNextToggleChar
import com.xaaav.mozukutsuchikey.flick.getSmallChar
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
    onFloatingStateChanged: ((Boolean) -> Unit)? = null,
) {
    val dims = getQwertyDimensions()
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val gridKeys = if (screenWidthDp > 600) GRID_KEYS_SPLIT else GRID_KEYS
    val context = LocalContext.current

    val useFlickKeyboard = screenWidthDp <= 600
    var isJapaneseMode by remember { mutableStateOf(useFlickKeyboard) }
    var symbolMode by remember { mutableStateOf(false) }
    var flickMode by remember { mutableStateOf(FlickInputMode.JAPANESE) }

    // Sync state when switching between flick and QWERTY (e.g. fold/unfold)
    LaunchedEffect(useFlickKeyboard) {
        if (useFlickKeyboard) {
            isJapaneseMode = flickMode == FlickInputMode.JAPANESE
        }
        symbolMode = false
        if (!isJapaneseMode) mozcController.reset()
    }

    var isFloating by remember { mutableStateOf(true) }
    LaunchedEffect(isFloating) {
        onFloatingStateChanged?.invoke(isFloating)
    }
    var showClipboard by remember { mutableStateOf(false) }
    val clipboardItems by clipboardHistory.items.collectAsStateWithLifecycle()
    var isListening by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    val voiceScope = rememberCoroutineScope()
    var idleTimeoutJob by remember { mutableStateOf<Job?>(null) }

    // Toggle cycle state for flick keyboard tap-repeat
    var toggleTapChar by remember { mutableStateOf<Char?>(null) }
    var toggleCurrentChar by remember { mutableStateOf<Char?>(null) }
    var toggleConfirmJob by remember { mutableStateOf<Job?>(null) }
    val toggleScope = rememberCoroutineScope()

    fun confirmToggle() {
        toggleConfirmJob?.cancel()
        toggleConfirmJob = null
        toggleTapChar = null
        toggleCurrentChar = null
    }

    var speechRecognizer by remember {
        mutableStateOf(SpeechRecognizer.createSpeechRecognizer(context))
    }
    DisposableEffect(Unit) {
        onDispose {
            toggleConfirmJob?.cancel()
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

    // inputActive が false になったら（アプリ切替時など）マイクを停止 + トグル確定
    LaunchedEffect(Unit) {
        inputActive.collect { active ->
            if (!active) {
                confirmToggle()
                if (isListening) stopVoiceInput()
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
        val downTime = android.os.SystemClock.uptimeMillis()
        val flags = AndroidKeyEvent.FLAG_SOFT_KEYBOARD or AndroidKeyEvent.FLAG_KEEP_TOUCH_MODE
        ic.sendKeyEvent(AndroidKeyEvent(downTime, downTime, AndroidKeyEvent.ACTION_DOWN, keyCode, 0, metaState, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, flags))
        ic.sendKeyEvent(AndroidKeyEvent(downTime, android.os.SystemClock.uptimeMillis(), AndroidKeyEvent.ACTION_UP, keyCode, 0, metaState, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, flags))
    }

    fun buildMetaState(): Int {
        var meta = 0
        if (isCtrlActive) meta = meta or AndroidKeyEvent.META_CTRL_ON or AndroidKeyEvent.META_CTRL_LEFT_ON
        if (isAltActive) meta = meta or AndroidKeyEvent.META_ALT_ON or AndroidKeyEvent.META_ALT_LEFT_ON
        if (isShiftActive) meta = meta or AndroidKeyEvent.META_SHIFT_ON or AndroidKeyEvent.META_SHIFT_LEFT_ON
        return meta
    }

    fun moveCursor(offset: Int) {
        val ic = currentInputConnection() ?: return
        ic.beginBatchEdit()
        val extracted = ic.getExtractedText(android.view.inputmethod.ExtractedTextRequest(), 0)
        if (extracted != null) {
            val newPos = (extracted.selectionStart + offset).coerceIn(0, extracted.text.length)
            ic.setSelection(newPos, newPos)
        }
        ic.endBatchEdit()
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
                } else if (key.keyCode == AndroidKeyEvent.KEYCODE_DEL && !isCtrlActive && !isAltActive) {
                    val ic = currentInputConnection()
                    val sel = ic?.getSelectedText(0)
                    if (!sel.isNullOrEmpty()) {
                        ic.commitText("", 1)
                    } else {
                        ic?.deleteSurroundingText(1, 0)
                    }
                } else if (key.keyCode == AndroidKeyEvent.KEYCODE_FORWARD_DEL && !isCtrlActive && !isAltActive) {
                    val ic = currentInputConnection()
                    val sel = ic?.getSelectedText(0)
                    if (!sel.isNullOrEmpty()) {
                        ic.commitText("", 1)
                    } else {
                        ic?.deleteSurroundingText(0, 1)
                    }
                } else if (key.keyCode == AndroidKeyEvent.KEYCODE_DPAD_LEFT && !isCtrlActive && !isAltActive) {
                    moveCursor(-1)
                } else if (key.keyCode == AndroidKeyEvent.KEYCODE_DPAD_RIGHT && !isCtrlActive && !isAltActive) {
                    moveCursor(1)
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
                    if (mozcController.isComposing) mozcController.reset()
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

            is Key.DockToggle -> {
                isFloating = !isFloating
            }
        }
    }

    // Connect Mozc commit to InputConnection
    mozcController.onCommit = { text ->
        currentInputConnection()?.commitText(text, 1)
    }

    // Update composing text in InputConnection
    val composingText = mozcController.composingText
    val prevComposingText = remember { mutableStateOf("") }
    if (composingText != prevComposingText.value) {
        val wasComposing = prevComposingText.value.isNotEmpty()
        prevComposingText.value = composingText
        val ic = currentInputConnection()
        if (composingText.isNotEmpty()) {
            ic?.setComposingText(composingText, 1)
        } else if (wasComposing) {
            ic?.finishComposingText()
        }
    }

    val isSplitLayout = screenWidthDp > 600

    // ==================== Flick keyboard event handler ====================

    fun handleFlickEvent(event: FlickEvent) {
        when (event) {
            is FlickEvent.CharInput -> {
                val ch = event.char
                val tapChar = event.tapChar

                if (tapChar != null && toggleTapChar == tapChar && toggleCurrentChar != null) {
                    // Same key re-tapped: cycle to next char
                    val nextChar = toggleCurrentChar!!.getNextToggleChar(tapChar)
                    if (nextChar != null) {
                        // Delete previous char and send next
                        if (flickMode == FlickInputMode.JAPANESE) {
                            mozcController.handleSpecialKey(SpecialKey.BACKSPACE)
                            mozcController.handleCodePoint(nextChar.code)
                        } else {
                            val ic = currentInputConnection()
                            ic?.deleteSurroundingText(1, 0)
                            ic?.commitText(nextChar.toString(), 1)
                        }
                        toggleCurrentChar = nextChar
                        // Restart auto-confirm timer
                        toggleConfirmJob?.cancel()
                        toggleConfirmJob = toggleScope.launch {
                            delay(1000)
                            confirmToggle()
                        }
                        return
                    }
                    // No next char in cycle — fall through to confirm + new char
                }

                // Confirm any pending toggle before sending new char
                confirmToggle()

                if (flickMode == FlickInputMode.JAPANESE) {
                    mozcController.ensureInitialized()
                    mozcController.handleCodePoint(ch.code)
                } else {
                    currentInputConnection()?.commitText(ch.toString(), 1)
                }

                // Start toggle tracking if this was a tap and cycle exists
                if (tapChar != null && ch.getNextToggleChar(tapChar) != null) {
                    toggleTapChar = tapChar
                    toggleCurrentChar = ch
                    toggleConfirmJob = toggleScope.launch {
                        delay(1000)
                        confirmToggle()
                    }
                }
            }
            is FlickEvent.DakutenInput -> {
                confirmToggle()
                if (flickMode == FlickInputMode.JAPANESE && mozcController.isComposing) {
                    // Apply dakuten/handakuten/small to last char of composing text
                    val text = mozcController.composingText
                    if (text.isNotEmpty()) {
                        val lastChar = text.last()
                        val converted = when (event.direction) {
                            FlickDirection.TAP -> lastChar.getDakutenSmall()
                            FlickDirection.LEFT -> lastChar.getDakuten()
                            FlickDirection.RIGHT -> lastChar.getHandakuten()
                            FlickDirection.TOP -> lastChar.getSmallChar()
                            FlickDirection.BOTTOM -> null
                        }
                        if (converted != null) {
                            // Delete last char and re-send the converted one
                            mozcController.handleSpecialKey(SpecialKey.BACKSPACE)
                            mozcController.handleCodePoint(converted.code)
                        }
                    }
                } else if (flickMode == FlickInputMode.ENGLISH) {
                    // Toggle case of last committed char — handled via composing
                    val ic = currentInputConnection() ?: return
                    val before = ic.getTextBeforeCursor(1, 0)
                    if (!before.isNullOrEmpty()) {
                        val ch = before[0]
                        val toggled = ch.getDakutenSmall()
                        if (toggled != null) {
                            ic.deleteSurroundingText(1, 0)
                            ic.commitText(toggled.toString(), 1)
                        }
                    }
                } else if (flickMode == FlickInputMode.NUMBER) {
                    // Number mode dakuten key: ()[]
                    val ch = when (event.direction) {
                        FlickDirection.TAP -> '('
                        FlickDirection.LEFT -> ')'
                        FlickDirection.TOP -> '['
                        FlickDirection.RIGHT -> ']'
                        FlickDirection.BOTTOM -> null
                    }
                    if (ch != null) {
                        currentInputConnection()?.commitText(ch.toString(), 1)
                    }
                }
            }
            is FlickEvent.Delete -> {
                confirmToggle()
                if (mozcController.isComposing) {
                    mozcController.handleSpecialKey(SpecialKey.BACKSPACE)
                } else {
                    val ic = currentInputConnection()
                    val sel = ic?.getSelectedText(0)
                    if (!sel.isNullOrEmpty()) {
                        ic.commitText("", 1)
                    } else {
                        ic?.deleteSurroundingText(1, 0)
                    }
                }
            }
            is FlickEvent.Enter -> {
                confirmToggle()
                if (mozcController.isComposing) {
                    mozcController.handleSpecialKey(SpecialKey.ENTER)
                } else {
                    sendKeyEvent(AndroidKeyEvent.KEYCODE_ENTER)
                }
            }
            is FlickEvent.Space -> {
                confirmToggle()
                if (mozcController.isComposing) {
                    mozcController.handleSpecialKey(SpecialKey.SPACE)
                } else {
                    currentInputConnection()?.commitText(" ", 1)
                }
            }
            is FlickEvent.CursorLeft -> {
                confirmToggle()
                if (mozcController.isComposing) {
                    mozcController.handleSpecialKey(SpecialKey.LEFT)
                } else {
                    moveCursor(-1)
                }
            }
            is FlickEvent.CursorRight -> {
                confirmToggle()
                if (mozcController.isComposing) {
                    mozcController.handleSpecialKey(SpecialKey.RIGHT)
                } else {
                    moveCursor(1)
                }
            }
            is FlickEvent.ModeChanged -> {
                confirmToggle()
                flickMode = event.mode
                isJapaneseMode = event.mode == FlickInputMode.JAPANESE
                if (!isJapaneseMode) mozcController.reset()
            }
            is FlickEvent.VoiceInput -> {
                confirmToggle()
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
        }
    }

    // ==================== Candidate bar content ====================

    val candidateBarContent: @Composable () -> Unit = {
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
    }

    // ==================== QWERTY keyboard content ====================

    val qwertyKeyboardContent: @Composable () -> Unit = {
        Column {
            candidateBarContent()

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

    // ==================== Layout selection ====================

    if (useFlickKeyboard) {
        // Flick keyboard for narrow screens (≤600dp) — opaque, pushes app content up
        Surface(
            modifier = modifier.windowInsetsPadding(WindowInsets.navigationBars),
            color = FlickKeyboardBackground,
        ) {
            Column {
                candidateBarContent()
                FlickKeyboard(
                    mode = flickMode,
                    onEvent = { handleFlickEvent(it) },
                    isListening = isListening,
                    isSpeaking = isSpeaking,
                )
            }
        }
    } else if (isSplitLayout) {
        if (isFloating) {
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
                    qwertyKeyboardContent()
                }
            }
        } else {
            Surface(
                modifier = modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .onGloballyPositioned { coords ->
                        onKeyboardBoundsChanged?.invoke(coords.boundsInWindow())
                    },
                color = FlickKeyboardBackground,
            ) {
                qwertyKeyboardContent()
            }
        }
    } else {
        Surface(
            modifier = modifier.windowInsetsPadding(WindowInsets.navigationBars),
            color = KeyboardBackground,
        ) {
            qwertyKeyboardContent()
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

        is Key.DockToggle -> QwertyKeyButton(
            icon = if (isFloating) Icons.Default.VerticalAlignBottom else Icons.Default.OpenInFull,
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
