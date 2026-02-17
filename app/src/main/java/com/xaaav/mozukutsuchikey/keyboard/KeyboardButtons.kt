package com.xaaav.mozukutsuchikey.keyboard

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun QwertyKeyButton(
    label: String? = null,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fontSize: Int,
    cornerRadius: Dp,
    backgroundColor: Color = CharKeyBackground,
    pressedBackgroundColor: Color = KeyPressedBackground,
    showPreview: Boolean = true,
    repeatable: Boolean = false,
    isFloating: Boolean = false,
) {
    if (repeatable) {
        val currentOnClick by rememberUpdatedState(onClick)
        val coroutineScope = rememberCoroutineScope()
        var isPressed by remember { mutableStateOf(false) }
        var repeatJob by remember { mutableStateOf<Job?>(null) }

        DisposableEffect(Unit) { onDispose { repeatJob?.cancel() } }

        val bgColor = when {
            isPressed -> pressedBackgroundColor
            isFloating && backgroundColor.alpha >= 1f -> Color.Transparent
            else -> backgroundColor
        }
        val textColor = if (isFloating) FloatingKeyTextColor else KeyTextColor
        val shape = RoundedCornerShape(cornerRadius)

        Surface(
            modifier = modifier
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            repeatJob = coroutineScope.launch {
                                delay(500)
                                if (!isPressed) return@launch
                                currentOnClick()
                                delay(100)
                                while (isPressed) {
                                    currentOnClick()
                                    delay(50)
                                }
                            }
                            val released = tryAwaitRelease()
                            isPressed = false
                            if (released && repeatJob?.isActive == true) {
                                repeatJob?.cancel()
                                currentOnClick()
                            } else {
                                repeatJob?.cancel()
                            }
                        }
                    )
                }
                .then(if (isFloating) Modifier.border(1.dp, KeyBorderColor, shape) else Modifier),
            shape = shape,
            color = bgColor,
            shadowElevation = if (isPressed || isFloating) 0.dp else 1.dp,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                if (isPressed && showPreview && label != null && label.isNotEmpty()) {
                    KeyPreviewPopup(label = label, fontSize = fontSize, cornerRadius = cornerRadius, backgroundColor = backgroundColor)
                }
                if (label != null && label.isNotEmpty()) {
                    Text(
                        text = label,
                        color = textColor,
                        fontSize = fontSize.sp,
                        maxLines = 1,
                    )
                } else if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size((fontSize + 4).dp),
                    )
                }
            }
        }
    } else {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val bgColor = when {
            isPressed -> pressedBackgroundColor
            isFloating && backgroundColor.alpha >= 1f -> Color.Transparent
            else -> backgroundColor
        }
        val textColor = if (isFloating) FloatingKeyTextColor else KeyTextColor
        val shape = RoundedCornerShape(cornerRadius)

        Surface(
            modifier = modifier
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                )
                .then(if (isFloating) Modifier.border(1.dp, KeyBorderColor, shape) else Modifier),
            shape = shape,
            color = bgColor,
            shadowElevation = if (isPressed || isFloating) 0.dp else 1.dp,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                if (isPressed && showPreview && label != null && label.isNotEmpty()) {
                    KeyPreviewPopup(label = label, fontSize = fontSize, cornerRadius = cornerRadius, backgroundColor = backgroundColor)
                }
                if (label != null && label.isNotEmpty()) {
                    Text(
                        text = label,
                        color = textColor,
                        fontSize = fontSize.sp,
                        maxLines = 1,
                    )
                } else if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size((fontSize + 4).dp),
                    )
                }
            }
        }
    }
}

@Composable
fun KeyPreviewPopup(
    label: String,
    fontSize: Int,
    cornerRadius: Dp,
    backgroundColor: Color = CharKeyBackground,
) {
    val density = LocalDensity.current
    val offsetY = with(density) { -55.dp.roundToPx() }
    Popup(
        alignment = Alignment.TopCenter,
        offset = IntOffset(0, offsetY),
    ) {
        Surface(
            shape = RoundedCornerShape(cornerRadius + 2.dp),
            color = backgroundColor,
            shadowElevation = 4.dp,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = label,
                    color = KeyTextColor,
                    fontSize = (fontSize * 1.6).sp,
                )
            }
        }
    }
}

