package com.softhome.core.model

/**
 * P5: the package -> drawer-category resolver (the 8 `ciHU3` groups).
 *
 * The design groups (`TpzL1`/`ciHU3`) are **app-specific** and cannot be derived from the
 * raw `ApplicationInfo.category` Int (e.g. "Finance & Shopping" holds DANA/GoPay/Shopee).
 * So the primary path is the per-package table [DrawerIconMap]; the OS category Int is only
 * a fallback for apps the table does not know, and [DrawerCategory.Other] is the catch-all.
 *
 * This is the single resolver the drawer + category pager use. `DrawerCategoryMapper`
 * remains for the legacy single-Int mapping (`tabFor`), delegating here.
 */
object DrawerCategoryResolver {

    /** The category for [packageName], or null when the design table does not know it. */
    fun knownCategoryOf(packageName: String): DrawerCategory? =
        DrawerIconMap.forPackage(packageName)?.category

    /**
     * The category for an app: the design table first, then the OS-category Int fallback,
     * then [DrawerCategory.Other].
     */
    fun categoryOf(app: AppInfo): DrawerCategory =
        knownCategoryOf(app.packageName) ?: fallbackFor(app.category)

    /**
     * The OS-category Int fallback (only used for apps the design table doesn't know).
     * Kept identical to the historical `DrawerCategoryMapper.fallbackTabFor` so existing
     * behaviour for uncatalogued apps is unchanged.
     */
    fun fallbackFor(category: Int?): DrawerCategory = when (category) {
        AppCategory.SOCIAL -> DrawerCategory.Communication
        AppCategory.GAME, AppCategory.AUDIO, AppCategory.VIDEO -> DrawerCategory.SocialEntertainment
        AppCategory.MAPS -> DrawerCategory.MapsTravel
        AppCategory.IMAGE -> DrawerCategory.CameraMedia
        AppCategory.NEWS -> DrawerCategory.BrowserSearch
        AppCategory.PRODUCTIVITY -> DrawerCategory.ProductivityTools
        else -> DrawerCategory.Other
    }

    /** True when [app] belongs under [tab] ([DrawerCategory.All] matches everything). */
    fun matches(app: AppInfo, tab: DrawerCategory): Boolean =
        tab == DrawerCategory.All || categoryOf(app) == tab

    /** The ordered pager tabs: [DrawerCategory.All] first, then the 8 design groups. */
    val pagerTabs: List<DrawerCategory> = listOf(DrawerCategory.All) + DrawerCategory.tabs
}
