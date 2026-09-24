package com.softhome.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P3.5 token coverage: every drawer-icon color field must be populated in BOTH the
 * light and dark palettes, so no screen ever renders a default/black glyph or tile.
 *
 * Guards against adding a `SoftColors` drawer field but forgetting to fill it in one
 * of the two palettes (which would silently fall back to `Color.Unspecified` -> black).
 */
class DrawerColorsTest {

    private fun drawerFields(c: SoftColors): Map<String, Color> = mapOf(
        "drawerTileCream" to c.drawerTileCream,
        "drawerTileSelected" to c.drawerTileSelected,
        "drawerIconOnSelected" to c.drawerIconOnSelected,
        "drawerIconCommunication" to c.drawerIconCommunication,
        "drawerIconSocial" to c.drawerIconSocial,
        "drawerIconProductivity" to c.drawerIconProductivity,
        "drawerIconMedia" to c.drawerIconMedia,
        "drawerIconTravel" to c.drawerIconTravel,
        "drawerIconFinance" to c.drawerIconFinance,
        "drawerIconNeutral" to c.drawerIconNeutral,
    )

    @Test
    fun `light palette populates every drawer color field`() {
        drawerFields(LightSoftColors).forEach { (name, color) ->
            assertNotEquals("$name must be set (light)", Color.Unspecified, color)
            assertTrue("$name must be fully opaque (light)", color.alpha == 1f)
        }
    }

    @Test
    fun `dark palette populates every drawer color field`() {
        drawerFields(DarkSoftColors).forEach { (name, color) ->
            assertNotEquals("$name must be set (dark)", Color.Unspecified, color)
            assertTrue("$name must be fully opaque (dark)", color.alpha == 1f)
        }
    }

    @Test
    fun `light palette uses the verbatim TpzL1 drawer hexes`() {
        // The seven TpzL1 icon colors + tile/selected tokens, locked to the .pen.
        assertTrue(LightSoftColors.drawerTileCream == Color(0xFFF6F0E7))
        assertTrue(LightSoftColors.drawerTileSelected == Color(0xFFD19B62))
        assertTrue(LightSoftColors.drawerIconOnSelected == Color(0xFFF5EFE6))
        assertTrue(LightSoftColors.drawerIconCommunication == Color(0xFF5F7A72))
        assertTrue(LightSoftColors.drawerIconSocial == Color(0xFF6E8B86))
        assertTrue(LightSoftColors.drawerIconProductivity == Color(0xFF8A5F43))
        assertTrue(LightSoftColors.drawerIconMedia == Color(0xFFD19B62))
        assertTrue(LightSoftColors.drawerIconTravel == Color(0xFFB06F52))
        assertTrue(LightSoftColors.drawerIconFinance == Color(0xFF4D7C8A))
        assertTrue(LightSoftColors.drawerIconNeutral == Color(0xFF625B52))
    }
}
