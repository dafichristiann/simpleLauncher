package com.softhome.feature.home

import android.content.Context
import androidx.activity.compose.BackHandler
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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.softhome.core.designsystem.atom.AppContextMenu
import com.softhome.core.designsystem.atom.BatteryStorageRowContent
import com.softhome.core.designsystem.atom.CalendarRowContent
import com.softhome.core.designsystem.atom.ContextMenuItem
import com.softhome.core.designsystem.atom.DragInsertionLine
import com.softhome.core.designsystem.atom.DragPreviewLayer
import com.softhome.core.designsystem.atom.DragRowChip
import com.softhome.core.designsystem.atom.HomeDivider
import com.softhome.core.designsystem.atom.HomeRow
import com.softhome.core.designsystem.atom.LineIcon
import com.softhome.core.designsystem.atom.LineIconImage
import com.softhome.core.designsystem.atom.NotesRowContent
import com.softhome.core.designsystem.atom.RailIcon
import com.softhome.core.designsystem.atom.warmPress
import com.softhome.core.designsystem.atom.dragSource
import com.softhome.core.designsystem.atom.dragSourceAlpha
import com.softhome.core.designsystem.atom.dropTarget
import com.softhome.core.designsystem.atom.rememberDragController
import com.softhome.core.designsystem.theme.ClockLarge
import com.softhome.core.designsystem.theme.DateNumber
import com.softhome.core.designsystem.theme.Dimens
import com.softhome.core.designsystem.theme.MotionTokens
import com.softhome.core.designsystem.theme.rememberReducedMotion
import com.softhome.core.designsystem.theme.RowDisplay
import com.softhome.core.designsystem.theme.RowMeta
import com.softhome.core.designsystem.theme.Spacing
import com.softhome.core.designsystem.theme.TweakLabel
import com.softhome.core.designsystem.theme.TweakLabelLean
import com.softhome.core.designsystem.theme.softColors
import com.softhome.core.model.HomeRowKind
import com.softhome.core.model.AppInfo
import com.softhome.core.model.RailConfigLogic
import com.softhome.core.model.RailItemId
import com.softhome.core.model.RailShortcutId
import com.softhome.core.model.ThemeMode
import com.softhome.feature.iconpack.data.IconPackDrawableLoader
import com.softhome.feature.iconpack.ui.DrawerAppIcon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

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
    appsState: AppsUiState = AppsUiState(),
    prefsState: PrefsUiState = PrefsUiState(),
    notesState: NotesUiState = NotesUiState(),
    railState: RailUiState = RailUiState(),
    deviceStatusState: DeviceStatusUiState = DeviceStatusUiState(),
    homeRowsState: HomeRowsUiState = HomeRowsUiState(),
    onNotesChange: (String) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onReorderRow: (HomeRowKind, Int) -> Unit = { _, _ -> },
    onReorderRail: (String, Int) -> Unit = { _, _ -> },
    onLaunchApp: (AppInfo) -> Unit = {},
    onSetThemeMode: (ThemeMode) -> Unit = {},
    /** Loader used to mirror the All Apps icon-pack artwork in the right rail. */
    drawerDrawableLoader: IconPackDrawableLoader? = null,
    /**
     * P3: invoked with the row whose **tap** should launch an app. When null (default)
     * the real [RowLaunchResolver] opens the related app; pass a lambda in tests to
     * assert the tap/long-press split (Q2) without a device.
     */
    onLaunchRow: ((HomeRowKind) -> Unit)? = null,
) {
    val context = LocalContext.current
    val colors = MaterialTheme.softColors
    val minuteKey = rememberMinuteKey()
    val time = remember(minuteKey) { homeTime(Date(minuteKey * 60_000L)) }
    val date = remember(minuteKey) { homeDate(Date(minuteKey * 60_000L)) }
    val reducedMotion = rememberReducedMotion()
    val weather = remember { WeatherUiState() }


    val railResolver = rememberRailResolver(context)
    val onShortcut: (RailShortcut) -> Unit = { shortcut ->
        railResolver.intentFor(shortcut)?.let { intent ->
            runCatching { context.startActivity(intent) }
        }
    }

    // P3: tapping a row launches the related app (Clock / Calendar / Weather / Search /
    // Music) through the graceful fallback chain (Q2/Q3). A long-press keeps the in-place
    // expand for Search / Music / Notes. `onVoiceSearch` is the "no app found" hook the
    // launcher uses to surface a message (kept a no-op-by-default callback for tests).
    val rowResolver = rememberRowResolver(context)
    val launchRow: (HomeRowKind) -> Unit = onLaunchRow ?: { kind ->
        rowResolver.intentFor(kind)?.let { intent ->
            runCatching { context.startActivity(intent) }
        }
    }

    var homeState by remember { mutableStateOf(HomeState.Idle) }
    val scrollState = rememberScrollState()

    // P4a: one drag controller for the home surface. Rows are the drag sources; the
    // drop target index is derived from the pointer's Y over the measured row bounds.
    val dragController = rememberDragController()
    val dragState = dragController.state
    var rowBoundsByIndex by remember { mutableStateOf<Map<Int, Rect>>(emptyMap()) }
    // The outer surface's window top-left: converts window-space pointer to local for
    // the floating preview layer (which is a child of that same Box).
    var surfaceOriginInWindow by remember { mutableStateOf(Offset.Zero) }

    val rows = homeRowsState.visibleRows
    // While dragging a row: which index it would drop into (null = cancel).
    val dropIndex: Int? = if (dragState.isDragging && dragState.draggingId?.startsWith("row:") == true) {
        nearestRowIndex(dragState.pointerWindowPx.y, rowBoundsByIndex, rows.size)
    } else null

    // Back / tapping outside a row returns to Idle.
    BackHandler(enabled = homeState != HomeState.Idle) { homeState = homeState.reset() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { surfaceOriginInWindow = it.boundsInWindow().topLeft },
    ) {
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
                    .dropTarget(targetId = "rowlist", controller = dragController)
                    .padding(
                        start = Dimens.homeRowPaddingX,
                        end = Dimens.homeRowPaddingX,
                        top = Spacing.xxl,
                        bottom = Spacing.sm,
                    ),
            ) {
                // P3 (G/Widgets): the row list is user-configurable (visibility +
                // order). P4a adds drag-to-reorder on top of the up/down buttons.
                rows.forEachIndexed { index, kind ->
                    val beingDragged = dragState.draggingId == "row:${kind.name}"
                    HomeRowSlot(
                        kind = kind,
                        time = time,
                        date = date,
                        appsState = appsState,
                        prefsState = prefsState,
                        notesState = notesState,
                        deviceStatusState = deviceStatusState,
                        homeState = homeState,
                        weather = weather,
                        reducedMotion = reducedMotion,
                        onNotesChange = onNotesChange,
                        onTapRow = { homeState = homeState.onTapRow(it) },
                        onLaunchRow = launchRow,
                        modifier = Modifier
                            .onGloballyPositioned { coords ->
                                val b = coords.boundsInWindow()
                                // Only write when the bounds actually changed, so a layout
                                // pass that leaves a row in place does not invalidate the
                                // snapshot state (and recompose every row reading it).
                                if (rowBoundsByIndex[index] != b) {
                                    rowBoundsByIndex = rowBoundsByIndex + (index to b)
                                }
                            }
                            .dragSourceAlpha(beingDragged)
                            .dragSource(
                                id = "row:${kind.name}",
                                controller = dragController,
                                onDrop = { target, pointer ->
                                    if (target != null) {
                                        val dropHere = nearestRowIndex(
                                            pointer.y,
                                            rowBoundsByIndex,
                                            rows.size,
                                        )
                                        onReorderRow(kind, dropHere)
                                    }
                                },
                            ),
                    )
                    if (index != rows.lastIndex) {
                        if (dropIndex == index + 1 && dragState.isDragging) {
                            DragInsertionLine()
                        } else {
                            HomeDivider()
                        }
                    }
                }
            }

            HomeRightRail(
                resolver = railResolver,
                onShortcut = onShortcut,
                onOpenSettings = onOpenSettings,
                expanded = homeState.isSearching,
                railItems = railState.railItems,
                railApps = appsState.railApps,
                activePack = prefsState.activePack,
                drawableLoader = drawerDrawableLoader,
                onReorder = onReorderRail,
                onLaunchApp = onLaunchApp,
                themeMode = prefsState.themeMode,
                onThemeChange = { onSetThemeMode(it) },
            )
        }

        // Floating drag preview (a compact row chip).
        DragPreviewLayer(controller = dragController, windowOrigin = surfaceOriginInWindow) { draggingId ->
            val label = rows.firstOrNull { "row:${it.name}" == draggingId }?.let(::homeRowLabel).orEmpty()
            DragRowChip(label = label)
        }
    }
}

