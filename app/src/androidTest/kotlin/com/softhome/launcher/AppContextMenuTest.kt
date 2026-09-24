package com.softhome.launcher

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.softhome.core.designsystem.atom.AppContextMenu
import com.softhome.core.designsystem.atom.ContextMenuItem
import com.softhome.core.designsystem.atom.LineIcon
import com.softhome.core.designsystem.theme.SoftHomeTheme
import com.softhome.core.model.AppActionLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for the P3 [AppContextMenu] atom (F3).
 * Instrumented -- runs on the emulator (Phase 7).
 */
class AppContextMenuTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(
        isSystemApp: Boolean = false,
        onAction: (AppActionLogic.Action) -> Unit = {},
    ) {
        composeRule.setContent {
            var dismissed = false
            SoftHomeTheme {
                AppContextMenu(
                    items = listOf(
                        ContextMenuItem("Open", LineIcon.ArrowUpRight, onClick = { onAction(AppActionLogic.Action.Open) }),
                        ContextMenuItem("App Info", LineIcon.Info, onClick = { onAction(AppActionLogic.Action.AppInfo) }),
                        ContextMenuItem(
                            "Uninstall",
                            LineIcon.Trash2,
                            enabled = AppActionLogic.isEnabled(
                                AppActionLogic.Action.Uninstall,
                                isSystemApp = isSystemApp,
                                isRemovable = true,
                            ),
                            destructive = true,
                            onClick = { onAction(AppActionLogic.Action.Uninstall) },
                        ),
                    ),
                    onDismiss = { dismissed = true },
                )
            }
        }
    }

    @Test
    fun shows_expected_rows() {
        show()
        composeRule.onNodeWithText("Open").assertIsDisplayed()
        composeRule.onNodeWithText("App Info").assertIsDisplayed()
        composeRule.onNodeWithText("Uninstall").assertIsDisplayed()
    }

    @Test
    fun tapping_a_row_fires_its_action() {
        val fired = ArrayList<AppActionLogic.Action>()
        show(onAction = { fired.add(it) })
        composeRule.onNodeWithText("App Info").performClick()
        assertEquals(listOf(AppActionLogic.Action.AppInfo), fired)
    }

    @Test
    fun uninstall_row_is_present_but_disabled_for_system_apps() {
        var fired = false
        show(isSystemApp = true, onAction = { if (it == AppActionLogic.Action.Uninstall) fired = true })
        composeRule.onNodeWithText("Uninstall").assertIsDisplayed().performClick()
        // Greyed row must not fire its action (P3-4).
        assertTrue("system-app uninstall must be a no-op", !fired)
    }
}
