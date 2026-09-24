package com.softhome.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.unit.dp
import com.softhome.core.designsystem.theme.DarkSoftColors
import com.softhome.core.designsystem.theme.LightSoftColors
import com.softhome.core.designsystem.theme.SoftHomeTheme
import com.softhome.core.designsystem.theme.softColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * P4c instrumented tests: the theme applies the KkPN3 dark palette app-wide, so a
 * composable that reads `MaterialTheme.softColors` gets the dark values, and the
 * rendered background is the KkPN3 `#18191A` (not the light cream).
 */
class DarkThemeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dark_theme_provides_dark_soft_colors() {
        var bg: Color? = null
        composeRule.setContent {
            SoftHomeTheme(darkTheme = true) {
                bg = MaterialTheme.softColors.background
            }
        }
        composeRule.waitForIdle()
        assertEquals(Color(0xFF18191A), bg)
        assertNotEquals(LightSoftColors.background, bg)
    }

    @Test
    fun dark_theme_renders_the_kkpn3_background() {
        composeRule.setContent {
            SoftHomeTheme(darkTheme = true) {
                Box(Modifier.size(64.dp).background(MaterialTheme.softColors.background)) {
                    Text("x", color = MaterialTheme.softColors.textPrimary)
                }
            }
        }
        val image = composeRule.onRoot().captureToImage()
        val pixel = image.toPixelMap()[image.width / 2, image.height / 2]
        // Center pixel should be the KkPN3 background (allow tiny rounding).
        assertTrue("bg should be near #18191A, was $pixel", pixel.red < 0.2f && pixel.blue < 0.2f)
        assertTrue(pixel.alpha > 0.99f)
    }

    @Test
    fun light_theme_still_uses_the_cream_background() {
        var bg: Color? = null
        composeRule.setContent {
            SoftHomeTheme(darkTheme = false) {
                bg = MaterialTheme.softColors.background
            }
        }
        composeRule.waitForIdle()
        assertEquals(LightSoftColors.background, bg)
        assertNotEquals(DarkSoftColors.background, bg)
    }
}
