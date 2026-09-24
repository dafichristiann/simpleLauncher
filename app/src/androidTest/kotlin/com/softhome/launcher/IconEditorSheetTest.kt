package com.softhome.launcher

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.softhome.core.designsystem.theme.SoftHomeTheme
import com.softhome.core.model.DrawerIconTokenName
import com.softhome.core.model.IconOverride
import com.softhome.core.model.IconPack
import com.softhome.feature.iconpack.data.IconPackDrawableLoader
import com.softhome.feature.iconpack.domain.IconEditor
import com.softhome.feature.iconpack.ui.IconEditorSheet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

/**
 * P4b instrumented tests: the icon editor body is a pure composable. We drive it with a
 * fixed [IconEditor.State] (the state machine itself is covered by the JVM
 * `IconEditorTest`) and assert the mode rendering + the callbacks the drawer relies on.
 */
class IconEditorSheetTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun packOf(vararg drawables: String) = IconPack(
        id = "pack", name = "Pack", sourcePath = "/p",
        entries = drawables.associate { "com.x/$it" to it }, hasAppFilter = true,
    )

    @Test
    fun packApp_showsDrawableGrid_andSaveIsGatedUntilChanged() {
        var saved = false
        val state = IconEditor.start(
            componentKey = "com.foo/Main",
            packDrawables = listOf("a", "b"),
            automaticGlyph = "Phone",
            automaticColor = DrawerIconTokenName.Communication,
            existing = IconOverride.Pack("a"), // unchanged from selection -> Save disabled
        )
        composeRule.setContent {
            SoftHomeTheme {
                IconEditorSheet(
                    appLabel = "Foo",
                    state = state,
                    activePack = packOf("a", "b"),
                    drawableLoader = IconPackDrawableLoader(androidx.test.core.app.ApplicationProvider.getApplicationContext()),
                    category = null,
                    onSelectMode = {},
                    onSelectDrawable = {},
                    onSelectGlyph = {},
                    onSelectColor = {},
                    onReset = {},
                    onSave = { saved = true },
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("Edit icon \u00B7 Foo").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Pack drawables").assertIsDisplayed()
        // Save is disabled (unchanged) -> clicking does nothing.
        composeRule.onNodeWithText("Save").performClick()
        composeRule.waitForIdle()
        assertEquals("Save must be a no-op when nothing changed", false, saved)
    }

    @Test
    fun nonPackApp_showsGlyphGrid_andSelectingAGlyphFires() {
        var selectedGlyph: String? = null
        val state = IconEditor.start(
            componentKey = "com.foo/Main",
            packDrawables = emptyList(),
            automaticGlyph = "Phone",
            automaticColor = DrawerIconTokenName.Communication,
            existing = null,
        )
        composeRule.setContent {
            SoftHomeTheme {
                IconEditorSheet(
                    appLabel = "Foo",
                    state = state,
                    activePack = null,
                    drawableLoader = IconPackDrawableLoader(androidx.test.core.app.ApplicationProvider.getApplicationContext()),
                    category = null,
                    onSelectMode = {},
                    onSelectDrawable = {},
                    onSelectGlyph = { selectedGlyph = it },
                    onSelectColor = {},
                    onReset = {},
                    onSave = {},
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Glyph choices").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Camera").performClick()
        composeRule.waitForIdle()
        assertNotNull("tapping a glyph must report it", selectedGlyph)
        assertEquals("Camera", selectedGlyph)
    }
}
