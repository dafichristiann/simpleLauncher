package com.softhome.core.model

/**
 * Drawer category tabs (design/homeApp.pen node `B6gGM`:
 * "All / Communication / Entertainment / Tools").
 *
 * Mapping is from the raw `ApplicationInfo.category` Int so core:model stays
 * free of Android imports. Unknown / uncategorized apps are only shown under
 * [All]; [Tools] is the catch-all for mapped-but-unsorted categories.
 */
enum class DrawerCategory(val label: String) {
    All("All"),
    Communication("Communication"),
    Entertainment("Entertainment"),
    Tools("Tools"),
}

/**
 * Android `ApplicationInfo.category` constants, mirrored as plain Ints.
 * (Values match `android.content.pm.ApplicationInfo` and are stable API.)
 */
object AppCategory {
    const val UNDEFINED = -1
    const val GAME = 0
    const val AUDIO = 1
    const val VIDEO = 2
    const val IMAGE = 3
    const val SOCIAL = 4
    const val NEWS = 5
    const val MAPS = 6
    const val PRODUCTIVITY = 7
}

/** Pure mapping: raw category Int -> drawer tab. */
object DrawerCategoryMapper {

    fun tabFor(category: Int?): DrawerCategory = when (category) {
        AppCategory.SOCIAL -> DrawerCategory.Communication
        AppCategory.GAME, AppCategory.AUDIO, AppCategory.VIDEO -> DrawerCategory.Entertainment
        AppCategory.IMAGE, AppCategory.NEWS, AppCategory.MAPS, AppCategory.PRODUCTIVITY ->
            DrawerCategory.Tools
        else -> DrawerCategory.All
    }

    /** True when [app] belongs under [tab]. */
    fun matches(app: AppInfo, tab: DrawerCategory): Boolean =
        tab == DrawerCategory.All || tabFor(app.category) == tab

    fun filter(apps: List<AppInfo>, tab: DrawerCategory): List<AppInfo> =
        apps.filter { matches(it, tab) }
}
