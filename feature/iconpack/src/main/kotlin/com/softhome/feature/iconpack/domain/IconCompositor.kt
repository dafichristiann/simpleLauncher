package com.softhome.feature.iconpack.domain

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build

/**
 * Renders a real app [Drawable] into the SOFT squircle as a monochrome mark
 * (docs/08 §3a auto-mask). Runs entirely off-device-testable rules where possible:
 * the geometry/colour decisions live in [AutoMask]; this class only paints.
 *
 * The output is a square ARGB bitmap: charcoal squircle backdrop + the app icon,
 * desaturated and tinted to the cream palette so a masked icon reads as part of the
 * same family as a decoded icon-pack drawable.
 */
object IconCompositor {

    /** Minimum fraction of non-transparent pixels for a usable silhouette. */
    private const val MIN_COVERAGE = 0.04f

    /**
     * Maximum fraction of non-transparent pixels. A near-full opaque layer is a
     * "blob" (e.g. launcher mono that fills the canvas with a solid colour) and
     * must not be preferred over the full adaptive composite.
     */
    private const val MAX_COVERAGE = 0.72f

    /**
     * Compose a masked icon. [sizePx] is the final square size.
     *
     * @param icon            the app's real icon (from PackageManager)
     * @param tileColor       charcoal squircle colour (design token)
     * @param tintColor       cream monochrome tint (design token)
     * @param monochrome      when true, desaturate + tint the icon to cream
     * @param iconInsetRatio  how much of the tile the icon occupies
     */
    fun mask(
        icon: Drawable,
        sizePx: Int,
        tileColor: Int,
        tintColor: Int,
        monochrome: Boolean = true,
        iconInsetRatio: Float = AutoMask.ICON_INSET_RATIO,
    ): Bitmap {
        val size = sizePx.coerceAtLeast(1)
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)

