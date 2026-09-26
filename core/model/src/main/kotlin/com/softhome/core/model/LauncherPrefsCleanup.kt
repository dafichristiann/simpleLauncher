package com.softhome.core.model

/**
 * Pure helpers to prune **dead app references** from persisted user state when an app is
 * uninstalled (or otherwise disappears from the installed set).
 *
 * Motivation: the launcher keys all per-app state by `componentKey` (`package/class`).
 * When the app goes away, those keys would otherwise linger forever:
 *  - `hiddenApps` would keep a key that can never be unhidden by tapping anything;
 *  - `iconOverrides` would keep a stale icon choice;
 *  - `Folder.apps` would keep a membership that renders nothing;
 *  - `railItems` could keep an app slot that resolves to a dead component.
 *
 * Each function is **total** and returns the input unchanged when there is nothing to
 * prune, so a no-op refresh never triggers a redundant DataStore write. Kept Android-free
 * so it is unit-tested directly (mirroring the other `*Logic` objects in this module).
 */
object LauncherPrefsCleanup {

    /** Remove any `componentKey` not present in [present]. Returns the same instance when
     *  nothing changed, so callers can skip the write. */
    fun pruneHiddenApps(hiddenApps: Set<String>, present: Set<String>): Set<String> =
        if (hiddenApps.all { it in present }) hiddenApps else hiddenApps.filterTo(LinkedHashSet()) { it in present }

    /** Remove overrides whose componentKey is no longer installed. */
    fun pruneIconOverrides(
        overrides: Map<String, IconOverride>,
        present: Set<String>,
    ): Map<String, IconOverride> =
        if (overrides.keys.all { it in present }) overrides else overrides.filterKeys { it in present }

    /** Remove rail app items whose componentKey is gone. System items are always kept. */
    fun pruneRailItems(items: List<RailItemId>, present: Set<String>): List<RailItemId> {
        val pruned = items.filterNot { it is RailItemId.App && it.componentKey !in present }
        return if (pruned.size == items.size) items else pruned
    }

    /** Remove dead componentKeys from every folder's membership. Folders themselves are
     *  never deleted (an emptied folder stays, ready to be refilled). */
    fun pruneFolders(folders: List<Folder>, present: Set<String>): List<Folder> {
        var changed = false
        val out = folders.map { folder ->
            val kept = folder.apps.filter { it in present }
            if (kept.size == folder.apps.size) {
                folder
            } else {
                changed = true
                folder.copy(apps = kept)
            }
        }
        return if (changed) out else folders
    }

    /**
     * Convenience: apply every prune in one pass over [prefs]. Returns a pair of the
     * cleaned prefs and whether **any** field changed (so the caller can skip the write).
     */
    fun prunePrefs(prefs: LauncherPrefs, present: Set<String>): Pair<LauncherPrefs, Boolean> {
        val hidden = pruneHiddenApps(prefs.hiddenApps, present)
        val overrides = pruneIconOverrides(prefs.iconOverrides, present)
        val rail = pruneRailItems(prefs.railItems, present)
        val changed = hidden !== prefs.hiddenApps ||
            overrides !== prefs.iconOverrides ||
            rail !== prefs.railItems
        return if (changed) {
            prefs.copy(hiddenApps = hidden, iconOverrides = overrides, railItems = rail) to true
        } else {
            prefs to false
        }
    }
}
