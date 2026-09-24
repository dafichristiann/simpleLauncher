package com.softhome.launcher

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent

/**
 * Wallpaper entry point (P3 / G, decision P3-6).
 *
 * The launcher ships a **flat warm default** (it simply paints `background`); this
 * opens the **system** wallpaper picker so the user can choose a live/photo wallpaper.
 * The live-wallpaper *engine* (drawing our own) stays deferred -- see docs/04 #12.
 *
 * Prefers the modern `ACTION_CHANGE_LIVE_WALLPAPER` when a live picker is present,
 * else the legacy `ACTION_SET_WALLPAPER`. Never throws when nothing resolves.
 */
object WallpaperIntents {

    fun openPicker(context: Context) {
        val live = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                android.content.ComponentName(context, HomeActivity::class.java),
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (resolves(context, live)) {
            if (runCatching { context.startActivity(live) }.isSuccess) return
        }

        val legacy = Intent(Intent.ACTION_SET_WALLPAPER)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (resolves(context, legacy)) {
            runCatching { context.startActivity(legacy) }
        }
    }

    private fun resolves(context: Context, intent: Intent): Boolean =
        intent.resolveActivity(context.packageManager) != null
}
