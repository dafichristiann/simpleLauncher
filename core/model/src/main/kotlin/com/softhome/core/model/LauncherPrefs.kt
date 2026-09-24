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
)

enum class ThemeMode { Light, Dark, System }