/** Human label for a row's drag chip. */
private fun homeRowLabel(kind: HomeRowKind): String = when (kind) {
    HomeRowKind.Time -> "Time"
    HomeRowKind.Date -> "Date"
    HomeRowKind.Weather -> "Weather"
    HomeRowKind.Search -> "Search"
    HomeRowKind.Calendar -> "Calendar"
    HomeRowKind.BatteryStorage -> "Battery & Storage"
    HomeRowKind.Notes -> "Quick notes"
}

/**
 * The insertion index for a pointer at [pointerWindowY] given per-row window
 * [bounds]. Rows are equal-ish height; we pick the row whose vertical midpoint the
 * pointer has passed. Clamped to `0..rowCount`.
 */
private fun nearestRowIndex(
    pointerWindowY: Float,
    bounds: Map<Int, Rect>,
    rowCount: Int,
): Int {
    if (bounds.isEmpty()) return 0
    val sorted = bounds.entries.sortedBy { it.key }
    for ((index, rect) in sorted) {
        if (pointerWindowY < rect.center.y) return index
    }
    return sorted.last().key + 1
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
    appsState: AppsUiState,
    prefsState: PrefsUiState,
    notesState: NotesUiState,
    deviceStatusState: DeviceStatusUiState,
    homeState: HomeState,
    weather: WeatherUiState,
    reducedMotion: Boolean,
    onNotesChange: (String) -> Unit,
    onTapRow: (HomeRowId) -> Unit,
    onLaunchRow: (HomeRowKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        when (kind) {
            // P3: each of these rows **taps to launch** its related app.
            HomeRowKind.Time -> TimeRow(time, reducedMotion, onLaunch = { onLaunchRow(kind) })
            HomeRowKind.Date -> DateRow(date, onLaunch = { onLaunchRow(kind) })
            HomeRowKind.Weather -> WeatherRow(weather, reducedMotion, isVisible = true, onLaunch = { onLaunchRow(kind) })
            // Q2: tap launches; the in-place expand moves to **long-press**.
            HomeRowKind.Search -> SearchRow(
                focused = homeState.isSearching,
                onClick = { onLaunchRow(kind) },
                onLongClick = { onTapRow(HomeRowId.Search) },
            )
            HomeRowKind.Calendar -> CalendarRowContent(
                dayOfMonth = date.day,
                weekday = date.weekday,
                month = date.month,
                events = emptyList(),
            )
            HomeRowKind.BatteryStorage -> BatteryStorageRowContent(
                batteryPercent = deviceStatusState.deviceStatus.batteryPercent,
                storageUsedPercent = deviceStatusState.deviceStatus.storageUsedPercent,
                storageFreeLabel = deviceStatusState.deviceStatus.storageFreeBytes?.let(::formatBytes),
            )
            HomeRowKind.Notes -> NotesRow(
                text = notesState.notes,
                expanded = homeState.isNotesOpen,
                onTextChange = onNotesChange,
                onLongClick = { onTapRow(HomeRowId.Notes) },
            )
        }
    }
}

