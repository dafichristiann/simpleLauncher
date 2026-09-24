package com.softhome.launcher

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.softhome.core.designsystem.theme.SoftHomeTheme
import com.softhome.core.model.DrawerIconTokenName
import com.softhome.core.model.IconOverride
import com.softhome.core.model.IconPack
import com.softhome.feature.iconpack.data.IconPackDrawableLoader
import com.softhome.feature.iconpack.domain.IconEditor
import com.softhome.feature.iconpack.ui.IconEditorSheet
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * P4b evidence: renders the icon editor (pack mode and glyph mode) inside a phone-sized
 * box and writes PNGs to the device, so the on-device verification can pull real
 * screenshots even though a long-press gesture is awkward to script over adb.
 *
 * Files land in `context.getExternalFilesDir(null)`:
 *   p4b-editor-pack.png, p4b-editor-glyph.png
 */
class IconEditorScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun packOf(vararg drawables: String, name: String = "SoftMonoTest") = IconPack(
        id = "pack", name = name, sourcePath = "/p",
        entries = drawables.associate { "com.x/$it" to it }, hasAppFilter = true,
    )

    private fun save(bitmap: Bitmap, fileName: String) {
        // Use the *target* app context so the file lands in the debug APK's external
        // files dir (pullable over adb), not the test APK's.
        val context = androidx.test.platform.app.InstrumentationRegistry
            .getInstrumentation().targetContext
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        File(dir, fileName).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun capture_pack_mode_editor() {
        val state = IconEditor.start(
            componentKey = "com.foo/Main",
            packDrawables = listOf("icon_a", "icon_b", "icon_c", "icon_d", "icon_e"),
            automaticGlyph = "Phone",
            automaticColor = DrawerIconTokenName.Communication,
            existing = IconOverride.Pack("icon_b"),
        )
        composeRule.setContent {
            SoftHomeTheme {
                Box(Modifier.size(390.dp, 720.dp).background(Color(0xFFDCCDBA)), Alignment.Center) {
                    IconEditorSheet(
                        appLabel = "Calendar",
                        state = state,
                        activePack = packOf("icon_a", "icon_b", "icon_c", "icon_d", "icon_e"),
                        drawableLoader = IconPackDrawableLoader(
                            ApplicationProvider.getApplicationContext(),
                        ),
                        category = null,
                        onSelectMode = {}, onSelectDrawable = {}, onSelectGlyph = {},
                        onSelectColor = {}, onReset = {}, onSave = {}, onDismiss = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()
        save(composeRule.onRoot().captureToImage().asAndroidBitmap(), "p4b-editor-pack.png")
    }

    @Test
    fun capture_glyph_mode_editor() {
        val state = IconEditor.start(
            componentKey = "com.foo/Main",
            packDrawables = emptyList(),
            automaticGlyph = "Music",
            automaticColor = DrawerIconTokenName.Media,
            existing = IconOverride.Glyph("Camera", DrawerIconTokenName.Travel),
        )
        composeRule.setContent {
            SoftHomeTheme {
                Box(Modifier.size(390.dp, 720.dp).background(Color(0xFFDCCDBA)), Alignment.Center) {
                    IconEditorSheet(
                        appLabel = "Photos",
                        state = state,
                        activePack = null,
                        drawableLoader = IconPackDrawableLoader(
                            ApplicationProvider.getApplicationContext(),
                        ),
                        category = null,
                        onSelectMode = {}, onSelectDrawable = {}, onSelectGlyph = {},
                        onSelectColor = {}, onReset = {}, onSave = {}, onDismiss = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()
        save(composeRule.onRoot().captureToImage().asAndroidBitmap(), "p4b-editor-glyph.png")
    }
}
