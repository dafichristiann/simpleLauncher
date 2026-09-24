package com.softhome.launcher.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softhome.core.data.repository.AppRepository
import com.softhome.core.data.repository.PrefsRepository
import com.softhome.core.model.AppInfo
import com.softhome.core.model.GridConfig
import com.softhome.core.model.HomeRowKind
import com.softhome.core.model.HomeRowLogic
import com.softhome.core.model.HomeRowPref
import com.softhome.core.model.IconPack
import com.softhome.core.model.LauncherPrefs
import com.softhome.core.model.SpacingScale
import com.softhome.core.model.ThemeMode
import com.softhome.feature.iconpack.data.IconPackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One hidden app, resolved for display in the "Hidden apps" manager. */
data class HiddenApp(val componentKey: String, val label: String)

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.System,
    val activePackName: String? = null,
    val grid: GridConfig = GridConfig.Default,
    val spacing: SpacingScale = SpacingScale.Normal,
    val homeRows: List<HomeRowPref> = HomeRowLogic.default(),
    val hiddenApps: List<HiddenApp> = emptyList(),
) {
    val visibleRowOrder: List<HomeRowKind> get() = homeRows.map { it.kind }
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefsRepository: PrefsRepository,
    private val iconPackRepository: IconPackRepository,
    private val appRepository: AppRepository,
) : ViewModel() {

    private val appsFlow = MutableStateFlow<List<AppInfo>>(emptyList())

    val uiState: StateFlow<SettingsUiState> = combine(
        prefsRepository.prefs, iconPackRepository.activePack, appsFlow,
    ) { prefs: LauncherPrefs, pack: IconPack?, apps: List<AppInfo> ->
        SettingsUiState(
            themeMode = prefs.darkTheme,
            activePackName = pack?.name,
            grid = prefs.grid,
            spacing = prefs.spacing,
            homeRows = prefs.homeRows,
            hiddenApps = prefs.hiddenApps.map { key ->
                HiddenApp(key, apps.firstOrNull { it.componentKey == key }?.label ?: key)
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    init {
        viewModelScope.launch { appsFlow.value = appRepository.getInstalledApps() }
    }

    // --- Appearance -----------------------------------------------------------

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { prefsRepository.setDarkTheme(mode) }

    fun setGridColumns(columns: Int) = viewModelScope.launch {
        prefsRepository.setGrid(uiState.value.grid.copy(columns = columns.coerceIn(3, 7)))
    }

    fun setSpacing(scale: SpacingScale) = viewModelScope.launch { prefsRepository.setSpacing(scale) }

    fun unhideApp(componentKey: String) = viewModelScope.launch { prefsRepository.unhideApp(componentKey) }

    // --- Widgets (home rows) --------------------------------------------------

    fun toggleHomeRow(kind: HomeRowKind) = viewModelScope.launch {
        prefsRepository.setHomeRows(HomeRowLogic.toggle(uiState.value.homeRows, kind))
    }

    fun moveHomeRowUp(kind: HomeRowKind) = viewModelScope.launch {
        prefsRepository.setHomeRows(HomeRowLogic.moveUp(uiState.value.homeRows, kind))
    }

    fun moveHomeRowDown(kind: HomeRowKind) = viewModelScope.launch {
        prefsRepository.setHomeRows(HomeRowLogic.moveDown(uiState.value.homeRows, kind))
    }
}