@Composable
private fun TimeRow(time: String, reducedMotion: Boolean, onLaunch: () -> Unit) {
    val colors = MaterialTheme.softColors
    HomeRow(
        showDivider = false,
        onClick = onLaunch,
        onClickLabel = stringResource(R.string.home_open_clock),
        contentDescription = stringResource(R.string.home_time_a11y),
    ) {
        AnimatedClockText(
            time = time,
            reducedMotion = reducedMotion,
            color = colors.textPrimary,
            modifier = Modifier.padding(vertical = Spacing.xl),
        )
    }
}

@Composable
private fun AnimatedClockText(
    time: String,
    reducedMotion: Boolean,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        time.forEachIndexed { index, character ->
            androidx.compose.runtime.key(index) {
                AnimatedContent(
                    targetState = character,
                    transitionSpec = {
                        if (reducedMotion) {
                            EnterTransition.None togetherWith ExitTransition.None
                        } else {
                            (slideInVertically(
                                animationSpec = MotionTokens.clockDigit(),
                                initialOffsetY = { it / 2 },
                            ) + fadeIn(MotionTokens.clockDigit())) togetherWith
                                (slideOutVertically(
                                    animationSpec = MotionTokens.clockDigit(),
                                    targetOffsetY = { -it / 2 },
                                ) + fadeOut(MotionTokens.clockDigit()))
                        }
                    },
                    label = "clockDigit_$index",
                ) { digit ->
                    Text(text = digit.toString(), style = ClockLarge, color = color)
                }
            }
        }
    }
}

