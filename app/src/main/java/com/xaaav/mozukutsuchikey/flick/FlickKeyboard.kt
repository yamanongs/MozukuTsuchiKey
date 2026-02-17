package com.xaaav.mozukutsuchikey.flick

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.xaaav.mozukutsuchikey.keyboard.ActionKeyBackground
import com.xaaav.mozukutsuchikey.keyboard.CharKeyBackground
import com.xaaav.mozukutsuchikey.keyboard.EnModeBackground
import com.xaaav.mozukutsuchikey.keyboard.JpModeBackground
import com.xaaav.mozukutsuchikey.keyboard.EnterKeyBackground
import com.xaaav.mozukutsuchikey.keyboard.KeyBorderColor
import com.xaaav.mozukutsuchikey.keyboard.KeyPressedBackground
import com.xaaav.mozukutsuchikey.keyboard.KeyTextColor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Events emitted by FlickKeyboard to the parent */
sealed class FlickEvent {
    data class CharInput(val char: Char, val tapChar: Char? = null) : FlickEvent()
    data class DakutenInput(val direction: FlickDirection) : FlickEvent()
    data object Delete : FlickEvent()
    data object Enter : FlickEvent()
    data object Space : FlickEvent()
    data object CursorLeft : FlickEvent()
    data object CursorRight : FlickEvent()
    data class ModeChanged(val mode: FlickInputMode) : FlickEvent()
}

