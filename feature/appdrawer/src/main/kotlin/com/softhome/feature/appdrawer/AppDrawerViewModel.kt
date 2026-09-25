package com.softhome.feature.appdrawer

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softhome.core.data.repository.AppActionsRepository
import com.softhome.core.data.repository.AppRepository
import com.softhome.core.data.repository.FolderRepository
import com.softhome.core.data.repository.PrefsRepository
import com.softhome.core.model.AppInfo
import com.softhome.core.model.DrawerCategory
import com.softhome.core.model.DrawerCategoryResolver
import com.softhome.core.model.DrawerGridItem
import com.softhome.core.model.DrawerIconAssignment
import com.softhome.core.model.Folder
import com.softhome.core.model.FolderDropResolver
import com.softhome.core.model.FolderLogic
import com.softhome.core.model.IconPack
import com.softhome.core.model.IconSource
import com.softhome.core.model.ResolvedIcon
import com.softhome.core.designsystem.atom.LineIcon
import com.softhome.feature.iconpack.data.IconPackDrawableLoader
import com.softhome.feature.iconpack.data.IconPackRepository
import com.softhome.feature.iconpack.domain.IconBitmapProvider
import com.softhome.feature.iconpack.domain.DrawerIconColor
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

/**
 * P4: extra **resolvable** glyphs the uniqueness pass cycles through when a whole glyph's
 * color column is already taken. These are the generic lucide glyphs that always have a
 * bundled drawable, so a de-duplicated tile is still renderable (never a fake name).
 * P7: marked @Stable so Compose skips recomposition of children when this is passed.
 */
@Stable
data class DrawerEntry(
    val app: AppInfo,
    val resolved: ResolvedIcon,
    val symbolName: String,
    /**
     * P1.2: the drawer glyph **color token**, computed ONCE per app here instead of on
     * every recomposition of the cell. During scroll each cell recomposes repeatedly;
     * before this change [DrawerAppIcon] re-ran `DrawerIconColor.tokenFor(...)` (a string
     * heuristic) on every frame. Precomputing it (with [symbolName]) removes that
     * per-frame UI-thread work.
     */
    val colorToken: com.softhome.feature.iconpack.domain.DrawerIconColor.Token,
)

/** One cell in the drawer grid: an app, or a folder (P2 / D1). P7: marked @Stable. */
@Stable
sealed interface DrawerCell {
    data class AppEntry(val entry: DrawerEntry) : DrawerCell
    data class FolderCell(val folder: Folder, val preview: List<DrawerEntry>) : DrawerCell
}

/**
 * P5: one page of the drawer category pager. Each page holds the cells (folders-first)
 * and the alphabet letters for its own category, so swiping shows that category's grid
 * without re-deriving it in the composable. P7: marked @Stable.
 */
@Stable
data class DrawerPage(
    val category: DrawerCategory,
    val label: String,
    val cells: List<DrawerCell>,
    val indexLetters: List<Char>,
)

