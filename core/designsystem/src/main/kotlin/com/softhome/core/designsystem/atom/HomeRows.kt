package com.softhome.core.designsystem.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
@Composable
fun HomeRow(
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    contentDescription: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClickLabel = onClickLabel, onClick = onClick) else Modifier)
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
    Box(
        modifier = modifier
            .size(Dimens.railIcon)
            .then(
                if (onClick != null || onLongClick != null) {
                    Modifier
                        .clip(CircleShape)
                        .combinedClickable(
                            role = Role.Button,
                            onClickLabel = contentDescription,
                            onClick = { onClick?.invoke() },
                            onLongClickLabel = "Options",
                            onLongClick = onLongClick,
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

/**
 * Music player row (static UI while MediaSession integration is deferred).
 * Nodes: QTwqr title, juzvC artist, E7w0Vk track, mdsQP album circle (64,
 * fill #F5EFE6, stroke #8A5F43), x5SL5 controls, pvQO0 track (#B9AA98) +
 * d41sbp fill (#8A5F43).
 *
 * @param progress 0f..1f fill of the progress bar (design shows ~0.31).
 */
@Composable
fun MusicPlayerRow(
    title: String,
    artist: String,
    track: String,
    modifier: Modifier = Modifier,
    progress: Float = 0.31f,
    onPlayToggle: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.softColors
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(text = title, style = RowDisplay, color = colors.textPrimary)
            Text(text = artist, style = TweakLabel.copy(fontSize = 10.sp), color = colors.accent)
            Text(text = track, style = TweakLabelLean, color = colors.textMuted)

            Spacer(Modifier.height(Spacing.sm))

            // Controls: skip-back 16, play 18, skip-forward 16 (gap 18).
            Row(verticalAlignment = Alignment.CenterVertically) {
                MusicControl(LineIcon.SkipBack, Dimens.musicControlSmall, "Previous")
                Spacer(Modifier.width(18.dp))
                MusicControl(LineIcon.Play, Dimens.musicControl, "Play", onClick = onPlayToggle)
                Spacer(Modifier.width(18.dp))
                MusicControl(LineIcon.SkipForward, Dimens.musicControlSmall, "Next")
            }

            Spacer(Modifier.height(Spacing.md))

            // Progress: 104 wide track + accent fill.
            Box(
                modifier = Modifier
                    .width(104.dp)
                    .height(Dimens.progressHeight)
                    .clip(RoundedCornerShape(3.dp))
                    .background(colors.progressTrack),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(Dimens.progressHeight)
                        .clip(RoundedCornerShape(3.dp))
                        .background(colors.accent),
                )
            }
        }

        // Album artwork circle (64) + disc-3 mark.
        Box(
            modifier = Modifier
                .size(Dimens.albumArt)
                .clip(CircleShape)
                .background(colors.tileWarm),
            contentAlignment = Alignment.Center,
        ) {
            LineIconImage(icon = LineIcon.Disc3, size = 20.dp, tint = colors.textBody, contentDescription = null)
        }
    }
}

@Composable
private fun MusicControl(
    icon: LineIcon,
    size: Dp,
    contentDescription: String,
    onClick: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.softColors
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClickLabel = contentDescription, onClick = onClick) else Modifier)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        LineIconImage(icon = icon, size = size, tint = colors.textPrimary, contentDescription = null)
    }
}
