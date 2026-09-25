package com.softhome.core.designsystem.atom

import androidx.compose.foundation.background
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
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
                            onLongClickLabel = "Options",
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

/**
 * Music player row. Renders a live playback state supplied by the caller (the home wires
 * a [com.softhome.feature.home.PlaybackController]); the design system itself owns no
 * playback. Nodes: QTwqr title, juzvC artist, E7w0Vk track, mdsQP album circle (64,
 * fill #F5EFE6, stroke #8A5F43), x5SL5 controls, pvQO0 track (#B9AA98) +
 * d41sbp fill (#8A5F43).
 *
 * Media/session integration is deferred; a future MediaSession adapter would drive the
 * same state without changing this composable.
 *
 * @param progress 0f..1f fill of the progress bar, derived from
 *   `currentTimeMs / durationMs` (a deterministic function of the playback state).
 */
@Composable
fun MusicPlayerRow(
    title: String,
    artist: String,
    track: String,
    modifier: Modifier = Modifier,
    progress: Float = 0f,
    expanded: Boolean = false,
    reducedMotion: Boolean = false,
    isPlaying: Boolean = false,
    currentTimeMs: Long = 0L,
    durationMs: Long = 210_000L,
    onSeek: ((Long) -> Unit)? = null,
    onPlayToggle: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.softColors
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = com.softhome.core.designsystem.theme.MotionTokens.musicProgress(),
        label = "musicProgress",
    )
    AnimatedContent(
        targetState = expanded,
        transitionSpec = {
            if (reducedMotion) {
                EnterTransition.None togetherWith ExitTransition.None
            } else {
                (fadeIn(com.softhome.core.designsystem.theme.MotionTokens.musicExpand()) +
                    scaleIn(com.softhome.core.designsystem.theme.MotionTokens.musicExpand(), initialScale = 0.96f) +
                    slideInVertically(com.softhome.core.designsystem.theme.MotionTokens.musicExpand()) { it / 5 }) togetherWith
                    (fadeOut(com.softhome.core.designsystem.theme.MotionTokens.musicExpand()) +
                        scaleOut(com.softhome.core.designsystem.theme.MotionTokens.musicExpand(), targetScale = 0.98f) +
                        slideOutVertically(com.softhome.core.designsystem.theme.MotionTokens.musicExpand()) { -it / 5 })
            }
        },
        label = "musicPlayerExpanded",
        modifier = modifier.fillMaxWidth(),
    ) { isExpanded ->
        if (isExpanded) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(Dimens.expandedAlbumArt)
                        .clip(CircleShape)
                        .background(colors.tileWarm),
                    contentAlignment = Alignment.Center,
                ) {
                    LineIconImage(icon = LineIcon.Disc3, size = 42.dp, tint = colors.textBody, contentDescription = "Album artwork")
                }
                Spacer(Modifier.height(Spacing.md))
                Text(text = title, style = RowDisplay, color = colors.textPrimary)
                Text(text = artist, style = TweakLabel.copy(fontSize = 11.sp), color = colors.accent)
                Text(text = track, style = TweakLabelLean, color = colors.textMuted)
                Spacer(Modifier.height(Spacing.md))
                MusicProgressBar(
                    progress = animatedProgress,
                    currentTimeMs = currentTimeMs,
                    durationMs = durationMs,
                    onSeek = onSeek,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(Spacing.sm))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatPlaybackTime(currentTimeMs), style = TweakLabelLean, color = colors.textMuted)
                    Text(formatPlaybackTime(durationMs), style = TweakLabelLean, color = colors.textMuted)
                }
                Spacer(Modifier.height(Spacing.sm))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MusicControl(LineIcon.SkipBack, Dimens.musicControlSmall, "Previous")
                    Spacer(Modifier.width(24.dp))
                    MusicControl(if (isPlaying) LineIcon.Pause else LineIcon.Play, Dimens.musicControl, if (isPlaying) "Pause" else "Play", onClick = onPlayToggle)
                    Spacer(Modifier.width(24.dp))
                    MusicControl(LineIcon.SkipForward, Dimens.musicControlSmall, "Next")
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(text = title, style = RowDisplay, color = colors.textPrimary)
                    Text(text = artist, style = TweakLabel.copy(fontSize = 10.sp), color = colors.accent)
                    Text(text = track, style = TweakLabelLean, color = colors.textMuted)
                    Spacer(Modifier.height(Spacing.sm))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MusicControl(LineIcon.SkipBack, Dimens.musicControlSmall, "Previous")
                        Spacer(Modifier.width(18.dp))
                        MusicControl(if (isPlaying) LineIcon.Pause else LineIcon.Play, Dimens.musicControl, if (isPlaying) "Pause" else "Play", onClick = onPlayToggle)
                        Spacer(Modifier.width(18.dp))
                        MusicControl(LineIcon.SkipForward, Dimens.musicControlSmall, "Next")
                    }
                    Spacer(Modifier.height(Spacing.md))
                    MusicProgressBar(
                        progress = animatedProgress,
                        currentTimeMs = currentTimeMs,
                        durationMs = durationMs,
                        onSeek = onSeek,
                        modifier = Modifier.width(104.dp),
                    )
                }
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
    }
}

@Composable
private fun MusicProgressBar(
    progress: Float,
    currentTimeMs: Long,
    durationMs: Long,
    onSeek: ((Long) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.softColors
    BoxWithConstraints(
        modifier = modifier
            .height(Dimens.progressHeight)
            .clip(RoundedCornerShape(3.dp))
            .background(colors.progressTrack)
            .then(
                if (onSeek == null) Modifier else Modifier.pointerInput(onSeek, durationMs) {
                    detectTapGestures { offset ->
                        val widthPx = size.width.toFloat()
                        if (widthPx > 0f) {
                            onSeek?.invoke((durationMs * (offset.x / widthPx).coerceIn(0f, 1f)).toLong())
                        }
                    }
                },
            ),
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

private fun formatPlaybackTime(timeMs: Long): String {
    val totalSeconds = (timeMs.coerceAtLeast(0L) / 1_000L).toInt()
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

@Composable
private fun MusicControl(
    icon: LineIcon,
    size: Dp,
    contentDescription: String,
    onClick: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.softColors
    val interactionSource = androidx.compose.runtime.remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .then(
                if (onClick != null) {
                    Modifier
                        .warmPress(interactionSource)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            role = Role.Button,
                            onClickLabel = contentDescription,
                            onClick = onClick,
                        )
                } else {
                    Modifier
                },
            )
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        LineIconImage(icon = icon, size = size, tint = colors.textPrimary, contentDescription = null)
    }
}