@Stable
data class DrawerUiState(
    val allApps: List<DrawerEntry> = emptyList(),
    val cells: List<DrawerCell> = emptyList(),
    val indexLetters: List<Char> = emptyList(),
    /**
     * P5: a page per category ([DrawerCategory.All] first, then the 8 design groups).
     * `cells`/`indexLetters` above mirror the page for the current [category] so existing
     * consumers keep working.
     */
    val pages: List<DrawerPage> = emptyList(),
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
    /** P4b: the app whose icon editor is open, or null. */
    val editingEntry: DrawerEntry? = null,
    /** P3/Q2: total hidden apps (for the settings "Hidden apps" row count). */
    val hiddenApps: Set<String> = emptySet(),
    /** P4b: current per-app icon overrides (for the editor's seed). */
    val iconOverrides: Map<String, com.softhome.core.model.IconOverride> = emptyMap(),
    /**
     * P1.2: the drawer grid spacing multiplier from settings (P3 `SpacingScale`).
     * Previously persisted but never consumed -- the grid used a fixed gap. Now the
     * vertical gap honours this factor.
     */
    val spacingFactor: Float = 1f,
    /**
     * Audit P6: the drawer column count from settings (P3 "Drawer grid size", persisted
     * in `LauncherPrefs.grid.columns`). Previously persisted but never applied -- the
     * drawer always used a fixed 4 columns.
     */
    val gridColumns: Int = 4,
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
    private val editingKeyFlow = MutableStateFlow<String?>(null)

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
            menuKey to quad
        }
        .combine(editingKeyFlow) { (menuKey, quad), editingKey ->
            buildState(
                quad.core.apps, quad.core.query, quad.core.category, quad.core.loading, quad.core.prefs,
                quad.pack, quad.folders, quad.openId, menuKey, editingKey,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DrawerUiState())

    private data class Quad(
        val core: Core,
        val pack: IconPack?,
        val folders: List<Folder>,
        val openId: String?,
    )

    /**
     * Audit P5: the per-app `DrawerEntry` (glyph + color assignment + resolved icon) is
     * the expensive part of [buildState] -- it runs the design-map lookup, the
     * uniqueness pass, and the icon resolver for every visible app. It only depends on
     * the app list, the active pack and the icon/visibility prefs -- NOT on the query,
     * category, or which overlay is open. So we memoize it and only recompute when one
     * of those inputs actually changes. Before this, typing in the search box re-ran the
     * whole derivation on every keystroke.
     */
    private data class EntriesKey(
        val apps: List<AppInfo>,
        val packId: String?,
        val hidden: Set<String>,
        val overrides: Map<String, com.softhome.core.model.IconOverride>,
        val maskUnsupported: Boolean,
    )

    private var cachedEntriesKey: EntriesKey? = null
    private var cachedEntries: Map<String, DrawerEntry> = emptyMap()
    private var cachedVisibleApps: List<AppInfo> = emptyList()
    
    // P7.2: memoize page derivation to avoid O(n*m) recomputation on every query change
    private data class PagesKey(
        val visibleAppKeys: Set<String>,
        val folderIds: Set<String>,
        val queryHash: Int,
    )
    private var cachedPagesKey: PagesKey? = null
    private var cachedPages: List<DrawerPage> = emptyList()

    /** Compute (or reuse) the per-app entries for the current inputs. */
    private fun entriesFor(
        apps: List<AppInfo>,
        pack: IconPack?,
        prefs: com.softhome.core.model.LauncherPrefs,
    ): Pair<List<AppInfo>, Map<String, DrawerEntry>> {
        val key = EntriesKey(
            apps = apps,
            packId = pack?.id,
            hidden = prefs.hiddenApps,
            overrides = prefs.iconOverrides,
            maskUnsupported = prefs.maskUnsupportedApps,
        )
        if (key == cachedEntriesKey) return cachedVisibleApps to cachedEntries

        val visibleApps = apps.filterNot { it.componentKey in prefs.hiddenApps }
        val byPackage = visibleApps.associateBy { it.packageName }
        val assignments = DrawerIconAssignment.assign(
            identities = visibleApps.map { it.componentKey to it.packageName },
            heuristicGlyph = { pkg -> byPackage[pkg]?.let { IconMasker.symbolFor(it) } ?: "AppWindow" },
            heuristicColor = { pkg ->
                val app = byPackage[pkg]
                DrawerIconColor.nameOf(
                    DrawerIconColor.tokenFor(app?.category, app?.let { IconMasker.symbolFor(it) } ?: "AppWindow"),
                )
            },
            glyphFallbacks = DrawerIconAssignment.DEFAULT_GLYPH_FALLBACKS,
        )
        val entries = visibleApps.associate { app ->
            val assigned = assignments[app.componentKey]
            val symbol = assigned?.glyph?.let { LineIcon.fromLucide(it).name }
                ?: IconMasker.symbolFor(app)
            app.componentKey to DrawerEntry(
                app = app,
                symbolName = symbol,
                resolved = resolveIcon(app, prefs, pack, symbol),
                colorToken = assigned?.colorToken
                    ?.let { com.softhome.feature.iconpack.domain.DrawerIconColor.tokenForName(it) }
                    ?: com.softhome.feature.iconpack.domain.DrawerIconColor
                        .tokenFor(app.category, symbol),
            )
        }
        cachedEntriesKey = key
        cachedEntries = entries
        cachedVisibleApps = visibleApps
        return visibleApps to entries
    }

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
        editingKey: String?,
    ): DrawerUiState {
        // Audit P5: hidden-app filtering + the per-app entry/assignment derivation are
        // memoized (see [entriesFor]); only the cheap query/category/folder projection
        // below runs on every state emission.
        val (visibleApps, entries) = entriesFor(apps, pack, prefs)
        val matchesQuery: (AppInfo) -> Boolean =
            { query.isBlank() || it.label.contains(query, ignoreCase = true) }

        // P7.2: memoize page derivation (O(n*m) operation). Only recompute when:
        // - visible app set changes, OR
        // - folder set changes, OR
        // - query changes
        val pagesKey = PagesKey(
            visibleAppKeys = visibleApps.map { it.componentKey }.toSet(),
            folderIds = folders.map { it.id }.toSet(),
            queryHash = query.hashCode(),
        )
        
        val pages = if (pagesKey == cachedPagesKey) {
            cachedPages
        } else {
            // P5: build one page per category (All + the 8 design groups). Global search
            // (`matchesQuery`) applies to every page; folders lead each page (P2-1).
            val pagerTabs = DrawerCategoryResolver.pagerTabs
            val computedPages = pagerTabs.map { tab ->
                val inTab = visibleApps.filter { DrawerCategoryResolver.matches(it, tab) }
                    .map { it.componentKey }
                    .toSet()

                val tabEntries = visibleApps
                    .filter { it.componentKey in inTab && matchesQuery(it) }
                    .mapNotNull { entries[it.componentKey] }

                val folderCells = folders.mapNotNull { folder ->
                    val memberEntries = folder.apps.mapNotNull { entries[it] }
                        .filter { it.app.componentKey in inTab && matchesQuery(it.app) }
                    if (memberEntries.isEmpty()) return@mapNotNull null
                    DrawerCell.FolderCell(
                        folder = folder,
                        preview = FoldersPreview.entriesFor(folder, memberEntries).take(FolderLogic.PREVIEW_CAPACITY),
                    )
                }

                val appCells = tabEntries
                    .filterNot { FolderLogic.isInsideFolder(folders, it.app.componentKey) }
                    .map { DrawerCell.AppEntry(it) }

                DrawerPage(
                    category = tab,
                    label = tab.label,
                    cells = folderCells + appCells,
                    // Index only apps that are actually rendered as grid cells. Apps
                    // inside folders are not standalone All Apps entries and must not
                    // create alphabet letters that jump to an unrelated row.
                    // Keep the visual rail stable (A-Z/#); the screen separately marks
                    // unavailable buckets so empty letters never disappear or shift layout.
                    indexLetters = AlphabetIndex.allBuckets,
                )
            }
            cachedPagesKey = pagesKey
            cachedPages = computedPages
            computedPages
        }
        
        val currentPage = pages.firstOrNull { it.category == category } ?: pages.firstOrNull()

        val openFolder = folders.firstOrNull { it.id == openFolderId }

        // P7: pre-coerce grid columns here so it's not recalculated on every Compose recomposition.
        val coercedGridColumns = prefs.grid.columns.coerceIn(3, 7)
        
        return DrawerUiState(
            allApps = entries.values.toList(),
            cells = currentPage?.cells ?: emptyList(),
            indexLetters = currentPage?.indexLetters ?: emptyList(),
            pages = pages,
            query = query,
            category = category,
            availableCategories = DrawerCategoryResolver.pagerTabs,
            loading = loading,
            activePack = pack,
            folders = folders,
            openFolder = openFolder,
            menuEntry = menuKey?.let { entries[it] },
            editingEntry = editingKey?.let { entries[it] },
            hiddenApps = prefs.hiddenApps,
            iconOverrides = prefs.iconOverrides,
            spacingFactor = prefs.spacing.factor,
            gridColumns = coercedGridColumns,
        )
    }

    private fun resolveIcon(
        app: AppInfo,
        prefs: com.softhome.core.model.LauncherPrefs,
        pack: IconPack?,
        automaticSymbol: String,
    ): ResolvedIcon {
        val source = iconResolver.resolve(
            app = app,
            activePack = pack,
            overrides = prefs.iconOverrides,
            maskUnsupported = prefs.maskUnsupportedApps,
        )
        val drawableName = when (source) {
            is IconSource.FromPack -> source.drawableName
            is IconSource.Override -> source.drawableName
            is IconSource.Glyph -> null
            IconSource.AutoMask, IconSource.System -> null
        }
        // P4b: a glyph override carries both the chosen glyph and its color token. P4:
        // otherwise use the design-mapped symbol (DrawerIconMap), not the raw heuristic.
        val (symbolName, overrideToken) = when (source) {
            is IconSource.Glyph -> source.symbolName to source.colorToken
            else -> automaticSymbol to null
        }
        return ResolvedIcon(
            source = source,
            drawableName = drawableName,
            symbolName = symbolName,
            componentKey = app.componentKey,
            packageName = app.packageName,
            className = app.className,
            overrideColorToken = overrideToken,
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

    // --- P4b: icon editor ------------------------------------------------------

    /** Open the icon editor for [entry] (from the menu's "Edit Icon" row). */
    fun openIconEditor(entry: DrawerEntry) {
        menuKeyFlow.value = null
        editingKeyFlow.value = entry.app.componentKey
    }

    fun closeIconEditor() { editingKeyFlow.value = null }

    /**
     * Build the pure [IconEditor.State] for [entry] from current prefs/pack. Called by
     * the screen each recomposition; cheap (no I/O) and keeps the UI a pure function.
     */
    fun iconEditorState(entry: DrawerEntry): com.softhome.feature.iconpack.domain.IconEditor.State {
        val prefs = uiState.value
        val pack = prefs.activePack
        // Distinct drawables the active pack maps (its curated repertoire).
        val packDrawables = pack?.entries?.values?.distinct()?.sorted().orEmpty()
        val automaticGlyph = entry.symbolName
        val automaticColor = com.softhome.feature.iconpack.domain.DrawerIconColor.nameOf(
            com.softhome.feature.iconpack.domain.DrawerIconColor.tokenFor(
                entry.app.category, automaticGlyph,
            ),
        )
        val existing = currentOverride(entry.app.componentKey)
        return com.softhome.feature.iconpack.domain.IconEditor.start(
            componentKey = entry.app.componentKey,
            packDrawables = packDrawables,
            automaticGlyph = automaticGlyph,
            automaticColor = automaticColor,
            existing = existing,
        )
    }

    private fun currentOverride(componentKey: String): com.softhome.core.model.IconOverride? =
        uiState.value.iconOverrides[componentKey]

    /** Persist the chosen override for [componentKey]. */
    fun applyIconOverride(componentKey: String, override: com.softhome.core.model.IconOverride) {
        viewModelScope.launch { prefsRepository.setIconOverride(componentKey, override) }
        closeIconEditor()
    }

    /** Clear any override for [componentKey] (Reset to automatic). */
    fun resetIconOverride(componentKey: String) {
        viewModelScope.launch { prefsRepository.setIconOverride(componentKey, null) }
        closeIconEditor()
    }

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

    // --- P4a: drag-and-drop drop targets --------------------------------------

    /**
     * App dropped onto a folder tile (P4a). Same membership path as the popup's
     * "Add app"; resolution is the pure [FolderDropResolver.assign].
     */
    fun assignToFolder(folderId: String, componentKey: String) {
        val next = FolderDropResolver.assign(foldersFlow.value, folderId, componentKey)
        if (next != foldersFlow.value) persist(next)
    }

    /**
     * App dropped onto the "New folder" action (P4a): a folder containing only it is
     * created. Uses [FolderDropResolver.createWith] for the pure decision, then persists.
     */
    fun createFolderWith(componentKey: String) {
        val id = "folder_${System.currentTimeMillis()}"
        persist(FolderDropResolver.createWith(foldersFlow.value, componentKey, id, "New folder"))
    }

    /** App dragged out of an open folder (P4a): removed from that folder. */
    fun moveOutOfFolder(folderId: String, componentKey: String) {
        val next = FolderDropResolver.removeFrom(foldersFlow.value, folderId, componentKey)
        if (next != foldersFlow.value) persist(next)
    }

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
