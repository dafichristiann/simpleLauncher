package com.softhome.feature.iconpack.data

import com.google.common.truth.Truth.assertThat
import com.softhome.core.model.IconPackParseResult
import org.junit.Test

class AppFilterParserTest {

    private val parser = AppFilterParser()

    @Test
    fun `parses a componentinfo entry with short class`() {
        val xml = """
            <resources>
              <item component="ComponentInfo{com.foo.bar/.Main}" drawable="foo"/>
            </resources>
        """.trimIndent()
        val result = parser.parseString(xml, "pack", "Pack", "/p")
        assertThat(result).isInstanceOf(IconPackParseResult.Success::class.java)
        val pack = (result as IconPackParseResult.Success).pack
        assertThat(pack.entries).containsKey("com.foo.bar/com.foo.bar.Main")
        assertThat(pack.entries["com.foo.bar/com.foo.bar.Main"]).isEqualTo("foo")
        assertThat(pack.hasAppFilter).isTrue()
    }

    @Test
    fun `parses a fully qualified class`() {
        val xml = """
            <resources>
              <item component="ComponentInfo{com.foo/com.foo.ui.MainActivity}" drawable="foo"/>
            </resources>
        """.trimIndent()
        val pack = (parser.parseString(xml, "pack", "P", "/p") as IconPackParseResult.Success).pack
        assertThat(pack.entries.keys).containsExactly("com.foo/com.foo.ui.MainActivity")
    }

    @Test
    fun `duplicate entries are last-write-wins`() {
        val xml = """
            <resources>
              <item component="ComponentInfo{com.foo/.A}" drawable="first"/>
              <item component="ComponentInfo{com.foo/.A}" drawable="second"/>
            </resources>
        """.trimIndent()
        val pack = (parser.parseString(xml, "pack", "P", "/p") as IconPackParseResult.Success).pack
        assertThat(pack.entries["com.foo/com.foo.A"]).isEqualTo("second")
    }

    @Test
    fun `non componentinfo component values are skipped`() {
        val xml = """
            <resources>
              <item component="just.a.string" drawable="x"/>
              <item component="ComponentInfo{com.ok/.Go}" drawable="ok"/>
            </resources>
        """.trimIndent()
        val pack = (parser.parseString(xml, "pack", "P", "/p") as IconPackParseResult.Success).pack
        assertThat(pack.entries.keys).containsExactly("com.ok/com.ok.Go")
    }

    @Test
    fun `empty or itemless xml is reported as no appfilter`() {
        assertThat(parser.parseString("<resources></resources>", "pack", "P", "/p"))
            .isInstanceOf(IconPackParseResult.NoAppFilter::class.java)
        assertThat(parser.parseString("", "pack", "P", "/p"))
            .isInstanceOf(IconPackParseResult.NoAppFilter::class.java)
    }

    @Test
    fun `normalizeComponent expands short class`() {
        assertThat(parser.normalizeComponent("ComponentInfo{com.a/.B}")).isEqualTo("com.a/com.a.B")
        assertThat(parser.normalizeComponent("com.a/com.a.B")).isEqualTo("com.a/com.a.B")
        // A bare fully-qualified class with no '/' is not a valid ComponentInfo{...}
        // payload (the pkg/cls separator is required), so it is rejected.
        assertThat(parser.normalizeComponent("nonsense")).isNull()
        assertThat(parser.normalizeComponent("com.a.c.D")).isNull()
    }

    // --- P1.5 regressions/edges -------------------------------------------------

    @Test
    fun `normalizeComponent never throws on brace heavy input`() {
        // Regression: a regex-based implementation threw PatternSyntaxException on
        // device for these otherwise-valid ComponentInfo payloads.
        assertThat(parser.normalizeComponent("ComponentInfo{com.a/.B}")).isEqualTo("com.a/com.a.B")
        assertThat(parser.normalizeComponent("ComponentInfo{com.a/com.a.B}")).isEqualTo("com.a/com.a.B")
        assertThat(parser.normalizeComponent("  ComponentInfo{ com.a/.B }  ")).isEqualTo("com.a/com.a.B")
        // No throw for empty/garbage either.
        assertThat(parser.normalizeComponent("")).isNull()
        assertThat(parser.normalizeComponent("{}")).isNull()
        assertThat(parser.normalizeComponent("ComponentInfo{}")).isNull()
        assertThat(parser.normalizeComponent("{/}")).isNull()
    }

    @Test
    fun `normalizeComponent accepts non-componentinfo pkg-slash-class`() {
        assertThat(parser.normalizeComponent("com.a/com.a.B")).isEqualTo("com.a/com.a.B")
        assertThat(parser.normalizeComponent("com.a/.B")).isEqualTo("com.a/com.a.B")
    }

    @Test
    fun `parse handles attribute order and single quotes`() {
        val xml = """
            <resources>
              <item drawable='bar' component='ComponentInfo{com.bar/.Main}'/>
            </resources>
        """.trimIndent()
        val pack = (parser.parseString(xml, "pack", "P", "/p") as IconPackParseResult.Success).pack
        assertThat(pack.entries["com.bar/com.bar.Main"]).isEqualTo("bar")
    }

    @Test
    fun `parse ignores items missing component or drawable`() {
        val xml = """
            <resources>
              <item component="ComponentInfo{com.a/.A}"/>
              <item drawable="only_drawable"/>
              <item component="ComponentInfo{com.ok/.Go}" drawable="ok"/>
            </resources>
        """.trimIndent()
        val pack = (parser.parseString(xml, "pack", "P", "/p") as IconPackParseResult.Success).pack
        assertThat(pack.entries.keys).containsExactly("com.ok/com.ok.Go")
    }

    @Test
    fun `malformed xml still yields the valid items it can find`() {
        // Real packs are messy; we are lenient and keep what parses. A missing `>`
        // must not throw, and the well-formed trailing item must survive.
        val xml = "<resources><item component=\"ComponentInfo{com.a/.A}\" drawable=\"a\"" +
            "<item component=\"ComponentInfo{com.b/.B}\" drawable=\"b\"/></resources>"
        val result = parser.parseString(xml, "pack", "P", "/p")
        assertThat(result).isInstanceOf(IconPackParseResult.Success::class.java)
        val pack = (result as IconPackParseResult.Success).pack
        assertThat(pack.entries).isNotEmpty()
        // The valid trailing entry is always recovered.
        assertThat(pack.entries).containsKey("com.b/com.b.B")
    }

    @Test
    fun `pack reports its entry and drawable counts`() {
        val xml = """
            <resources>
              <item component="ComponentInfo{com.a/.A}" drawable="shared"/>
              <item component="ComponentInfo{com.b/.B}" drawable="shared"/>
              <item component="ComponentInfo{com.c/.C}" drawable="unique"/>
            </resources>
        """.trimIndent()
        val pack = (parser.parseString(xml, "pack", "P", "/p") as IconPackParseResult.Success).pack
        assertThat(pack.iconCount).isEqualTo(3)
        // 3 entries but only 2 distinct drawables.
        assertThat(pack.drawableCount).isEqualTo(2)
    }
}