@Composable
private fun DateRow(date: HomeDate, onLaunch: () -> Unit) {
    val colors = MaterialTheme.softColors
    HomeRow(
        showDivider = false,
        onClick = onLaunch,
        onClickLabel = stringResource(R.string.home_open_calendar),
        contentDescription = stringResource(R.string.home_date_a11y),
    ) {
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
private fun WeatherRow(weather: WeatherUiState, reducedMotion: Boolean, isVisible: Boolean, onLaunch: () -> Unit) {
    val colors = MaterialTheme.softColors
    HomeRow(
        showDivider = false,
        contentDescription = stringResource(R.string.home_weather_a11y),
        onClick = onLaunch,
        onClickLabel = stringResource(R.string.home_open_weather),
    ) {
        Row(
            modifier = Modifier.padding(vertical = Spacing.xl),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val iconScale = rememberWeatherScale(reducedMotion, isVisible)
            AnimatedContent(
                targetState = weather.condition,
                transitionSpec = {
                    if (reducedMotion) {
                        fadeIn(tween(80)) togetherWith fadeOut(tween(80))
                    } else {
                        (fadeIn(MotionTokens.weatherTransition()) + scaleIn(MotionTokens.weatherTransition(), initialScale = 0.94f)) togetherWith
                            (fadeOut(MotionTokens.weatherTransition()) + scaleOut(MotionTokens.weatherTransition(), targetScale = 0.96f))
                    }
                },
                label = "weatherCondition",
            ) { condition ->
                LineIconImage(
                    icon = condition.weatherIcon(),
                    size = Dimens.searchRowIcon,
                    tint = colors.statusText,
                    contentDescription = condition.name,
                    modifier = Modifier.scale(iconScale),
                )
            }
            Spacer(Modifier.width(Spacing.md))
            Text(text = weather.temperatureLabel, style = RowDisplay, color = colors.statusText)
        }
    }
}

@Composable
private fun rememberWeatherScale(reducedMotion: Boolean, weatherRowVisible: Boolean): Float {
    // P8: Skip animation entirely if reduced motion or row not visible
    if (reducedMotion || !weatherRowVisible) return 1f
    
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "weatherAmbient")
    val scale by transition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = tween(4_000, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "weatherBreathing",
    )
    return scale
}

@Composable
private fun SearchRow(
    focused: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val colors = MaterialTheme.softColors
    val placeholder = stringResource(R.string.home_search_placeholder).toSpacedLetters()
    val searching = stringResource(R.string.home_searching).toSpacedLetters()
    HomeRow(
        showDivider = false,
        onClick = onClick,
        onClickLabel = stringResource(R.string.home_open_search),
        onLongClick = onLongClick,
        onLongClickLabel = stringResource(R.string.home_search_in_place),
        contentDescription = stringResource(R.string.home_find_something),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.xl),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (focused) searching else placeholder,
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

/**
 * Quick-notes row (P2 / E5). **Long-press** toggles Idle <-> NOTES (Q2: tap is reserved
 * for launching; notes has no app target so its tap is a no-op). When expanded the row
 * grows in-place (same motion family as the search row) and reveals the editor.
 */
@Composable
private fun NotesRow(
    text: String,
    expanded: Boolean,
    onTextChange: (String) -> Unit,
    onLongClick: () -> Unit,
) {
    val grow by animateDpAsState(
        targetValue = if (expanded) 12.dp else 0.dp,
        animationSpec = MotionTokens.notesExpand(),
        label = "notesExpand",
    )
    // The notes editor itself handles taps/focus; the row-level long-press toggles expand.
    HomeRow(
        showDivider = false,
        contentDescription = stringResource(R.string.home_notes_a11y),
        onLongClick = onLongClick,
        onLongClickLabel = stringResource(R.string.home_expand_notes),
    ) {
        NotesRowContent(
            text = text,
            expanded = expanded,
            onTextChange = onTextChange,
            onClick = onLongClick,
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
    railItems: List<RailItemId>,
    railApps: Map<String, AppIconUi>,
    activePack: com.softhome.core.model.IconPack?,
    drawableLoader: IconPackDrawableLoader?,
    onReorder: (String, Int) -> Unit,
    onLaunchApp: (AppInfo) -> Unit,
    themeMode: ThemeMode = ThemeMode.System,
    onThemeChange: (ThemeMode) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.softColors
    val railA11y = stringResource(R.string.home_rail_a11y)
    // SEARCH state: the rail "expands" (RAIL_SLIDE 220ms) -- icons nudge in.
    val slide by animateDpAsState(
        targetValue = if (expanded) 0.dp else (-6).dp,
        animationSpec = MotionTokens.railSlide(),
        label = "railSlide",
    )
    // Long-press menu state (F3): which rail shortcut is being configured.
    var menuFor by remember { mutableStateOf<RailShortcut?>(null) }
    val dragController = rememberDragController()
    val dragState = dragController.state
    var railOriginInWindow by remember { mutableStateOf(Offset.Zero) }
    val ordered = remember(railItems) { RailConfigLogic.sanitize(railItems) }
    val draggingName = dragState.draggingId?.removePrefix("rail:")
    val draggingIndex = ordered.indexOfFirst { it.storageId == draggingName }
    val hoveredIndex = dragState.hoveredTargetId
        ?.removePrefix("rail:")
        ?.toIntOrNull()
        ?.takeIf { it in ordered.indices }
    val hoveredEnd = dragState.hoveredTargetId == "rail:end"
    val slotShift = Dimens.railTouchTarget + Spacing.railGap

    Box(
        modifier = modifier
            .width(Dimens.railWidth)
            .fillMaxHeight()
            .semantics { contentDescription = railA11y },
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.systemBars))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(
                        RoundedCornerShape(
                            topStart = Dimens.railCornerRadius,
                            bottomStart = Dimens.railCornerRadius,
                        ),
                    )
                    .background(colors.railBg)
                    .onGloballyPositioned { railOriginInWindow = it.boundsInWindow().topLeft },
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = Dimens.railPadTop, bottom = Dimens.railPadBottom),
                    verticalArrangement = Arrangement.spacedBy(Spacing.railGap),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Theme toggle at the very top
                    ThemeToggleButton(
                        currentMode = themeMode,
                        onThemeChange = onThemeChange,
                        modifier = Modifier.offset(y = slide),
                    )
                    
                    // Filter out Sparkles (theme toggle) from the rail items
                    val itemsWithoutSparkles = ordered.filterNot { item ->
                        (item as? RailItemId.System)?.shortcut == com.softhome.core.model.RailShortcutId.Sparkles
                    }
                    
                    itemsWithoutSparkles.forEachIndexed { index, item ->
                        androidx.compose.runtime.key(item.storageId) {
                            val sourceIndex = ordered.indexOf(item)
                            val shortcut = (item as? RailItemId.System)?.shortcut
                                ?.let(::toRailShortcut)
                            val app = (item as? RailItemId.App)?.let { railApps[it.componentKey] }
                            val appInfo = app?.app
                            val enabled = when {
                                shortcut != null -> resolver.intentFor(shortcut) != null
                                appInfo != null -> true
                                else -> false
                            }
                            val showDropIndicator = dragState.isDragging &&
                                hoveredIndex == index && draggingIndex != index
                            val shiftTarget = when {
                                draggingIndex < 0 || hoveredIndex == null -> 0.dp
                                draggingIndex < hoveredIndex && index in (draggingIndex + 1)..hoveredIndex -> -slotShift
                                draggingIndex > hoveredIndex && index in hoveredIndex until draggingIndex -> slotShift
                                else -> 0.dp
                            }
                            val shift by animateDpAsState(
                                targetValue = shiftTarget,
                                animationSpec = MotionTokens.dragReorder(),
                                label = "railReorderShift_${item.storageId}",
                            )
                            if (showDropIndicator) {
                                RailDropIndicator()
                            }
                            val railInteractionSource = remember { MutableInteractionSource() }
                            Box(
                                modifier = Modifier
                                    .dropTarget("rail:$index", dragController)
                                    .offset(y = slide + shift)
                                    .dragSourceAlpha(draggingName == item.storageId)
                                    .warmPress(railInteractionSource)
                                    .dragSource(
                                        id = "rail:${item.storageId}",
                                        controller = dragController,
                                        interactionSource = railInteractionSource,
                                        onTap = when {
                                            shortcut == RailShortcut.PanelLeft -> onOpenSettings
                                            shortcut != null && enabled -> { { onShortcut(shortcut) } }
                                            appInfo != null -> { { onLaunchApp(appInfo) } }
                                            else -> null
                                        },
                                        onLongPress = { if (shortcut != null) menuFor = shortcut },
                                        onDrop = { target, _ ->
                                            val targetIndex = target
                                                ?.removePrefix("rail:")
                                                ?.toIntOrNull()
                                            val destination = when {
                                                target == "rail:end" -> ordered.size
                                                targetIndex == null -> return@dragSource
                                                sourceIndex < targetIndex -> targetIndex - 1
                                                else -> targetIndex
                                            }
                                            onReorder(item.storageId, destination)
                                        },
                                    ),
                            ) {
                                // Gesture ownership lives on dragSource so tap, stationary
                                // long-press, and long-press+drag cannot fire together.
                                if (app != null && drawableLoader != null) {
                                    DrawerAppIcon(
                                        resolved = app.resolved,
                                        size = Dimens.railIcon,
                                        activePack = activePack,
                                        drawableLoader = drawableLoader,
                                        category = app.app.category,
                                        colorToken = app.drawerColorToken,
                                        showShadow = false,
                                        normalizedContentSize = Dimens.railIcon,
                                        contentDescription = app.app.label,
                                    )
                                } else {
                                    RailIcon(
                                        icon = shortcut?.lineIcon()
                                            ?: app?.let { LineIcon.fromLucide(it.resolved.symbolName) }
                                            ?: LineIcon.AppWindow,
                                        contentDescription = shortcut?.label() ?: app?.app?.label ?: "Unavailable app",
                                    )
                                }
                            }
                        }
                    }
                    // A forgiving zone below the last icon lets a drag append an
                    // item instead of always inserting before the final icon.
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .dropTarget("rail:end", dragController),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        if (hoveredEnd && draggingIndex != ordered.lastIndex) {
                            RailDropIndicator()
                        }
                    }
                }
                // The preview follows the pointer using only transform/opacity-friendly motion.
                DragPreviewLayer(controller = dragController, windowOrigin = railOriginInWindow) { id ->
                    val item = ordered.firstOrNull { it.storageId == id.removePrefix("rail:") }
                    val shortcut = (item as? RailItemId.System)?.shortcut?.let(::toRailShortcut)
                    val app = (item as? RailItemId.App)?.let { railApps[it.componentKey] }
                    if (app != null && drawableLoader != null) {
                        DrawerAppIcon(
                            resolved = app.resolved,
                            size = Dimens.railIcon,
                            activePack = activePack,
                            drawableLoader = drawableLoader,
                            category = app.app.category,
                            colorToken = app.drawerColorToken,
                            showShadow = false,
                            normalizedContentSize = Dimens.railIcon,
                            contentDescription = app.app.label,
                        )
                    } else {
                        RailIcon(
                            icon = shortcut?.lineIcon()
                                ?: app?.let { LineIcon.fromLucide(it.resolved.symbolName) }
                                ?: LineIcon.AppWindow,
                            contentDescription = shortcut?.label() ?: app?.app?.label ?: "Unavailable app",
                        )
                    }
                }
            }
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

