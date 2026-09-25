package com.softhome.core.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween

/**
 * Motion system -- "Small movement, clear purpose."
 *
 * Source: design/homeApp.pen board `L7ZAp`, motion token card node `Z5V7Y9`,
 * text node `Cvy3V`:
 *
 *   RAIL SLIDE   220ms
 *   SEARCH FADE  160ms
 *   MUSIC RISE   280ms
 *   EASE  cubic-bezier(0.2, 0.8, 0.2, 1)
 *
 * Rule: screens never hardcode a duration or easing -- every animation goes
 * through one of the helpers below.
 */
object MotionTokens {
    const val RAIL_SLIDE_MS = 220
    const val SEARCH_FADE_MS = 160
    const val MUSIC_RISE_MS = 280
    const val PRESS_FEEDBACK_MS = 160
    /** Directional nudge for drawer category changes; intentionally not a full-page slide. */
    const val CATEGORY_MICRO_SLIDE_DP = 10
    /** Final P6 tuning: launcher-like deceleration with a softer, longer settle. */
    const val CATEGORY_LAUNCHER_MS = 280
    const val OVERLAY_ENTER_MS = 220
    const val OVERLAY_EXIT_MS = 180
    const val THEME_TRANSITION_MS = 220
    const val CLOCK_DIGIT_MS = 240
    const val WEATHER_TRANSITION_MS = 320
    const val MUSIC_EXPAND_MS = 360
    const val MUSIC_PROGRESS_MS = 120

    /** cubic-bezier(0.2, 0.8, 0.2, 1) */
    val WarmEase: Easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

    fun <T> railSlide(): TweenSpec<T> = tween(RAIL_SLIDE_MS, easing = WarmEase)

    fun <T> searchFade(): TweenSpec<T> = tween(SEARCH_FADE_MS, easing = WarmEase)

    /** P6 final: 10dp micro-slide + Compose's natural launcher-style easing. */
    fun <T> categoryLauncher(): TweenSpec<T> =
        tween(CATEGORY_LAUNCHER_MS, easing = FastOutSlowInEasing)

    fun <T> musicRise(): TweenSpec<T> = tween(MUSIC_RISE_MS, easing = WarmEase)

    fun <T> pressFeedback(): TweenSpec<T> = tween(PRESS_FEEDBACK_MS, easing = WarmEase)

    fun <T> overlayEnter(): TweenSpec<T> = tween(OVERLAY_ENTER_MS, easing = WarmEase)

    fun <T> overlayExit(): TweenSpec<T> = tween(OVERLAY_EXIT_MS, easing = WarmEase)

    fun <T> themeTransition(): TweenSpec<T> = tween(THEME_TRANSITION_MS, easing = WarmEase)

    fun <T> clockDigit(): TweenSpec<T> = tween(CLOCK_DIGIT_MS, easing = WarmEase)

    fun <T> weatherTransition(): TweenSpec<T> = tween(WEATHER_TRANSITION_MS, easing = WarmEase)

    fun <T> musicExpand(): TweenSpec<T> = tween(MUSIC_EXPAND_MS, easing = WarmEase)

    /** Playback progress is time-based by nature, so its interpolation remains linear. */
    fun <T> musicProgress(): TweenSpec<T> = tween(MUSIC_PROGRESS_MS, easing = LinearEasing)

    /**
     * P2: the quick-notes row grows in-place on tap-to-expand. Same motion family
     * as [musicRise] (in-place grow, 280ms, WarmEase) -- deliberately reusing the
     * established vocabulary rather than inventing a new duration.
     */
    fun <T> notesExpand(): TweenSpec<T> = tween(MUSIC_RISE_MS, easing = WarmEase)

    // --- P4a: drag & drop -----------------------------------------------------

    /** Drag lift: the item scales up under the finger. Fade-class speed (160ms). */
    fun <T> dragLift(): TweenSpec<T> = tween(SEARCH_FADE_MS, easing = WarmEase)

    /** Drop / snap-back settle. Slightly longer than the lift so it reads as landing. */
    fun <T> dragSnapBack(): TweenSpec<T> = overlayExit()

    /**
     * Live row shift while dragging an item over a list. Reuses the in-place grow
     * family (280ms, WarmEase) so reorder matches the rest of the Warm vocabulary.
     */
    fun <T> dragReorder(): TweenSpec<T> = tween(MUSIC_RISE_MS, easing = WarmEase)
}
