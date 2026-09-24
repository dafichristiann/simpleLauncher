package com.softhome.launcher

import androidx.compose.foundation.layout.Row
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.softhome.core.designsystem.theme.SoftHomeTheme
import com.softhome.core.model.AppCategory
import com.softhome.core.model.IconPack
import com.softhome.core.model.ResolvedIcon
import com.softhome.core.model.IconSource
import com.softhome.feature.iconpack.data.IconPackDrawableLoader
import com.softhome.feature.iconpack.ui.DrawerAppIcon
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

/**
 * P3.5 Compose smoke tests for the drawer icon system (design/homeApp.pen frame
 * `TpzL1`). Asserts each rendering branch composes without crashing and that the
 * tile is present for: a pack app (real artwork), a non-pack app (category glyph),
 * and a selected tile. The exact colors are locked by the JVM tests
 * (`DrawerIconColorTest`, `DrawerAppIconColorTest`) and verified by screenshot.
 */
class DrawerAppIconTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun icon(source: IconSource, drawableName: String? = null, symbol: String = "AppWindow") =
        ResolvedIcon(
            source = source,
            drawableName = drawableName,
            symbolName = symbol,
            componentKey = "pkg/Cls",
            packageName = "pkg",
            className = "Cls",
        )

    @Test
    fun pack_app_renders_untinted_over_cream() {
        composeRule.setContent {
            val ctx = LocalContext.current
            SoftHomeTheme {
                DrawerAppIcon(
                    resolved = icon(IconSource.FromPack("pack", "whatsapp"), "whatsapp", "MessageCircle"),
                    size = 68.dp,
                    activePack = IconPack.invalid("pack", "Pack", "/x"),
                    drawableLoader = IconPackDrawableLoader(ctx),
                    category = AppCategory.SOCIAL,
                    contentDescription = "pack-app",
                )
            }
        }
        composeRule.onNodeWithContentDescription("pack-app").assertIsDisplayed()
    }

    @Test
    fun non_pack_app_renders_category_glyph() {
        composeRule.setContent {
            val ctx = LocalContext.current
            SoftHomeTheme {
                DrawerAppIcon(
                    resolved = icon(IconSource.System, symbol = "Phone"),
                    size = 68.dp,
                    activePack = null,
                    drawableLoader = IconPackDrawableLoader(ctx),
                    category = null,
                    contentDescription = "glyph-app",
                )
            }
        }
        composeRule.onNodeWithContentDescription("glyph-app").assertIsDisplayed()
    }

    @Test
    fun selected_tile_renders_amber_variant() {
        composeRule.setContent {
            val ctx = LocalContext.current
            SoftHomeTheme {
                Row {
                    DrawerAppIcon(
                        resolved = icon(IconSource.System, symbol = "Calculator"),
                        size = 68.dp,
                        activePack = null,
                        drawableLoader = IconPackDrawableLoader(ctx),
                        category = AppCategory.PRODUCTIVITY,
                        selected = false,
                        contentDescription = "tile-default",
                    )
                    DrawerAppIcon(
                        resolved = icon(IconSource.System, symbol = "Calculator"),
                        size = 68.dp,
                        activePack = null,
                        drawableLoader = IconPackDrawableLoader(ctx),
                        category = AppCategory.PRODUCTIVITY,
                        selected = true,
                        contentDescription = "tile-selected",
                    )
                }
            }
        }
        composeRule.onNodeWithContentDescription("tile-default").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("tile-selected").assertIsDisplayed()
    }
}
