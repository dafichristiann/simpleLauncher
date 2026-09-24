package com.softhome.feature.iconpack.data

import com.softhome.core.model.IconPack
import com.softhome.core.model.IconPackParseResult
import java.io.InputStream
import javax.inject.Inject

/**
 * Parses an `appfilter.xml` into an [IconPack].
 *
 * Spec: docs/08-ICONPACK-FORMAT.md ?1. Rules:
 *  - `component` = ComponentInfo{pkg/cls}; cls may be fully-qualified or short (.Main)
 *  - normalize to "pkg/class"
 *  - ignore unknown tags; be lenient
 *  - duplicate component -> last-write-wins
 *  - malformed XML -> Invalid (caller falls back to system icons)
 *
 * Implemented with a hand-rolled reader so it is unit-testable off-device (no XmlPullParser
 * dependency on the Android framework) and tolerant of real-world pack quirks.
 */
class AppFilterParser @Inject constructor() {

    fun parse(input: InputStream, packId: String, packName: String, path: String): IconPackParseResult {
        val content = try {
            input.bufferedReader().use { it.readText() }
        } catch (t: Throwable) {
            return IconPackParseResult.Invalid("Could not read appfilter: ${t.message}")
        }
        return parseString(content, packId, packName, path)
    }

    fun parseString(xml: String, packId: String, packName: String, path: String): IconPackParseResult {
        if (xml.isBlank() || !xml.contains("<item", ignoreCase = true)) {
            // No <item> entries at all -> treat as "no appfilter" (docs/08 ?1 variants).
            return IconPackParseResult.NoAppFilter(
                IconPack.invalid(packId, packName, path).copy(name = packName),
            )
        }

        val entries = LinkedHashMap<String, String>()
        try {
            // Split on `<item` boundaries rather than matching `<item ...>`, so a
            // missing `>` on one entry cannot swallow the next one (real packs are
            // occasionally truncated). Each chunk holds one entry's attributes.
            val chunks = xml.split(Regex("""<item\b""", RegexOption.IGNORE_CASE)).drop(1)
            var matchedAny = false
            for (chunk in chunks) {
                // Attributes live before the first `>` or the next `<`.
                val cut = chunk.indexOfFirst { it == '>' || it == '<' }.let { if (it < 0) chunk.length else it }
                val attrs = chunk.substring(0, cut)
                val component = attr(attrs, "component") ?: continue
                val drawable = attr(attrs, "drawable") ?: continue
                val key = normalizeComponent(component) ?: continue
                entries[key] = drawable
                matchedAny = true
            }
            if (!matchedAny) {
                return IconPackParseResult.NoAppFilter(
                    IconPack.invalid(packId, packName, path).copy(name = packName),
                )
            }
            return IconPackParseResult.Success(
                IconPack(
                    id = packId, name = packName, sourcePath = path,
                    entries = entries, hasAppFilter = true, iconCount = entries.size,
                ),
            )
        } catch (t: Throwable) {
            return IconPackParseResult.Invalid("Malformed appfilter: ${t.message}")
        }
    }

    private fun attr(attrs: String, name: String): String? {
        val m = Regex("""\b$name\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE).find(attrs)
        return m?.groupValues?.get(1)
    }

    /**
     * "ComponentInfo{com.foo/.Main}" -> "com.foo/com.foo.Main"
     * Already-normalized "pkg/cls" also accepted.
     *
     * Parsed with plain string ops (no regex) on purpose: real packs wrap the payload
     * in `{}` and regex escaping of `{` / `}` varies across engines, which made a
     * regex version throw `PatternSyntaxException` on-device for otherwise valid input.
     */
    fun normalizeComponent(raw: String): String? {
        var s = raw.trim()
        // Strip a leading `ComponentInfo{ ... }` wrapper if present.
        val open = s.indexOf('{')
        val close = s.lastIndexOf('}')
        if (open >= 0 && close > open && s.substring(0, open).trim().equals("ComponentInfo", ignoreCase = true)) {
            s = s.substring(open + 1, close).trim()
        }
        if (!s.contains('/')) return null
        val (pkg, cls) = s.split('/', limit = 2)
        if (pkg.isBlank() || cls.isBlank()) return null
        // Reject any leftover brace noise (e.g. malformed `{/}`) - a real package or
        // class name never contains `{` or `}`.
        if (pkg.contains('{') || pkg.contains('}') || cls.contains('{') || cls.contains('}')) return null
        val fullCls = if (cls.startsWith('.')) pkg + cls else cls
        return "$pkg/$fullCls"
    }
}
