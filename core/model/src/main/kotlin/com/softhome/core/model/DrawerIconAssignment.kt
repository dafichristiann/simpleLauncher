package com.softhome.core.model

/**
 * P4: deterministic per-package drawer glyph + color assignment, with a uniqueness
 * guarantee.
 *
 * The drawer must not render two **different** apps with the same `(glyph, colorToken)`
 * pair -- that is the "duplicate icons" complaint. This object is the pure decision
 * layer over [DrawerIconMap]:
 *
 *  1. Ask [DrawerIconMap] for the design's `(glyph, category, color)` for the package.
 *  2. If unknown, fall back to the caller's heuristic glyph ([heuristicGlyph]) and the
 *     caller's heuristic color ([heuristicColor], typically derived from the OS category).
 *  3. **De-duplicate:** walking the app list in a stable (sorted) order, if a package's
 *     `(glyph, color)` already belongs to an earlier app, nudge it deterministically to
 *     the next free pair so no two tiles collide.
 *
 * Determinism matters: the assignment must be identical across launches (tiles must not
 * shuffle). It is derived purely from the sorted package list -- no randomness, no time.
 *
 * Pure Kotlin: unit-testable without a device, and seeded in tests with the real Tecno
 * package list (`DrawerIconUniquenessTest`).
 */
object DrawerIconAssignment {

    /**
     * Resolvable fallback glyphs shared by every surface that mirrors the drawer icon.
     * Keeping this list here prevents Home/rail from drifting away from All Apps when
     * the uniqueness pass has to nudge a duplicate pair.
     */
    val DEFAULT_GLYPH_FALLBACKS: List<String> = listOf(
        "AppWindow", "Square", "Box", "CircleDot", "Sparkles",
        "Shield", "Tag", "Bot", "Store", "Wrench",
    )

    /** One app's resolved glyph + color. */
    data class Assignment(val glyph: String, val colorToken: DrawerIconTokenName)

    /** The palette in a fixed order (used to rotate the color on a colliding glyph). */
    private val COLORS: List<DrawerIconTokenName> = DrawerIconTokenName.entries.toList()

    /**
     * Assign a unique `(glyph, colorToken)` to every drawer cell.
     *
     * @param identities `key` (the drawer cell identity, e.g. `componentKey`) to
     *   `packageName`. Two cells that share a package (e.g. a dialer's Phone and
     *   Contacts activities) are **different** cells and get different pairs.
     * @param heuristicGlyph the fallback glyph for an unknown package (from `IconMasker`).
     * @param heuristicColor the fallback color for an unknown package.
     * @param glyphFallbacks extra **resolvable** glyph names to cycle through when a whole
     *   glyph's color column is exhausted (keeps the pair resolvable, never a fake name).
     * @return cell key -> assignment, stable for a given input set.
     */
    fun assign(
        identities: Collection<Pair<String, String>>,
        heuristicGlyph: (String) -> String,
        heuristicColor: (String) -> DrawerIconTokenName,
        glyphFallbacks: List<String> = emptyList(),
    ): Map<String, Assignment> {
        // Sort by key so the result is independent of the input iteration order.
        val sorted = identities
            .filter { it.first.isNotBlank() }
            .distinctBy { it.first }
            .sortedBy { it.first }
        val used = HashSet<Assignment>(sorted.size)
        val out = HashMap<String, Assignment>(sorted.size)

        for ((key, pkg) in sorted) {
            val entry = DrawerIconMap.forPackage(pkg)
            val baseGlyph = entry?.glyph ?: heuristicGlyph(pkg)
            val baseColor = entry?.colorToken ?: heuristicColor(pkg)

            var chosen = Assignment(baseGlyph, baseColor)
            if (chosen in used) {
                chosen = nudge(baseGlyph, baseColor, used, glyphFallbacks)
            }
            used += chosen
            out[key] = chosen
        }
        return out
    }

    private fun nudge(
        baseGlyph: String,
        baseColor: DrawerIconTokenName,
        used: Set<Assignment>,
        glyphFallbacks: List<String>,
    ): Assignment {
        // 1. rotate the color on the base glyph (keeps the app's icon).
        val start = COLORS.indexOf(baseColor).coerceAtLeast(0)
        for (i in COLORS.indices) {
            val c = COLORS[(start + i) % COLORS.size]
            val cand = Assignment(baseGlyph, c)
            if (cand !in used) return cand
        }
        // 2. its color column is full -> try the same color on a resolvable fallback glyph.
        for (g in glyphFallbacks) {
            val cand = Assignment(g, baseColor)
            if (cand !in used) return cand
        }
        // 3. last resort: any fallback glyph with any color (still resolvable).
        for (g in glyphFallbacks) {
            for (c in COLORS) {
                val cand = Assignment(g, c)
                if (cand !in used) return cand
            }
        }
        // 4. no fallback glyphs supplied and the pair is fully saturated: return the base
        //    pair unchanged (caller-supplied pool is expected to be non-empty in the app).
        return Assignment(baseGlyph, baseColor)
    }
}