@Composable
fun FlickKeyboard(
    mode: FlickInputMode,
    onEvent: (FlickEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val flickKeys = flickKeysForMode(mode)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(horizontal = 2.dp, vertical = 2.dp),
    ) {
        // 4 rows × 5 columns
        // Row 1: [←戻] [あ] [か] [さ] [BS]
        // Row 2: [←]  [た] [な] [は] [→]
        // Row 3: [記号] [ま] [や] [ら] [空白]
        // Row 4: [JP/EN] [小゛゜] [わ] [、。] [Enter]
        val rowWeight = 1f

        // Row 1
        Row(modifier = Modifier.fillMaxWidth().weight(rowWeight)) {
            SideKeyButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                onClick = { onEvent(FlickEvent.CursorLeft) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
                repeatable = true,
            )
            FlickCharKey(
                flickChar = flickKeys[0], label = flickKeyLabel(mode, 0),
                onFlick = { dir -> emitCharEvent(flickKeys[0], dir, onEvent) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            FlickCharKey(
                flickChar = flickKeys[1], label = flickKeyLabel(mode, 1),
                onFlick = { dir -> emitCharEvent(flickKeys[1], dir, onEvent) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            FlickCharKey(
                flickChar = flickKeys[2], label = flickKeyLabel(mode, 2),
                onFlick = { dir -> emitCharEvent(flickKeys[2], dir, onEvent) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            SideKeyButton(
                icon = Icons.AutoMirrored.Filled.Backspace,
                onClick = { onEvent(FlickEvent.Delete) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
                repeatable = true,
            )
        }

        // Row 2
        Row(modifier = Modifier.fillMaxWidth().weight(rowWeight)) {
            SideKeyButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                onClick = { onEvent(FlickEvent.CursorRight) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
                repeatable = true,
            )
            FlickCharKey(
                flickChar = flickKeys[3], label = flickKeyLabel(mode, 3),
                onFlick = { dir -> emitCharEvent(flickKeys[3], dir, onEvent) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            FlickCharKey(
                flickChar = flickKeys[4], label = flickKeyLabel(mode, 4),
                onFlick = { dir -> emitCharEvent(flickKeys[4], dir, onEvent) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            FlickCharKey(
                flickChar = flickKeys[5], label = flickKeyLabel(mode, 5),
                onFlick = { dir -> emitCharEvent(flickKeys[5], dir, onEvent) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            SideKeyButton(
                icon = Icons.Filled.SpaceBar,
                onClick = { onEvent(FlickEvent.Space) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }

        // Row 3
        Row(modifier = Modifier.fillMaxWidth().weight(rowWeight)) {
            // Mode switch
            SideKeyButton(
                label = mode.next().label,
                onClick = { onEvent(FlickEvent.ModeChanged(mode.next())) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
                backgroundColor = when (mode) {
                    FlickInputMode.JAPANESE -> JpModeBackground.copy(alpha = 0.5f)
                    FlickInputMode.ENGLISH -> EnModeBackground.copy(alpha = 0.5f)
                    FlickInputMode.NUMBER -> ActionKeyBackground
                },
            )
            FlickCharKey(
                flickChar = flickKeys[6], label = flickKeyLabel(mode, 6),
                onFlick = { dir -> emitCharEvent(flickKeys[6], dir, onEvent) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            FlickCharKey(
                flickChar = flickKeys[7], label = flickKeyLabel(mode, 7),
                onFlick = { dir -> emitCharEvent(flickKeys[7], dir, onEvent) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            FlickCharKey(
                flickChar = flickKeys[8], label = flickKeyLabel(mode, 8),
                onFlick = { dir -> emitCharEvent(flickKeys[8], dir, onEvent) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            SideKeyButton(
                icon = Icons.AutoMirrored.Filled.KeyboardReturn,
                onClick = { onEvent(FlickEvent.Enter) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
                backgroundColor = EnterKeyBackground,
            )
        }

        // Row 4
        Row(modifier = Modifier.fillMaxWidth().weight(rowWeight)) {
            // Empty spacer to align the grid
            SideKeyButton(
                label = mode.label,
                onClick = { /* no-op, display only */ },
                modifier = Modifier.weight(1f).fillMaxHeight(),
                backgroundColor = when (mode) {
                    FlickInputMode.JAPANESE -> JpModeBackground.copy(alpha = 0.3f)
                    FlickInputMode.ENGLISH -> EnModeBackground.copy(alpha = 0.3f)
                    FlickInputMode.NUMBER -> ActionKeyBackground
                },
            )
            // Dakuten key
            FlickDakutenKey(
                label = flickKeyLabel(mode, 9),
                onFlick = { dir -> onEvent(FlickEvent.DakutenInput(dir)) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
                mode = mode,
            )
            FlickCharKey(
                flickChar = flickKeys[10], label = flickKeyLabel(mode, 10),
                onFlick = { dir -> emitCharEvent(flickKeys[10], dir, onEvent) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            FlickCharKey(
                flickChar = flickKeys[11], label = flickKeyLabel(mode, 11),
                onFlick = { dir -> emitCharEvent(flickKeys[11], dir, onEvent) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            // Second spacer / empty
            SideKeyButton(
                label = "",
                onClick = {},
                modifier = Modifier.weight(1f).fillMaxHeight(),
                backgroundColor = Color.Transparent,
            )
        }
    }
}

private fun emitCharEvent(flickChar: FlickChar, direction: FlickDirection, onEvent: (FlickEvent) -> Unit) {
    val ch = flickChar.charForDirection(direction) ?: return
    val tapChar = if (direction == FlickDirection.TAP) flickChar.tap else null
    onEvent(FlickEvent.CharInput(ch, tapChar))
}

// ==================== Individual Key Composables ====================

private val keyShape = RoundedCornerShape(6.dp)
private const val FLICK_THRESHOLD_DP = 30

@Composable
private fun FlickCharKey(
    flickChar: FlickChar,
    label: String,
    onFlick: (FlickDirection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val thresholdPx = with(density) { FLICK_THRESHOLD_DP.dp.toPx() }
    var flickDirection by remember { mutableStateOf(FlickDirection.TAP) }
    var isPressed by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    val currentOnFlick by rememberUpdatedState(onFlick)

    Box(
        modifier = modifier
            .padding(1.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    isPressed = true
                    dragOffset = Offset.Zero
                    flickDirection = FlickDirection.TAP

                    // Track drag until all pointers are up
                    while (true) {
                        val event = awaitPointerEvent()
                        val changes = event.changes
                        if (changes.all { it.changedToUp() }) {
                            changes.forEach { it.consume() }
                            break
                        }
                        val primary = changes.firstOrNull() ?: continue
                        val delta = primary.positionChange()
                        if (delta != Offset.Zero) {
                            dragOffset += delta
                            flickDirection = computeFlickDirection(dragOffset, thresholdPx)
                            primary.consume()
                        }
                    }

                    isPressed = false
                    currentOnFlick(flickDirection)
                    flickDirection = FlickDirection.TAP
                    dragOffset = Offset.Zero
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        // Key background
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, KeyBorderColor, keyShape),
            shape = keyShape,
            color = if (isPressed) KeyPressedBackground else Color.Transparent,
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    text = label,
                    color = KeyTextColor,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }

        // Flick direction popup
        if (isPressed && flickDirection != FlickDirection.TAP) {
            val previewChar = flickChar.charForDirection(flickDirection)
            if (previewChar != null) {
                FlickPreviewPopup(
                    char = previewChar,
                    direction = flickDirection,
                )
            }
        }
        // Tap popup (show tap char enlarged)
        if (isPressed && flickDirection == FlickDirection.TAP && flickChar.tap != null) {
            FlickPreviewPopup(
                char = flickChar.tap,
                direction = FlickDirection.TAP,
            )
        }
    }
}

@Composable
private fun FlickDakutenKey(
    label: String,
    onFlick: (FlickDirection) -> Unit,
    modifier: Modifier = Modifier,
    mode: FlickInputMode,
) {
    val density = LocalDensity.current
    val thresholdPx = with(density) { FLICK_THRESHOLD_DP.dp.toPx() }
    var flickDirection by remember { mutableStateOf(FlickDirection.TAP) }
    var isPressed by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    val currentOnFlick by rememberUpdatedState(onFlick)

    val dirLabels = when (mode) {
        FlickInputMode.JAPANESE -> mapOf(
            FlickDirection.TAP to "゛゜",
            FlickDirection.LEFT to "゛",
            FlickDirection.TOP to "小",
            FlickDirection.RIGHT to "゜",
        )
        FlickInputMode.ENGLISH -> mapOf(
            FlickDirection.TAP to "A/a",
        )
        FlickInputMode.NUMBER -> mapOf(
            FlickDirection.TAP to "()",
            FlickDirection.LEFT to ")",
            FlickDirection.TOP to "[",
            FlickDirection.RIGHT to "]",
        )
    }

    Box(
        modifier = modifier
            .padding(1.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    isPressed = true
                    dragOffset = Offset.Zero
                    flickDirection = FlickDirection.TAP

                    while (true) {
                        val event = awaitPointerEvent()
                        val changes = event.changes
                        if (changes.all { it.changedToUp() }) {
                            changes.forEach { it.consume() }
                            break
                        }
                        val primary = changes.firstOrNull() ?: continue
                        val delta = primary.positionChange()
                        if (delta != Offset.Zero) {
                            dragOffset += delta
                            flickDirection = computeFlickDirection(dragOffset, thresholdPx)
                            primary.consume()
                        }
                    }

                    isPressed = false
                    currentOnFlick(flickDirection)
                    flickDirection = FlickDirection.TAP
                    dragOffset = Offset.Zero
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, KeyBorderColor, keyShape),
            shape = keyShape,
            color = if (isPressed) KeyPressedBackground else Color.Transparent,
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    text = label,
                    color = KeyTextColor,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }

        if (isPressed && flickDirection != FlickDirection.TAP) {
            val previewText = dirLabels[flickDirection]
            if (previewText != null) {
                FlickPreviewPopup(
                    char = previewText.firstOrNull() ?: ' ',
                    direction = flickDirection,
                    labelOverride = previewText,
                )
            }
        }
    }
}

@Composable
private fun SideKeyButton(
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
            .padding(1.dp)
            .pointerInput(repeatable) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    isPressed = true
                    didRepeat = false

                    if (repeatable) {
                        repeatJob = coroutineScope.launch {
                            delay(400) // long-press threshold
                            didRepeat = true
                            while (true) {
                                currentOnClick()
                                delay(50)
                            }
                        }
                    }

                    // Wait for release
                    while (true) {
                        val event = awaitPointerEvent()
                        val changes = event.changes
                        if (changes.all { it.changedToUp() }) {
                            changes.forEach { it.consume() }
                            break
                        }
                        changes.forEach { it.consume() }
                    }

                    if (repeatable) {
                        repeatJob?.cancel()
                        if (!didRepeat) currentOnClick() // short tap: fire once
                    } else {
                        currentOnClick()
                    }
                    isPressed = false
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, KeyBorderColor, keyShape),
            shape = keyShape,
            color = if (isPressed) KeyPressedBackground else backgroundColor.copy(alpha = 0.15f),
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
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

// ==================== Popup ====================

@Composable
private fun FlickPreviewPopup(
    char: Char,
    direction: FlickDirection,
    labelOverride: String? = null,
) {
    val density = LocalDensity.current
    val offsetX = when (direction) {
        FlickDirection.LEFT -> with(density) { (-48).dp.roundToPx() }
        FlickDirection.RIGHT -> with(density) { 48.dp.roundToPx() }
        else -> 0
    }
    val offsetY = when (direction) {
        FlickDirection.TOP -> with(density) { (-56).dp.roundToPx() }
        FlickDirection.BOTTOM -> with(density) { 56.dp.roundToPx() }
        FlickDirection.TAP -> with(density) { (-56).dp.roundToPx() }
        else -> 0
    }

    Popup(
        alignment = Alignment.Center,
        offset = IntOffset(offsetX, offsetY),
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = CharKeyBackground,
            shadowElevation = 4.dp,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = labelOverride ?: char.toString(),
                    color = Color.White,
                    fontSize = 24.sp,
                )
            }
        }
    }
}

// ==================== Helpers ====================

private fun computeFlickDirection(offset: Offset, threshold: Float): FlickDirection {
    val absX = abs(offset.x)
    val absY = abs(offset.y)
    if (absX < threshold && absY < threshold) return FlickDirection.TAP
    return if (absX > absY) {
        if (offset.x < 0) FlickDirection.LEFT else FlickDirection.RIGHT
    } else {
        if (offset.y < 0) FlickDirection.TOP else FlickDirection.BOTTOM
    }
}
