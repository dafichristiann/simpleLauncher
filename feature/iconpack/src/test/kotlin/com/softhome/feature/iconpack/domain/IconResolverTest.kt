package com.softhome.feature.iconpack.domain

import com.google.common.truth.Truth.assertThat
import com.softhome.core.model.AppInfo
import com.softhome.core.model.DrawerIconTokenName
import com.softhome.core.model.IconOverride
import com.softhome.core.model.IconPack
import com.softhome.core.model.IconSource
import org.junit.Test

class IconResolverTest {

    private val resolver = IconResolver()
    private val app = AppInfo("com.foo", "com.foo.Main", "Foo")

    @Test
    fun `pack match wins over mask`() {
        val pack = packOf(mapOf(app.componentKey to "foo_drawable"))
        val source = resolver.resolve(app, pack, emptyMap(), maskUnsupported = true)
        assertThat(source).isInstanceOf(IconSource.FromPack::class.java)
        assertThat((source as IconSource.FromPack).drawableName).isEqualTo("foo_drawable")
    }

    @Test
    fun `pack override wins over pack`() {
        val pack = packOf(mapOf(app.componentKey to "from_pack", "com.other/Main" to "alt_drawable"))
        val overrides = mapOf(app.componentKey to IconOverride.Pack("alt_drawable"))
        val source = resolver.resolve(app, pack, overrides, maskUnsupported = true)
        assertThat(source).isInstanceOf(IconSource.Override::class.java)
        assertThat((source as IconSource.Override).drawableName).isEqualTo("alt_drawable")
    }

    @Test
    fun `pack override with a drawable absent from the pack falls through to the normal path`() {
        val pack = packOf(mapOf(app.componentKey to "from_pack"))
        val overrides = mapOf(app.componentKey to IconOverride.Pack("ghost_drawable"))
        val source = resolver.resolve(app, pack, overrides, maskUnsupported = true)
        // Ghost name is not in the pack -> the override is ignored -> FromPack wins.
        assertThat(source).isInstanceOf(IconSource.FromPack::class.java)
    }

    @Test
    fun `glyph override wins over pack and produced a Glyph source`() {
        val pack = packOf(mapOf(app.componentKey to "from_pack"))
        val overrides = mapOf(
            app.componentKey to IconOverride.Glyph("Phone", DrawerIconTokenName.Communication),
        )
        val source = resolver.resolve(app, pack, overrides, maskUnsupported = true)
        assertThat(source).isInstanceOf(IconSource.Glyph::class.java)
        assertThat((source as IconSource.Glyph).symbolName).isEqualTo("Phone")
        assertThat(source.colorToken).isEqualTo(DrawerIconTokenName.Communication)
    }

    @Test
    fun `unsupported app gets auto mask when enabled`() {
        val pack = packOf(emptyMap())
        val source = resolver.resolve(app, pack, emptyMap(), maskUnsupported = true)
        assertThat(source).isEqualTo(IconSource.AutoMask)
    }

    @Test
    fun `unsupported app falls to system when masking off`() {
        val source = resolver.resolve(app, null, emptyMap(), maskUnsupported = false)
        assertThat(source).isEqualTo(IconSource.System)
    }

    @Test
    fun `corrupt pack with no appfilter routes to mask`() {
        val corrupt = IconPack.invalid("p", "P", "/x")
        val source = resolver.resolve(app, corrupt, emptyMap(), maskUnsupported = true)
        assertThat(source).isEqualTo(IconSource.AutoMask)
    }

    @Test
    fun `pack match exposes the drawable name for real decode`() {
        val pack = packOf(mapOf(app.componentKey to "pack_gmail"))
        val source = resolver.resolve(app, pack, emptyMap(), maskUnsupported = true)
        assertThat((source as IconSource.FromPack).drawableName).isEqualTo("pack_gmail")
    }

    @Test
    fun `no override leaves the pipeline unchanged`() {
        val pack = packOf(mapOf(app.componentKey to "from_pack"))
        val source = resolver.resolve(app, pack, emptyMap(), maskUnsupported = true)
        assertThat(source).isInstanceOf(IconSource.FromPack::class.java)
    }

    @Test
    fun `pack without an appfilter never matches and falls through to mask`() {
        val noFilter = IconPack.invalid("p", "P", "/x").copy(
            entries = mapOf(app.componentKey to "should_be_ignored"),
            hasAppFilter = false,
        )
        val source = resolver.resolve(app, noFilter, emptyMap(), maskUnsupported = true)
        assertThat(source).isEqualTo(IconSource.AutoMask)
    }

    private fun packOf(entries: Map<String, String>) = IconPack(
        id = "pack", name = "Pack", sourcePath = "/p",
        entries = entries, hasAppFilter = true,
    )
}
