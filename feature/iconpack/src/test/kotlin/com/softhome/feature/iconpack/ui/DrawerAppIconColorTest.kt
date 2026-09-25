package com.softhome.feature.iconpack.ui

import com.softhome.core.designsystem.theme.DarkSoftColors
import com.softhome.core.designsystem.theme.LightSoftColors
import com.softhome.feature.iconpack.domain.DrawerIconColor
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * P3.5: the token -> theme-color resolution used by the drawer icon composable.
 * Pure (no Android rendering); asserts each token maps to the right `SoftColors`
 * field in both palettes, so a Token can never resolve to the wrong/blank color.
 */
class DrawerAppIconColorTest {

    @Test
    fun `light palette resolves each token to its field`() {
        val c = LightSoftColors
        assertEquals(c.drawerIconCommunication, tokenColor(c, DrawerIconColor.Token.Communication))
        assertEquals(c.drawerIconSocial, tokenColor(c, DrawerIconColor.Token.Social))
        assertEquals(c.drawerIconProductivity, tokenColor(c, DrawerIconColor.Token.Productivity))
        assertEquals(c.drawerIconMedia, tokenColor(c, DrawerIconColor.Token.Media))
        assertEquals(c.drawerIconTravel, tokenColor(c, DrawerIconColor.Token.Travel))
        assertEquals(c.drawerIconFinance, tokenColor(c, DrawerIconColor.Token.Finance))
        assertEquals(c.drawerIconNeutral, tokenColor(c, DrawerIconColor.Token.Neutral))
        assertEquals(c.drawerIconBrowser, tokenColor(c, DrawerIconColor.Token.Browser))
    }

    @Test
    fun `dark palette resolves each token to its field`() {
        val c = DarkSoftColors
        assertEquals(c.drawerIconCommunication, tokenColor(c, DrawerIconColor.Token.Communication))
        assertEquals(c.drawerIconSocial, tokenColor(c, DrawerIconColor.Token.Social))
        assertEquals(c.drawerIconProductivity, tokenColor(c, DrawerIconColor.Token.Productivity))
        assertEquals(c.drawerIconMedia, tokenColor(c, DrawerIconColor.Token.Media))
        assertEquals(c.drawerIconTravel, tokenColor(c, DrawerIconColor.Token.Travel))
        assertEquals(c.drawerIconFinance, tokenColor(c, DrawerIconColor.Token.Finance))
        assertEquals(c.drawerIconNeutral, tokenColor(c, DrawerIconColor.Token.Neutral))
        assertEquals(c.drawerIconBrowser, tokenColor(c, DrawerIconColor.Token.Browser))
    }
}
