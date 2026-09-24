package com.softhome.launcher

import android.content.Context
import android.content.Intent

/**
 * Intent factory for the in-app settings surface (P3 / G, decision Q1).
 *
 * The rail's `panel-left` shortcut and the Home settings entry points open our own
 * settings panel. The panel itself contains a "System settings" row that fires the
 * OS Settings screen -- see [systemSettings].
 */
object SettingsIntents {

    /** Opens SOFT / HOME's own settings panel. */
    fun settings(context: Context): Intent =
        Intent(context, SettingsStubActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Opens the OS Settings app. */
    fun systemSettings(context: Context): Intent =
        Intent(android.provider.Settings.ACTION_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
