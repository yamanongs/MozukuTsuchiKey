package com.xaaav.mozukutsuchikey.flick

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xaaav.mozukutsuchikey.keyboard.ActionKeyBackground
import com.xaaav.mozukutsuchikey.keyboard.CharKeyBackground
import com.xaaav.mozukutsuchikey.keyboard.EnModeBackground
import com.xaaav.mozukutsuchikey.keyboard.EnterKeyBackground
import com.xaaav.mozukutsuchikey.keyboard.JpModeBackground
import com.xaaav.mozukutsuchikey.keyboard.KeyPressedBackground
import com.xaaav.mozukutsuchikey.keyboard.KeyTextColor
import com.xaaav.mozukutsuchikey.keyboard.ModifierLockedBackground
import com.xaaav.mozukutsuchikey.keyboard.VoiceActiveBackground
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Compact QWERTY keyboard for narrow screens (≤600dp).
 * Used for English and Symbol input modes (Japanese uses FlickKeyboard).
 */
@Composable
fun MiniQwertyKeyboard(
    mode: FlickInputMode,
    onEvent: (FlickEvent) -> Unit,
    modifier: Modifier = Modifier,
    isListening: Boolean = false,
    isSpeaking: Boolean = false,
) {
    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp.dp
    val keyboardHeight = (screenHeightDp * 0.38f).coerceIn(200.dp, 300.dp)

    // Internal state for shift and symbol pages
    var isShifted by remember { mutableStateOf(false) }
    var symbolPage by remember { mutableIntStateOf(0) } // 0=english, 1=symbol1, 2=symbol2

    // When mode changes externally, reset internal state
    val isSymbolMode = mode == FlickInputMode.NUMBER
    if (!isSymbolMode) {
        symbolPage = 0
    } else if (symbolPage == 0) {
        symbolPage = 1
    }

    val currentPage = if (isSymbolMode) symbolPage else 0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(keyboardHeight)
            .padding(horizontal = 2.dp, vertical = 4.dp),
    ) {
        when (currentPage) {
            0 -> EnglishLayout(
                isShifted = isShifted,
                onChar = { ch -> onEvent(FlickEvent.CharInput(ch)) },
                onShift = { isShifted = !isShifted },
                onDelete = { onEvent(FlickEvent.Delete) },
                onModeToJapanese = { onEvent(FlickEvent.ModeChanged(FlickInputMode.JAPANESE)) },
                onModeToSymbol = { onEvent(FlickEvent.ModeChanged(FlickInputMode.NUMBER)) },
                onSpace = { onEvent(FlickEvent.Space) },
                onEnter = { onEvent(FlickEvent.Enter) },
                onVoice = { onEvent(FlickEvent.VoiceInput) },
                isListening = isListening,
                isSpeaking = isSpeaking,
            )
            1 -> Symbol1Layout(
                onChar = { ch -> onEvent(FlickEvent.CharInput(ch)) },
                onMore = { symbolPage = 2 },
                onDelete = { onEvent(FlickEvent.Delete) },
                onModeToJapanese = { onEvent(FlickEvent.ModeChanged(FlickInputMode.JAPANESE)) },
                onModeToEnglish = { onEvent(FlickEvent.ModeChanged(FlickInputMode.ENGLISH)) },
                onSpace = { onEvent(FlickEvent.Space) },
                onEnter = { onEvent(FlickEvent.Enter) },
                onVoice = { onEvent(FlickEvent.VoiceInput) },
                isListening = isListening,
                isSpeaking = isSpeaking,
            )
            2 -> Symbol2Layout(
                onChar = { ch -> onEvent(FlickEvent.CharInput(ch)) },
                onBack = { symbolPage = 1 },
                onDelete = { onEvent(FlickEvent.Delete) },
                onModeToJapanese = { onEvent(FlickEvent.ModeChanged(FlickInputMode.JAPANESE)) },
                onModeToEnglish = { onEvent(FlickEvent.ModeChanged(FlickInputMode.ENGLISH)) },
                onSpace = { onEvent(FlickEvent.Space) },
                onEnter = { onEvent(FlickEvent.Enter) },
                onVoice = { onEvent(FlickEvent.VoiceInput) },
                isListening = isListening,
                isSpeaking = isSpeaking,
            )
        }
    }
}

// ==================== English Layout ====================

