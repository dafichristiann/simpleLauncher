package com.softhome.feature.home

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import com.softhome.core.model.HomeRowKind

/**
 * P3: resolves a tappable home row to a **launchable intent** for the right app, with a
 * graceful fallback chain so a tap never crashes and never silently does nothing.
 *
 * Mirrors [RailShortcutResolver]: the pure decision (which intent to build, in what
 * order) lives here and is unit-tested with a fake resolver; the Android wiring passes a
 * real `PackageManager.resolveActivity`-backed lambda.
 *
 * **Fallback chain per row (the user's explicit requirement):**
 *  1. the primary **implicit** intent (an OEM-registered category/action);
 *  2. else a set of **known launcher packages** (Tecno-first, then AOSP/Google);
 *  3. else a **chooser** over the best remaining intent, if any;
 *  4. else `null` -> the caller shows a "no app" message (never crashes).
 *
 * Decisions (plan §9, user-approved):
 *  - **Q2:** a row **tap launches** the app; the in-place expand stays on **long-press**.
 *  - **Q3:** when no weather app resolves, fall back to a **browser weather URL**, not a
 *    toast.
 */
class RowLaunchResolver(
    /** Returns the resolved component for an intent, or null when nothing handles it. */
    private val resolve: (Intent) -> ComponentName?,
) {

    /**
     * The launch intent for [kind], or null when nothing can handle it (the caller shows
     * a "No app found" message). Rows with no meaningful app target (e.g. none today)
     * return null.
     */
    fun intentFor(kind: HomeRowKind): Intent? {
        val candidates = primaryIntents(kind)
        // 1. Primary implicit intent(s) that actually resolve.
        candidates.firstOrNull { resolve(it) != null }?.let { return withLauncherFlags(it) }

        // 2. Known-package launcher activity (Tecno-first, then AOSP/Google).
        knownPackages(kind).forEach { pkg ->
            val explicit = explicitLauncherIntent(pkg)
            if (resolve(explicit) != null) return withLauncherFlags(explicit)
        }

        // 3. Chooser over the best remaining intent (a row whose target is "any handler").
        chooserIntent(kind)?.let { return it }

        // 4. No target.
        return null
    }

    /** The primary implicit intent(s) to try, best first. */
    private fun primaryIntents(kind: HomeRowKind): List<Intent> = when (kind) {
        // A tap on the clock opens the Clock app. Android has **no** clock category, and a
        // bare SET_ALARM is gated behind `com.android.alarm.permission.SET_ALARM` (it
        // RESOLVES but `startActivity` is denied on the Tecno -- verified on-device), so the
        // primary path is empty and the known clock package (below) opens the app itself.
        HomeRowKind.Time -> emptyList()
        HomeRowKind.Date -> listOf(
            Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_CALENDAR) },
        )
        HomeRowKind.Weather -> listOf(
            Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_WEATHER) },
        )
        HomeRowKind.Search -> listOf(
            // Honors the user's default browser/search app; no app is hardcoded (Q3 spirit).
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")),
        )
        // Non-launching rows in the current design.
        HomeRowKind.Calendar, HomeRowKind.BatteryStorage, HomeRowKind.Notes -> emptyList()
    }

    /** Known launcher packages per row (Tecno first, then AOSP/Google). */
    private fun knownPackages(kind: HomeRowKind): List<String> = when (kind) {
        HomeRowKind.Time -> listOf(
            "com.transsion.deskclock", "com.google.android.deskclock", "com.android.deskclock",
        )
        HomeRowKind.Date -> listOf(
            "com.transsion.calendar", "com.google.android.calendar", "com.android.calendar",
        )
        HomeRowKind.Weather -> listOf(
            "com.rlk.weathers", "com.google.android.apps.weather", "com.transsion.weather",
        )
        else -> emptyList()
    }

    /**
     * The last-resort chooser. Only Weather gets one (Q3): if no weather app exists at all,
     * open the browser at a weather URL for the current location rather than a dead tap.
     */
    private fun chooserIntent(kind: HomeRowKind): Intent? = when (kind) {
        HomeRowKind.Weather -> withLauncherFlags(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=weather")),
        )
        else -> null
    }

    private fun explicitLauncherIntent(pkg: String): Intent =
        Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setPackage(pkg)

    private fun withLauncherFlags(intent: Intent): Intent =
        intent.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

    companion object {
        /**
         * Real resolver bound to a `(Intent) -> ComponentName?` (typically
         * `context.packageManager.resolveActivity(intent, MATCH_DEFAULT_ONLY)?.activityInfo
         * ?.let { ComponentName(it.packageName, it.name) }`), kept as a plain lambda so the
         * resolver is testable without a device.
         */
        fun forResolver(resolve: (Intent) -> ComponentName?): RowLaunchResolver =
            RowLaunchResolver(resolve)
    }
}
