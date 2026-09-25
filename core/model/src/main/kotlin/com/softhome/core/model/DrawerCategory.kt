package com.softhome.core.model

/**
 * Drawer category tabs. Expanded from 4 to the **8 design categories** (design/homeApp.pen
 * node `ciHU3` "Warm Right Rail -- Icon Language Library"), which is also the source for
 * the per-package icon mapping ([DrawerIconMap]).
 *
 * Order matches the design library, top group first, with [All] pinned first as the
 * default view and [Other] as the catch-all for unknowns.
 *
 * The mapping is **app-specific** (package -> category) via [DrawerIconMap], NOT derived
 * from `ApplicationInfo.category` -- the design groups (e.g. "Finance & Shopping" with
 * DANA/GoPay/Shopee) cannot be expressed by the raw OS category. [DrawerCategoryMapper]
 * keeps the legacy OS-category mapping as a *fallback* for apps the map does not know.
 */
enum class DrawerCategory(val label: String) {
    All("All"),
    Communication("Communication"),
    SocialEntertainment("Social & Entertainment"),
    ProductivityTools("Productivity & Tools"),
    BrowserSearch("Browser & Search"),
    CameraMedia("Camera & Media"),
    MapsTravel("Maps & Travel"),
    FinanceShopping("Finance & Shopping"),
    FoodLifestyle("Food & Lifestyle"),
    Other("Other");

    companion object {
        /**
         * The real tabs (the 8 design groups); the pager shows All + these, with [Other]
         * as the catch-all (not a tab).
         */
        val tabs: List<DrawerCategory> = entries.filter { it != All && it != Other }
    }
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

/**
 * Pure mapping. Two paths:
 *  - **primary:** package -> category through [DrawerIconMap] (the design's grouping);
 *  - **fallback:** the legacy `ApplicationInfo.category` Int for unknown packages.
 *
 * Because the resolver lives in the same package, `matches`/`filter` consult it first.
 */
object DrawerCategoryMapper {

    /**
     * The drawer tab for [app]: the design's per-package category when known, else the
     * OS-category fallback, else [DrawerCategory.Other]. [All] matches everything.
     * (Delegates to [DrawerCategoryResolver] -- one resolver, no drift.)
     */
    fun categoryOf(app: AppInfo): DrawerCategory = DrawerCategoryResolver.categoryOf(app)

    /** Legacy OS-category fallback (only used when the per-package map doesn't know the app). */
    fun fallbackTabFor(category: Int?): DrawerCategory = DrawerCategoryResolver.fallbackFor(category)

    /** The legacy single-arg mapping (kept for compatibility + tests). */
    fun tabFor(category: Int?): DrawerCategory = DrawerCategoryResolver.fallbackFor(category)

    /** True when [app] belongs under [tab]. */
    fun matches(app: AppInfo, tab: DrawerCategory): Boolean =
        DrawerCategoryResolver.matches(app, tab)

    fun filter(apps: List<AppInfo>, tab: DrawerCategory): List<AppInfo> =
        apps.filter { matches(it, tab) }
}
