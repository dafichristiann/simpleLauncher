package com.softhome.feature.home

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.softhome.core.designsystem.atom.AppContextMenu
import com.softhome.core.designsystem.atom.BatteryStorageRowContent
import com.softhome.core.designsystem.atom.CalendarRowContent
import com.softhome.core.designsystem.atom.ContextMenuItem
import com.softhome.core.designsystem.atom.HomeDivider
import com.softhome.core.designsystem.atom.HomeRow
import com.softhome.core.designsystem.atom.LineIcon
import com.softhome.core.designsystem.atom.LineIconImage
import com.softhome.core.designsystem.atom.MusicPlayerRow
import com.softhome.core.designsystem.atom.NotesRowContent
import com.softhome.core.designsystem.atom.RailIcon
import com.softhome.core.designsystem.theme.ClockLarge
import com.softhome.core.designsystem.theme.DateNumber
import com.softhome.core.designsystem.theme.Dimens
import com.softhome.core.designsystem.theme.MotionTokens
import com.softhome.core.designsystem.theme.RowDisplay
import com.softhome.core.designsystem.theme.RowMeta
import com.softhome.core.designsystem.theme.Spacing
import com.softhome.core.designsystem.theme.TweakLabel
import com.softhome.core.designsystem.theme.TweakLabelLean
import com.softhome.core.designsystem.theme.softColors
import com.softhome.core.model.HomeRowKind
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The SOFT / HOME home screen -- "Warm Right Rail".
 *
 * Mirrors design/homeApp.pen frame `znb90` (390x720, r44, bg #E8DFD0):
 *   right rail `hrsLU` (72 wide, fill #D8C8B6, 8 line icons)
 *   + vertical list of full-width rows separated by 1px #D0C2B1 dividers:
 *     time (IDSBb) -> date (ApuhU/KkRQg/ftENm) -> weather (lLPZS) ->
 *     search (Q1cGYj/N7ICqS) -> music player (QTwqr..).
 *
 * P2 adds three widget rows below the music row (docs/04 section I #48): calendar,
 * battery/storage, quick notes. There is no widget mock in the `.pen`; they follow
 * the existing Warm row patterns.
 *
 * Replaces the P1 squircle icon grid. The app drawer keeps the grid.
 */
@Composable
fun HomeScreen(
    onOpenDrawer: () -> Unit,
    onVoiceSearch: () -> Unit,
    modifier: Modifier = Modifier,
    state: HomeUiState = HomeUiState(),
    onNotesChange: (String) -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {
    val context = LocalContext.current
    val colors = MaterialTheme.softColors
    val time = homeTime()
    val date = homeDate()

    val railResolver = rememberRailResolver(context)
    val onShortcut: (RailShortcut) -> Unit = { shortcut ->
        railResolver.intentFor(shortcut)?.let { intent ->
            runCatching { context.startActivity(intent) }
        }
    }

    var homeState by remember { mutableStateOf(HomeState.Idle) }
    val scrollState = rememberScrollState()

    // Back / tapping outside a row returns to Idle.
    BackHandler(enabled = homeState != HomeState.Idle) { homeState = homeState.reset() }

    Box(modifier = modifier.fillMaxSize()) {
        // Dismiss layer (behind content): tapping empty space collapses any
        // active state. Rows / rail sit on top and consume their own taps.
        if (homeState != HomeState.Idle) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { homeState = homeState.reset() },
            )
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.surface),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
                    // Scroll only while the notes editor is open: it makes the (taller)
                    // expanded row reachable, while at rest the list fits and the swipe-up
                    // gesture reaches the drawer layer behind the content.
                    .then(
                        if (homeState.isNotesOpen) Modifier.verticalScroll(scrollState) else Modifier,
                    )
                    .padding(
                        start = Dimens.homeRowPaddingX,
                        end = Dimens.homeRowPaddingX,
                        top = Spacing.xxl,
                        bottom = Spacing.sm,
                    ),
            ) {
                // P3 (G/Widgets): the row list is user-configurable (visibility +
                // order). `visibleRows` already includes the locked set + ordering;
                // dividers are rendered only between visible rows.
                val rows = state.visibleRows
                rows.forEachIndexed { index, kind ->
                    HomeRowSlot(
                        kind = kind,
                        time = time,
                        date = date,
                        state = state,
                        homeState = homeState,
                        onNotesChange = onNotesChange,
                        onTapRow = { homeState = homeState.onTapRow(it) },
                    )
                    if (index != rows.lastIndex) HomeDivider()
                }
            }

            HomeRightRail(
                resolver = railResolver,
                onShortcut = onShortcut,
                onOpenSettings = onOpenSettings,
                expanded = homeState.isSearching,
            )
        }
    }
}

/**
 * Renders the single home row for [kind], wired to [homeState] / [state]. Extracted
 * so the row list is data-driven (P3 / G Widgets section: visibility + order).
 */
