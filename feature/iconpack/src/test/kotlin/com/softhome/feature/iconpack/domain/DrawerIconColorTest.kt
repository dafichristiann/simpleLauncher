package com.softhome.feature.iconpack.domain

import com.softhome.core.model.AppCategory
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * P3.5: the drawer category -> color-token rule (P3.5 spec section 3.2).
 * Pure logic -- no Android.
 */
class DrawerIconColorTest {

    private fun token(category: Int?, symbol: String = "AppWindow") =
        DrawerIconColor.tokenFor(category, symbol)

    @Test
    fun `social category maps to Social`() {
        assertEquals(DrawerIconColor.Token.Social, token(AppCategory.SOCIAL))
    }

    @Test
    fun `image category maps to Social`() {
        assertEquals(DrawerIconColor.Token.Social, token(AppCategory.IMAGE))
    }

    @Test
    fun `productivity category maps to Productivity`() {
        assertEquals(DrawerIconColor.Token.Productivity, token(AppCategory.PRODUCTIVITY))
    }

    @Test
    fun `game audio video map to Media`() {
        assertEquals(DrawerIconColor.Token.Media, token(AppCategory.GAME))
        assertEquals(DrawerIconColor.Token.Media, token(AppCategory.AUDIO))
        assertEquals(DrawerIconColor.Token.Media, token(AppCategory.VIDEO))
    }

    @Test
    fun `news maps to Finance (browser-search reuse)`() {
        assertEquals(DrawerIconColor.Token.Finance, token(AppCategory.NEWS))
    }

    @Test
    fun `maps maps to Travel`() {
        assertEquals(DrawerIconColor.Token.Travel, token(AppCategory.MAPS))
    }

    @Test
    fun `unknown category falls back to Neutral`() {
        assertEquals(DrawerIconColor.Token.Neutral, token(null))
        assertEquals(DrawerIconColor.Token.Neutral, token(AppCategory.UNDEFINED))
        assertEquals(DrawerIconColor.Token.Neutral, token(999))
    }

    @Test
    fun `communication glyph heuristic lifts unknown category to Communication`() {
        val commGlyphs = listOf("MessageCircle", "Phone", "PhoneCall", "Mail", "CalendarDays")
        commGlyphs.forEach { glyph ->
            assertEquals(
                "unknown category + $glyph should read as Communication",
                DrawerIconColor.Token.Communication,
                token(null, glyph),
            )
        }
    }

    @Test
    fun `known category wins over the glyph heuristic`() {
        // A game app whose label happens to contain "mail" stays Media, not Communication.
        assertEquals(DrawerIconColor.Token.Media, token(AppCategory.GAME, "Mail"))
    }

    @Test
    fun `non-communication glyph with unknown category stays Neutral`() {
        assertEquals(DrawerIconColor.Token.Neutral, token(null, "Calculator"))
        assertEquals(DrawerIconColor.Token.Neutral, token(AppCategory.UNDEFINED, "AppWindow"))
    }
}
