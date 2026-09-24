package com.softhome.feature.iconpack.domain

import com.google.common.truth.Truth.assertThat
import com.softhome.core.model.DrawerIconTokenName
import com.softhome.core.model.IconOverride
import org.junit.Test

/**
 * P4b: the pure icon-editor state machine. No Android, no Compose -- asserts the
 * mode default, the seed from an existing override, change-detection gating, and the
 * override each selection would persist.
 */
class IconEditorTest {

    private fun state(
        packDrawables: List<String> = listOf("a", "b", "c"),
        automaticGlyph: String = "Phone",
        automaticColor: DrawerIconTokenName = DrawerIconTokenName.Communication,
        existing: IconOverride? = null,
    ) = IconEditor.start("com.foo/Main", packDrawables, automaticGlyph, automaticColor, existing)

    @Test
    fun `pack app defaults to pack mode with the first drawable selected`() {
        val s = state()
        assertThat(s.mode).isEqualTo(IconEditor.Mode.Pack)
        assertThat(s.canPickPack).isTrue()
        assertThat(s.selectedDrawable).isEqualTo("a")
    }

    @Test
    fun `non-pack app defaults to glyph mode using the automatic glyph and color`() {
        val s = state(packDrawables = emptyList(), automaticGlyph = "Music",
            automaticColor = DrawerIconTokenName.Media)
        assertThat(s.mode).isEqualTo(IconEditor.Mode.Glyph)
        assertThat(s.canPickPack).isFalse()
        assertThat(s.selectedGlyph).isEqualTo("Music")
        assertThat(s.selectedColor).isEqualTo(DrawerIconTokenName.Media)
        assertThat(s.currentOverride)
            .isEqualTo(IconOverride.Glyph("Music", DrawerIconTokenName.Media))
    }

    @Test
    fun `existing pack override opens in pack mode on that drawable`() {
        val s = state(existing = IconOverride.Pack("c"))
        assertThat(s.mode).isEqualTo(IconEditor.Mode.Pack)
        assertThat(s.selectedDrawable).isEqualTo("c")
    }

    @Test
    fun `existing glyph override opens in glyph mode on that glyph and color`() {
        val s = state(existing = IconOverride.Glyph("Camera", DrawerIconTokenName.Travel))
        assertThat(s.mode).isEqualTo(IconEditor.Mode.Glyph)
        assertThat(s.selectedGlyph).isEqualTo("Camera")
        assertThat(s.selectedColor).isEqualTo(DrawerIconTokenName.Travel)
    }

    @Test
    fun `selecting a different drawable updates the current selection`() {
        val s = IconEditor.selectDrawable(state(), "b")
        assertThat(s.selectedDrawable).isEqualTo("b")
        assertThat(s.currentOverride).isEqualTo(IconOverride.Pack("b"))
    }

    @Test
    fun `selecting a glyph switches to glyph mode and updates the selection`() {
        val s = IconEditor.selectGlyph(state(), "Mail")
        assertThat(s.mode).isEqualTo(IconEditor.Mode.Glyph)
        assertThat(s.currentOverride)
            .isEqualTo(IconOverride.Glyph("Mail", DrawerIconTokenName.Communication))
    }

    @Test
    fun `selecting a color switches to glyph mode and keeps the glyph`() {
        val s = IconEditor.selectColor(state(), DrawerIconTokenName.Finance)
        assertThat(s.mode).isEqualTo(IconEditor.Mode.Glyph)
        assertThat(s.currentOverride)
            .isEqualTo(IconOverride.Glyph("Phone", DrawerIconTokenName.Finance))
    }

    @Test
    fun `fresh pack app with no stored override is unchanged until edited`() {
        // A pack app whose first drawable equals its stored default should not "change".
        val existing = IconOverride.Pack("a")
        val s = state(existing = existing)
        assertThat(s.changed).isFalse()
        assertThat(IconEditor.selectDrawable(s, "b").changed).isTrue()
    }

    @Test
    fun `existing glyph override unchanged until color changes`() {
        val existing = IconOverride.Glyph("Phone", DrawerIconTokenName.Communication)
        val s = state(existing = existing)
        assertThat(s.changed).isFalse()
        assertThat(IconEditor.selectColor(s, DrawerIconTokenName.Social).changed).isTrue()
    }

    @Test
    fun `token name mirror covers every DrawerIconColor token 1 to 1`() {
        val domainTokens = DrawerIconColor.Token.entries
        val names = domainTokens.map { DrawerIconColor.nameOf(it) }
        assertThat(names).containsExactlyElementsIn(DrawerIconTokenName.entries)
    }
}
