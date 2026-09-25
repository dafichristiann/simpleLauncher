package com.softhome.feature.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.softhome.core.designsystem.atom.LineIcon
import com.softhome.core.designsystem.atom.LineIconImage
import com.softhome.core.designsystem.atom.warmPress
import com.softhome.core.designsystem.theme.Dimens
import com.softhome.core.designsystem.theme.rememberReducedMotion
import com.softhome.core.designsystem.theme.softColors
import com.softhome.core.model.ThemeMode

/**
 * Premium dark/light theme toggle button for the top-right rail position.
 *
 * Animation:
 * - Press: scale 0.96 via warmPress modifier (consistent with rail)
 * - Icon transition: 280ms with subtle rotation and crossfade
 * - Settle: scale back to 1.0 smoothly
 *
 * The animation is event-based and triggers only on tap. No continuous animation
 * or global recomposition is triggered.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ThemeToggleButton(
    currentMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.softColors
    val reducedMotion = rememberReducedMotion()
    val haptics = LocalHapticFeedback.current
    
    // Local animation state: tracks the icon transition independently
    var animationKey by remember { mutableStateOf(0) }
    
    val nextMode = when (currentMode) {
        ThemeMode.Light -> ThemeMode.Dark
        ThemeMode.Dark -> ThemeMode.Light
        ThemeMode.System -> ThemeMode.Dark // System -> Dark by default
    }
    
    val icon = when (currentMode) {
        ThemeMode.Light -> LineIcon.Sun
        ThemeMode.Dark -> LineIcon.Moon
        ThemeMode.System -> LineIcon.Sun // Default display
    }
    
    val contentDesc = when (currentMode) {
        ThemeMode.Light -> "Switch to dark mode"
        ThemeMode.Dark -> "Switch to light mode"
        ThemeMode.System -> "Switch to dark mode"
    }
    
    val interactionSource = remember { MutableInteractionSource() }
    
    Box(
        modifier = modifier
            .size(Dimens.railTouchTarget)
            .clip(CircleShape)
            .warmPress(interactionSource)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClickLabel = contentDesc,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    animationKey++ // Trigger icon transition animation
                    onThemeChange(nextMode)
                },
            )
            .semantics { contentDescription = contentDesc },
        contentAlignment = Alignment.Center,
    ) {
        // Icon with crossfade + subtle rotation animation
        androidx.compose.runtime.key(animationKey) {
            AnimatedContent(
                targetState = icon,
                transitionSpec = {
                    if (reducedMotion) {
                        // Simple crossfade for reduced motion
                        fadeIn(tween(150)) togetherWith fadeOut(tween(150))
                    } else {
                        // Premium animation: subtle rotation + scale + fade
                        (fadeIn(tween(280)) + scaleIn(tween(280), initialScale = 0.85f)) togetherWith
                                (fadeOut(tween(280)) + scaleOut(tween(280), targetScale = 0.85f))
                    }
                },
                label = "themeToggleIcon",
            ) { targetIcon ->
                Box(
                    modifier = Modifier
                        .size(Dimens.railIcon)
                        .run {
                            if (!reducedMotion) {
                                // Subtle rotation for icon transition
                                rotate(
                                    if (animationKey % 2 == 0) 0f else 60f
                                )
                            } else {
                                this
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    LineIconImage(
                        icon = targetIcon,
                        size = Dimens.railIcon,
                        tint = colors.textPrimary,
                        contentDescription = null,
                    )
                }
            }
        }
    }
}
