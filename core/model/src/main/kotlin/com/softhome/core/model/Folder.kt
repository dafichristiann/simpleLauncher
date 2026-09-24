package com.softhome.core.model

/**
 * Folder on the launcher (P2 / D1-D2).
 *
 * Per decision P2-1, folders live in the **app drawer** (grid-based), not on the
 * row-based home. [apps] holds component keys (`package/class`), same identity as
 * [AppInfo.componentKey].
 */
data class Folder(
    val id: String,
    val name: String,
    val apps: List<String> = emptyList(),
)