@Composable
private fun RailDropIndicator() {
    Box(
        modifier = Modifier
            .padding(vertical = Dimens.railDropIndicatorGap)
            .width(Dimens.railDropIndicatorWidth)
            .height(Dimens.dragInsertionThickness)
            .background(MaterialTheme.softColors.accent.copy(alpha = 0.52f)),
    )
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

private fun toRailShortcut(id: RailShortcutId): RailShortcut =
    RailShortcut.entries.first { it.name == id.name }

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

@Composable
private fun rememberMinuteKey(): Long {
    var minuteKey by remember { mutableStateOf(System.currentTimeMillis() / 60_000L) }
    LaunchedEffect(Unit) {
        while (isActive) {
            val now = System.currentTimeMillis()
            val untilNextMinute = 60_000L - (now % 60_000L)
            delay(untilNextMinute.coerceAtLeast(100L))
            minuteKey = System.currentTimeMillis() / 60_000L
        }
    }
    return minuteKey
}

private fun homeTime(now: Date): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)

private fun homeDate(now: Date): HomeDate {
    val day = SimpleDateFormat("d", Locale.getDefault()).format(now)
    val weekday = SimpleDateFormat("EEEE", Locale.getDefault()).format(now)
        .lowercase(Locale.getDefault()).toSpacedCaps()
    val month = SimpleDateFormat("MMMM", Locale.getDefault()).format(now)
        .uppercase(Locale.getDefault()).toSpacedCaps()
    return HomeDate(day = day, weekday = weekday, month = month)
}

