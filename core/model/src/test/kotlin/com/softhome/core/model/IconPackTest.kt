package com.softhome.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class IconPackTest {

    @Test
    fun `invalid factory produces an empty non-appfilter pack`() {
        val p = IconPack.invalid("id", "Name", "/path")
        assertThat(p.entries).isEmpty()
        assertThat(p.hasAppFilter).isFalse()
        assertThat(p.iconCount).isEqualTo(0)
        assertThat(p.sourceKind).isEqualTo(IconPack.SourceKind.Zip)
    }

    @Test
    fun `zip source reports imported label`() {
        val p = IconPack(
            id = "p", name = "P", sourcePath = "/x",
            entries = emptyMap(), hasAppFilter = true,
            source = IconPackSource.Zip("/x/a.zip"),
        )
        assertThat(p.displaySource).isEqualTo("Imported .zip")
    }

    @Test
    fun `installed pack source reports installed-app label`() {
        val p = IconPack(
            id = "p", name = "P", sourcePath = "com.pack",
            entries = emptyMap(), hasAppFilter = true,
            source = IconPackSource.InstalledPack("com.pack"),
        )
        assertThat(p.displaySource).isEqualTo("Installed app")
    }

    @Test
    fun `assets source reports bundled label`() {
        val p = IconPack(
            id = "p", name = "P", sourcePath = "seed",
            entries = emptyMap(), hasAppFilter = true,
            source = IconPackSource.Assets("iconpacks/seed"),
        )
        assertThat(p.displaySource).isEqualTo("Bundled")
    }

    @Test
    fun `drawable count de-duplicates reused drawable names`() {
        val p = IconPack(
            id = "p", name = "P", sourcePath = "/x",
            entries = mapOf("a/A" to "shared", "b/B" to "shared", "c/C" to "unique"),
            hasAppFilter = true,
        )
        assertThat(p.iconCount).isEqualTo(3)
        assertThat(p.drawableCount).isEqualTo(2)
    }
}