@Composable
private fun ColumnScope.EnglishLayout(
    isShifted: Boolean,
    onChar: (Char) -> Unit,
    onShift: () -> Unit,
    onDelete: () -> Unit,
    onModeToJapanese: () -> Unit,
    onModeToSymbol: () -> Unit,
    onSpace: () -> Unit,
    onEnter: () -> Unit,
    onVoice: () -> Unit,
    isListening: Boolean,
    isSpeaking: Boolean,
) {
    val row1 = "qwertyuiop"
    val row2 = "asdfghjkl"
    val row3 = "zxcvbnm"

    // Row 1: q w e r t y u i o p
    Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
        for (ch in row1) {
            val display = if (isShifted) ch.uppercaseChar() else ch
            MiniCharKey(
                label = display.toString(),
                onClick = { onChar(display) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }

    // Row 2: (half-space) a s d f g h j k l (half-space)
    Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
        Box(modifier = Modifier.weight(0.5f))
        for (ch in row2) {
            val display = if (isShifted) ch.uppercaseChar() else ch
            MiniCharKey(
                label = display.toString(),
                onClick = { onChar(display) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
        Box(modifier = Modifier.weight(0.5f))
    }

    // Row 3: [⇧] z x c v b n m [⌫]
    Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
        MiniActionKey(
            icon = Icons.Default.KeyboardArrowUp,
            onClick = onShift,
            modifier = Modifier.weight(1.5f).fillMaxHeight(),
            backgroundColor = if (isShifted) ModifierLockedBackground else ActionKeyBackground,
        )
        for (ch in row3) {
            val display = if (isShifted) ch.uppercaseChar() else ch
            MiniCharKey(
                label = display.toString(),
                onClick = { onChar(display) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
        MiniActionKey(
            icon = Icons.AutoMirrored.Filled.Backspace,
            onClick = onDelete,
            modifier = Modifier.weight(1.5f).fillMaxHeight(),
            repeatable = true,
        )
    }

    // Row 4: [あ] [?123] [Space] [.] [Voice] [Enter]
    BottomRow(
        leftLabel = "あ",
        leftBg = JpModeBackground.copy(alpha = 0.5f),
        onLeft = onModeToJapanese,
        secondLabel = "?123",
        onSecond = onModeToSymbol,
        onSpace = onSpace,
        onPeriod = { onChar('.') },
        onVoice = onVoice,
        onEnter = onEnter,
        isListening = isListening,
        isSpeaking = isSpeaking,
    )
}

// ==================== Symbol Page 1 ====================

@Composable
private fun ColumnScope.Symbol1Layout(
    onChar: (Char) -> Unit,
    onMore: () -> Unit,
    onDelete: () -> Unit,
    onModeToJapanese: () -> Unit,
    onModeToEnglish: () -> Unit,
    onSpace: () -> Unit,
    onEnter: () -> Unit,
    onVoice: () -> Unit,
    isListening: Boolean,
    isSpeaking: Boolean,
) {
    val row1 = "1234567890"
    val row2 = "@#\$%&-+()"
    val row3 = "*\"':;!?"

    // Row 1
    Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
        for (ch in row1) {
            MiniCharKey(
                label = ch.toString(),
                onClick = { onChar(ch) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }

    // Row 2
    Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
        Box(modifier = Modifier.weight(0.5f))
        for (ch in row2) {
            MiniCharKey(
                label = ch.toString(),
                onClick = { onChar(ch) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
        Box(modifier = Modifier.weight(0.5f))
    }

    // Row 3: [more] * " ' : ; ! ? [⌫]
    Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
        MiniActionKey(
            label = "#+=",
            onClick = onMore,
            modifier = Modifier.weight(1.5f).fillMaxHeight(),
        )
        for (ch in row3) {
            MiniCharKey(
                label = ch.toString(),
                onClick = { onChar(ch) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
        MiniActionKey(
            icon = Icons.AutoMirrored.Filled.Backspace,
            onClick = onDelete,
            modifier = Modifier.weight(1.5f).fillMaxHeight(),
            repeatable = true,
        )
    }

    // Row 4
    BottomRow(
        leftLabel = "あ",
        leftBg = JpModeBackground.copy(alpha = 0.5f),
        onLeft = onModeToJapanese,
        secondLabel = "ABC",
        onSecond = onModeToEnglish,
        onSpace = onSpace,
        onPeriod = { onChar('.') },
        onVoice = onVoice,
        onEnter = onEnter,
        isListening = isListening,
        isSpeaking = isSpeaking,
    )
}

// ==================== Symbol Page 2 ====================

@Composable
private fun ColumnScope.Symbol2Layout(
    onChar: (Char) -> Unit,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onModeToJapanese: () -> Unit,
    onModeToEnglish: () -> Unit,
    onSpace: () -> Unit,
    onEnter: () -> Unit,
    onVoice: () -> Unit,
    isListening: Boolean,
    isSpeaking: Boolean,
) {
    val row1 = "~`|·√π÷×¶Δ"
    val row2 = "£¥€¢^°={}"
    val row3 = "\\©®™✓[]"

    // Row 1
    Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
        for (ch in row1) {
            MiniCharKey(
                label = ch.toString(),
                onClick = { onChar(ch) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }

    // Row 2
    Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
        Box(modifier = Modifier.weight(0.5f))
        for (ch in row2) {
            MiniCharKey(
                label = ch.toString(),
                onClick = { onChar(ch) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
        Box(modifier = Modifier.weight(0.5f))
    }

    // Row 3: [123] \ © ® ™ ✓ [ ] [⌫]
    Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
        MiniActionKey(
            label = "123",
            onClick = onBack,
            modifier = Modifier.weight(1.5f).fillMaxHeight(),
        )
        for (ch in row3) {
            MiniCharKey(
                label = ch.toString(),
                onClick = { onChar(ch) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
        MiniActionKey(
            icon = Icons.AutoMirrored.Filled.Backspace,
            onClick = onDelete,
            modifier = Modifier.weight(1.5f).fillMaxHeight(),
            repeatable = true,
        )
    }

    // Row 4
    BottomRow(
        leftLabel = "あ",
        leftBg = JpModeBackground.copy(alpha = 0.5f),
        onLeft = onModeToJapanese,
        secondLabel = "ABC",
        onSecond = onModeToEnglish,
        onSpace = onSpace,
        onPeriod = { onChar('.') },
        onVoice = onVoice,
        onEnter = onEnter,
        isListening = isListening,
        isSpeaking = isSpeaking,
    )
}

// ==================== Bottom Row (shared) ====================

@Composable
private fun ColumnScope.BottomRow(
    leftLabel: String,
    leftBg: Color,
    onLeft: () -> Unit,
    secondLabel: String,
    onSecond: () -> Unit,
    onSpace: () -> Unit,
    onPeriod: () -> Unit,
    onVoice: () -> Unit,
    onEnter: () -> Unit,
    isListening: Boolean,
    isSpeaking: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth().weight(1f)) {
        // [あ]
        MiniActionKey(
            label = leftLabel,
            onClick = onLeft,
            modifier = Modifier.weight(1.2f).fillMaxHeight(),
            backgroundColor = leftBg,
        )
        // [?123] or [ABC]
        MiniActionKey(
            label = secondLabel,
            onClick = onSecond,
            modifier = Modifier.weight(1.2f).fillMaxHeight(),
            backgroundColor = EnModeBackground.copy(alpha = 0.4f),
        )
        // [Space]
        MiniActionKey(
            icon = Icons.Filled.SpaceBar,
            onClick = onSpace,
            modifier = Modifier.weight(3.6f).fillMaxHeight(),
            backgroundColor = CharKeyBackground,
        )
        // [.]
        MiniCharKey(
            label = ".",
            onClick = onPeriod,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
        // [Voice]
        MiniActionKey(
            icon = if (isSpeaking) Icons.Default.GraphicEq else Icons.Default.Mic,
            onClick = onVoice,
            modifier = Modifier.weight(1.2f).fillMaxHeight(),
            backgroundColor = if (isListening) VoiceActiveBackground else ActionKeyBackground,
        )
        // [Enter]
        MiniActionKey(
            icon = Icons.AutoMirrored.Filled.KeyboardReturn,
            onClick = onEnter,
            modifier = Modifier.weight(1.2f).fillMaxHeight(),
            backgroundColor = EnterKeyBackground,
        )
    }
}

// ==================== Key Composables ====================

private val miniCharKeyShape = RoundedCornerShape(8.dp)
private val miniActionKeyShape = RoundedCornerShape(6.dp)

@Composable
private fun MiniCharKey(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnClick by rememberUpdatedState(onClick)
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .padding(1.5.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false).consume()
                    isPressed = true
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.all { it.changedToUp() }) {
                            event.changes.forEach { it.consume() }
                            break
                        }
                        event.changes.forEach { it.consume() }
                    }
                    currentOnClick()
                    isPressed = false
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = miniCharKeyShape,
            color = if (isPressed) KeyPressedBackground else CharKeyBackground,
            shadowElevation = if (isPressed) 0.dp else 2.dp,
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    text = label,
                    color = KeyTextColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun MiniActionKey(
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    label: String? = null,
    onClick: () -> Unit,
    backgroundColor: Color = ActionKeyBackground,
    repeatable: Boolean = false,
) {
    val currentOnClick by rememberUpdatedState(onClick)
    val coroutineScope = rememberCoroutineScope()
    var isPressed by remember { mutableStateOf(false) }
    var repeatJob by remember { mutableStateOf<Job?>(null) }
    var didRepeat by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { repeatJob?.cancel() }
    }

    Box(
        modifier = modifier
            .padding(1.5.dp)
            .pointerInput(repeatable) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false).consume()
                    isPressed = true
                    didRepeat = false

                    if (repeatable) {
                        repeatJob = coroutineScope.launch {
                            delay(400)
                            didRepeat = true
                            while (true) {
                                currentOnClick()
                                delay(50)
                            }
                        }
                    }

                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.all { it.changedToUp() }) {
                            event.changes.forEach { it.consume() }
                            break
                        }
                        event.changes.forEach { it.consume() }
                    }

                    if (repeatable) {
                        repeatJob?.cancel()
                        if (!didRepeat) currentOnClick()
                    } else {
                        currentOnClick()
                    }
                    isPressed = false
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = miniActionKeyShape,
            color = if (isPressed) KeyPressedBackground else backgroundColor.copy(alpha = 0.3f),
            shadowElevation = if (isPressed) 0.dp else 1.dp,
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = KeyTextColor,
                        modifier = Modifier.size(22.dp),
                    )
                } else if (label != null) {
                    Text(
                        text = label,
                        color = KeyTextColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