private fun WeatherCondition.weatherIcon(): LineIcon = when (this) {
    WeatherCondition.Clear -> LineIcon.Sun
    WeatherCondition.Cloudy -> LineIcon.CloudSun
    WeatherCondition.Rain -> LineIcon.CloudSun
    WeatherCondition.Night -> LineIcon.CloudSun
}

/** "tuesday" -> "t u e s d a y" (the design tracks each letter). */
private fun String.toSpacedCaps(): String =
    trim().map { it.toString() }.joinToString(" ")

/**
 * Builds the design's letter-spaced, lowercase micro-label ("find something" ->
 * "f i n d  s o m e t h i n g"). Letters get one space; words get a wider two-space
 * gap. Applied in code so the localized resource stays plain text.
 */
private fun String.toSpacedLetters(): String =
    trim().lowercase(Locale.getDefault())
        .split(" ").filter { it.isNotEmpty() }
        .joinToString("  ") { word -> word.map { it.toString() }.joinToString(" ") }


@Composable
private fun rememberRailResolver(context: Context): RailShortcutResolver =
    androidx.compose.runtime.remember(context) { RailShortcutResolver.forContext(context) }

/**
 * P3: builds the home-row launch resolver bound to the real `PackageManager`, so a row
 * tap opens the right app through the fallback chain in [RowLaunchResolver].
 */
@Composable
private fun rememberRowResolver(context: Context): RowLaunchResolver =
    androidx.compose.runtime.remember(context) {
        RowLaunchResolver.forResolver { intent ->
            @Suppress("DEPRECATION")
            context.packageManager.resolveActivity(intent, 0)?.activityInfo?.let {
                android.content.ComponentName(it.packageName, it.name)
            }
        }
    }
