package com.softhome.feature.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import java.util.concurrent.ConcurrentHashMap

/**
 * The 8 home right-rail shortcuts (design/homeApp.pen node `hrsLU` / `B6633e`).
 *
 * Order matches the `.pen` top-to-bottom:
 * sparkles, circle-dot, message-circle, send, camera, wind, panel-left, phone.
 *
 * Each entry maps to a launcher intent for the device's default app for that
 * role, or null for purely decorative icons (sparkles).
 */
enum class RailShortcut(
    val drawableName: String,
) {
    Sparkles("sparkles"),
    CircleDot("circle_dot"),
    MessageCircle("message_circle"),
    Send("send"),
    Camera("camera"),
    Wind("wind"),
    PanelLeft("panel_left"),
    Phone("phone");

    /** Decorative icons never resolve to an app. */
    val isDecorative: Boolean get() = this == Sparkles

    companion object {
        /** The rail order, exactly as drawn in the design. */
        val ordered: List<RailShortcut> = entries.toList()
    }
}

/**
 * Resolves a [RailShortcut] to a launchable intent for the device default app,
 * or null when there is no target (decorative, or nothing installed).
 *
 * Pure-resolution helper; kept separate from the composable so it can be tested
 * with a fake resolver. Results are cached per shortcut.
 */
class RailShortcutResolver(
    private val resolveIntent: (RailShortcut) -> Intent?,
) {
    // ConcurrentHashMap forbids null values, so a missing key means "not
    // resolved yet"; a stored sentinel keeps no-target results cacheable.
    private val cache = ConcurrentHashMap<RailShortcut, Intent>()
    private val noneMarker = Intent()

    fun intentFor(shortcut: RailShortcut): Intent? {
        val cached = cache[shortcut] ?: run {
            val resolved = if (shortcut.isDecorative) null else resolveIntent(shortcut)
            cache[shortcut] = resolved ?: noneMarker
            resolved
        }
        return cached.takeUnless { it === noneMarker }
    }

    companion object {
        /** Intent used for each shortcut role (no package pinned). */
        fun defaultIntent(shortcut: RailShortcut): Intent? = when (shortcut) {
            RailShortcut.Sparkles -> null
            RailShortcut.CircleDot -> Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
            RailShortcut.MessageCircle -> Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_MESSAGING)
            }
            RailShortcut.Send -> Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_EMAIL)
            }
            RailShortcut.Camera -> Intent("android.media.action.STILL_IMAGE_CAMERA")
            RailShortcut.Wind -> Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_WEATHER)
            }
            RailShortcut.PanelLeft -> Intent(Settings.ACTION_SETTINGS)
            RailShortcut.Phone -> Intent(Intent.ACTION_DIAL)
        }

        /** Real resolver bound to the app context. */
        fun forContext(@Suppress("UNUSED_PARAMETER") context: Context): RailShortcutResolver =
            RailShortcutResolver { shortcut ->
                defaultIntent(shortcut)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
    }
}
