package com.softhome.launcher

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import com.softhome.core.designsystem.theme.SoftHomeTheme
import com.softhome.core.model.DeviceStatusSnapshot
import com.softhome.core.model.HomeRowKind
import com.softhome.core.model.HomeRowLogic
import com.softhome.feature.home.AppsUiState
import com.softhome.feature.home.PrefsUiState
import com.softhome.feature.home.NotesUiState
import com.softhome.feature.home.RailUiState
import com.softhome.feature.home.DeviceStatusUiState
import com.softhome.feature.home.HomeRowsUiState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI smoke tests for the Warm Right Rail home screen
 * (design/homeApp.pen frame znb90). Verifies the row list + rail render,
 * plus the P2 widget rows (calendar / battery-storage / notes).
 */
class HomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setHome(
        appsState: AppsUiState = AppsUiState(),
        prefsState: PrefsUiState = PrefsUiState(),
        notesState: NotesUiState = NotesUiState(),
        railState: RailUiState = RailUiState(),
        deviceStatusState: DeviceStatusUiState = DeviceStatusUiState(),
        homeRowsState: HomeRowsUiState = HomeRowsUiState(),
    ) = composeRule.setContent {
        SoftHomeTheme {
            HomeScreen(
                onOpenDrawer = {},
                onVoiceSearch = {},
                appsState = appsState,
                prefsState = prefsState,
                notesState = notesState,
                railState = railState,
                deviceStatusState = deviceStatusState,
                homeRowsState = homeRowsState,
            )
        }
    }

    @Test
    fun shows_weather_and_notes_rows() {
        setHome()
        composeRule.onNodeWithText("Current 8\u00B0C").assertIsDisplayed()
    }

    @Test
    fun shows_search_row_placeholder() {
        setHome()
        composeRule.onNodeWithText("f i n d  s o m e t h i n g").assertIsDisplayed()
    }

    @Test
    fun rail_is_announced_and_has_shortcuts() {
        setHome()
        composeRule.onNodeWithContentDescription("Shortcut rail").assertIsDisplayed()
        // Camera is one of the 8 rail shortcuts.
        composeRule.onNodeWithContentDescription("Camera").assertExists()
    }

    // --- P2 widget rows -------------------------------------------------------

    /** P2: Calendar/Battery/Notes are hidden by default; these tests opt them back in. */
    private fun allRowsVisible() = HomeRowLogic.DEFAULT_ORDER.map {
        com.softhome.core.model.HomeRowPref(it, visible = true)
    }

    @Test
    fun shows_calendar_row_with_no_events() {
        setHome(homeRowsState = HomeRowsUiState(homeRows = allRowsVisible()))
        composeRule.onNodeWithText("No upcoming events").assertExists()
    }

    @Test
    fun shows_battery_row_with_real_values() {
        setHome(
            deviceStatusState = DeviceStatusUiState(
                deviceStatus = DeviceStatusSnapshot(batteryPercent = 42, storageUsedFraction = 0.5f),
            ),
            homeRowsState = HomeRowsUiState(homeRows = allRowsVisible()),
        )
        composeRule.onNodeWithText("Battery").assertExists()
        composeRule.onNodeWithText("42%").assertExists()
    }

    @Test
    fun shows_notes_row_collapsed_with_preview() {
        setHome(
            notesState = NotesUiState(notes = "remember the milk"),
            homeRowsState = HomeRowsUiState(homeRows = allRowsVisible()),
        )
        composeRule.onNodeWithContentDescription("Quick notes").assertExists()
        composeRule.onNodeWithText("remember the milk").assertExists()
    }

    @Test
    fun default_home_hides_calendar_battery_and_notes() {
        // P2: the default home shows only Time/Date/Weather/Search (music row removed).
        setHome()
        composeRule.onNodeWithText("No upcoming events").assertDoesNotExist()   // Calendar row
        composeRule.onNodeWithText("Battery").assertDoesNotExist()              // Battery row
        composeRule.onNodeWithContentDescription("Quick notes").assertDoesNotExist() // Notes row
        // The four kept rows are present.
        composeRule.onNodeWithText("Current 8\u00B0C").assertIsDisplayed()      // Weather
        composeRule.onNodeWithText("f i n d  s o m e t h i n g").assertIsDisplayed() // Search
    }

    // --- P3 (Q2): tap launches, long-press expands ---------------------------------

    @Test
    fun tapping_search_row_does_not_expand_it_launches_instead() {
        // Q2: a tap on the Search row is a LAUNCH (here onOpenRow is captured, not the
        // in-place expand). The row must not enter the "searching" state on a tap.
        var launched = false
        composeRule.setContent {
            SoftHomeTheme {
                HomeScreen(
                    onOpenDrawer = {},
                    onVoiceSearch = {},
                    appsState = AppsUiState(),
                    prefsState = PrefsUiState(),
                    notesState = NotesUiState(),
                    railState = RailUiState(),
                    deviceStatusState = DeviceStatusUiState(),
                    homeRowsState = HomeRowsUiState(),
                    onLaunchRow = { launched = true },
                )
            }
        }
        composeRule.onNodeWithContentDescription("Find something").performClick()
        composeRule.waitForIdle()
        assertTrue("tap on the Search row must launch, not expand (Q2)", launched)
    }

    @Test
    fun long_pressing_search_row_expands_in_place() {
        // Q2: the in-place expand stays on LONG-press. After a long-press the row shows
        // its focused "searching" text (HomeState.Search).
        composeRule.setContent {
            SoftHomeTheme {
                HomeScreen(
                    onOpenDrawer = {},
                    onVoiceSearch = {},
                    appsState = AppsUiState(),
                    prefsState = PrefsUiState(),
                    notesState = NotesUiState(),
                    railState = RailUiState(),
                    deviceStatusState = DeviceStatusUiState(),
                    homeRowsState = HomeRowsUiState(),
                    onLaunchRow = { },
                )
            }
        }
        composeRule.onNodeWithContentDescription("Find something").performTouchInput { longClick() }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("s e a r c h i n g").assertExists()
    }

    @Test
    fun tapping_the_settings_rail_icon_opens_settings() {
        var opened = false
        composeRule.setContent {
            SoftHomeTheme {
                HomeScreen(
                    onOpenDrawer = {},
                    onVoiceSearch = {},
                    appsState = AppsUiState(),
                    prefsState = PrefsUiState(),
                    notesState = NotesUiState(),
                    railState = RailUiState(),
                    deviceStatusState = DeviceStatusUiState(),
                    homeRowsState = HomeRowsUiState(),
                    onOpenSettings = { opened = true },
                )
            }
        }
        composeRule.onNodeWithContentDescription("Settings").performClick()
        assertTrue("panel-left rail icon must open settings (Q1)", opened)
    }

    @Test
    fun long_pressing_a_rail_icon_opens_the_context_menu() {
        composeRule.setContent {
            SoftHomeTheme {
                HomeScreen(
                    onOpenDrawer = {},
                    onVoiceSearch = {},
                    appsState = AppsUiState(),
                    prefsState = PrefsUiState(),
                    notesState = NotesUiState(),
                    railState = RailUiState(),
                    deviceStatusState = DeviceStatusUiState(),
                    homeRowsState = HomeRowsUiState(),
                )
            }
        }
        composeRule.onNodeWithContentDescription("Settings").performTouchInput { longClick() }
        // The row-style menu appears with an "App Info" row.
        composeRule.onNodeWithText("App Info").assertExists()
    }

    // --- P3 (G/Widgets): row visibility + order -------------------------------

    @Test
    fun hidden_row_is_not_rendered() {
        // Search is visible by default; toggling it off hides the row, while another
        // kept row (weather) still renders.
        val rows = HomeRowLogic.toggle(HomeRowLogic.default(), HomeRowKind.Search)
        setHome(homeRowsState = HomeRowsUiState(homeRows = rows))
        composeRule.onNodeWithText("f i n d  s o m e t h i n g").assertDoesNotExist()
        composeRule.onNodeWithText("Current 8\u00B0C").assertExists()
    }

    @Test
    fun locked_rows_render_even_when_all_toggleable_rows_hidden() {
        // Force every hideable row hidden regardless of its default (P2) state.
        var rows = HomeRowLogic.default()
        HomeRowLogic.DEFAULT_ORDER.filter { HomeRowLogic.canHide(it) }.forEach { kind ->
            if (rows.first { it.kind == kind }.visible) rows = HomeRowLogic.toggle(rows, kind)
        }
        setHome(homeRowsState = HomeRowsUiState(homeRows = rows))
        // Weather (locked) still renders; search (hidden) does not.
        composeRule.onNodeWithText("Current 8\u00B0C").assertExists()
        composeRule.onNodeWithText("f i n d  s o m e t h i n g").assertDoesNotExist()
    }
}
