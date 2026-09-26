package com.softhome.launcher.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softhome.core.data.repository.AppRepository
import com.softhome.core.data.repository.BackupRepository
import com.softhome.core.data.repository.BackupResult
import com.softhome.core.data.repository.BackupDecodeResult
import com.softhome.core.data.repository.BackupCodec
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
import com.softhome.core.model.Folder
import com.softhome.core.model.RailConfigLogic
import com.softhome.core.model.RailItemId
import com.softhome.core.model.RailItemIdCodec
import com.softhome.core.model.RailShortcutId
import com.softhome.feature.iconpack.data.IconPackRepository
import com.softhome.launcher.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.UUID

/** One hidden app, resolved for display in the "Hidden apps" manager. */
data class HiddenApp(val componentKey: String, val label: String)

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.System,
    val activePackName: String? = null,
    val grid: GridConfig = GridConfig.Default,
    val spacing: SpacingScale = SpacingScale.Normal,
    val homeRows: List<HomeRowPref> = HomeRowLogic.default(),
    val hiddenApps: List<HiddenApp> = emptyList(),
    /** P4d: transient result text for a backup/restore attempt (null = nothing to show). */
    val backupMessage: String? = null,
    /** P4d: true when a valid backup file is loaded and awaiting the user's confirm. */
    val pendingRestore: Boolean = false,
    val railItems: List<RailItemId> = RailConfigLogic.DEFAULT_ITEMS,
    val installedApps: List<AppInfo> = emptyList(),
) {
    val visibleRowOrder: List<HomeRowKind> get() = homeRows.map { it.kind }
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefsRepository: PrefsRepository,
    private val iconPackRepository: IconPackRepository,
    private val appRepository: AppRepository,
    private val backupRepository: BackupRepository,
    private val folderRepository: com.softhome.core.data.repository.FolderRepository,
    private val notesRepository: com.softhome.core.data.repository.NotesRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val appsFlow = MutableStateFlow<List<AppInfo>>(emptyList())
    private val backupMessage = MutableStateFlow<String?>(null)
    private val pendingRestore = MutableStateFlow(false)

    /** P4d: a validated backup document held until the user confirms the destructive import. */
    private var pendingDocument: com.softhome.core.model.BackupDocument? = null

    val uiState: StateFlow<SettingsUiState> = combine(
        prefsRepository.prefs, iconPackRepository.activePack, appsFlow, backupMessage, pendingRestore,
    ) { prefs: LauncherPrefs, pack: IconPack?, apps: List<AppInfo>, message: String?, pending: Boolean ->
        SettingsUiState(
            themeMode = prefs.darkTheme,
            activePackName = pack?.name,
            grid = prefs.grid,
            spacing = prefs.spacing,
            homeRows = prefs.homeRows,
            hiddenApps = prefs.hiddenApps.map { key ->
                HiddenApp(key, apps.firstOrNull { it.componentKey == key }?.label ?: key)
            },
            railItems = RailConfigLogic.sanitize(prefs.railItems),
            installedApps = apps,
            backupMessage = message,
            pendingRestore = pending,
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

    fun addRailApp(componentKey: String) {
        val next = RailConfigLogic.add(uiState.value.railItems, RailItemId.App(componentKey))
        if (next != uiState.value.railItems) viewModelScope.launch { prefsRepository.setRailItems(next) }
    }

    fun addRailShortcut(shortcut: RailShortcutId) {
        val next = RailConfigLogic.add(uiState.value.railItems, RailItemId.System(shortcut))
        if (next != uiState.value.railItems) viewModelScope.launch { prefsRepository.setRailItems(next) }
    }

    fun removeRailItem(item: RailItemId) {
        val next = RailConfigLogic.remove(uiState.value.railItems, item)
        if (next != uiState.value.railItems) viewModelScope.launch { prefsRepository.setRailItems(next) }
    }

    fun moveRailItem(storageId: String, targetIndex: Int) {
        val item = RailItemIdCodec.parse(storageId) ?: return
        val next = RailConfigLogic.move(uiState.value.railItems, item, targetIndex)
        if (next != uiState.value.railItems) viewModelScope.launch { prefsRepository.setRailItems(next) }
    }

    fun resetRailItems() {
        viewModelScope.launch { prefsRepository.setRailItems(RailConfigLogic.DEFAULT_ITEMS) }
    }

    fun createFolder() = viewModelScope.launch {
        val current = folderRepository.folders.first()
        folderRepository.save(
            current + Folder(
                id = UUID.randomUUID().toString(),
                name = context.getString(R.string.folder_default_name),
                apps = emptyList(),
            ),
        )
    }

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

    // --- Backup & restore (P4d) ----------------------------------------------

    /** Suggested file name for the SAF export picker (date-stamped). */
    fun suggestedBackupName(): String = "softhome-backup-${backupStamp()}.json"

    /** Writes a backup to the SAF [uri] (the stream is opened + closed inside). */
    fun exportBackup(resolver: android.content.ContentResolver, uri: android.net.Uri) =
        viewModelScope.launch {
            val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                    resolver.openOutputStream(uri)?.use { backupRepository.export(it) }
                }.getOrNull()
            }
            backupMessage.value = when (result) {
                is BackupResult.Success -> context.getString(R.string.settings_backup_saved)
                else -> context.getString(R.string.settings_backup_write_failed)
            }
        }

    /**
     * Validates the SAF [uri] and, if it is a valid SOFT / HOME backup, holds it for the
     * user's confirmation (restore is destructive -- P4d-3). A bad file sets a message and
     * changes nothing.
     */
    fun loadBackupForRestore(resolver: android.content.ContentResolver, uri: android.net.Uri) =
        viewModelScope.launch {
            val json = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching { resolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } }
                    .getOrNull()
            }
            when (val decoded = BackupCodec.decode(json)) {
                is BackupDecodeResult.Ok -> {
                    pendingDocument = decoded.document
                    pendingRestore.value = true
                    backupMessage.value = null
                }
                is BackupDecodeResult.NotABackup ->
                    backupMessage.value = context.getString(R.string.settings_backup_not_a_backup)
                is BackupDecodeResult.UnsupportedVersion ->
                    backupMessage.value = context.getString(R.string.settings_backup_newer_version)
                is BackupDecodeResult.Malformed ->
                    backupMessage.value = context.getString(R.string.settings_backup_unreadable)
            }
        }

    /** Applies the held backup document (after the user confirmed). */
    fun confirmRestore() = viewModelScope.launch {
        val doc = pendingDocument ?: return@launch
        prefsRepository.applyAll(doc.prefs)
        // Folders + notes are applied through the backup repository's own path by
        // re-importing the held document is overkill; instead apply directly here.
        folderRepository.save(doc.folders)
        notesRepository.setBody(doc.notes)
        pendingDocument = null
        pendingRestore.value = false
        backupMessage.value = context.getString(R.string.settings_backup_restored)
    }

    /** Dismisses a pending restore without applying it. */
    fun cancelRestore() {
        pendingDocument = null
        pendingRestore.value = false
    }

    /** Consumes the transient message (tap to dismiss). */
    fun consumeBackupMessage() { backupMessage.value = null }

    private fun backupStamp(): String {
        val now = java.time.LocalDate.now()
        return now.toString()
    }
}
