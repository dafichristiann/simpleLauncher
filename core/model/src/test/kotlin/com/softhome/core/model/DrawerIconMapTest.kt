package com.softhome.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * P4: design-fidelity tests for [DrawerIconMap], the per-package glyph table ported from
 * `design/homeApp.pen`.
 *
 * The table has two sources, and these tests pin **both**:
 *  - the 24 `TpzL1` "App Tile" drawables (exact glyph + `$icon-*` color), and
 *  - the 8 `ciHU3` "Icon Language Library" groups (glyph + category).
 *
 * The point is honesty: if someone edits the map, a mismatch with the `.pen` fails here.
 */
class DrawerIconMapTest {

    // --- TpzL1 tiles (the 24 apps the drawer mock draws) ----------------------

    @Test
    fun `tpzL1 tiles resolve to their exact design glyph and color`() {
        // (package used on the real Tecno -> expected lucide glyph, expected color slot)
        val expected = mapOf(
            "com.whatsapp" to ("message-circle" to DrawerIconTokenName.Communication),
            "com.instagram.android" to ("image" to DrawerIconTokenName.Social),
            "com.google.android.gm" to ("mail" to DrawerIconTokenName.Communication),
            "com.android.chrome" to ("globe" to DrawerIconTokenName.Browser),
            // Google Messages ships as ...apps.messaging.
            "com.google.android.apps.messaging" to ("message-square" to DrawerIconTokenName.Communication),
            "com.transsion.deskclock" to ("alarm-clock" to DrawerIconTokenName.Neutral),
            "com.android.settings" to ("settings-2" to DrawerIconTokenName.Neutral),
            "com.google.android.apps.photos" to ("images" to DrawerIconTokenName.Media),
            "com.google.android.youtube" to ("play" to DrawerIconTokenName.Media),
            "com.spotify.music" to ("disc-3" to DrawerIconTokenName.Media),
            "com.google.android.apps.maps" to ("map" to DrawerIconTokenName.Travel),
            "id.dana" to ("wallet-minimal" to DrawerIconTokenName.Finance),
            "org.sabda.alkitab" to ("book-open" to DrawerIconTokenName.Communication),
            "com.brave.browser" to ("shield-check" to DrawerIconTokenName.Productivity),
            // Agoda tile draws `tent` (ciHU3 lists `tag`; the tile wins).
            "com.agoda.mobile.consumer" to ("tent" to DrawerIconTokenName.Travel),
            "com.anthropic.claude" to ("sparkles" to DrawerIconTokenName.Browser),
            "com.discord" to ("messages-square" to DrawerIconTokenName.Communication),
            "com.transsion.calculator" to ("calculator" to DrawerIconTokenName.Media),
            "com.transsion.camera" to ("camera" to DrawerIconTokenName.Media),
        )
        expected.forEach { (pkg, want) ->
            val e = DrawerIconMap.forPackage(pkg)
            assertThat(e).isNotNull()
            assertThat(e!!.glyph).isEqualTo(want.first)
            assertThat(e.colorToken).isEqualTo(want.second)
        }
    }

    // --- ciHU3 groups ---------------------------------------------------------

    @Test
    fun `the eight design categories are each populated`() {
        DrawerCategory.tabs.forEach { tab ->
            // Every real category has at least one entry in the design library.
            assertThat(DrawerIconMap.allIn(tab)).isNotEmpty()
        }
    }

    @Test
    fun `representative library apps map to their group glyph`() {
        val expected = mapOf(
            "org.telegram.messenger" to ("send" to DrawerCategory.Communication),
            "com.google.android.apps.tachyon" to ("video" to DrawerCategory.Communication), // Meet
            "com.instagram.barcelona" to ("network" to DrawerCategory.Communication), // Threads
            "com.pinterest" to ("pin" to DrawerCategory.SocialEntertainment),
            "com.epicgames.fortnite" to ("gamepad-2" to DrawerCategory.SocialEntertainment),
            "com.JindoBlu.FourPlayers" to ("users-round" to DrawerCategory.SocialEntertainment),
            "com.transsion.notebook" to ("notebook-pen" to DrawerCategory.ProductivityTools),
            "com.google.android.apps.docs" to ("cloud-upload" to DrawerCategory.ProductivityTools),
            "com.google.android.googlequicksearchbox" to ("search" to DrawerCategory.BrowserSearch),
            "com.google.android.apps.bard" to ("sparkles" to DrawerCategory.BrowserSearch), // Gemini
            "com.openai.chatgpt" to ("bot" to DrawerCategory.BrowserSearch),
            "com.gallery20" to ("images" to DrawerCategory.CameraMedia), // AI Gallery
            "com.rlk.weathers" to ("cloud-sun" to DrawerCategory.MapsTravel),
            "com.gojek.app" to ("car-front" to DrawerCategory.MapsTravel),
            "com.gojek.gopay" to ("wallet-cards" to DrawerCategory.FinanceShopping), // GoPay, before gojek
            "com.tokopedia.tkpd" to ("shopping-bag" to DrawerCategory.FinanceShopping),
            "com.kfc.mobile" to ("utensils" to DrawerCategory.FoodLifestyle),
            "com.kopikenangan" to ("coffee" to DrawerCategory.FoodLifestyle),
        )
        expected.forEach { (pkg, want) ->
            val e = DrawerIconMap.forPackage(pkg)
            assertThat(e).isNotNull()
            assertThat(e!!.glyph).isEqualTo(want.first)
            assertThat(e.category).isEqualTo(want.second)
        }
    }

