package com.xaaav.mozukutsuchikey.keyboard

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
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
) {
    if (repeatable) {
        // Delegate to repeatable implementation
        QwertyRepeatableButton(
            label = label ?: "",
            icon = icon,
            onPress = onClick,
            modifier = modifier,
            fontSize = fontSize,
            cornerRadius = cornerRadius,
            backgroundColor = backgroundColor,
        )
        return
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val bgColor = if (isPressed) pressedBackgroundColor else backgroundColor
    var buttonSize by remember { mutableStateOf(IntSize.Zero) }

    Box(modifier = modifier.onGloballyPositioned { buttonSize = it.size }) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ),
            shape = RoundedCornerShape(cornerRadius),
            color = bgColor,
            shadowElevation = if (isPressed) 0.dp else 1.dp,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                if (label != null && label.isNotEmpty()) {
                    Text(
                        text = label,
                        color = KeyTextColor,
                        fontSize = fontSize.sp,
                        maxLines = 1,
                    )
                } else if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = KeyTextColor,
                        modifier = Modifier.size((fontSize + 4).dp),
                    )
                }
            }
        }

        // Key preview popup
        if (isPressed && showPreview && label != null && label.isNotEmpty() && buttonSize != IntSize.Zero) {
            KeyPreviewPopup(
                label = label,
                fontSize = fontSize,
                cornerRadius = cornerRadius,
                buttonSize = buttonSize,
            )
        }
    }
}

@Composable
fun KeyPreviewPopup(
    label: String,
    fontSize: Int,
    cornerRadius: Dp,
    buttonSize: IntSize,
) {
    val density = LocalDensity.current
    val previewWidth = with(density) { (buttonSize.width / density.density * 1.5f).dp }
    val previewHeight = with(density) { (buttonSize.height / density.density * 1.8f).dp }
    val offsetX = with(density) { ((buttonSize.width - previewWidth.toPx()) / 2).toInt() }
    val offsetY = with(density) { (-previewHeight.toPx()).toInt() }

    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(offsetX, offsetY),
        properties = PopupProperties(clippingEnabled = false),
    ) {
        Surface(
            modifier = Modifier.size(previewWidth, previewHeight),
            shape = RoundedCornerShape(cornerRadius * 1.5f),
            color = CharKeyBackground,
            shadowElevation = 4.dp,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                Text(
                    text = label,
                    color = KeyTextColor,
                    fontSize = (fontSize * 1.8).sp,
                    maxLines = 1,
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
) {
    val currentOnPress by rememberUpdatedState(onPress)
    val coroutineScope = rememberCoroutineScope()
    var isPressed by remember { mutableStateOf(false) }
    var repeatJob by remember { mutableStateOf<Job?>(null) }

    DisposableEffect(Unit) { onDispose { repeatJob?.cancel() } }

    val bgColor = if (isPressed) KeyPressedBackground else backgroundColor

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
            },
        shape = RoundedCornerShape(cornerRadius),
        color = bgColor,
        shadowElevation = if (isPressed) 0.dp else 1.dp,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = KeyTextColor,
                    modifier = Modifier.size((fontSize + 4).dp),
                )
            } else {
                Text(
                    text = label,
                    color = KeyTextColor,
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
) {
    val bgColor = when (level) {
        ModifierLevel.OFF -> ModifierOffBackground
        ModifierLevel.TRANSIENT -> ModifierTransientBackground
        ModifierLevel.LOCKED -> ModifierLockedBackground
    }
    val textColor = when (level) {
        ModifierLevel.OFF -> ModifierOffTextColor
        else -> Color.White
    }

    Surface(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        ),
        shape = RoundedCornerShape(cornerRadius),
        color = bgColor,
        shadowElevation = 1.dp,
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
