package com.softhome.feature.iconpack.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for the auto-mask rules (P1.5). These are the device-free parts of the
 * fallback that makes a non-pack app look like it belongs to the SOFT family.
 */
class AutoMaskTest {

    @Test
    fun `squircle radius is 30 percent of the tile`() {
        // docs/08 ?3: 62->18.6, 104->31.2, 512->153.6
        assertThat(AutoMask.radiusPx(62f)).isWithin(0.01f).of(18.6f)
        assertThat(AutoMask.radiusPx(104f)).isWithin(0.01f).of(31.2f)
        assertThat(AutoMask.radiusPx(512f)).isWithin(0.01f).of(153.6f)
    }

    @Test
    fun `radius matches the shared IconMasker ratio`() {
        assertThat(AutoMask.RADIUS_RATIO).isEqualTo(IconMasker.RADIUS_RATIO)
    }

    @Test
    fun `icon inset leaves a recognizable mark inside the tile`() {
        // The masked icon must be smaller than the tile (breathing room) but large
        // enough to read - between half and the full tile.
        assertThat(AutoMask.ICON_INSET_RATIO).isGreaterThan(0.5f)
        assertThat(AutoMask.ICON_INSET_RATIO).isLessThan(1f)
    }

    @Test
    fun `tint is stronger for dark source pixels than bright ones`() {
        // Dark line art must be pushed toward cream hard; already-light content less.
        assertThat(AutoMask.tintFor(0f)).isGreaterThan(AutoMask.tintFor(1f))
    }

    @Test
    fun `tint stays within the unit range`() {
        for (l in listOf(-1f, 0f, 0.25f, 0.5f, 0.75f, 1f, 2f)) {
            assertThat(AutoMask.tintFor(l)).isAtLeast(0f)
            assertThat(AutoMask.tintFor(l)).isAtMost(1f)
        }
    }

    @Test
    fun `relative luminance is monotonic and bounded`() {
        assertThat(AutoMask.relativeLuminance(0, 0, 0)).isWithin(0.001f).of(0f)
        assertThat(AutoMask.relativeLuminance(255, 255, 255)).isWithin(0.001f).of(1f)
        assertThat(AutoMask.relativeLuminance(128, 128, 128))
            .isGreaterThan(AutoMask.relativeLuminance(0, 0, 0))
        assertThat(AutoMask.relativeLuminance(128, 128, 128))
            .isLessThan(AutoMask.relativeLuminance(255, 255, 255))
        // green contributes most, blue least (Rec. 709 weights)
        assertThat(AutoMask.relativeLuminance(0, 255, 0))
            .isGreaterThan(AutoMask.relativeLuminance(0, 0, 255))
    }

    @Test
    fun `tintPixel at zero strength returns the source colour`() {
        val out = AutoMask.tintPixel(10, 20, 30, 232, 223, 208, tint = 0f)
        assertThat(out[0]).isEqualTo(10)
        assertThat(out[1]).isEqualTo(20)
        assertThat(out[2]).isEqualTo(30)
    }

    @Test
    fun `tintPixel at full strength returns the cream colour`() {
        val out = AutoMask.tintPixel(10, 20, 30, 232, 223, 208, tint = 1f)
        assertThat(out[0]).isEqualTo(232)
        assertThat(out[1]).isEqualTo(223)
        assertThat(out[2]).isEqualTo(208)
    }

    @Test
    fun `tintPixel is bounded for out of range input`() {
        val out = AutoMask.tintPixel(300, -20, 999, 232, 223, 208, tint = 2f)
        out.forEach {
            assertThat(it).isAtLeast(0)
            assertThat(it).isAtMost(255)
        }
    }
}