@Composable
private fun HomeRowSlot(
    kind: HomeRowKind,
    time: String,
    date: HomeDate,
    state: HomeUiState,
    homeState: HomeState,
    onNotesChange: (String) -> Unit,
    onTapRow: (HomeRowId) -> Unit,
) {
    when (kind) {
        HomeRowKind.Time -> TimeRow(time)
        HomeRowKind.Date -> DateRow(date)
        HomeRowKind.Weather -> WeatherRow()
        HomeRowKind.Search -> SearchRow(
            focused = homeState.isSearching,
            onClick = { onTapRow(HomeRowId.Search) },
        )
        HomeRowKind.Music -> MusicRow(
            expanded = homeState.isMusicOpen,
            onToggle = { onTapRow(HomeRowId.Music) },
        )
        HomeRowKind.Calendar -> CalendarRowContent(
            dayOfMonth = date.day,
            weekday = date.weekday,
            month = date.month,
            events = emptyList(),
        )
        HomeRowKind.BatteryStorage -> BatteryStorageRowContent(
            batteryPercent = state.deviceStatus.batteryPercent,
            storageUsedPercent = state.deviceStatus.storageUsedPercent,
            storageFreeLabel = state.deviceStatus.storageFreeBytes?.let(::formatBytes),
        )
        HomeRowKind.Notes -> NotesRow(
            text = state.notes,
            expanded = homeState.isNotesOpen,
            onTextChange = onNotesChange,
            onToggle = { onTapRow(HomeRowId.Notes) },
        )
    }
}

@Composable
private fun TimeRow(time: String) {
    val colors = MaterialTheme.softColors
    HomeRow(showDivider = false) {
        Text(
            text = time,
            style = ClockLarge,
            color = colors.textPrimary,
            modifier = Modifier.padding(vertical = Spacing.xl),
        )
    }
}

@Composable
private fun DateRow(date: HomeDate) {
    val colors = MaterialTheme.softColors
    HomeRow(showDivider = false) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.xl),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(text = date.day, style = DateNumber, color = colors.textPrimary)
            Spacer(Modifier.width(Spacing.lg))
            Column(Modifier.padding(bottom = Spacing.sm)) {
                Text(text = date.weekday, style = TweakLabel, color = colors.textBody)
                Text(text = date.month, style = TweakLabelLean, color = colors.textMuted)
            }
        }
    }
}

@Composable
private fun WeatherRow() {
    val colors = MaterialTheme.softColors
    HomeRow(showDivider = false, contentDescription = "Weather") {
        Text(
            text = "Current 8\u00B0C",
            style = RowDisplay,
            color = colors.statusText,
            modifier = Modifier.padding(vertical = Spacing.xl),
        )
    }
}