    @Test
    fun `reference device utility apps use explicit warm glyphs`() {
        val expected = mapOf(
            "bitpit.launcher" to ("package" to DrawerCategory.ProductivityTools),
            "com.honestbank.android" to ("banknote" to DrawerCategory.FinanceShopping),
            "com.growtons.call.blocker" to ("phone-off" to DrawerCategory.Communication),
            "com.ovelin.guitartuna" to ("music-2" to DrawerCategory.SocialEntertainment),
            "joerikros.nfcchecker" to ("shield-check" to DrawerCategory.ProductivityTools),
        )
        expected.forEach { (pkg, want) ->
            val e = DrawerIconMap.forPackage(pkg)
            assertThat(e).isNotNull()
            assertThat(e!!.glyph).isEqualTo(want.first)
            assertThat(e.category).isEqualTo(want.second)
        }
    }

    // --- specificity / robustness --------------------------------------------

    @Test
    fun `more specific fragments win over their generic substrings`() {
        // GoPay (`gojek.gopay`) must not be read as Gojek.
        assertThat(DrawerIconMap.forPackage("com.gojek.gopay")!!.glyph).isEqualTo("wallet-cards")
        assertThat(DrawerIconMap.forPackage("com.gojek.app")!!.glyph).isEqualTo("car-front")
        // Threads (`instagram.barcelona`) must not be read as Instagram.
        assertThat(DrawerIconMap.forPackage("com.instagram.barcelona")!!.glyph).isEqualTo("network")
        assertThat(DrawerIconMap.forPackage("com.instagram.android")!!.glyph).isEqualTo("image")
        // AI Gallery (`gallery20`) must not fall through to the generic gallery glyph.
        assertThat(DrawerIconMap.forPackage("com.gallery20")!!.glyph).isEqualTo("images")
        // ShopeePay must not be read as the Shopee shopping cart.
        assertThat(DrawerIconMap.forPackage("com.shopeepay.id")!!.glyph).isEqualTo("wallet-minimal")
        assertThat(DrawerIconMap.forPackage("com.shopee.id")!!.glyph).isEqualTo("shopping-cart")
        // Google TV (`google.android.videos`) must not be shadowed by the generic "google".
        assertThat(DrawerIconMap.forPackage("com.google.android.videos")!!.glyph).isEqualTo("tv")
        assertThat(DrawerIconMap.forPackage("com.google.android.googlequicksearchbox")!!.glyph)
            .isEqualTo("search")
    }

    @Test
    fun `real Tecno packages resolve to their intended glyph not a generic substring`() {
        // Shopee Food Driver contains "drive" -> must NOT be the Google Drive cloud.
        assertThat(DrawerIconMap.forPackage("com.shopee.foody.driver.id")!!.glyph).isEqualTo("bike")
        // Phone Master contains "phone" -> must NOT be the dialer handset.
        assertThat(DrawerIconMap.forPackage("com.transsion.phonemaster")!!.glyph).isEqualTo("wrench")
        // Gboard contains "google" -> must NOT be the Google search glyph.
        assertThat(DrawerIconMap.forPackage("com.google.android.inputmethod.latin")!!.glyph)
            .isEqualTo("keyboard")
    }

    @Test
    fun `unknown and blank packages return null`() {        assertThat(DrawerIconMap.forPackage("com.example.unknown.thing")).isNull()
        assertThat(DrawerIconMap.forPackage("")).isNull()
        assertThat(DrawerIconMap.forPackage("   ")).isNull()
    }

    @Test
    fun `no two entries share a fragment`() {
        val fragments = DrawerIconMap.all().map { it.fragment }
        assertThat(fragments).containsNoDuplicates()
        assertThat(DrawerIconMap.all().map { it.glyph }).containsNoneOf("", " ")
    }
}
