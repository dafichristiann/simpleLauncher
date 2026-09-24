package com.softhome.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * Helpers around being the device's Home app.
 *
 * P1 (docs/09 D-001): the HOME intent target is REAL and functional; the picker
 * flow is basic. We never silently hijack -- the user always confirms via the
 * system picker (docs/04 #11).
 */
object LauncherRole {

    fun isDefaultLauncher(context: Context): Boolean {
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = context.packageManager.resolveActivity(
            homeIntent,
            android.content.pm.PackageManager.MATCH_DEFAULT_ONLY,
        ) ?: return false
        return resolved.activityInfo?.packageName == context.packageName
    }

    /**
     * Opens the system Home-app picker so the user can choose SOFT / HOME.
     * Some OEMs lack the exact Settings screen, so we try a couple of intents.
     */
    fun requestDefaultLauncher(context: Context) {
        // Preferred: the dedicated "Home app" settings screen (API 21+).
        val homeSettings = Intent(Settings.ACTION_HOME_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (homeSettings.resolveActivity(context.packageManager) != null) {
            runCatching { context.startActivity(homeSettings) ; return }
        }
        // Fallback: the generic Home intent chooser.
        val chooser = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(chooser) }
    }

    fun componentName(context: Context): ComponentName =
        ComponentName(context, HomeActivity::class.java)
}
