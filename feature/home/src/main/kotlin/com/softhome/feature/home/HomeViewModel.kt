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
import com.softhome.core.model.IconPack
import com.softhome.core.model.IconSource
import com.softhome.core.model.LauncherPrefs
import com.softhome.core.model.ResolvedIcon
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
