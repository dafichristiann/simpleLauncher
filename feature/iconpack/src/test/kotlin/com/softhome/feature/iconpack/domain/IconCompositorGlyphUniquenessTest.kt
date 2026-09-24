package com.softhome.feature.iconpack.domain

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Regression guards for the "identical charcoal ring" failure mode:
 * two different source icons MUST produce different composited bitmaps, and a
 * full-bleed opaque "blob" must not be accepted as a visible glyph.
 *
 * Uses [Bitmap.eraseColor] / [Bitmap.setPixels] (not Canvas) so Robolectric's
 * limited Canvas shadows cannot silently produce empty bitmaps.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class IconCompositorGlyphUniquenessTest {

    private val tile = Color.parseColor("#2B2B2B")
    private val cream = Color.parseColor("#E8DFD0")

    @Test
    fun `two different source glyphs produce different fingerprints after mask`() {
        val circle = bitmapDrawable(filledCircle(128, Color.BLACK))
        val triangle = bitmapDrawable(filledTriangle(128, Color.BLACK))
        val bars = bitmapDrawable(filledBars(128, Color.BLACK))

        val a = IconCompositor.mask(circle, 128, tile, cream)
        val b = IconCompositor.mask(triangle, 128, tile, cream)
        val c = IconCompositor.mask(bars, 128, tile, cream)

        val fa = IconCompositor.fingerprint(a)
        val fb = IconCompositor.fingerprint(b)
        val fc = IconCompositor.fingerprint(c)

        assertThat(fa).isNotEqualTo(fb)
        assertThat(fb).isNotEqualTo(fc)
        assertThat(fa).isNotEqualTo(fc)

        assertThat(IconCompositor.hasVisibleGlyph(a, tile)).isTrue()
        assertThat(IconCompositor.hasVisibleGlyph(b, tile)).isTrue()
        assertThat(IconCompositor.hasVisibleGlyph(c, tile)).isTrue()
    }

    @Test
    fun `different solid ColorDrawables stay distinct after mask`() {
        val red = IconCompositor.mask(ColorDrawable(Color.RED), 96, tile, cream, monochrome = false)
        val blue = IconCompositor.mask(ColorDrawable(Color.BLUE), 96, tile, cream, monochrome = false)
        assertThat(IconCompositor.fingerprint(red)).isNotEqualTo(IconCompositor.fingerprint(blue))
    }

    @Test
    fun `charcoal-only tile is rejected by hasVisibleGlyph`() {
        val blank = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
        blank.eraseColor(tile)
        assertThat(IconCompositor.hasVisibleGlyph(blank, tile)).isFalse()
    }

    @Test
    fun `full-bleed opaque blob is rejected by hasVisibleGlyph`() {
        val blob = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
        blob.eraseColor(cream)
        assertThat(IconCompositor.hasVisibleGlyph(blob, tile)).isFalse()
    }

    @Test
    fun `loader-style unique bitmaps keep unique fingerprints after toBitmap`() {
        val ctx = RuntimeEnvironment.getApplication()
        val a = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).also { it.eraseColor(Color.RED) }
        val b = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).also { it.eraseColor(Color.BLUE) }
        assertThat(a.getPixel(0, 0)).isEqualTo(Color.RED)
        assertThat(b.getPixel(0, 0)).isEqualTo(Color.BLUE)

        val ra = IconCompositor.toBitmap(BitmapDrawable(ctx.resources, a), 96)!!
        val rb = IconCompositor.toBitmap(BitmapDrawable(ctx.resources, b), 96)!!
        assertThat(ra.getPixel(48, 48)).isNotEqualTo(rb.getPixel(48, 48))
        assertThat(IconCompositor.fingerprint(ra)).isNotEqualTo(IconCompositor.fingerprint(rb))
    }

    private fun bitmapDrawable(bmp: Bitmap): BitmapDrawable =
        BitmapDrawable(RuntimeEnvironment.getApplication().resources, bmp)

    private fun filledCircle(size: Int, color: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(size * size)
        val r = size * 0.35f
        val cx = size / 2f
        val cy = size / 2f
        for (y in 0 until size) for (x in 0 until size) {
            val dx = x - cx
            val dy = y - cy
            if (dx * dx + dy * dy <= r * r) pixels[y * size + x] = color
        }
        bmp.setPixels(pixels, 0, size, 0, 0, size, size)
        return bmp
    }

    private fun filledTriangle(size: Int, color: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(size * size)
        for (y in 0 until size) for (x in 0 until size) {
            // Simple top-pointing triangle
            val t = y / size.toFloat()
            val half = (t * size * 0.45f)
            val mid = size / 2f
            if (y > size * 0.2f && x >= mid - half && x <= mid + half) {
                pixels[y * size + x] = color
            }
        }
        bmp.setPixels(pixels, 0, size, 0, 0, size, size)
        return bmp
    }

    private fun filledBars(size: Int, color: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(size * size)
        val barW = size / 7
        for (i in 0 until 3) {
            val x0 = size / 5 + i * (barW + size / 10)
            for (y in size / 5 until size * 4 / 5) for (x in x0 until x0 + barW) {
                if (x in 0 until size) pixels[y * size + x] = color
            }
        }
        bmp.setPixels(pixels, 0, size, 0, 0, size, size)
        return bmp
    }
}
