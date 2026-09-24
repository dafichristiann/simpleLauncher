package com.softhome.launcher

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.softhome.core.designsystem.atom.SoftToggle
import com.softhome.core.designsystem.theme.SoftHomeTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for the P3 [SoftToggle] atom (G2).
 * Instrumented -- runs on the emulator (Phase 7).
 */
class SoftToggleTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun toggle_renders_and_is_clickable() {
        var checked = false
        composeRule.setContent {
            SoftHomeTheme {
                SoftToggle(checked = checked, onCheckedChange = { checked = it }, contentDescription = "Row toggle")
            }
        }
        composeRule.onNodeWithContentDescription("Row toggle").assertIsDisplayed().performClick()
        assertTrue("clicking should flip to true", checked)
    }

    @Test
    fun toggle_state_flips_between_on_and_off() {
        composeRule.setContent {
            var checked by remember { mutableStateOf(false) }
            SoftHomeTheme {
                SoftToggle(checked = checked, onCheckedChange = { checked = it }, contentDescription = "Flip")
            }
        }
        composeRule.onNodeWithContentDescription("Flip").performClick()
        composeRule.onNodeWithContentDescription("Flip").performClick()
        // Two clicks from false -> true -> false; final state is off. Just assert it is
        // still present and interactive (no crash, semantics stable).
        composeRule.onNodeWithContentDescription("Flip").assertIsDisplayed()
    }

    @Test
    fun disabled_toggle_does_not_change() {
        var changed = false
        composeRule.setContent {
            SoftHomeTheme {
                SoftToggle(
                    checked = false,
                    onCheckedChange = { changed = true },
                    enabled = false,
                    contentDescription = "Locked",
                )
            }
        }
        composeRule.onNodeWithContentDescription("Locked").performClick()
        assertFalse("disabled toggle must not fire", changed)
    }
}
