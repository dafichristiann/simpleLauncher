package com.softhome.feature.iconpack.domain

import com.google.common.truth.Truth.assertThat
import com.softhome.core.model.AppInfo
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
    fun `override wins over pack`() {
        val pack = packOf(mapOf(app.componentKey to "from_pack"))
        val overrides = mapOf(app.componentKey to pack.id)
        val source = resolver.resolve(app, pack, overrides, maskUnsupported = true)
        assertThat(source).isInstanceOf(IconSource.Override::class.java)
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
    fun `override exposes the overridden drawable name`() {
        val pack = packOf(mapOf(app.componentKey to "from_pack"))
        val overrides = mapOf(app.componentKey to pack.id)
        val source = resolver.resolve(app, pack, overrides, maskUnsupported = true)
        assertThat((source as IconSource.Override).drawableName).isEqualTo("from_pack")
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
