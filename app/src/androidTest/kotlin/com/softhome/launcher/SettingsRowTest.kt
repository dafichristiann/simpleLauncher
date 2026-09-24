package com.softhome.launcher

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.softhome.core.designsystem.atom.SettingsRow
import com.softhome.core.designsystem.atom.SoftToggle
import com.softhome.core.designsystem.theme.SoftHomeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for the P3 [SettingsRow] atom (G).
 * Instrumented -- runs on the emulator (Phase 7).
 */
class SettingsRowTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun renders_label_and_supporting_text() {
        composeRule.setContent {
            SoftHomeTheme {
                SettingsRow(label = "Theme", supporting = "System")
            }
        }
        composeRule.onNodeWithText("Theme").assertIsDisplayed()
        composeRule.onNodeWithText("System").assertIsDisplayed()
    }

    @Test
    fun clickable_row_fires_onClick() {
        var clicks = 0
        composeRule.setContent {
            SoftHomeTheme {
                SettingsRow(label = "Wallpaper", onClick = { clicks++ })
            }
        }
        composeRule.onNodeWithText("Wallpaper").performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun reorder_buttons_fire_move_callbacks() {
        val fired = ArrayList<String>()
        composeRule.setContent {
            SoftHomeTheme {
                SettingsRow(
                    label = "Notes",
                    onMoveUp = { fired.add("up") },
                    onMoveDown = { fired.add("down") },
                )
            }
        }
        composeRule.onNodeWithContentDescription("Move up").performClick()
        composeRule.onNodeWithContentDescription("Move down").performClick()
        assertEquals(listOf("up", "down"), fired)
    }

    @Test
    fun toggle_slot_renders_and_toggles() {
        var on = true
        composeRule.setContent {
            SoftHomeTheme {
                SettingsRow(
                    label = "Show Notes",
                    toggle = {
                        SoftToggle(checked = on, onCheckedChange = { on = it }, contentDescription = "Show Notes toggle")
                    },
                )
            }
        }
        composeRule.onNodeWithContentDescription("Show Notes toggle").performClick()
        assertEquals(false, on)
    }
}
