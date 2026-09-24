package com.softhome.core.model

/**
 * Pure folder operations for the **app drawer grid** (P2 / D1-D2).
 *
 * Decision P2-1: folders live in the drawer (the only grid surface left after the
 * Warm Right Rail redesign), NOT on the row-based home. Kept Android-free so unit
 * tests can cover grouping without a device.
 */
object FolderLogic {

    /** Apps shown in the 2x2 mini-grid preview (first [PREVIEW_CAPACITY]). */
    const val PREVIEW_CAPACITY = 4

    /** Apps shown in the 2x2 mini-grid preview (first [PREVIEW_CAPACITY]). */
    fun previewKeys(folder: Folder): List<String> =
        folder.apps.take(PREVIEW_CAPACITY)

    /** True when [componentKey] is inside any folder. */
    fun isInsideFolder(folders: List<Folder>, componentKey: String): Boolean =
        folders.any { componentKey in it.apps }

    /**
     * Drawer grid cells: folders lead the list (so a folded preview is visible
     * first), followed by every app that is not nested inside a folder, in their
     * original order. Apps already inside a folder are removed from the root so
     * they are not shown twice.
     */
    fun drawerGridItems(
        allApps: List<String>,
        folders: List<Folder>,
    ): List<DrawerGridItem> {
        val nested = folders.flatMap { it.apps }.toSet()
        val folderItems = folders.map { DrawerGridItem.FolderItem(it.id) }
        val rootApps = allApps.filterNot { it in nested }.map { DrawerGridItem.App(it) }
        return folderItems + rootApps
    }

    fun rename(folder: Folder, name: String): Folder =
        folder.copy(name = name.trim().ifBlank { folder.name })

    fun addApp(folder: Folder, componentKey: String): Folder =
        if (componentKey in folder.apps) folder
        else folder.copy(apps = folder.apps + componentKey)

    fun removeApp(folder: Folder, componentKey: String): Folder =
        folder.copy(apps = folder.apps.filterNot { it == componentKey })

    /** Build a new folder from an explicit id/name/keys (keys de-duplicated, kept in order). */
    fun createFolder(id: String, name: String, keys: List<String>): Folder =
        Folder(id = id, name = name.trim().ifBlank { "Folder" }, apps = keys.distinct())
}

/** One cell in the app-drawer grid after folders are applied. */
sealed interface DrawerGridItem {
    data class App(val componentKey: String) : DrawerGridItem
    data class FolderItem(val folderId: String) : DrawerGridItem
}
