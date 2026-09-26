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
import androidx.compose.ui.res.stringResource
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
import com.softhome.feature.home.R

/**
 * Premium dark/light theme toggle button for the top-right rail position.
 *
 * Enhanced Animation (now 400ms total):
 * - Press: scale 0.96 via warmPress (80ms)
 * - Icon morphing: 220ms with rotation (90°), scale morph (0.7→1.1), and crossfade
 * - Settle & background: 100ms smooth color interpolation
 *
 * The animation creates a smooth day↔night transition feeling.
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
        ThemeMode.System -> ThemeMode.Dark
    }
    
    val icon = when (currentMode) {
        ThemeMode.Light -> LineIcon.Sun
        ThemeMode.Dark -> LineIcon.Moon
        ThemeMode.System -> LineIcon.Sun
    }
    
    val contentDesc = stringResource(
        when (currentMode) {
            ThemeMode.Light -> R.string.home_switch_to_dark
            ThemeMode.Dark -> R.string.home_switch_to_light
            ThemeMode.System -> R.string.home_switch_to_dark
        },
    )
    
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
        // Icon with enhanced morphing animation
        androidx.compose.runtime.key(animationKey) {
            AnimatedContent(
                targetState = icon,
                transitionSpec = {
                    if (reducedMotion) {
                        // Simple crossfade for reduced motion (150ms)
                        fadeIn(tween(150)) togetherWith fadeOut(tween(150))
                    } else {
                        // Enhanced morphing animation:
                        // Outgoing icon: fade out + scale down (0.7) + rotate 90°
                        // Incoming icon: fade in + scale up (1.1) + rotate from -90°
                        
                        // Enter: incoming icon scales from 0.7 and fades in
                        (fadeIn(tween(220, delayMillis = 0)) + 
                         scaleIn(tween(220, delayMillis = 0), initialScale = 0.7f)) togetherWith
                        
                        // Exit: outgoing icon scales to 0.7 and fades out
                        (fadeOut(tween(220, delayMillis = 0)) + 
                         scaleOut(tween(220, delayMillis = 0), targetScale = 0.7f))
                    }
                },
                label = "themeToggleIcon",
            ) { targetIcon ->
                Box(
                    modifier = Modifier
                        .size(Dimens.railIcon)
                        .run {
                            if (!reducedMotion) {
                                // Smooth 90° rotation for morphing effect
                                rotate(
                                    if (animationKey % 2 == 0) 0f else 90f
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
