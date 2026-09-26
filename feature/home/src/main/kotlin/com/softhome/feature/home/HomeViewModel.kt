package com.softhome.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.Stable
import com.softhome.core.data.repository.AppRepository
import com.softhome.core.data.repository.DeviceStatusRepository
import com.softhome.core.data.repository.NotesRepository
import com.softhome.core.data.repository.PrefsRepository
import com.softhome.core.model.AppInfo
import com.softhome.core.model.DeviceStatusSnapshot
import com.softhome.core.model.DrawerIconAssignment
import com.softhome.core.model.GridConfig
import com.softhome.core.model.HomeRowDropResolver
import com.softhome.core.model.HomeRowKind
import com.softhome.core.model.HomeRowLogic
import com.softhome.core.model.HomeRowPref
import com.softhome.core.model.IconPack
import com.softhome.core.model.IconSource
import com.softhome.core.model.LauncherPrefs
import com.softhome.core.model.RailOrderLogic
import com.softhome.core.model.RailConfigLogic
import com.softhome.core.model.RailItemIdCodec
import com.softhome.core.model.ResolvedIcon
import com.softhome.core.model.SpacingScale
import com.softhome.core.model.ThemeMode
import com.softhome.feature.iconpack.data.IconPackDrawableLoader
import com.softhome.feature.iconpack.data.IconPackRepository
import com.softhome.feature.iconpack.domain.IconBitmapProvider
import com.softhome.feature.iconpack.domain.IconMasker
import com.softhome.feature.iconpack.domain.DrawerIconColor
import com.softhome.feature.iconpack.domain.IconResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One icon slot on the home grid (or the drawer). */
data class AppIconUi(
    val app: AppInfo,
    val resolved: ResolvedIcon,
    val symbolName: String,
    /** Same glyph/color assignment used by the All Apps drawer. */
    val drawerColorToken: DrawerIconColor.Token = DrawerIconColor.Token.Neutral,
    val showBadge: Boolean = false,
)

@Stable
data class AppsUiState(
    val apps: List<AppIconUi> = emptyList(),
    val loading: Boolean = true,
    val railApps: Map<String, AppIconUi> = emptyMap(),
)

@Stable
data class PrefsUiState(
    val grid: GridConfig = GridConfig.Default,
    val spacing: SpacingScale = SpacingScale.Normal,
    val themeMode: ThemeMode = ThemeMode.System,
    val activePack: IconPack? = null,
    val activePackName: String? = null,
)

@Stable
data class NotesUiState(
    val notes: String = "",
)

@Stable
data class RailUiState(
    val railOrder: List<String> = RailOrderLogic.DEFAULT,
    val railItems: List<com.softhome.core.model.RailItemId> = RailConfigLogic.DEFAULT_ITEMS,
)

@Stable
data class DeviceStatusUiState(
    val deviceStatus: DeviceStatusSnapshot = DeviceStatusSnapshot.EMPTY,
)

@Stable
data class HomeRowsUiState(
    val homeRows: List<HomeRowPref> = HomeRowLogic.default(),
) {
    /** Rows the home should render, in order (locked rows always included). */
    val visibleRows: List<HomeRowKind> get() = HomeRowLogic.visibleInOrder(homeRows)
}

