package com.softhome.feature.iconpack.domain

/**
 * Pure math + colour rules for the auto-mask fallback (docs/08 ?3).
 *
 * Auto-mask is what makes an app that is *not* in the active pack still look like
 * it belongs to the SOFT monochrome family: the app's real icon is composited into
 * the same charcoal squircle and pushed toward the cream monochrome palette.
 *
 * Kept device-free so the rules can be unit-tested without an emulator.
 */
object AutoMask {

    /** Squircle radius = 30% of the tile (shared with [IconMasker]). */
    const val RADIUS_RATIO = 0.30f

    /**
     * How much of the tile the real icon occupies when drawn inside the squircle.
     * Design tiles sit a cream mark at ~0.49 of the tile; the masked real icon is
     * a little larger so recognisable app shapes read clearly.
     */
    const val ICON_INSET_RATIO = 0.62f

    /**
     * Blend weight of the cream monochrome tint applied over the desaturated real
     * icon. 1.0 = fully cream (unreadable), 0.0 = raw icon. The design wants a
     * *recognisable but quiet* mark, so we tint strongly but keep luminance lift
     * so shapes stay legible: current value chosen to sit between the two.
     */
    const val TINT_STRENGTH = 0.82f

    /** Minimum relative luminance an icon must have to survive as-is (else flip). */
    const val LUMINANCE_FLOOR = 0.06f

    /** Squircle corner radius in raw pixels for a tile of [sizePx]. */
    fun radiusPx(sizePx: Float): Float = sizePx * RADIUS_RATIO

    /**
     * 0f..1f - how strongly we tint a pixel of relative luminance [lum] toward cream.
     * Dark pixels (icon line work on transparent, or dark logos) get the full tint so
     * they become cream; already-bright pixels get less, preserving a little contrast.
     */
    fun tintFor(lum: Float): Float {
        val clamped = lum.coerceIn(0f, 1f)
        // Invert: dark stays dark in the source but must become *cream*, so we let the
        // tint dominate on dark alpha, and ease off on already-light content.
        return (TINT_STRENGTH * (1f - clamped * 0.5f)).coerceIn(0f, 1f)
    }

    /**
     * Perceptual luminance (Rec. 709) of an 8-bit RGB triple, normalised to 0..1.
     */
    fun relativeLuminance(r: Int, g: Int, b: Int): Float {
        val lr = srgbToLinear(r / 255f)
        val lg = srgbToLinear(g / 255f)
        val lb = srgbToLinear(b / 255f)
        return (0.2126f * lr + 0.7152f * lg + 0.0722f * lb).coerceIn(0f, 1f)
    }

    private fun srgbToLinear(c: Float): Float =
        if (c <= 0.04045f) c / 12.92f else Math.pow(((c + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()

    /**
     * Blend a source pixel toward cream at [tint]. Keeps alpha. Pure function so the
     * colour rule is testable; the Android renderer calls it per pixel.
     */
    fun tintPixel(
        r: Int, g: Int, b: Int,
        creamR: Int, creamG: Int, creamB: Int,
        tint: Float,
    ): IntArray {
        val t = tint.coerceIn(0f, 1f)
        fun mix(src: Int, dst: Int) = (src + (dst - src) * t).toInt().coerceIn(0, 255)
        return intArrayOf(mix(r, creamR), mix(g, creamG), mix(b, creamB))
    }
}
