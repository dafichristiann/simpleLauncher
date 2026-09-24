package com.softhome.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * P4c token lock: the dark palette is the **`KkPN3` "Dark Editorial"** named palette from
 * `design/homeApp.pen`, not a derived warm-dark guess. These assertions pin the verbatim
 * values so a future edit cannot silently drift the dark mode away from the design.
 *
 * The few surfaces the `KkPN3` frame does not show (menu/popup/drawer cards) are derived
 * from the same neutral family and are asserted only as "set + opaque" (not exact).
 */
class DarkPaletteTest {

    @Test
    fun `dark palette uses the verbatim KkPN3 hexes`() {
        assertEquals(Color(0xFF18191A), DarkBackground)   // KkPN3 screen bg
        assertEquals(Color(0xFF18191A), DarkSurface)
        assertEquals(Color(0xFF2E3134), DarkRailBg)       // KkPN3 dark rail
        assertEquals(Color(0xFF343638), DarkDivider)      // KkPN3 divider
        assertEquals(Color(0xFFF2EEE7), DarkTextTitle)    // KkPN3 primary text
        assertEquals(Color(0xFFD2CBC1), DarkTextBody)     // KkPN3 "day"
        assertEquals(Color(0xFF918F8B), DarkTextMuted)    // KkPN3 "month"
        assertEquals(Color(0xFFE2DDD5), DarkStatusText)   // KkPN3 soft text
        assertEquals(Color(0xFF676866), DarkProgressTrack) // KkPN3 progress track
        assertEquals(Color(0xFF2E3134), DarkTileWarm)
        assertEquals(Color(0xFF2E3134), DarkDrawerTileCream)
    }

    @Test
    fun `every dark SoftColors field is populated and opaque`() {
        val c = DarkSoftColors
        val fields = listOf(
            "background" to c.background, "surface" to c.surface, "card" to c.card,
            "cardAlt" to c.cardAlt, "accentWash" to c.accentWash, "tile" to c.tile,
            "onTile" to c.onTile, "textPrimary" to c.textPrimary, "textBody" to c.textBody,
            "textMuted" to c.textMuted, "indexLetter" to c.indexLetter, "accent" to c.accent,
            "statusText" to c.statusText, "primaryAction" to c.primaryAction,
            "onPrimaryAction" to c.onPrimaryAction, "railBg" to c.railBg, "divider" to c.divider,
            "tileWarm" to c.tileWarm, "categoryWash" to c.categoryWash, "drawerBg" to c.drawerBg,
            "drawerStroke" to c.drawerStroke, "progressTrack" to c.progressTrack,
            "indexActive" to c.indexActive, "destructive" to c.destructive,
            "disabled" to c.disabled, "drawerTileCream" to c.drawerTileCream,
            "drawerTileSelected" to c.drawerTileSelected,
            "drawerIconOnSelected" to c.drawerIconOnSelected,
            "drawerIconCommunication" to c.drawerIconCommunication,
            "drawerIconSocial" to c.drawerIconSocial,
            "drawerIconProductivity" to c.drawerIconProductivity,
            "drawerIconMedia" to c.drawerIconMedia, "drawerIconTravel" to c.drawerIconTravel,
            "drawerIconFinance" to c.drawerIconFinance, "drawerIconNeutral" to c.drawerIconNeutral,
        )
        fields.forEach { (name, color) ->
            assertNotEquals("$name must be set (dark)", Color.Unspecified, color)
            assertEquals("$name must be fully opaque (dark)", 1f, color.alpha)
        }
    }

    @Test
    fun `dark is meaningfully different from light for the key surfaces`() {
        assertNotEquals(LightSoftColors.background, DarkSoftColors.background)
        assertNotEquals(LightSoftColors.textPrimary, DarkSoftColors.textPrimary)
        assertNotEquals(LightSoftColors.drawerTileCream, DarkSoftColors.drawerTileCream)
    }
}
