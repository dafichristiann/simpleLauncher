package com.softhome.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softhome.core.data.repository.AppRepository
import com.softhome.core.data.repository.DeviceStatusRepository
import com.softhome.core.data.repository.NotesRepository
import com.softhome.core.data.repository.PrefsRepository
import com.softhome.core.model.AppInfo
import com.softhome.core.model.DeviceStatusSnapshot
import com.softhome.core.model.GridConfig
import com.softhome.core.model.HomeRowKind
import com.softhome.core.model.HomeRowLogic
import com.softhome.core.model.HomeRowPref
import com.softhome.core.model.IconPack
import com.softhome.core.model.IconSource
import com.softhome.core.model.LauncherPrefs
import com.softhome.core.model.ResolvedIcon
import com.softhome.core.model.SpacingScale
import com.softhome.core.model.ThemeMode
import com.softhome.feature.iconpack.data.IconPackDrawableLoader
import com.softhome.feature.iconpack.data.IconPackRepository
import com.softhome.feature.iconpack.domain.IconBitmapProvider
import com.softhome.feature.iconpack.domain.IconMasker
import com.softhome.feature.iconpack.domain.IconResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One icon slot on the home grid (or the drawer). */
data class AppIconUi(
    val app: AppInfo,
    val resolved: ResolvedIcon,
    val symbolName: String,
    val showBadge: Boolean = false,
)

data class HomeUiState(
    val apps: List<AppIconUi> = emptyList(),
    val grid: GridConfig = GridConfig.Default,
    val loading: Boolean = true,
    val activePack: IconPack? = null,
    val activePackName: String? = null,
    /** P2 / E5: persisted quick-notes body. */
    val notes: String = "",
    /** P2 / E4: real device status (battery + storage). */
    val deviceStatus: DeviceStatusSnapshot = DeviceStatusSnapshot.EMPTY,
    /** P3 (G/Widgets): which home rows show and in what order. */
    val homeRows: List<HomeRowPref> = HomeRowLogic.default(),
    /** P3 (G/Appearance): drawer + row spacing preset (Q3). */
    val spacing: SpacingScale = SpacingScale.Normal,
    /** P3 (G/Appearance): theme mode (Light/Dark/System). */
    val themeMode: ThemeMode = ThemeMode.System,
) {
    /** Rows the home should render, in order (locked rows always included). */
    val visibleRows: List<HomeRowKind> get() = HomeRowLogic.visibleInOrder(homeRows)
}

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
) : ViewModel() {

    private val appsFlow = MutableStateFlow<List<AppInfo>>(emptyList())
    private val loadingFlow = MutableStateFlow(true)
    private val deviceStatusFlow = MutableStateFlow(DeviceStatusSnapshot.EMPTY)

    private data class Core(
        val apps: List<AppInfo>,
        val prefs: LauncherPrefs,
        val loading: Boolean,
        val pack: IconPack?,
    )

    val uiState: StateFlow<HomeUiState> = combine(
        appsFlow, prefsRepository.prefs, loadingFlow, iconPackRepository.activePack,
    ) { apps, prefs, loading, pack -> Core(apps, prefs, loading, pack) }
        .combine(notesRepository.notes) { core, notes -> core to notes }
        .combine(deviceStatusFlow) { (core, notes), status ->
            HomeUiState(
                apps = core.apps.map { toUi(it, core.prefs, core.pack) },
                grid = core.prefs.grid,
                loading = core.loading,
                activePack = core.pack,
                activePackName = core.pack?.name,
                notes = notes.body,
                deviceStatus = status,
                homeRows = core.prefs.homeRows,
                spacing = core.prefs.spacing,
                themeMode = core.prefs.darkTheme,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(),
        )

    init {
        refreshApps()
        refreshDeviceStatus()
    }

    fun refreshApps() {
        viewModelScope.launch {
            loadingFlow.value = true
            appsFlow.value = appRepository.getInstalledApps()
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
            val current = uiState.value.homeRows
            prefsRepository.setHomeRows(HomeRowLogic.toggle(current, kind))
        }
    }

    fun moveHomeRowUp(kind: HomeRowKind) {
        viewModelScope.launch {
            prefsRepository.setHomeRows(HomeRowLogic.moveUp(uiState.value.homeRows, kind))
        }
    }

    fun moveHomeRowDown(kind: HomeRowKind) {
        viewModelScope.launch {
            prefsRepository.setHomeRows(HomeRowLogic.moveDown(uiState.value.homeRows, kind))
        }
    }

    fun launchApp(app: AppInfo) = appRepository.launchApp(app)


    private fun toUi(
        app: AppInfo,
        prefs: LauncherPrefs,
        pack: IconPack?,
    ): AppIconUi {
        val source = iconResolver.resolve(
            app = app,
            activePack = pack,
            overrides = prefs.iconOverrides,
            maskUnsupported = prefs.maskUnsupportedApps,
        )
        val symbol = IconMasker.symbolFor(app)
        val drawableName = when (source) {
            is IconSource.FromPack -> source.drawableName
            is IconSource.Override -> source.drawableName
            IconSource.AutoMask, IconSource.System -> null
        }
        val resolved = ResolvedIcon(
            source = source,
            drawableName = drawableName,
            symbolName = symbol,
            componentKey = app.componentKey,
            packageName = app.packageName,
            className = app.className,
        )
        return AppIconUi(app = app, resolved = resolved, symbolName = symbol)
    }
}
