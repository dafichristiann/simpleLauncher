package com.softhome.core.model

/**
 * A launchable app on the device.
 *
 * Pure domain model -- no Android imports. The data layer maps PackageManager
 * entries into this.
 */
data class AppInfo(
    val packageName: String,
    val className: String,
    val label: String,
    /** Component key = "package/class", used as a stable identity. */
    val componentKey: String = "$packageName/$className",
    val isSystem: Boolean = false,
    /**
     * Raw `ApplicationInfo.category` value (e.g. CATEGORY_SOCIAL = 4), or null
     * when the app declares none. Kept as a raw Int so core:model stays free of
     * Android imports; mapped to a drawer tab in the appdrawer feature.
     */
    val category: Int? = null,
) {
    /** First sortable letter for the alphabet index; "#" for non-letters. */
    val indexLetter: Char
        get() {
            val c = label.trim().firstOrNull()?.uppercaseChar() ?: return '#'
            return if (c in 'A'..'Z') c else '#'
        }
}
