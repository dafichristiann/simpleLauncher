package com.softhome.feature.appdrawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softhome.core.data.repository.AppActionsRepository
import com.softhome.core.data.repository.AppRepository
import com.softhome.core.data.repository.FolderRepository
import com.softhome.core.data.repository.PrefsRepository
import com.softhome.core.model.AppInfo
import com.softhome.core.model.DrawerCategory
import com.softhome.core.model.DrawerCategoryMapper
import com.softhome.core.model.DrawerGridItem
import com.softhome.core.model.Folder
import com.softhome.core.model.FolderLogic
import com.softhome.core.model.IconPack
import com.softhome.core.model.IconSource
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

data class DrawerEntry(
    val app: AppInfo,
    val resolved: ResolvedIcon,
    val symbolName: String,
)

/** One cell in the drawer grid: an app, or a folder (P2 / D1). */
sealed interface DrawerCell {
    data class AppEntry(val entry: DrawerEntry) : DrawerCell
    data class FolderCell(val folder: Folder, val preview: List<DrawerEntry>) : DrawerCell
}

data class DrawerUiState(
    val allApps: List<DrawerEntry> = emptyList(),
    val cells: List<DrawerCell> = emptyList(),
    val indexLetters: List<Char> = emptyList(),
    val query: String = "",
    val category: DrawerCategory = DrawerCategory.All,
    val availableCategories: List<DrawerCategory> = DrawerCategory.entries.toList(),
    val loading: Boolean = true,
    val activePack: IconPack? = null,
    val folders: List<Folder> = emptyList(),
    /** Folder currently open in the popup, or null. */
    val openFolder: Folder? = null,
    /** P3/F3: the app whose long-press context menu is open, or null. */
    val menuEntry: DrawerEntry? = null,
    /** P3/Q2: total hidden apps (for the settings "Hidden apps" row count). */
    val hiddenApps: Set<String> = emptySet(),
)

/** Per-app facts the context menu needs (P3 / F3), computed on demand. */
data class DrawerEntryActions(
    val canUninstall: Boolean,
    val shortcutCount: Int,
)

