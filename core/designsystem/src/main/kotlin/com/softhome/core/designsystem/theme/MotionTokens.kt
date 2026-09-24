package com.softhome.core.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
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

    /** cubic-bezier(0.2, 0.8, 0.2, 1) */
    val WarmEase: Easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

    fun <T> railSlide(): TweenSpec<T> = tween(RAIL_SLIDE_MS, easing = WarmEase)

    fun <T> searchFade(): TweenSpec<T> = tween(SEARCH_FADE_MS, easing = WarmEase)

    fun <T> musicRise(): TweenSpec<T> = tween(MUSIC_RISE_MS, easing = WarmEase)

    /**
     * P2: the quick-notes row grows in-place on tap-to-expand. Same motion family
     * as [musicRise] (in-place grow, 280ms, WarmEase) -- deliberately reusing the
     * established vocabulary rather than inventing a new duration.
     */
    fun <T> notesExpand(): TweenSpec<T> = tween(MUSIC_RISE_MS, easing = WarmEase)
}
