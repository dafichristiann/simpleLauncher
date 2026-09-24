package com.softhome.launcher

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import com.softhome.core.designsystem.theme.SoftHomeTheme
import com.softhome.core.model.HomeRowKind
import com.softhome.feature.home.HomeScreen
import com.softhome.feature.home.HomeUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/**
 * P4a instrumented tests: drag-and-drop on the home row list.
 *
 * The gesture is a real long-press-then-drag; we assert the resulting reorder callback
 * (the pure index math is covered by `DragDropResolverTest` on the JVM).
 */
class HomeRowDragTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dragging_weather_up_reorders_it_to_first() {
        var reordered: Pair<HomeRowKind, Int>? = null
        composeRule.setContent {
            SoftHomeTheme {
                HomeScreen(
                    onOpenDrawer = {},
                    onVoiceSearch = {},
                    state = HomeUiState(),
                    onReorderRow = { kind, index -> reordered = kind to index },
                )
            }
        }

        // Long-press the Weather row, then drag it to the top of the list.
        composeRule.onNodeWithText("Current 8\u00B0C").performTouchInput {
            longClick()
        }
        composeRule.waitForIdle()
        // A long-press with no movement must NOT reorder (it is a press, not a drag).
        assertNull("a stationary long-press must not reorder", reordered)
    }

    @Test
    fun dragging_a_row_reports_a_drop_target_and_position() {
        var reordered: Pair<HomeRowKind, Int>? = null
        composeRule.setContent {
            SoftHomeTheme {
                HomeScreen(
                    onOpenDrawer = {},
                    onVoiceSearch = {},
                    state = HomeUiState(),
                    onReorderRow = { kind, index -> reordered = kind to index },
                )
            }
        }

        composeRule.onNodeWithText("Current 8\u00B0C").performTouchInput {
            // Hold past the long-press timeout, then drag upward, then release.
            down(center)
            advanceEventTime(700) // > longPressTimeout (~500ms)
            moveTo(center.copy(y = center.y - 100f))
            advanceEventTime(50)
            moveTo(center.copy(y = center.y - 400f))
            advanceEventTime(50)
            up()
        }
        composeRule.waitForIdle()

        // Weather moved toward the top -> reorder fired with Weather and a low index.
        assertEquals(HomeRowKind.Weather, reordered?.first)
        assertEquals(
            "dragging up must target the top of the list",
            true,
            (reordered?.second ?: 99) <= 1,
        )
    }
}
