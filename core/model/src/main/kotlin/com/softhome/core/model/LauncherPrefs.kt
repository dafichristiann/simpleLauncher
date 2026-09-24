package com.softhome.core.model

/** Launcher-wide user preferences (persisted via DataStore). */
data class LauncherPrefs(
    val grid: GridConfig = GridConfig.Default,
    val activeIconPackId: String? = null,
    val maskUnsupportedApps: Boolean = true,
    val darkTheme: ThemeMode = ThemeMode.System,
    val showNotificationBadges: Boolean = true,
    /** component key -> pack id (per-app icon override). */
    val iconOverrides: Map<String, String> = emptyMap(),
    /** P3 (G/Widgets): which home rows show and in what order. */
    val homeRows: List<HomeRowPref> = HomeRowLogic.default(),
    /** P3 (G/Appearance): drawer + row spacing multiplier preset (Q3). */
    val spacing: SpacingScale = SpacingScale.Normal,
    /** P3 (Q2): component keys hidden from the drawer (reversible "Remove"). */
    val hiddenApps: Set<String> = emptySet(),
)

enum class ThemeMode { Light, Dark, System }

/**
 * Spacing preset (P3 / Q3). A **multiplier** applied to drawer grid gaps + home row
 * vertical padding, not a set of exact dp values.
 */
enum class SpacingScale(val factor: Float) {
    Compact(0.88f),
    Normal(1.00f),
    Roomy(1.12f),
}

