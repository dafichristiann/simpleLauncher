package com.softhome.core.designsystem.atom

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * P1.1: a **pass-through** vertical-swipe observer.
 *
 * Unlike `detectVerticalDragGestures` (which *consumes* the drag and therefore steals
 * taps / long-press drag-reorder from its children), this observer runs on the
 * [PointerEventPass.Initial] pass **without ever consuming** the event. Children keep
 * receiving every event afterwards, so row taps, long-press drag-to-reorder and the
 * notes scroll all behave exactly as before.
 *
 * When the finger lifts after moving more than [threshold] px in the watched
 * direction, [onSwipe] fires once. `direction = -1f` watches an **upward** swipe
 * (finger travels toward the top / negative Y); `direction = +1f` watches a
 * **downward** swipe. Horizontal-dominant gestures are ignored so the drawer's own
 * pager/scroll and any horizontal swipe are untouched.
 *
 * @param key a key that restarts the observer when it changes (e.g. the current
 *   screen state), matching `pointerInput`'s semantics.
 */
fun Modifier.verticalSwipe(
    key: Any?,
    direction: Float,
    threshold: Float = 60f,
    ignoreStart: (Offset) -> Boolean = { false },
    onSwipe: () -> Unit,
): Modifier = composed {
    val currentOnSwipe = androidx.compose.runtime.rememberUpdatedState(onSwipe)
    val currentIgnoreStart = androidx.compose.runtime.rememberUpdatedState(ignoreStart)
    this.pointerInput(key, direction, threshold) {
        val thresholdPx = threshold.dp.toPx()
        val swipe = { currentOnSwipe.value() }
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            if (currentIgnoreStart.value(down.position)) return@awaitEachGesture
            var accumulated = 0f
            var horizontal = 0f
            var lifted = false
            var claimed = false
            while (!lifted) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (change.positionChanged()) {
                    val delta = change.position - change.previousPosition
                    accumulated += delta.y
                    horizontal += abs(delta.x)
                    // Once the gesture is clearly a vertical swipe, claim this
                    // pointer stream so child dragSource/clickable modifiers cannot
                    // reinterpret the same movement as a tap on the tile underneath.
                    val wanted = if (direction < 0f) accumulated <= -thresholdPx else accumulated >= thresholdPx
                    if (!claimed && wanted && abs(accumulated) > horizontal) {
                        change.consume()
                        claimed = true
                    }
                }
                if (!change.pressed) lifted = true
            }
            // A vertical-dominant swipe past the threshold, in the watched direction.
            val wanted = if (direction < 0f) accumulated <= -thresholdPx else accumulated >= thresholdPx
            if (wanted && abs(accumulated) > horizontal) swipe()
        }
    }
}