@HiltViewModel
class AppDrawerViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val prefsRepository: PrefsRepository,
    private val appActionsRepository: AppActionsRepository,
    private val folderRepository: FolderRepository,
    val iconPackRepository: IconPackRepository,
    val drawableLoader: IconPackDrawableLoader,
    val bitmapProvider: IconBitmapProvider,
    private val iconResolver: IconResolver,
) : ViewModel() {

    private val appsFlow = MutableStateFlow<List<AppInfo>>(emptyList())
    private val queryFlow = MutableStateFlow("")
    private val categoryFlow = MutableStateFlow(DrawerCategory.All)
    private val loadingFlow = MutableStateFlow(true)
    private val openFolderIdFlow = MutableStateFlow<String?>(null)
    private val menuKeyFlow = MutableStateFlow<String?>(null)

    private data class Core(
        val apps: List<AppInfo>,
        val query: String,
        val category: DrawerCategory,
        val loading: Boolean,
        val prefs: com.softhome.core.model.LauncherPrefs,
    )

    /** Mirror of the persisted folders, kept in memory for synchronous updates. */
    private val foldersFlow = MutableStateFlow<List<Folder>>(emptyList())

    val uiState: StateFlow<DrawerUiState> = combine(
        appsFlow, queryFlow, categoryFlow, loadingFlow, prefsRepository.prefs,
    ) { apps, query, category, loading, prefs ->
        Core(apps, query, category, loading, prefs)
    }.combine(iconPackRepository.activePack) { core, pack -> core to pack }
        .combine(foldersFlow) { (core, pack), folders ->
            Triple(core, pack, folders)
        }
        .combine(openFolderIdFlow) { (core, pack, folders), openId ->
            Quad(core, pack, folders, openId)
        }
        .combine(menuKeyFlow) { quad, menuKey ->
            buildState(
                quad.core.apps, quad.core.query, quad.core.category, quad.core.loading, quad.core.prefs,
                quad.pack, quad.folders, quad.openId, menuKey,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DrawerUiState())

    private data class Quad(
        val core: Core,
        val pack: IconPack?,
        val folders: List<Folder>,
        val openId: String?,
    )

    private fun buildState(
        apps: List<AppInfo>,
        query: String,
        category: DrawerCategory,
        loading: Boolean,
        prefs: com.softhome.core.model.LauncherPrefs,
        pack: IconPack?,
        folders: List<Folder>,
        openFolderId: String?,
        menuKey: String?,
    ): DrawerUiState {
        // P3/Q2: hidden apps are filtered out of the drawer entirely.
        val hidden = prefs.hiddenApps
        val visibleApps = apps.filterNot { it.componentKey in hidden }
        val entries = visibleApps.associate { app ->
            app.componentKey to DrawerEntry(
                app = app,
                symbolName = IconMasker.symbolFor(app),
                resolved = resolveIcon(app, prefs, pack),
            )
        }
        val inCategory = DrawerCategoryMapper.filter(visibleApps, category)
            .map { it.componentKey }
            .toSet()
        val matchesQuery: (AppInfo) -> Boolean =
            { query.isBlank() || it.label.contains(query, ignoreCase = true) }

        // Apps that pass the current filter, in original (alphabetical) order.
        val visibleEntries = visibleApps
            .filter { it.componentKey in inCategory && matchesQuery(it) }
            .mapNotNull { entries[it.componentKey] }

        // Folders: show a folder cell when >= 1 of its apps passes the filter.
        // Inside an open folder, the popup lists the folder's visible apps.
        val visibleFolderCells = folders.mapNotNull { folder ->
            val memberEntries = folder.apps.mapNotNull { entries[it] }
                .filter { it.app.componentKey in inCategory && matchesQuery(it.app) }
            if (memberEntries.isEmpty()) return@mapNotNull null
            DrawerCell.FolderCell(
                folder = folder,
                preview = FoldersPreview.entriesFor(folder, memberEntries).take(FolderLogic.PREVIEW_CAPACITY),
            )
        }

        // App cells = apps not inside any folder.
        val appCells = visibleEntries
            .filterNot { FolderLogic.isInsideFolder(folders, it.app.componentKey) }
            .map { DrawerCell.AppEntry(it) }

        // Folders lead the grid (P2-1), matching FolderLogic.drawerGridItems order.
        val cells = visibleFolderCells + appCells

        val openFolder = folders.firstOrNull { it.id == openFolderId }

        return DrawerUiState(
            allApps = entries.values.toList(),
            cells = cells,
            indexLetters = AlphabetIndex.lettersPresentIn(visibleEntries.map { it.app.label }),
            query = query,
            category = category,
            availableCategories = DrawerCategory.entries.toList(),
            loading = loading,
            activePack = pack,
            folders = folders,
            openFolder = openFolder,
            menuEntry = menuKey?.let { entries[it] },
            hiddenApps = hidden,
        )
    }

    private fun resolveIcon(
        app: AppInfo,
        prefs: com.softhome.core.model.LauncherPrefs,
        pack: IconPack?,
    ): ResolvedIcon {
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
        return ResolvedIcon(
            source = source,
            drawableName = drawableName,
            symbolName = symbol,
            componentKey = app.componentKey,
            packageName = app.packageName,
            className = app.className,
        )
    }

    init {
        viewModelScope.launch {
            loadingFlow.value = true
            appsFlow.value = appRepository.getInstalledApps()
            loadingFlow.value = false
        }
        // Load persisted folders into the in-memory mirror.
        viewModelScope.launch {
            folderRepository.folders.collect { foldersFlow.value = it }
        }
    }

    fun onQueryChange(q: String) { queryFlow.value = q }
    fun onCategoryChange(category: DrawerCategory) { categoryFlow.value = category }
    fun launchApp(app: AppInfo) = appRepository.launchApp(app)

    // --- Long-press context menu (P3 / F3) ------------------------------------

    fun openMenu(entry: DrawerEntry) { menuKeyFlow.value = entry.app.componentKey }
    fun closeMenu() { menuKeyFlow.value = null }

    /** Whether the app can be uninstalled (P3-4): not system + actually removable. */
    fun canUninstall(app: AppInfo): Boolean =
        !app.isSystem && appActionsRepository.isRemovable(app)

    /** Opens the system App Info screen for [app]. */
    fun openAppInfo(app: AppInfo) { appActionsRepository.openAppInfo(app) }

    /** Starts the system uninstall flow for [app]; no-op when not permitted. */
    fun requestUninstall(app: AppInfo) {
        if (canUninstall(app)) appActionsRepository.requestUninstall(app)
    }

    /** P3/Q2 "Remove": hide the app from the drawer (reversible in settings). */
    fun hideApp(app: AppInfo) {
        viewModelScope.launch { prefsRepository.hideApp(app.componentKey) }
        closeMenu()
    }

    /** Restore a hidden app (settings "Hidden apps" row). */
    fun unhideApp(componentKey: String) {
        viewModelScope.launch { prefsRepository.unhideApp(componentKey) }
    }

    // --- Folders (P2 / D1-D2) -------------------------------------------------

    fun openFolder(folderId: String) { openFolderIdFlow.value = folderId }
    fun closeFolder() { openFolderIdFlow.value = null }

    /** Create a new empty folder, then open it (P2 "New folder" entry point). */
    fun createFolder() {
        val folder = FolderLogic.createFolder(
            id = "folder_${System.currentTimeMillis()}",
            name = "New folder",
            keys = emptyList(),
        )
        persist(foldersFlow.value + folder)
        openFolder(folder.id)
    }

    fun renameFolder(folderId: String, name: String) =
        updateFolder(folderId) { FolderLogic.rename(it, name) }

    fun addAppToFolder(folderId: String, componentKey: String) =
        updateFolder(folderId) { FolderLogic.addApp(it, componentKey) }

    fun removeAppFromFolder(folderId: String, componentKey: String) =
        updateFolder(folderId) { FolderLogic.removeApp(it, componentKey) }

    private fun updateFolder(folderId: String, transform: (Folder) -> Folder) {
        val updated = foldersFlow.value.map { folder ->
            if (folder.id == folderId) transform(folder) else folder
        }
        persist(updated)
    }

    private fun persist(folders: List<Folder>) {
        foldersFlow.value = folders
        viewModelScope.launch { folderRepository.save(folders) }
    }
}

/** Pure helper: a folder's member entries for the 2x2 preview, order-preserved. */
internal object FoldersPreview {
    fun entriesFor(folder: Folder, memberEntries: List<DrawerEntry>): List<DrawerEntry> =
        FolderLogic.previewKeys(folder).mapNotNull { key ->
            memberEntries.firstOrNull { it.app.componentKey == key }
        }
}

