package com.softhome.launcher

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.provider.Settings
import android.view.View
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * System UI appearance for the launcher (P3 / F1 + F2).
 *
 * F1 -- status bar icon color follows the app theme: transparent bars, dark
 * (charcoal) icons on the cream theme, light icons on the warm-dark theme.
 *
 * F2 -- navigation bar: hidden (transient) under gesture navigation so the home is
 * edge-to-edge; a minimal/transparent bar under 3-button navigation. Detection is
 * best-effort; anything unknown degrades to the minimal style and never crashes.
 */
object SystemBarAppearance {

    /**
     * Apply the appearance for [darkTheme] to [activity]'s window.
     *
     * @param hideNavBar When true (gesture nav) hide the nav bar with transient
     *   reveal; when false show a transparent, minimal nav bar.
     */
    fun apply(activity: Activity, darkTheme: Boolean, hideNavBar: Boolean) {
        val window = activity.window
        val decor = window.decorView
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val controller = WindowInsetsControllerCompat(window, decor)
        // isAppearanceLight*Bars = true -> dark icons (for a light background).
        controller.isAppearanceLightStatusBars = !darkTheme
        controller.isAppearanceLightNavigationBars = !darkTheme

        if (hideNavBar) {
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.navigationBars())
        } else {
            controller.show(WindowInsetsCompat.Type.navigationBars())
        }
    }

    /** Transparent status + nav bars (icons drawn by the system). */
    @Suppress("DEPRECATION")
    fun makeBarsTransparent(activity: Activity) {
        activity.window.statusBarColor = Color.TRANSPARENT
        activity.window.navigationBarColor = Color.TRANSPARENT
    }

    /**
     * Best-effort gesture-navigation detection.
     *
     * The most reliable signal is the `Settings.Secure`
     * `navigation_mode` key (0 = 3-button, 1 = 2-button, 2 = gesture). It is not
     * always readable, so on failure we return false (show the minimal bar) --
     * hiding the bar is the riskier default.
     */
    fun isGestureNavigation(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return runCatching {
            Settings.Secure.getInt(
                activity.contentResolver,
                "navigation_mode",
                /* def = */ 0,
            ) == 2
        }.getOrDefault(false)
    }

    /** Convenience for callers that only have a [View]. */
    fun applyFromView(view: View, darkTheme: Boolean) {
        val activity = view.context as? Activity ?: return
        val controller = WindowInsetsControllerCompat(activity.window, view)
        controller.isAppearanceLightStatusBars = !darkTheme
        controller.isAppearanceLightNavigationBars = !darkTheme
    }
}