        val radius = AutoMask.radiusPx(size.toFloat())
        val squircle = Path().apply {
            addRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), radius, radius, Path.Direction.CW)
        }

        // 1. charcoal squircle backdrop
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = tileColor }
        canvas.drawPath(squircle, bgPaint)

        // 2. clip to the squircle so the icon never spills the tile shape
        canvas.save()
        canvas.clipPath(squircle)

        val plan = resolveGlyphPlan(icon)
        val effectiveInsetRatio = if (plan.isAdaptive) 0.74f else iconInsetRatio
        val inset = (size * effectiveInsetRatio).toInt().coerceIn(1, size)
        val offset = (size - inset) / 2
        val src = toBitmap(plan.glyph, inset)

        android.util.Log.d(
            "SOFTHOME_PIPELINE",
            "[COMPOSITOR] mask() plan=${plan.mode} iconClass=${icon.javaClass.name} " +
                "glyphClass=${plan.glyph.javaClass.name} sizePx=$size inset=$inset " +
                "srcNull=${src == null} coverage=${plan.coverage} monochrome=$monochrome",
        )

        if (src != null) {
            val useSrcIn = monochrome && plan.mode == GlyphMode.MonoSilhouette
            val processedBmp = if (monochrome && !useSrcIn) {
                monochrome(src, tintColor)
            } else {
                null
            }
            val drawSrc = processedBmp ?: src
            val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                isFilterBitmap = true
                if (useSrcIn) {
                    colorFilter = PorterDuffColorFilter(tintColor, PorterDuff.Mode.SRC_IN)
                }
            }
            canvas.drawBitmap(drawSrc, offset.toFloat(), offset.toFloat(), iconPaint)
            // toBitmap()/scaleCopy() always return owned copies — safe to recycle.
            if (processedBmp != null && processedBmp !== src) processedBmp.recycle()
            src.recycle()
        }

        canvas.restore()
        val centerPixel = out.getPixel(size / 2, size / 2)
        val glyphPixels = countNonTilePixels(out, tileColor)
        android.util.Log.d(
            "SOFTHOME_PIPELINE",
            "[COMPOSITOR] output size=${out.width}x${out.height} " +
                "centerPixel=${Integer.toHexString(centerPixel)} " +
                "nonTilePixels=$glyphPixels fingerprint=${fingerprint(out)}",
        )
        return out
    }

    /**
     * True when [bitmap] has enough non-tile pixels to count as a visible glyph.
     * Used by [IconBitmapProvider] to reject charcoal-only / blob failures so the
     * UI can fall through to the category glyph.
     */
    fun hasVisibleGlyph(bitmap: Bitmap, tileColor: Int): Boolean {
        val total = bitmap.width * bitmap.height
        if (total <= 0) return false
        val nonTile = countNonTilePixels(bitmap, tileColor)
        val coverage = nonTile.toFloat() / total.toFloat()
        // Too sparse = charcoal-only ring. Too dense = solid cream blob (e.g. bad
        // monochrome layer). Either way the tile loses per-app identity.
        return coverage in MIN_COVERAGE..MAX_COVERAGE
    }

    /**
     * Stable fingerprint for regression tests: FNV-1a over a dense pixel grid.
     * Two visually distinct icons must not collide.
     */
    fun fingerprint(bitmap: Bitmap): Long {
        var h = -0x340d0ceb48e2430dL // FNV offset basis (signed long form of 0xCBF2…UL)
        val w = bitmap.width
        val hgt = bitmap.height
        if (w <= 0 || hgt <= 0) return 0L
        h = h xor w.toLong()
        h *= 0x100000001B3L
        h = h xor hgt.toLong()
        h *= 0x100000001B3L
        val steps = 16
        for (yi in 0 until steps) {
            for (xi in 0 until steps) {
                val x = (xi * (w - 1)) / (steps - 1).coerceAtLeast(1)
                val y = (yi * (hgt - 1)) / (steps - 1).coerceAtLeast(1)
                val p = bitmap.getPixel(x, y).toLong() and 0xFFFFFFFFL
                h = h xor p
                h *= 0x100000001B3L
            }
        }
        return h
    }

    /** Rasterise a [Drawable] into a square ARGB bitmap of [sizePx]. */
    fun toBitmap(drawable: Drawable, sizePx: Int): Bitmap? {
        val size = sizePx.coerceAtLeast(1)
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            val src = drawable.bitmap
            return scaleCopy(src, size)
        }
        return try {
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bmp
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Desaturate + tint a bitmap to a single cream tone, preserving alpha. Used when
     * we want a true *monochrome* mark rather than a hue-shifted logo.
     */
    fun monochrome(bitmap: Bitmap, tintColor: Int): Bitmap {
        val out = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(out.width * out.height)
        out.getPixels(pixels, 0, out.width, 0, 0, out.width, out.height)
        val cr = Color.red(tintColor)
        val cg = Color.green(tintColor)
        val cb = Color.blue(tintColor)
        for (i in pixels.indices) {
            val p = pixels[i]
            val a = Color.alpha(p)
            if (a == 0) continue
            // Preserve luminance contrast: dark source -> stronger cream, bright -> softer.
            val lum = AutoMask.relativeLuminance(Color.red(p), Color.green(p), Color.blue(p))
            val tint = AutoMask.tintFor(lum)
            val mixed = AutoMask.tintPixel(
                Color.red(p), Color.green(p), Color.blue(p),
                cr, cg, cb,
                tint,
            )
            pixels[i] = Color.argb(a, mixed[0], mixed[1], mixed[2])
        }
        out.setPixels(pixels, 0, out.width, 0, 0, out.width, out.height)
        return out
    }

    // -------------------------------------------------------------------------

    private enum class GlyphMode { MonoSilhouette, FullAdaptive, Plain }

    private data class GlyphPlan(
        val glyph: Drawable,
        val mode: GlyphMode,
        val isAdaptive: Boolean,
        val coverage: Float,
    )

    private fun resolveGlyphPlan(icon: Drawable): GlyphPlan {
        val adaptive = icon as? AdaptiveIconDrawable
        if (adaptive == null) {
            return GlyphPlan(icon, GlyphMode.Plain, isAdaptive = false, coverage = -1f)
        }

        val mono = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            adaptive.monochrome
        } else {
            null
        }
        if (mono != null) {
            val probe = toBitmap(mono, 96)
            val coverage = probe?.let { alphaCoverage(it) } ?: 0f
            probe?.recycle()
            if (coverage in MIN_COVERAGE..MAX_COVERAGE) {
                return GlyphPlan(mono, GlyphMode.MonoSilhouette, isAdaptive = true, coverage = coverage)
            }
            android.util.Log.w(
                "SOFTHOME_PIPELINE",
                "[COMPOSITOR] Rejecting monochrome layer (coverage=$coverage) — " +
                    "falling back to full AdaptiveIconDrawable",
            )
        }

        // Draw the full adaptive (bg + fg + mask). Prefer this over bare
        // foreground: many apps put brand colour in the background layer.
        return GlyphPlan(adaptive, GlyphMode.FullAdaptive, isAdaptive = true, coverage = -1f)
    }

    private fun alphaCoverage(bitmap: Bitmap): Float {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var opaque = 0
        for (p in pixels) {
            if (Color.alpha(p) > 16) opaque++
        }
        return opaque.toFloat() / pixels.size.toFloat()
    }

    private fun countNonTilePixels(bitmap: Bitmap, tileColor: Int): Int {
        val tr = Color.red(tileColor)
        val tg = Color.green(tileColor)
        val tb = Color.blue(tileColor)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var count = 0
        for (p in pixels) {
            if (Color.alpha(p) < 16) continue
            val dr = kotlin.math.abs(Color.red(p) - tr)
            val dg = kotlin.math.abs(Color.green(p) - tg)
            val db = kotlin.math.abs(Color.blue(p) - tb)
            if (dr + dg + db > 36) count++
        }
        return count
    }

    /** Always return a *copy* so callers can safely recycle without touching PM caches. */
    private fun scaleCopy(src: Bitmap, sizePx: Int): Bitmap {
        return if (src.width == sizePx && src.height == sizePx) {
            src.copy(Bitmap.Config.ARGB_8888, /* mutable = */ false)
                ?: Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888).also { copy ->
                    Canvas(copy).drawBitmap(src, 0f, 0f, null)
                }
        } else {
            Bitmap.createScaledBitmap(src, sizePx, sizePx, true)
        }
    }
}
