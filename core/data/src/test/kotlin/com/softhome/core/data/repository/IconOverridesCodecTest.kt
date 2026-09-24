package com.softhome.core.data.repository

import com.google.common.truth.Truth.assertThat
import com.softhome.core.model.DrawerIconTokenName
import com.softhome.core.model.IconOverride
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * P4b: the per-app icon override codec. Uses Robolectric for the real `org.json`
 * implementation (the Android `org.json` stub throws on plain JVM tests -- the same
 * reason [FolderRepositoryTest] is Robolectric).
 */
@RunWith(RobolectricTestRunner::class)
class IconOverridesCodecTest {

    @Test
    fun `pack override round-trips`() {
        val original = mapOf("com.a/Main" to IconOverride.Pack("a_drawable"))
        assertThat(IconOverridesCodec.decode(IconOverridesCodec.encode(original)))
            .isEqualTo(original)
    }

    @Test
    fun `glyph override round-trips`() {
        val original = mapOf(
            "com.b/Main" to IconOverride.Glyph("Phone", DrawerIconTokenName.Communication),
            "com.c/Main" to IconOverride.Glyph("Music", DrawerIconTokenName.Media),
        )
        assertThat(IconOverridesCodec.decode(IconOverridesCodec.encode(original)))
            .isEqualTo(original)
    }

    @Test
    fun `empty map round-trips`() {
        assertThat(IconOverridesCodec.decode(IconOverridesCodec.encode(emptyMap()))).isEmpty()
    }

    @Test
    fun `malformed json decodes to empty (never throws)`() {
        assertThat(IconOverridesCodec.decode("{not json")).isEmpty()
        assertThat(IconOverridesCodec.decode("   ")).isEmpty()
        assertThat(IconOverridesCodec.decode(null)).isEmpty()
        assertThat(IconOverridesCodec.decode("[]")).isEmpty()
    }

    @Test
    fun `unknown type is dropped`() {
        assertThat(IconOverridesCodec.decode("""{"com.a/Main":{"type":"future"}}""")).isEmpty()
    }

    @Test
    fun `glyph with unknown color token is dropped`() {
        val json = """{"com.a/Main":{"type":"glyph","symbol":"Phone","color":"Neon"}}"""
        assertThat(IconOverridesCodec.decode(json)).isEmpty()
    }

    @Test
    fun `pack with blank name is dropped`() {
        assertThat(IconOverridesCodec.decode("""{"com.a/Main":{"type":"pack","name":""}}""")).isEmpty()
    }

    @Test
    fun `legacy packId string value decodes to no override (migration)`() {
        // The old shape stored a bare packId string per key; it cannot express a drawable,
        // so it decodes to no override (the app returns to the P3.5 hybrid). Never throws.
        assertThat(IconOverridesCodec.decode("""{"com.a/Main":"whicons"}""")).isEmpty()
    }

    @Test
    fun `good entries survive alongside junk entries`() {
        val json = """
            {"com.a/Main":{"type":"pack","name":"x"},
             "com.b/Main":{"type":"glyph","symbol":"Phone","color":"Bogus"},
             "com.c/Main":{"type":"unknown"},
             "com.d/Main":{"type":"glyph","symbol":"Music","color":"Media"}}
        """.trimIndent()
        val decoded = IconOverridesCodec.decode(json)
        assertThat(decoded).containsKey("com.a/Main")
        assertThat(decoded).containsKey("com.d/Main")
        assertThat(decoded).doesNotContainKey("com.b/Main")
        assertThat(decoded).doesNotContainKey("com.c/Main")
        assertThat(decoded["com.d/Main"])
            .isEqualTo(IconOverride.Glyph("Music", DrawerIconTokenName.Media))
    }

    @Test
    fun `encoder drops blank keys and blank payload`() {
        val junk = mapOf(
            "" to IconOverride.Pack("x"),
            "com.a/Main" to IconOverride.Pack(""),
        )
        assertThat(IconOverridesCodec.decode(IconOverridesCodec.encode(junk))).isEmpty()
    }
}
