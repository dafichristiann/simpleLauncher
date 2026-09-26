package com.softhome.core.data.packages

import android.content.Context
import com.softhome.core.common.DispatcherProvider
import com.softhome.core.data.repository.AppRepository
import com.softhome.core.data.repository.FolderRepository
import com.softhome.core.data.repository.PrefsRepository
import com.softhome.core.model.AppInfo
import com.softhome.core.model.LauncherPrefsCleanup
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates the "installed apps" view with the persisted per-app references so the
 * launcher stays correct when apps are installed / uninstalled at runtime.
 *
 * Responsibilities:
 *  - [currentApps]: the launchable app list (delegates to the same query the drawer uses).
 *  - [refreshAndPrune]: reload the list **and** drop dead `componentKey`s from the hidden
 *    set, icon overrides, folder membership and rail slots. Idempotent, so several
 *    observers (home + drawer) can call it without conflict.
 *
 * This lives in the data layer because it is the single writer of the cross-repository
 * cleanup; the ViewModels only observe [PackageEventMonitor] and call it (docs/01 layer
 * rule: UI never writes repositories directly).
 *
 * It depends on the [AppRepository] **interface** (not the impl) so tests that inject a
 * fake app source exercise the same code path.
 */
@Singleton
class AppInventoryCoordinator @Inject constructor(
    private val appRepository: AppRepository,
    private val prefsRepository: PrefsRepository,
    private val folderRepository: FolderRepository,
    private val dispatchers: DispatcherProvider,
) {

    /** Current launchable apps (main-thread-safe; the repository runs the query on IO). */
    suspend fun currentApps(): List<AppInfo> = appRepository.getInstalledApps()

    /**
     * Reload the launchable apps and prune every persisted reference to an app that is no
     * longer installed. Returns the fresh list. Writes nothing when nothing was removed.
     */
    suspend fun refreshAndPrune(): List<AppInfo> {
        val apps = currentApps()
        val present = apps.mapTo(HashSet()) { it.componentKey }

        // Prefs (hidden / overrides / rail) — one write only if something changed.
        val prefs = prefsRepository.prefs.first()
        val (prunedPrefs, changed) = LauncherPrefsCleanup.prunePrefs(prefs, present)
        if (changed) {
            withContext(dispatchers.io) { prefsRepository.applyPruned(prunedPrefs) }
        }

        // Folders — save only when a membership actually shrank.
        val folders = folderRepository.folders.first()
        val prunedFolders = LauncherPrefsCleanup.pruneFolders(folders, present)
        if (prunedFolders !== folders) {
            withContext(dispatchers.io) { folderRepository.save(prunedFolders) }
        }

        return apps
    }
}
