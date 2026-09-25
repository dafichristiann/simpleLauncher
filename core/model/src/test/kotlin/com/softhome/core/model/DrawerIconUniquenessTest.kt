package com.softhome.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * P4: the uniqueness guarantee.
 *
 * The user's complaint was "duplicate / generic" drawer icons -- two different apps showing
 * the same glyph. [DrawerIconAssignment] must give every package in the **real Tecno
 * launcher list** a distinct `(glyph, colorToken)` pair, deterministically (so tiles never
 * shuffle between launches).
 *
 * The package list below is the actual `query-activities ... CATEGORY_LAUNCHER --brief`
 * output from the Tecno Camon 50 Pro (TECNO CN7c, API 36) captured during P4.
 */
class DrawerIconUniquenessTest {

    /** The real Tecno launcher packages (77 activities; distinct packages). */
    private val tecnoPackages = listOf(
        "bitpit.launcher",
        "com.agoda.mobile.consumer",
        "com.android.chrome",
        "com.android.settings",
        "com.android.stk",
        "com.android.vending",
        "com.anthropic.claude",
        "com.bcadigital.blu",
        "com.brave.browser",
        "com.circlekindonesia.circlekshopping",
        "com.discord",
        "com.epicgames.fortnite",
        "com.facebook.katana",
        "com.gallery20",
        "com.gojek.app",
        "com.gojek.gopay",
        "com.google.android.apps.bard",
        "com.google.android.apps.docs",
        "com.google.android.apps.maps",
        "com.google.android.apps.nbu.files",
        "com.google.android.apps.photos",
        "com.google.android.apps.safetyhub",
        "com.google.android.apps.tachyon",
        "com.google.android.apps.youtube.music",
        "com.google.android.gm",
        "com.google.android.googlequicksearchbox",
        "com.google.android.inputmethod.latin",
        "com.google.android.videos",
        "com.google.android.youtube",
        "com.grabtaxi.passenger",
        "com.growtons.call.blocker.block.spam.calls.blacklist.unknown.calls.block.robo.calls",
        "com.honestbank.android",
        "com.instagram.android",
        "com.instagram.barcelona",
        "com.jago.digitalBanking",
        "com.JindoBlu.FourPlayers",
        "com.kfc.mobile",
        "com.kopikenangan",
        "com.openai.chatgpt",
        "com.pinterest",
        "com.pure.indosat.care",
        "com.rlk.weathers",
        "com.sh.smart.caller",
        "com.shopee.foody.driver.id",
        "com.shopee.id",
        "com.shopeepay.id",
        "com.smartlife.nebula",
        "com.spotify.music",
        "com.ss.android.ugc.trill",
        "com.tokopedia.tkpd",
        "com.transsion.aivoiceassistant",
        "com.transsion.calculator",
        "com.transsion.calendar",
        "com.transsion.camera",
        "com.transsion.carlcare",
        "com.transsion.compass",
        "com.transsion.deskclock",
        "com.transsion.gamespace.app",
        "com.transsion.notebook",
        "com.transsion.phonemaster",
        "com.transsion.smartmessage",
        "com.transsion.soundrecorder",
        "com.transsion.theme",
        "com.transsnet.store",
        "com.transtech.gotii",
        "com.whatsapp",
        "com.yup.bank.digital.kartu.kredit.debit.pascabayar.pinjaman.investasi",
        "ctrip.english",
        "id.co.bankbkemobile.digitalbank",
        "id.co.sevima.edlink",
        "id.dana",
        "id.tix.android",
        "joerikros.nfcchecker",
        "net.tandem",
        "org.sabda.alkitab",
        "org.telegram.messenger",
    )

    /** The app-side heuristic, mirrored here purely for the fallback path. */
    private fun heuristicGlyph(pkg: String): String = "AppWindow"

    private fun heuristicColor(pkg: String): DrawerIconTokenName = DrawerIconTokenName.Neutral

    private val fallbacks = listOf(
        "AppWindow", "Square", "Box", "CircleDot", "Sparkles",
        "Shield", "Tag", "Bot", "Store", "Wrench",
    )

    private fun assign(pkgs: List<String>) =
        DrawerIconAssignment.assign(
            identities = pkgs.map { it to it },
            heuristicGlyph = ::heuristicGlyph,
            heuristicColor = ::heuristicColor,
            glyphFallbacks = fallbacks,
        )

    @Test
    fun `every tecno package gets a unique glyph-color pair`() {
        val map = assign(tecnoPackages)
        assertThat(map).hasSize(tecnoPackages.size)
        val pairs = map.values.map { it.glyph to it.colorToken }
        assertThat(pairs).containsNoDuplicates()
    }

    @Test
    fun `the assignment is deterministic and order independent`() {
        val a = assign(tecnoPackages)
        val b = assign(tecnoPackages.shuffled(java.util.Random(42)))
        val c = assign(tecnoPackages.reversed())
        assertThat(a).isEqualTo(b)
        assertThat(a).isEqualTo(c)
    }

    @Test
    fun `design apps keep their intended glyph from the map`() {
        val map = assign(tecnoPackages)
        // A known design app not colliding with anything keeps its exact glyph.
        assertThat(map["com.whatsapp"]!!.glyph).isEqualTo("message-circle")
        assertThat(map["com.spotify.music"]!!.glyph).isEqualTo("disc-3")
        assertThat(map["id.dana"]!!.glyph).isEqualTo("wallet-minimal")
        assertThat(map["com.android.chrome"]!!.glyph).isEqualTo("globe")
    }

    @Test
    fun `known packages do not all collapse to AppWindow`() {
        val map = assign(tecnoPackages)
        val appWindowCount = map.values.count { it.glyph == "AppWindow" }
        // The whole point of the map: most Tecno apps are recognised (only a handful of
        // genuinely-unknown packages may share the generic window glyph).
        assertThat(appWindowCount).isLessThan(10)
    }

    @Test
    fun `two activities from one package get distinct pairs`() {
        // TECNO ships its Phone and Contacts as two launcher activities of one package
        // (com.sh.smart.caller); they are two drawer cells and must not look identical.
        val cells = listOf(
            "com.sh.smart.caller/com.android.dialer.main.impl.MainActivity" to "com.sh.smart.caller",
            "com.sh.smart.caller/com.android.dialer.ContactMainActivity" to "com.sh.smart.caller",
        )
        val map = DrawerIconAssignment.assign(
            identities = cells, heuristicGlyph = ::heuristicGlyph,
            heuristicColor = ::heuristicColor, glyphFallbacks = fallbacks,
        )
        assertThat(map).hasSize(2)
        assertThat(map.values.map { it.glyph to it.colorToken }).containsNoDuplicates()
    }
}
