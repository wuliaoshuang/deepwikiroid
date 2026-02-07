package com.moxiang.deepwiki.core.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role

private const val DefaultPressedScale = 0.97f
private const val DefaultPressedAlpha = 0.92f

private val PressSpring = spring<Float>(
    stiffness = Spring.StiffnessMedium,
    dampingRatio = 0.7f
)

@Composable
fun Modifier.pressEffect(
    interactionSource: InteractionSource,
    pressedScale: Float = DefaultPressedScale,
    pressedAlpha: Float = DefaultPressedAlpha
): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = PressSpring,
        label = "press-scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isPressed) pressedAlpha else 1f,
        animationSpec = PressSpring,
        label = "press-alpha"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
        this.alpha = alpha
    }
}

@Composable
fun Modifier.pressable(
    enabled: Boolean = true,
    bounded: Boolean = true,
    showRipple: Boolean = true,
    haptic: Boolean = true,
    pressedScale: Float = DefaultPressedScale,
    pressedAlpha: Float = DefaultPressedAlpha,
    role: Role? = null,
    onClickLabel: String? = null,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    val indication = if (showRipple) ripple(bounded = bounded) else null

    return this
        .pressEffect(
            interactionSource = interactionSource,
            pressedScale = pressedScale,
            pressedAlpha = pressedAlpha
        )
        .clickable(
            interactionSource = interactionSource,
            indication = indication,
            enabled = enabled,
            role = role,
            onClickLabel = onClickLabel
        ) {
            if (haptic) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            onClick()
        }
}

@Composable
fun PressableIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    haptic: Boolean = true,
    pressedScale: Float = 0.92f,
    pressedAlpha: Float = 0.88f,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    IconButton(
        onClick = {
            if (haptic) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            onClick()
        },
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
        modifier = modifier.pressEffect(
            interactionSource = interactionSource,
            pressedScale = pressedScale,
            pressedAlpha = pressedAlpha
        )
    ) {
        content()
    }
}

@Composable
fun PressableFilledTonalIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    haptic: Boolean = true,
    pressedScale: Float = 0.92f,
    pressedAlpha: Float = 0.88f,
    colors: IconButtonColors = IconButtonDefaults.filledTonalIconButtonColors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    FilledTonalIconButton(
        onClick = {
            if (haptic) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            onClick()
        },
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
        modifier = modifier.pressEffect(
            interactionSource = interactionSource,
            pressedScale = pressedScale,
            pressedAlpha = pressedAlpha
        )
    ) {
        content()
    }
}

@Composable
fun PressableTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    haptic: Boolean = true,
    pressedScale: Float = 0.98f,
    pressedAlpha: Float = 0.92f,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    val haptics = LocalHapticFeedback.current
    TextButton(
        onClick = {
            if (haptic) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            onClick()
        },
        enabled = enabled,
        colors = colors,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        modifier = modifier.pressEffect(
            interactionSource = interactionSource,
            pressedScale = pressedScale,
            pressedAlpha = pressedAlpha
        ),
        content = content
    )
}

@Composable
fun PressableButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    haptic: Boolean = true,
    pressedScale: Float = DefaultPressedScale,
    pressedAlpha: Float = DefaultPressedAlpha,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Button(
        onClick = {
            if (haptic) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            onClick()
        },
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
        modifier = modifier.pressEffect(
            interactionSource = interactionSource,
            pressedScale = pressedScale,
            pressedAlpha = pressedAlpha
        ),
        content = content
    )
}

@Composable
fun PressableFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    haptic: Boolean = true,
    pressedScale: Float = 0.94f,
    pressedAlpha: Float = DefaultPressedAlpha,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onPrimaryContainer,
    content: @Composable () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val pressModifier = if (enabled) {
        modifier.pressEffect(
            interactionSource = interactionSource,
            pressedScale = pressedScale,
            pressedAlpha = pressedAlpha
        )
    } else {
        modifier.graphicsLayer { alpha = 0.6f }
    }
    FloatingActionButton(
        onClick = {
            if (enabled) {
                if (haptic) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                onClick()
            }
        },
        interactionSource = interactionSource,
        containerColor = containerColor,
        contentColor = contentColor,
        modifier = pressModifier,
        content = content
    )
}
