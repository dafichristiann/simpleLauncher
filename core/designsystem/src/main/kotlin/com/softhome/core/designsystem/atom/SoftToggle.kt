package com.softhome.core.designsystem.atom

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.softhome.core.designsystem.R
import com.softhome.core.designsystem.theme.Dimens
import com.softhome.core.designsystem.theme.MotionTokens
import com.softhome.core.designsystem.theme.ToggleShape
import com.softhome.core.designsystem.theme.softColors

/**
 * SOFT / HOME pill toggle (P3 / G2, node spec `UPa9N` in the retired P1 file --
 * no P3 mock exists, so this follows the design **token set** rather than a node).
 *
 * 42 x 22 pill, charcoal track when on, 16dp cream knob. Reuses
 * [MotionTokens.railSlide] (220ms, WarmEase) for the knob -- no new duration.
 * Accessible as a Switch with an On/Off state description.
 */
@Composable
fun SoftToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
) {
    val colors = MaterialTheme.softColors
    val haptics = LocalHapticFeedback.current
    val onLabel = stringResource(R.string.atom_toggle_on)
    val offLabel = stringResource(R.string.atom_toggle_off)

    // Track: charcoal (on) / warm track grey (off); dimmed when disabled.
    val trackColor = if (checked) colors.tile else colors.progressTrack
    val knobColor = if (checked) colors.onTile else colors.card

    val knobOffset by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = MotionTokens.railSlide(),
        label = "toggleKnob",
    )

    val trackWidth = Dimens.toggleWidth
    val knob = Dimens.toggleKnob
    // Travel = track width - knob - 2*inset(3dp each side).
    val inset = 3.dp
    val travel = trackWidth - knob - inset * 2

    Box(
        modifier = modifier
            .width(trackWidth)
            .height(Dimens.toggleHeight)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(ToggleShape)
            .background(trackColor)
            .then(
                if (enabled) {
                    val interactionSource = remember { MutableInteractionSource() }
                    Modifier
                        .warmPress(interactionSource, enabled = enabled)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            role = Role.Switch,
                            onClickLabel = contentDescription,
                        ) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onCheckedChange(!checked)
                        }
                } else {
                    Modifier
                },
            )
            .semantics {
                if (contentDescription != null) this.contentDescription = contentDescription
                stateDescription = if (checked) onLabel else offLabel
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = inset + travel * knobOffset)
                .size(knob)
                .clip(CircleShape)
                .background(knobColor),
        )
    }
}
