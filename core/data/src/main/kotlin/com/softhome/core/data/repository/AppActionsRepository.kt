package com.softhome.core.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.net.Uri
import android.provider.Settings
import com.softhome.core.common.DispatcherProvider
import com.softhome.core.model.AppInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-level actions for the long-press context menu (P3 / F3).
 *
 * Every method is **safe**: unresolvable intents return null / empty and never throw.
 * The UI greys or hides rows accordingly (spec section 4.11).
 */
interface AppActionsRepository {
    /** Launch intents the OS offers for this app (Fallback-free; empty when none). */
    suspend fun shortcutsFor(app: AppInfo): List<ShortcutInfo>
    /** "App Info" system screen intent, or null when unresolvable. */
    fun appInfoIntent(app: AppInfo): Intent?
    /** Uninstall intent (ACTION_DELETE), or null when unresolvable. */
    fun uninstallIntent(app: AppInfo): Intent?
    /** Whether the app is removable (installed for user + not system-only). */
    fun isRemovable(app: AppInfo): Boolean
    /** Ask the system to launch the uninstall flow; false when it cannot. */
    fun requestUninstall(app: AppInfo): Boolean
    /** Open the app's system info screen; false when it cannot. */
    fun openAppInfo(app: AppInfo): Boolean
}

@Singleton
class AppActionsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider,
) : AppActionsRepository {

    override suspend fun shortcutsFor(app: AppInfo): List<ShortcutInfo> {
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
            ?: return emptyList()
        return withContext(dispatchers.io) {
            runCatching {
                val user = android.os.Process.myUserHandle()
                launcherApps.getShortcuts(
                    android.content.pm.LauncherApps.ShortcutQuery().apply {
                        setPackage(app.packageName)
                        setQueryFlags(
                            android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                                android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                                android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED,
                        )
                    },
                    user,
                ) ?: emptyList()
            }.getOrDefault(emptyList())
        }
    }

    override fun appInfoIntent(app: AppInfo): Intent? {
        val intent = AppIntentFactory.appInfo(app)
        return intent.takeIf { it.resolveActivity(context.packageManager) != null }
    }

    override fun uninstallIntent(app: AppInfo): Intent? {
        val intent = AppIntentFactory.uninstall(app)
        return intent.takeIf { it.resolveActivity(context.packageManager) != null }
    }

    override fun isRemovable(app: AppInfo): Boolean {
        return runCatching {
            val info = context.packageManager.getApplicationInfo(
                app.packageName,
                PackageManager.ApplicationInfoFlags.of(0),
            )
            val installed = (info.flags and android.content.pm.ApplicationInfo.FLAG_INSTALLED) != 0
            installed && !app.isSystem
        }.getOrDefault(false)
    }

    override fun requestUninstall(app: AppInfo): Boolean {
        val intent = uninstallIntent(app) ?: return false
        return runCatching { context.startActivity(intent) }.isSuccess
    }

    override fun openAppInfo(app: AppInfo): Boolean {
        val intent = appInfoIntent(app) ?: return false
        return runCatching { context.startActivity(intent) }.isSuccess
    }
}

/**
 * Pure intent builders for the app actions (P3 / F3). Kept separate from the
 * `resolveActivity` guards so the constructed intents are unit-testable without a
 * fully-populated PackageManager.
 */
object AppIntentFactory {

    fun appInfo(app: AppInfo): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", app.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    fun uninstall(app: AppInfo): Intent =
        Intent(Intent.ACTION_DELETE).apply {
            data = Uri.parse("package:${app.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
}
