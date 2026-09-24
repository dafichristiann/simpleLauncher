package com.softhome.core.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Guards the Warm Right Rail motion tokens against drift from the design
 * spec (homeApp.pen board L7ZAp -> card Z5V7Y9 -> text Cvy3V):
 *
 *   RAIL SLIDE   220ms
 *   SEARCH FADE  160ms
 *   MUSIC RISE   280ms
 *   EASE  cubic-bezier(0.2, 0.8, 0.2, 1)
 */
class MotionTokensTest {

    @Test
    fun durations_match_design_spec() {
        assertEquals(220, MotionTokens.RAIL_SLIDE_MS)
        assertEquals(160, MotionTokens.SEARCH_FADE_MS)
        assertEquals(280, MotionTokens.MUSIC_RISE_MS)
    }

    @Test
    fun warm_ease_matches_cubic_bezier() {
        val expected = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
        assertEquals(expected, MotionTokens.WarmEase)
    }

    @Test
    fun tween_helpers_use_token_durations() {
        assertEquals(220, MotionTokens.railSlide<Float>().durationMillis)
        assertEquals(160, MotionTokens.searchFade<Float>().durationMillis)
        assertEquals(280, MotionTokens.musicRise<Float>().durationMillis)
        // P2: notes tap-to-expand reuses the in-place-grow family (280ms).
        assertEquals(280, MotionTokens.notesExpand<Float>().durationMillis)
    }

    @Test
    fun tween_helpers_share_warm_easing() {
        assertEquals(MotionTokens.WarmEase, MotionTokens.railSlide<Float>().easing)
        assertEquals(MotionTokens.WarmEase, MotionTokens.searchFade<Float>().easing)
        assertEquals(MotionTokens.WarmEase, MotionTokens.musicRise<Float>().easing)
        assertEquals(MotionTokens.WarmEase, MotionTokens.notesExpand<Float>().easing)
    }
}
