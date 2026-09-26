package com.softhome.core.designsystem.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.softhome.core.designsystem.R
import com.softhome.core.designsystem.theme.Dimens
import com.softhome.core.designsystem.theme.Spacing
import com.softhome.core.designsystem.theme.softColors

/**
 * A settings row (P3 / G). Cream `card` surface, r18, min height 52dp.
 *
 * Generic by design so the settings panel can compose every section from one atom:
 *  - [onClick] with [trailing] = navigation row (chevron / value)
 *  - [toggle] slot = a [SoftToggle] row
 *  - [moveUp]/[moveDown] = the inline reorder buttons (P3 / Q5) on Widgets rows
 *
 * The row is disabled (dimmed, non-interactive) when [enabled] is false.
 */
@Composable
fun SettingsRow(
    label: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    toggle: (@Composable () -> Unit)? = null,
    showChevron: Boolean = false,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.softColors
    val interactive = enabled && onClick != null
    val interactionSource = androidx.compose.runtime.remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.settingsRowRadius))
            .background(colors.card)
            .alpha(if (enabled) 1f else 0.5f)
            .then(
                if (interactive) {
                    Modifier
                        .warmPress(interactionSource, enabled = enabled)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            role = Role.Button,
                            onClickLabel = onClickLabel,
                            onClick = onClick,
                        )
                } else {
                    Modifier
                },
            )
            .defaultMinSize(minHeight = Dimens.settingsRowMinHeight)
            .padding(horizontal = Spacing.xl, vertical = Spacing.md)
            .semantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingContent != null) {
            leadingContent()
            Spacer(Modifier.width(Spacing.md))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textPrimary,
            )
            if (supporting != null) {
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textMuted,
                )
            }
        }

        // Inline reorder buttons (Q5): shown when requested, else nothing.
        if (onMoveUp != null || onMoveDown != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                ReorderButton(
                    icon = LineIcon.ArrowUp,
                    label = stringResource(R.string.atom_move_up_a11y),
                    enabled = enabled,
                    onClick = onMoveUp,
                )
                ReorderButton(
                    icon = LineIcon.ArrowDown,
                    label = stringResource(R.string.atom_move_down_a11y),
                    enabled = enabled,
                    onClick = onMoveDown,
                )
            }
            Spacer(Modifier.width(Spacing.sm))
        }

        if (trailing != null) {
            trailing()
            if (showChevron) Spacer(Modifier.width(Spacing.sm))
        }

        if (showChevron) {
            LineIconImage(
                icon = LineIcon.ChevronRight,
                size = Dimens.settingsChevron,
                tint = colors.textMuted,
                contentDescription = null,
            )
        }

        if (toggle != null) {
            Spacer(Modifier.width(Spacing.md))
            toggle()
        }
    }
}

@Composable
private fun ReorderButton(
    icon: LineIcon,
    label: String,
    enabled: Boolean,
    onClick: (() -> Unit)?,
) {
    val colors = MaterialTheme.softColors
    val active = enabled && onClick != null
    val interactionSource = androidx.compose.runtime.remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(Dimens.moveButton)
            .clip(RoundedCornerShape(Dimens.moveButton / 2))
            .then(
                if (active) {
                    Modifier
                        .warmPress(interactionSource, enabled = active)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            role = Role.Button,
                            onClickLabel = label,
                            onClick = onClick,
                        )
                } else {
                    Modifier
                },
            )
            .alpha(if (active) 1f else 0.35f)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        LineIconImage(
            icon = icon,
            size = Dimens.settingsChevron,
            tint = tintFor(colors.textPrimary, active),
            contentDescription = null,
        )
    }
}

private fun tintFor(color: Color, active: Boolean): Color = if (active) color else color.copy(alpha = 0.6f)
