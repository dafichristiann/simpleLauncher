package com.softhome.launcher

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.softhome.core.designsystem.theme.SoftHomeTheme
import com.softhome.core.model.DeviceStatusSnapshot
import com.softhome.feature.home.HomeScreen
import com.softhome.feature.home.HomeUiState
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

    private fun setHome(state: HomeUiState = HomeUiState()) = composeRule.setContent {
        SoftHomeTheme {
            HomeScreen(onOpenDrawer = {}, onVoiceSearch = {}, state = state)
        }
    }

    @Test
    fun shows_weather_and_music_rows() {
        setHome()
        composeRule.onNodeWithText("Current 8\u00B0C").assertIsDisplayed()
        composeRule.onNodeWithText("play music.").assertIsDisplayed()
        composeRule.onNodeWithText("Djo").assertIsDisplayed()
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

    @Test
    fun shows_calendar_row_with_no_events() {
        setHome()
        composeRule.onNodeWithText("No upcoming events").assertExists()
    }

    @Test
    fun shows_battery_row_with_real_values() {
        setHome(
            HomeUiState(
                deviceStatus = DeviceStatusSnapshot(batteryPercent = 42, storageUsedFraction = 0.5f),
            ),
        )
        composeRule.onNodeWithText("Battery").assertExists()
        composeRule.onNodeWithText("42%").assertExists()
    }

    @Test
    fun shows_notes_row_collapsed_with_preview() {
        setHome(HomeUiState(notes = "remember the milk"))
        composeRule.onNodeWithContentDescription("Quick notes").assertExists()
        composeRule.onNodeWithText("remember the milk").assertExists()
    }
}
