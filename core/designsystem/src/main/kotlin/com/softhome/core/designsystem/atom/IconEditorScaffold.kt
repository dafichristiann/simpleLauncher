package com.softhome.core.designsystem.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.softhome.core.designsystem.theme.Dimens
import com.softhome.core.designsystem.theme.Spacing
import com.softhome.core.designsystem.theme.softColors

/**
 * P4b: the "Edit Icon" editor shell. A **cream r24 card over a dim scrim** -- the same
 * vocabulary as the P2 `FolderPopupBody` and the P3 `AppContextMenu` (not the platform
 * `PopupMenu`, not an M3 sheet). Consumers pass the body; the shell owns the scrim,
 * the card, the title row, and dismiss (scrim tap / close button).
 *
 * This is a design-system atom so the editor's look stays in the token layer; the
 * feature module owns only the editor's *content* and its state.
 */
@Composable
fun IconEditorCard(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    body: @Composable () -> Unit,
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
        Column(
            modifier = Modifier
                .widthIn(max = Dimens.editorCardMaxWidth)
                .padding(horizontal = Spacing.xl)
                .clip(RoundedCornerShape(Dimens.menuRadius))
                .background(colors.card)
                // Swallow taps inside the card so the scrim does not dismiss.
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .size(Dimens.menuIcon + 8.dp)
                        .clip(RoundedCornerShape(Dimens.choiceTileRadius))
                        .clickable(onClick = onDismiss)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    LineIconImage(LineIcon.X, Dimens.menuIcon, colors.textBody)
                }
            }
            body()
        }
    }
}

/**
 * One selectable tile in the editor's drawable/glyph grid: a rounded square that shows
 * either a [painter] (a decoded pack drawable, untinted) or a [symbol] glyph, ringed in
 * the accent color when [selected].
 */
@Composable
fun ChoiceTile(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = Dimens.choiceTile,
    background: Color? = null,
    painter: androidx.compose.ui.graphics.painter.Painter? = null,
    symbol: LineIcon? = null,
    symbolTint: Color? = null,
    contentDescription: String? = null,
) {
    val colors = MaterialTheme.softColors
    val shape = RoundedCornerShape(Dimens.choiceTileRadius)
    val bg = background ?: colors.tileWarm
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(bg)
            .then(
                if (selected) Modifier.border(Dimens.swatchRingWidth, colors.accent, shape)
                else Modifier,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        when {
            painter != null -> androidx.compose.foundation.Image(
                painter = painter,
                contentDescription = contentDescription,
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                modifier = Modifier.size(size * 0.62f),
            )

            symbol != null -> LineIconImage(
                icon = symbol,
                size = Dimens.choiceIcon,
                tint = symbolTint ?: colors.textBody,
                contentDescription = contentDescription,
            )
        }
    }
}

/** A row of selectable color swatches (glyph mode). [onSelect] gets the index tapped. */
@Composable
fun ColorSwatchRow(
    swatches: List<Color>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.softColors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        swatches.forEachIndexed { index, color ->
            val shape = RoundedCornerShape(Dimens.swatchRadius)
            Box(
                modifier = Modifier
                    .size(Dimens.swatch)
                    .clip(shape)
                    .background(color)
                    .then(
                        if (index == selectedIndex) {
                            Modifier.border(Dimens.swatchRingWidth, colors.accent, shape)
                        } else {
                            Modifier
                        },
                    )
                    .clickable { onSelect(index) },
            )
        }
    }
}

/** A small muted section label used between editor sections. */
@Composable
fun EditorSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.softColors.textMuted,
        modifier = modifier,
    )
}

/** Transparent spacer helper kept local so callers do not import Compose layout. */
@Composable
fun EditorVSpace(height: Dp) {
    Spacer(Modifier.height(height))
}
