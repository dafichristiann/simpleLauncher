package com.softhome.core.designsystem.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.softhome.core.designsystem.theme.Dimens
import com.softhome.core.designsystem.theme.Spacing
import com.softhome.core.designsystem.theme.softColors

/**
 * One row in an [AppContextMenu].
 *
 * [destructive] tints the label with the warm clay destructive token (row style, no
 * red -- see the warm palette note in docs/00). [enabled] = false renders a greyed,
 * non-interactive row (P3-4: Uninstall for system apps).
 */
data class ContextMenuItem(
    val label: String,
    val icon: LineIcon,
    val enabled: Boolean = true,
    val destructive: Boolean = false,
    val onClick: () -> Unit,
)

/**
 * Row-style long-press menu (P3 / F3). A cream r24 card over a dim scrim, rows
 * separated by 1px dividers, leading 20dp line icons -- the same vocabulary as the
 * P2 folder popup, deliberately **not** the platform `PopupMenu`.
 *
 * Anchoring: when [anchorX]/[anchorY] are provided the card is placed near the press
 * point (clamped to the scrim bounds by the caller's layout); when null the card is
 * centred. Both consume the card's own taps and dismiss on scrim tap.
 */
@Composable
fun AppContextMenu(
    items: List<ContextMenuItem>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    anchorX: Dp? = null,
    anchorY: Dp? = null,
) {
    val colors = MaterialTheme.softColors
    val scrim = Color.Black.copy(alpha = Dimens.menuScrim)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(scrim)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        val anchored = anchorX != null && anchorY != null
        val cardModifier = Modifier
            .widthIn(min = 220.dp, max = 280.dp)
            .then(
                if (anchored) {
                    Modifier.offset(x = anchorX ?: 0.dp, y = anchorY ?: 0.dp)
                } else {
                    Modifier
                },
            )
            .clip(RoundedCornerShape(Dimens.menuRadius))
            .background(colors.card)
            // Swallow taps inside the card so the scrim does not dismiss.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            )
            .padding(vertical = Spacing.sm)

        Box(modifier = cardModifier) {
            Column(modifier = Modifier.fillMaxWidth()) {
                items.forEachIndexed { index, item ->
                    ContextMenuRow(item = item, onDismiss = onDismiss)
                    if (index != items.lastIndex) HomeDivider()
                }
            }
        }
    }
}

@Composable
private fun ContextMenuRow(item: ContextMenuItem, onDismiss: () -> Unit) {
    val colors = MaterialTheme.softColors
    val tint = when {
        !item.enabled -> colors.disabled
        item.destructive -> colors.destructive
        else -> colors.textBody
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (item.enabled) 1f else 0.55f)
            .then(
                if (item.enabled) {
                    Modifier.clickable(role = Role.Button, onClickLabel = item.label) {
                        item.onClick()
                        onDismiss()
                    }
                } else {
                    Modifier
                },
            )
            .padding(horizontal = Spacing.xl, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        LineIconImage(icon = item.icon, size = Dimens.menuIcon, tint = tint, contentDescription = null)
        Text(
            text = item.label,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
            color = tint,
            modifier = Modifier.weight(1f),
        )
    }
}