@Composable
private fun SearchRow(focused: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.softColors
    HomeRow(showDivider = false, onClick = onClick, onClickLabel = "Search", contentDescription = "Find something") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.xl),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (focused) "s e a r c h i n g" else "f i n d  s o m e t h i n g",
                style = RowMeta,
                color = if (focused) colors.textPrimary else colors.textMuted,
                modifier = Modifier.weight(1f),
            )
            LineIconImage(
                icon = LineIcon.Search,
                size = Dimens.searchRowIcon,
                tint = if (focused) colors.accent else colors.textPrimary,
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun MusicRow(expanded: Boolean, onToggle: () -> Unit) {
    // MUSIC state grows the row in-place (MUSIC_RISE 280ms); other rows stay.
    val grow by animateDpAsState(
        targetValue = if (expanded) 16.dp else 0.dp,
        animationSpec = MotionTokens.musicRise(),
        label = "musicRise",
    )
    HomeRow(showDivider = false, contentDescription = "Music player", onClick = onToggle, onClickLabel = "Music") {
        MusicPlayerRow(
            title = "play music.",
            artist = "Djo",
            track = "End of Beginning \u00B7 Live from Chicago",
            modifier = Modifier.padding(top = Spacing.xl + grow, bottom = Spacing.xl + grow),
        )
    }
}

/**
 * Quick-notes row (P2 / E5). Tapping toggles Idle <-> NOTES; when expanded the row
 * grows in-place (same motion family as music) and reveals the editor.
 */
@Composable
private fun NotesRow(
    text: String,
    expanded: Boolean,
    onTextChange: (String) -> Unit,
    onToggle: () -> Unit,
) {
    val grow by animateDpAsState(
        targetValue = if (expanded) 12.dp else 0.dp,
        animationSpec = MotionTokens.notesExpand(),
        label = "notesExpand",
    )
    HomeRow(showDivider = false, contentDescription = "Quick notes") {
        NotesRowContent(
            text = text,
            expanded = expanded,
            onTextChange = onTextChange,
            onClick = onToggle,
            modifier = Modifier.padding(top = grow, bottom = grow),
        )
    }
}

/** "1.5 GB" / "780 MB" from a byte count (for the storage meta line). */
private fun formatBytes(bytes: Long): String {
    val kb = 1024.0
    val mb = kb * 1024
    val gb = mb * 1024
    return when {
        bytes >= gb -> String.format(Locale.US, "%.1f GB", bytes / gb)
        bytes >= mb -> String.format(Locale.US, "%.0f MB", bytes / mb)
        bytes >= kb -> String.format(Locale.US, "%.0f KB", bytes / kb)
        else -> "$bytes B"
    }
}

/**
 * Right rail -- design node `hrsLU` / `B6633e`.
 * 72dp wide, fill #D8C8B6, vertical, gap 22, pad top 58 / bottom 26,
 * 8 line icons @ 20dp #2B2B2B.
 */
@Composable
private fun HomeRightRail(
    resolver: RailShortcutResolver,
    onShortcut: (RailShortcut) -> Unit,
    onOpenSettings: () -> Unit,
    expanded: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.softColors
    // SEARCH state: the rail "expands" (RAIL_SLIDE 220ms) -- icons nudge in.
    val slide by animateDpAsState(
        targetValue = if (expanded) 0.dp else (-6).dp,
        animationSpec = MotionTokens.railSlide(),
        label = "railSlide",
    )
    // Long-press menu state (F3): which rail shortcut is being configured.
    var menuFor by remember { mutableStateOf<RailShortcut?>(null) }

    Column(
        modifier = modifier
            .width(Dimens.railWidth)
            .fillMaxHeight()
            .background(colors.railBg)
            .padding(top = Dimens.railPadTop, bottom = Dimens.railPadBottom)
            .semantics { contentDescription = "Shortcut rail" },
        verticalArrangement = Arrangement.spacedBy(Spacing.railGap),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RailShortcut.ordered.forEach { shortcut ->
            val enabled = resolver.intentFor(shortcut) != null
            RailIcon(
                icon = shortcut.lineIcon(),
                contentDescription = shortcut.label(),
                modifier = Modifier.offset(y = slide),
                onClick = when {
                    // Q1: the panel-left (Settings) icon opens our own settings panel.
                    shortcut == RailShortcut.PanelLeft -> onOpenSettings
                    enabled -> { { onShortcut(shortcut) } }
                    else -> null
                },
                onLongClick = { menuFor = shortcut },
            )
        }
    }

    menuFor?.let { shortcut ->
        RailContextMenu(
            shortcut = shortcut,
            resolver = resolver,
            onLaunch = { onShortcut(shortcut) },
            onOpenSettings = onOpenSettings,
            onDismiss = { menuFor = null },
        )
    }
}

/**
 * Long-press menu for a rail icon (P3 / F3, decision P3-3). Row-style card
 * ([AppContextMenu]); "Open" fires the shortcut, "Settings" opens our panel for the
 * settings icon, "Remove" clears the pending menu (rail membership is fixed in P3).
 */
@Composable
private fun RailContextMenu(
    shortcut: RailShortcut,
    resolver: RailShortcutResolver,
    onLaunch: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    val items = buildList {
        if (resolver.intentFor(shortcut) != null) {
            add(ContextMenuItem("Open", LineIcon.ArrowUpRight, onClick = onLaunch))
        }
        if (shortcut == RailShortcut.PanelLeft) {
            add(ContextMenuItem("Settings", LineIcon.Settings, onClick = onOpenSettings))
        }
        add(ContextMenuItem("App Info", LineIcon.Info, onClick = onOpenSettings))
    }
    AppContextMenu(items = items, onDismiss = onDismiss)
}

private fun RailShortcut.lineIcon(): LineIcon = when (this) {
    RailShortcut.Sparkles -> LineIcon.Sparkles
    RailShortcut.CircleDot -> LineIcon.CircleDot
    RailShortcut.MessageCircle -> LineIcon.MessageCircle
    RailShortcut.Send -> LineIcon.Mail
    RailShortcut.Camera -> LineIcon.Camera
    RailShortcut.Wind -> LineIcon.Wind
    RailShortcut.PanelLeft -> LineIcon.PanelLeft
    RailShortcut.Phone -> LineIcon.Phone
}

private fun RailShortcut.label(): String = when (this) {
    RailShortcut.Sparkles -> "More"
    RailShortcut.CircleDot -> "Web"
    RailShortcut.MessageCircle -> "Messages"
    RailShortcut.Send -> "Mail"
    RailShortcut.Camera -> "Camera"
    RailShortcut.Wind -> "Weather"
    RailShortcut.PanelLeft -> "Settings"
    RailShortcut.Phone -> "Phone"
}

// --- date helpers -----------------------------------------------------------

private data class HomeDate(
    val day: String,
    val weekday: String,
    val month: String,
)

private fun homeTime(): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

private fun homeDate(): HomeDate {
    val now = Date()
    val day = SimpleDateFormat("d", Locale.getDefault()).format(now)
    val weekday = SimpleDateFormat("EEEE", Locale.getDefault()).format(now)
        .lowercase(Locale.getDefault()).toSpacedCaps()
    val month = SimpleDateFormat("MMMM", Locale.getDefault()).format(now)
        .uppercase(Locale.getDefault()).toSpacedCaps()
    return HomeDate(day = day, weekday = weekday, month = month)
}

/** "tuesday" -> "t u e s d a y" (the design tracks each letter). */
private fun String.toSpacedCaps(): String =
    trim().map { it.toString() }.joinToString(" ")


@Composable
private fun rememberRailResolver(context: Context): RailShortcutResolver =
    androidx.compose.runtime.remember(context) { RailShortcutResolver.forContext(context) }
