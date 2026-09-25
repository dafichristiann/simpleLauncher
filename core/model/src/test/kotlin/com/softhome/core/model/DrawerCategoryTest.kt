package com.softhome.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * P5: the 8-category drawer model. Categories come primarily from the per-package
 * [DrawerIconMap] (the design's grouping), with the OS-category Int as a fallback.
 */
class DrawerCategoryTest {

    private fun app(name: String, category: Int?, pkg: String = "com.example.$name") = AppInfo(
        packageName = pkg,
        className = "Main",
        label = name,
        category = category,
    )

    @Test
    fun there_are_eight_real_tabs_plus_all_and_other() {
        // All + 8 design groups + Other = 10 entries; tabs = the 8 design groups.
        assertThat(DrawerCategory.tabs).hasSize(8)
        assertThat(DrawerCategory.tabs.map { it.label }).containsExactly(
            "Communication", "Social & Entertainment", "Productivity & Tools",
            "Browser & Search", "Camera & Media", "Maps & Travel",
            "Finance & Shopping", "Food & Lifestyle",
        ).inOrder()
    }

    @Test
    fun known_package_uses_the_design_category() {
        // Spotify -> Social & Entertainment (from the .pen), NOT the OS category.
        val spotify = app("Spotify", AppCategory.AUDIO, pkg = "com.spotify.music")
        assertThat(DrawerCategoryMapper.categoryOf(spotify))
            .isEqualTo(DrawerCategory.SocialEntertainment)
        // DANA -> Finance & Shopping.
        val dana = app("DANA", null, pkg = "id.dana")
        assertThat(DrawerCategoryMapper.categoryOf(dana))
            .isEqualTo(DrawerCategory.FinanceShopping)
    }

    @Test
    fun unknown_package_falls_back_to_os_category() {
        val social = app("Foo", AppCategory.SOCIAL, pkg = "com.unknown.foo")
        assertThat(DrawerCategoryMapper.categoryOf(social))
            .isEqualTo(DrawerCategory.Communication)
        val game = app("Bar", AppCategory.GAME, pkg = "com.unknown.bar")
        assertThat(DrawerCategoryMapper.categoryOf(game))
            .isEqualTo(DrawerCategory.SocialEntertainment)
        val maps = app("Baz", AppCategory.MAPS, pkg = "com.unknown.baz")
        assertThat(DrawerCategoryMapper.categoryOf(maps))
            .isEqualTo(DrawerCategory.MapsTravel)
    }

    @Test
    fun unknown_package_with_no_category_is_other() {
        val mystery = app("Mystery", null, pkg = "com.unknown.mystery")
        assertThat(DrawerCategoryMapper.categoryOf(mystery)).isEqualTo(DrawerCategory.Other)
    }

    @Test
    fun all_tab_matches_everything() {
        val apps = listOf(app("A", AppCategory.SOCIAL), app("B", null))
        assertThat(DrawerCategoryMapper.filter(apps, DrawerCategory.All)).hasSize(2)
    }

    @Test
    fun filter_by_category_is_exact_and_other_is_the_catch_all() {
        val apps = listOf(
            app("Chat", AppCategory.SOCIAL, pkg = "com.unknown.chat"),
            app("Game", AppCategory.GAME, pkg = "com.unknown.game"),
            app("Docs", AppCategory.PRODUCTIVITY, pkg = "com.unknown.docs"),
            app("Mystery", null, pkg = "com.unknown.mystery"),
        )
        assertThat(DrawerCategoryMapper.filter(apps, DrawerCategory.Communication))
            .containsExactly(apps[0])
        assertThat(DrawerCategoryMapper.filter(apps, DrawerCategory.SocialEntertainment))
            .containsExactly(apps[1])
        assertThat(DrawerCategoryMapper.filter(apps, DrawerCategory.ProductivityTools))
            .containsExactly(apps[2])
        // Uncategorized apps land in "Other", not in a real group.
        assertThat(DrawerCategoryMapper.filter(apps, DrawerCategory.Other))
            .containsExactly(apps[3])
    }
}
