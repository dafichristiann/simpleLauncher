package com.softhome.core.designsystem.atom

import androidx.compose.foundation.background
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.softhome.core.designsystem.theme.ClockLarge
import com.softhome.core.designsystem.theme.Dimens
import com.softhome.core.designsystem.theme.MotionTokens
import com.softhome.core.designsystem.theme.RowDisplay
import com.softhome.core.designsystem.theme.Spacing
import com.softhome.core.designsystem.theme.TweakLabel
import com.softhome.core.designsystem.theme.TweakLabelLean
import com.softhome.core.designsystem.theme.softColors

/**
 * P2 home widget rows -- built in the **Warm Right Rail** language (flat rows,
 * 1px dividers, normal-weight typography), NOT the retired cream card style.
 *
 * The `.pen` has no widget mock; these are documented assumptions (docs/04
 * section I #48). Each row is meant to be wrapped in a [HomeRow] so it gets the
 * shared full-width row + divider treatment.
 */

/**
 * Calendar row (E3): big day number + weekday/month, plus up to two static
 * upcoming-event lines.
 *
 * Live `CalendarContract` is **deferred** (same class of problem as weather:
 * needs READ_CALENDAR + permission/edge-case handling), so [events] is supplied
 * by the home layer and is empty in P2.
 */
@Composable
fun CalendarRowContent(
    dayOfMonth: String,
    weekday: String,
    month: String,
    events: List<String>,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.softColors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xl),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = dayOfMonth, style = ClockLarge, color = colors.textPrimary)
        Spacer(Modifier.width(Spacing.lg))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = weekday, style = TweakLabel, color = colors.textBody)
            Text(text = month, style = TweakLabelLean, color = colors.textMuted)
            if (events.isEmpty()) {
                Text(
                    text = "No upcoming events",
                    style = TweakLabelLean,
                    color = colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                events.take(2).forEach { line ->
                    Text(
                        text = line,
                        style = TweakLabelLean,
                        color = colors.textBody,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * Battery / storage row (E4) -- REAL device values (decision P2-3).
 *
 * Layout: a leading icon, the label + big battery %, and (below) a thin Warm
 * progress bar + a storage meta line. Null values are omitted, never shown as 0.
 */
@Composable
fun BatteryStorageRowContent(
    batteryPercent: Int?,
    storageUsedPercent: Int?,
    storageFreeLabel: String?,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.softColors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LineIconImage(
                icon = LineIcon.Battery,
                size = Dimens.searchRowIcon,
                tint = colors.textPrimary,
                contentDescription = null,
            )
            Spacer(Modifier.width(Spacing.md))
            Text(
                text = "Battery",
                style = RowDisplay,
                color = colors.statusText,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = batteryPercent?.let { "$it%" } ?: "--",
                style = RowDisplay,
                color = colors.textPrimary,
            )
        }

        ThinWarmProgressBar(fraction = (batteryPercent ?: 0) / 100f)

        if (storageUsedPercent != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LineIconImage(
                    icon = LineIcon.HardDrive,
                    size = 16.dp,
                    tint = colors.textMuted,
                    contentDescription = null,
                )
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    text = buildString {
                        append("Storage ").append(storageUsedPercent).append("%")
                        if (!storageFreeLabel.isNullOrBlank()) {
                            append(" \u00B7 ").append(storageFreeLabel).append(" free")
                        }
                    },
                    style = TweakLabelLean,
                    color = colors.textMuted,
                )
            }
            ThinWarmProgressBar(fraction = storageUsedPercent / 100f)
        }
    }
}

/**
 * Quick-notes row (E5) -- tap-to-expand (decision P2-2).
 *
 * Collapsed: a label + one-line preview. Expanded: an inline editor. The parent
 * (HomeScreen) drives [expanded] from the NOTES home state and commits text via
 * [onTextChange] (persisted in DataStore by the ViewModel).
 */
@Composable
fun NotesRowContent(
    text: String,
    expanded: Boolean,
    onTextChange: (String) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Quick notes",
) {
    val colors = MaterialTheme.softColors
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (!expanded) {
                    Modifier
                        .warmPress(interactionSource)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            role = Role.Button,
                            onClickLabel = placeholder,
                            onClick = onClick,
                        )
                } else {
                    Modifier
                },
            )
            .padding(vertical = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LineIconImage(
                icon = LineIcon.PenLine,
                size = Dimens.searchRowIcon,
                tint = colors.textPrimary,
                contentDescription = null,
            )
            Spacer(Modifier.width(Spacing.md))
            Text(
                text = placeholder,
                style = RowDisplay,
                color = colors.statusText,
                modifier = Modifier.weight(1f),
            )
        }

        if (!expanded) {
            Text(
                text = text.replace('\n', ' ').trim().ifBlank { "Nothing yet" },
                style = TweakLabelLean,
                color = colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .semantics { contentDescription = "Notes editor" },
                textStyle = TextStyle(
                    color = colors.textBody,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                ),
                cursorBrush = SolidColor(colors.accent),
                decorationBox = { inner ->
                    Box {
                        if (text.isBlank()) {
                            Text(
                                text = "Write something\u2026",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textMuted,
                            )
                        }
                        inner()
                    }
                },
            )
        }
    }
}

/** 5px Warm progress bar: `progressTrack` track + `accent` fill (matches the music row). */
@Composable
fun ThinWarmProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.softColors
    val animatedFraction by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = MotionTokens.musicRise(),
        label = "widgetProgress",
    )
    Box(
        modifier = modifier
            .fillMaxWidth(0.5f)
            .height(Dimens.widgetProgressHeight)
            .clip(RoundedCornerShape(3.dp))
            .background(colors.progressTrack),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedFraction)
                .height(Dimens.widgetProgressHeight)
                .clip(RoundedCornerShape(3.dp))
                .background(colors.accent),
        )
    }
}
