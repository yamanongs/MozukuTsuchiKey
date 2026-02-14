package com.xaaav.mozukutsuchikey.keyboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val bgColor = if (isPressed) KeyPressedBackground else backgroundColor

    Surface(
        modifier = modifier.clickable(
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
