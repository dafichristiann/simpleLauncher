package com.softhome.core.designsystem.atom

import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.softhome.core.designsystem.R
import com.softhome.core.designsystem.theme.Dimens
import com.softhome.core.designsystem.theme.RowDisplay
import com.softhome.core.designsystem.theme.Spacing
import com.softhome.core.designsystem.theme.TweakLabel
import com.softhome.core.designsystem.theme.TweakLabelLean
import com.softhome.core.designsystem.theme.softColors

/**
 * Warm Right Rail home atoms.
 *
 * Source: design/homeApp.pen frames `znb90` (home mockup) and `RwVPc` (system
 * board mockup). The home screen is a vertical list of full-width rows separated
 * by 1px dividers -- NOT the old cream card / squircle grid.
 */

/**
 * One full-width home row: content with a horizontal divider below it.
 * The design separates rows by a 1px `#D0C2B1` line (nodes MrrnQ / RDJW1 /
 * LKAGP / FH9n4), not by cards.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HomeRow(
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    /**
     * P3: a secondary **long-press** action. Used by the home rows so a tap launches the
     * related app while a long-press keeps the in-place expand (Q2). When null, a tap-only
     * `clickable` is used (unchanged from before).
     */
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    contentDescription: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    Column(modifier = modifier.fillMaxWidth()) {
        val interactionSource = androidx.compose.runtime.remember { MutableInteractionSource() }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    when {
                        onLongClick != null -> Modifier
                            .warmPress(interactionSource)
                            .combinedClickable(
                                interactionSource = interactionSource,
                                indication = null,
                                role = Role.Button,
                                onClickLabel = onClickLabel,
                                onClick = { onClick?.invoke() },
                                onLongClickLabel = onLongClickLabel,
                                onLongClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onLongClick()
                                },
                            )
                        onClick != null -> Modifier
                            .warmPress(interactionSource)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                role = Role.Button,
                                onClickLabel = onClickLabel,
                                onClick = onClick,
                            )
                        else -> Modifier
                    },
                )
                .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier),
            content = content,
        )
        if (showDivider) HomeDivider()
    }
}

/** Horizontal row divider -- 1px, `SoftDivider` (#D0C2B1). */
@Composable
fun HomeDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.dividerHeight)
            .background(MaterialTheme.softColors.divider),
    )
}

/**
 * A right-rail icon button. 20dp line icon on the rail background.
 * Node `hrsLU` / `B6633e`: 20x20 lucide icons, `#2B2B2B`.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun RailIcon(
    icon: LineIcon,
    contentDescription: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.softColors.textPrimary,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val interactionSource = androidx.compose.runtime.remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    val optionsLabel = stringResource(R.string.atom_options_a11y)
    Box(
        modifier = modifier
            .size(Dimens.railTouchTarget)
            .then(
                if (onClick != null || onLongClick != null) {
                    Modifier
                        .clip(CircleShape)
                        .warmPress(interactionSource)
                        .combinedClickable(
                            interactionSource = interactionSource,
                            indication = null,
                            role = Role.Button,
                            onClickLabel = contentDescription,
                            onClick = { onClick?.invoke() },
                            onLongClickLabel = optionsLabel,
                            onLongClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onLongClick?.invoke()
                            },
                        )
                } else {
                    Modifier
                },
            )
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        LineIconImage(icon = icon, size = Dimens.railIcon, tint = tint, contentDescription = null)
    }
}