@Stable
data class HomeUiState(
    val apps: AppsUiState = AppsUiState(),
    val prefs: PrefsUiState = PrefsUiState(),
    val notes: NotesUiState = NotesUiState(),
    val rail: RailUiState = RailUiState(),
    val deviceStatus: DeviceStatusUiState = DeviceStatusUiState(),
    val homeRows: HomeRowsUiState = HomeRowsUiState(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val prefsRepository: PrefsRepository,
    private val notesRepository: NotesRepository,
    private val deviceStatusRepository: DeviceStatusRepository,
    val iconPackRepository: IconPackRepository,
    val drawableLoader: IconPackDrawableLoader,
    val bitmapProvider: IconBitmapProvider,
    private val iconResolver: IconResolver,
    private val packageEventMonitor: com.softhome.core.data.packages.PackageEventMonitor,
    private val appInventory: com.softhome.core.data.packages.AppInventoryCoordinator,
) : ViewModel() {

    private val appsFlow = MutableStateFlow<List<AppInfo>>(emptyList())
    private val loadingFlow = MutableStateFlow(true)
    private val deviceStatusFlow = MutableStateFlow(DeviceStatusSnapshot.EMPTY)

    // SPLIT FLOW 1: Apps (only recomposes Home grid + Rail)
    val appsState: StateFlow<AppsUiState> = combine(
        appsFlow, loadingFlow, iconPackRepository.activePack, prefsRepository.prefs
    ) { apps, loading, pack, prefs ->
        val drawerAssignments = drawerAssignments(apps, prefs)
        val appIcons = apps.associate { app ->
            app.componentKey to toUi(
                app = app,
                prefs = prefs,
                pack = pack,
                drawerAssignment = drawerAssignments[app.componentKey],
            )
        }
        AppsUiState(
            apps = appIcons.values.toList(),
            loading = loading,
            railApps = appIcons,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppsUiState(),
    )

    // SPLIT FLOW 2: Prefs (only recomposes Settings panels + Theme)
    val prefsState: StateFlow<PrefsUiState> = combine(
        prefsRepository.prefs, iconPackRepository.activePack
    ) { prefs, pack ->
        PrefsUiState(
            grid = prefs.grid,
            spacing = prefs.spacing,
            themeMode = prefs.darkTheme,
            activePack = pack,
            activePackName = pack?.name,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PrefsUiState(),
    )

    // SPLIT FLOW 3: Notes (only recomposes Notes row)
    val notesState: StateFlow<NotesUiState> = notesRepository.notes
        .map { notes -> NotesUiState(notes = notes.body) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NotesUiState(),
        )

    // SPLIT FLOW 4: Rail (only recomposes Rail)
    val railState: StateFlow<RailUiState> = prefsRepository.prefs
        .map { prefs ->
            RailUiState(
                railOrder = prefs.railOrder,
                railItems = RailConfigLogic.sanitize(prefs.railItems),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RailUiState(),
        )

    // SPLIT FLOW 5: DeviceStatus (only recomposes Status row)
    val deviceStatusState: StateFlow<DeviceStatusUiState> = deviceStatusFlow
        .map { status -> DeviceStatusUiState(deviceStatus = status) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DeviceStatusUiState(),
        )

    // SPLIT FLOW 6: HomeRows (only recomposes Row visibility)
    val homeRowsState: StateFlow<HomeRowsUiState> = prefsRepository.prefs
        .map { prefs -> HomeRowsUiState(homeRows = prefs.homeRows) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeRowsUiState(),
        )

    // Unified state for backward compatibility (low-priority subscribers only)
    val uiState: StateFlow<HomeUiState> = combine(
        appsState,
        prefsState,
        notesState,
        railState,
        deviceStatusState,
        homeRowsState,
    ) { args: Array<*> ->
        @Suppress("UNCHECKED_CAST")
        HomeUiState(
            apps = args[0] as AppsUiState,
            prefs = args[1] as PrefsUiState,
            notes = args[2] as NotesUiState,
            rail = args[3] as RailUiState,
            deviceStatus = args[4] as DeviceStatusUiState,
            homeRows = args[5] as HomeRowsUiState,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    init {
        refreshApps()
        refreshDeviceStatus()
        // QW1: reflect installs / uninstalls live instead of only at process start.
        packageEventMonitor.start()
        viewModelScope.launch {
            packageEventMonitor.changes.collect { refreshApps() }
        }
    }

    fun refreshApps() {
        viewModelScope.launch {
            loadingFlow.value = true
            // reload + prune dead component keys (uninstalled apps) in one pass.
            appsFlow.value = appInventory.refreshAndPrune()
            loadingFlow.value = false
        }
    }

    /** Re-read real battery/storage (called on ON_RESUME -- decision P2-3 / #55). */
    fun refreshDeviceStatus() {
        viewModelScope.launch {
            deviceStatusFlow.value = deviceStatusRepository.snapshot()
        }
    }

    /** Persist the quick-notes body (decision P2-2). */
    fun setNotes(body: String) {
        viewModelScope.launch { notesRepository.setBody(body) }
    }

    // --- P3 (G): settings-backed setters --------------------------------------

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { prefsRepository.setDarkTheme(mode) }
    }

    fun setSpacing(scale: SpacingScale) {
        viewModelScope.launch { prefsRepository.setSpacing(scale) }
    }

    fun setHomeRows(rows: List<HomeRowPref>) {
        viewModelScope.launch { prefsRepository.setHomeRows(rows) }
    }

    fun toggleHomeRow(kind: HomeRowKind) {
        viewModelScope.launch {
            val current = homeRowsState.value.homeRows
            prefsRepository.setHomeRows(HomeRowLogic.toggle(current, kind))
        }
    }

    fun moveHomeRowUp(kind: HomeRowKind) {
        viewModelScope.launch {
            prefsRepository.setHomeRows(HomeRowLogic.moveUp(homeRowsState.value.homeRows, kind))
        }
    }

    fun moveHomeRowDown(kind: HomeRowKind) {
        viewModelScope.launch {
            prefsRepository.setHomeRows(HomeRowLogic.moveDown(homeRowsState.value.homeRows, kind))
        }
    }

    /**
      * P4a: drag a home row to [targetIndex] (a position in the resulting list). Same
      * persistence path as the up/down buttons; the resolution is the pure
      * [HomeRowLogic.move] (see `HomeRowDropResolver`).
      */
    fun reorderHomeRow(kind: HomeRowKind, targetIndex: Int) {
        val next = HomeRowDropResolver.reorder(homeRowsState.value.homeRows, kind, targetIndex)
        if (next != homeRowsState.value.homeRows) {
            viewModelScope.launch { prefsRepository.setHomeRows(next) }
        }
    }

    /** P7: reorder a right-rail shortcut and persist it through the existing DataStore. */
    fun reorderRail(shortcutName: String, targetIndex: Int) {
        val item = RailItemIdCodec.parse(shortcutName) ?:
            com.softhome.core.model.RailShortcutId.entries
                .firstOrNull { it.name == shortcutName }
                ?.let(com.softhome.core.model.RailItemId::System)
        if (item != null) {
            val next = RailConfigLogic.move(railState.value.railItems, item, targetIndex)
            if (next != railState.value.railItems) {
                viewModelScope.launch { prefsRepository.setRailItems(next) }
            }
        }
    }

    fun launchApp(app: AppInfo) = appRepository.launchApp(app)


    private fun toUi(
        app: AppInfo,
        prefs: LauncherPrefs,
        pack: IconPack?,
        drawerAssignment: DrawerIconAssignment.Assignment? = null,
    ): AppIconUi {
        val source = iconResolver.resolve(
            app = app,
            activePack = pack,
            overrides = prefs.iconOverrides,
            maskUnsupported = prefs.maskUnsupportedApps,
        )
        // All Apps is the source of truth for application glyphs. The assignment is
        // deterministic over the complete visible app list, so the same component gets
        // the same mapped glyph on the drawer and the sidebar.
        val symbol = drawerAssignment?.glyph
            ?.let { com.softhome.core.designsystem.atom.LineIcon.fromLucide(it).name }
            ?: IconMasker.symbolFor(app)
        val drawableName = when (source) {
            is IconSource.FromPack -> source.drawableName
            is IconSource.Override -> source.drawableName
            is IconSource.Glyph -> null
            IconSource.AutoMask, IconSource.System -> null
        }
        // P4b: a glyph override carries both the chosen glyph and its color token.
        val (symbolName, overrideToken) = when (source) {
            is IconSource.Glyph -> source.symbolName to source.colorToken
            else -> symbol to null
        }
        val resolved = ResolvedIcon(
            source = source,
            drawableName = drawableName,
            symbolName = symbolName,
            componentKey = app.componentKey,
            packageName = app.packageName,
            className = app.className,
            overrideColorToken = overrideToken,
        )
        val drawerColor = drawerAssignment?.colorToken
            ?.let(DrawerIconColor::tokenForName)
            ?: DrawerIconColor.tokenFor(app.category, symbol)
        return AppIconUi(
            app = app,
            resolved = resolved,
            symbolName = symbolName,
            drawerColorToken = drawerColor,
        )
    }

    /** Mirrors AppDrawerViewModel's assignment inputs exactly. */
    private fun drawerAssignments(
        apps: List<AppInfo>,
        prefs: LauncherPrefs,
    ): Map<String, DrawerIconAssignment.Assignment> {
        val visibleApps = apps.filterNot { it.componentKey in prefs.hiddenApps }
        val byPackage = visibleApps.associateBy { it.packageName }
        return DrawerIconAssignment.assign(
            identities = visibleApps.map { it.componentKey to it.packageName },
            heuristicGlyph = { pkg -> byPackage[pkg]?.let(IconMasker::symbolFor) ?: "AppWindow" },
            heuristicColor = { pkg ->
                val app = byPackage[pkg]
                DrawerIconColor.nameOf(
                    DrawerIconColor.tokenFor(
                        app?.category,
                        app?.let(IconMasker::symbolFor) ?: "AppWindow",
                    ),
                )
            },
            glyphFallbacks = DrawerIconAssignment.DEFAULT_GLYPH_FALLBACKS,
        )
    }
}