@Composable
fun QwertyRepeatableButton(
    label: String,
    icon: ImageVector? = null,
    onPress: () -> Unit,
    modifier: Modifier = Modifier,
    fontSize: Int,
    cornerRadius: Dp,
    backgroundColor: Color = CharKeyBackground,
    showPreview: Boolean = false,
    isFloating: Boolean = false,
) {
    val currentOnPress by rememberUpdatedState(onPress)
    val coroutineScope = rememberCoroutineScope()
    var isPressed by remember { mutableStateOf(false) }
    var repeatJob by remember { mutableStateOf<Job?>(null) }

    DisposableEffect(Unit) { onDispose { repeatJob?.cancel() } }

    val bgColor = when {
        isPressed -> KeyPressedBackground
        isFloating -> Color.Transparent
        else -> backgroundColor
    }
    val textColor = if (isFloating) FloatingKeyTextColor else KeyTextColor
    val shape = RoundedCornerShape(cornerRadius)

    Surface(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        repeatJob = coroutineScope.launch {
                            delay(400)
                            if (!isPressed) return@launch
                            currentOnPress()
                            delay(200)
                            while (isPressed) {
                                currentOnPress()
                                delay(50)
                            }
                        }
                        val released = tryAwaitRelease()
                        isPressed = false
                        if (released && repeatJob?.isActive == true) {
                            repeatJob?.cancel()
                            currentOnPress()
                        } else {
                            repeatJob?.cancel()
                        }
                    }
                )
            }
            .then(if (isFloating) Modifier.border(1.dp, KeyBorderColor, shape) else Modifier),
        shape = shape,
        color = bgColor,
        shadowElevation = if (isPressed || isFloating) 0.dp else 1.dp,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            if (isPressed && showPreview) {
                KeyPreviewPopup(label = label, fontSize = fontSize, cornerRadius = cornerRadius, backgroundColor = backgroundColor)
            }
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = textColor,
                    modifier = Modifier.size((fontSize + 4).dp),
                )
            } else {
                Text(
                    text = label,
                    color = textColor,
                    fontSize = fontSize.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun QwertyModifierButton(
    label: String,
    level: ModifierLevel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fontSize: Int,
    cornerRadius: Dp,
    isFloating: Boolean = false,
) {
    val bgColor = when {
        level != ModifierLevel.OFF -> when (level) {
            ModifierLevel.TRANSIENT -> ModifierTransientBackground
            ModifierLevel.LOCKED -> ModifierLockedBackground
            else -> ModifierOffBackground
        }.let { if (isFloating) it.copy(alpha = 0.3f) else it }
        isFloating -> Color.Transparent
        else -> ModifierOffBackground
    }
    val textColor = when {
        level != ModifierLevel.OFF -> Color.White
        isFloating -> FloatingKeyTextColor
        else -> ModifierOffTextColor
    }
    val shape = RoundedCornerShape(cornerRadius)

    Surface(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .then(if (isFloating) Modifier.border(1.dp, KeyBorderColor, shape) else Modifier),
        shape = shape,
        color = bgColor,
        shadowElevation = if (isFloating) 0.dp else 1.dp,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Text(
                text = label,
                color = textColor,
                fontSize = fontSize.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun VoicePulseRings(
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "voicePulse")
    val ring1 by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ring1",
    )
    val ring2 by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, delayMillis = 400),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ring2",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val cx = size.width / 2
        val cy = size.height / 2
        val maxRadius = minOf(cx, cy) * 0.9f

        drawCircle(
            color = Color.White.copy(alpha = (1f - ring1) * 0.4f),
            radius = maxRadius * ring1,
            center = center,
            style = Stroke(width = 2f),
        )
        drawCircle(
            color = Color.White.copy(alpha = (1f - ring2) * 0.4f),
            radius = maxRadius * ring2,
            center = center,
            style = Stroke(width = 2f),
        )
    }
}
