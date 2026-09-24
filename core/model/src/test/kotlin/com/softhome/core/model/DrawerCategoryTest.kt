package com.softhome.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DrawerCategoryTest {

    private fun app(name: String, category: Int?) = AppInfo(
        packageName = "com.example.$name",
        className = "Main",
        label = name,
        category = category,
    )

    @Test
    fun social_maps_to_communication() {
        assertThat(DrawerCategoryMapper.tabFor(AppCategory.SOCIAL))
            .isEqualTo(DrawerCategory.Communication)
    }

    @Test
    fun game_audio_video_map_to_entertainment() {
        assertThat(DrawerCategoryMapper.tabFor(AppCategory.GAME))
            .isEqualTo(DrawerCategory.Entertainment)
        assertThat(DrawerCategoryMapper.tabFor(AppCategory.AUDIO))
            .isEqualTo(DrawerCategory.Entertainment)
        assertThat(DrawerCategoryMapper.tabFor(AppCategory.VIDEO))
            .isEqualTo(DrawerCategory.Entertainment)
    }

    @Test
    fun productivity_maps_to_tools() {
        assertThat(DrawerCategoryMapper.tabFor(AppCategory.PRODUCTIVITY))
            .isEqualTo(DrawerCategory.Tools)
    }

    @Test
    fun undefined_and_null_map_to_all_only() {
        assertThat(DrawerCategoryMapper.tabFor(null)).isEqualTo(DrawerCategory.All)
        assertThat(DrawerCategoryMapper.tabFor(AppCategory.UNDEFINED))
            .isEqualTo(DrawerCategory.All)
    }

    @Test
    fun all_tab_matches_everything() {
        val apps = listOf(app("A", AppCategory.SOCIAL), app("B", null))
        assertThat(DrawerCategoryMapper.filter(apps, DrawerCategory.All)).hasSize(2)
    }

    @Test
    fun filter_by_category_is_exact() {
        val apps = listOf(
            app("Chat", AppCategory.SOCIAL),
            app("Game", AppCategory.GAME),
            app("Docs", AppCategory.PRODUCTIVITY),
            app("Mystery", null),
        )
        assertThat(DrawerCategoryMapper.filter(apps, DrawerCategory.Communication))
            .containsExactly(apps[0])
        assertThat(DrawerCategoryMapper.filter(apps, DrawerCategory.Entertainment))
            .containsExactly(apps[1])
        assertThat(DrawerCategoryMapper.filter(apps, DrawerCategory.Tools))
            .containsExactly(apps[2])
        // Uncategorized apps never appear outside "All".
        assertThat(DrawerCategoryMapper.filter(apps, DrawerCategory.Tools))
            .doesNotContain(apps[3])
    }
}
