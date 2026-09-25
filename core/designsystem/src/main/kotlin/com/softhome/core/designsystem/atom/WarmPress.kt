package com.softhome.core.designsystem.atom

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Indication
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import com.softhome.core.designsystem.theme.MotionTokens
import com.softhome.core.designsystem.theme.softColors

/**
 * Shared Warm Right Rail press treatment. Callers pass the same interaction source
 * to clickable/combinedClickable with indication disabled so the source owns both the
 * scale/opacity state and the Warm-colored ripple.
 */
@Composable
@Suppress("DEPRECATION", "DEPRECATION_ERROR")
fun Modifier.warmPress(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (enabled && pressed) 0.96f else 1f,
        animationSpec = MotionTokens.pressFeedback(),
        label = "warmPressScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (enabled && pressed) 0.9f else 1f,
        animationSpec = MotionTokens.pressFeedback(),
        label = "warmPressAlpha",
    )
    val indication: Indication = rememberRipple(
        bounded = true,
        color = MaterialTheme.softColors.accent,
    )
    return this
        .scale(scale)
        .alpha(alpha)
        .indication(interactionSource, indication)
}
