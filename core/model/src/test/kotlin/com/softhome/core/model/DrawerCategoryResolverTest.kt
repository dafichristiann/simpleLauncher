package com.softhome.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * P5: the package -> 8-category resolver ([DrawerCategoryResolver]), the data behind the
 * category pager. The design grouping comes from `DrawerIconMap`; the OS-category Int is
 * only a fallback for apps the design table does not know.
 */
class DrawerCategoryResolverTest {

    private fun app(pkg: String, label: String = "App", category: Int? = null) =
        AppInfo(packageName = pkg, className = "$pkg.Main", label = label, category = category)

    @Test
    fun `design table drives the category for known packages`() {
        // Spotify is Audio per the OS, but the design groups it under Social & Entertainment.
        assertThat(DrawerCategoryResolver.categoryOf(app("com.spotify.music", category = AppCategory.AUDIO)))
            .isEqualTo(DrawerCategory.SocialEntertainment)
        assertThat(DrawerCategoryResolver.categoryOf(app("id.dana")))
            .isEqualTo(DrawerCategory.FinanceShopping)
        assertThat(DrawerCategoryResolver.categoryOf(app("com.gojek.gopay")))
            .isEqualTo(DrawerCategory.FinanceShopping) // GoPay, not Gojek
        assertThat(DrawerCategoryResolver.categoryOf(app("com.android.chrome")))
            .isEqualTo(DrawerCategory.BrowserSearch)
        assertThat(DrawerCategoryResolver.categoryOf(app("com.kfc.mobile")))
            .isEqualTo(DrawerCategory.FoodLifestyle)
        assertThat(DrawerCategoryResolver.categoryOf(app("com.rlk.weathers")))
            .isEqualTo(DrawerCategory.MapsTravel)
    }

    @Test
    fun `unknown packages fall back to the OS category Int`() {
        assertThat(DrawerCategoryResolver.categoryOf(app("com.unknown.chat", category = AppCategory.SOCIAL)))
            .isEqualTo(DrawerCategory.Communication)
        assertThat(DrawerCategoryResolver.categoryOf(app("com.unknown.game", category = AppCategory.GAME)))
            .isEqualTo(DrawerCategory.SocialEntertainment)
        assertThat(DrawerCategoryResolver.categoryOf(app("com.unknown.map", category = AppCategory.MAPS)))
            .isEqualTo(DrawerCategory.MapsTravel)
        assertThat(DrawerCategoryResolver.categoryOf(app("com.unknown.docs", category = AppCategory.PRODUCTIVITY)))
            .isEqualTo(DrawerCategory.ProductivityTools)
    }

    @Test
    fun `unknown package with no OS category is Other`() {
        assertThat(DrawerCategoryResolver.categoryOf(app("com.unknown.mystery")))
            .isEqualTo(DrawerCategory.Other)
    }

    @Test
    fun `knownCategoryOf is null for unknown packages`() {
        assertThat(DrawerCategoryResolver.knownCategoryOf("com.unknown.mystery")).isNull()
        assertThat(DrawerCategoryResolver.knownCategoryOf("id.dana"))
            .isEqualTo(DrawerCategory.FinanceShopping)
    }

    @Test
    fun `matches is exact and All matches everything`() {
        val dana = app("id.dana")
        assertThat(DrawerCategoryResolver.matches(dana, DrawerCategory.All)).isTrue()
        assertThat(DrawerCategoryResolver.matches(dana, DrawerCategory.FinanceShopping)).isTrue()
        assertThat(DrawerCategoryResolver.matches(dana, DrawerCategory.BrowserSearch)).isFalse()
    }

    @Test
    fun `pagerTabs is All followed by the eight design groups`() {
        assertThat(DrawerCategoryResolver.pagerTabs.first()).isEqualTo(DrawerCategory.All)
        assertThat(DrawerCategoryResolver.pagerTabs).hasSize(9)
        assertThat(DrawerCategoryResolver.pagerTabs.drop(1)).isEqualTo(DrawerCategory.tabs)
        assertThat(DrawerCategoryResolver.pagerTabs).doesNotContain(DrawerCategory.Other)
    }
}
