package com.softhome.feature.home

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HomeStateTest {

    @Test
    fun starts_idle_by_convention() {
        assertThat(HomeState.Idle.isSearching).isFalse()
        assertThat(HomeState.Idle.isMusicOpen).isFalse()
    }

    @Test
    fun tapping_search_row_enters_search() {
        val next = HomeState.Idle.onTapRow(HomeRowId.Search)
        assertThat(next).isEqualTo(HomeState.Search)
        assertThat(next.isSearching).isTrue()
    }

    @Test
    fun tapping_music_row_enters_music() {
        val next = HomeState.Idle.onTapRow(HomeRowId.Music)
        assertThat(next).isEqualTo(HomeState.Music)
        assertThat(next.isMusicOpen).isTrue()
    }

    @Test
    fun tapping_notes_row_enters_notes() {
        val next = HomeState.Idle.onTapRow(HomeRowId.Notes)
        assertThat(next).isEqualTo(HomeState.Notes)
        assertThat(next.isNotesOpen).isTrue()
    }

    @Test
    fun tapping_the_active_notes_row_toggles_back_to_idle() {
        assertThat(HomeState.Notes.onTapRow(HomeRowId.Notes)).isEqualTo(HomeState.Idle)
    }

    @Test
    fun tapping_calendar_or_battery_rows_returns_to_idle() {
        assertThat(HomeState.Notes.onTapRow(HomeRowId.Calendar)).isEqualTo(HomeState.Idle)
        assertThat(HomeState.Notes.onTapRow(HomeRowId.BatteryStorage)).isEqualTo(HomeState.Idle)
    }

    @Test
    fun search_and_music_flags_are_unaffected_by_notes() {
        assertThat(HomeState.Notes.isSearching).isFalse()
        assertThat(HomeState.Notes.isMusicOpen).isFalse()
    }

    @Test
    fun tapping_the_active_row_toggles_back_to_idle() {
        assertThat(HomeState.Search.onTapRow(HomeRowId.Search)).isEqualTo(HomeState.Idle)
        assertThat(HomeState.Music.onTapRow(HomeRowId.Music)).isEqualTo(HomeState.Idle)
    }

    @Test
    fun tapping_a_non_interactive_row_returns_to_idle() {
        assertThat(HomeState.Music.onTapRow(HomeRowId.Weather)).isEqualTo(HomeState.Idle)
    }

    @Test
    fun switching_between_rows_crosses_states() {
        val search = HomeState.Idle.onTapRow(HomeRowId.Search)
        val music = search.onTapRow(HomeRowId.Music)
        assertThat(music).isEqualTo(HomeState.Music)
    }

    @Test
    fun reset_returns_to_idle() {
        assertThat(HomeState.Music.reset()).isEqualTo(HomeState.Idle)
    }
}
