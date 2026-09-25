package com.softhome.core.designsystem.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.softhome.core.designsystem.theme.Dimens
import com.softhome.core.designsystem.theme.MotionTokens
import com.softhome.core.designsystem.theme.TileShadow
import com.softhome.core.designsystem.theme.softColors
import com.softhome.core.designsystem.theme.softShadow
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt

/**
 * P4a drag-and-drop engine (shared by the home row reorder **and** the drawer
 * app->folder drop). One engine, two thin adapters -- see the P4a spec section 2.
 *
 * This file is Compose-only and **payload-agnostic**: it keys a drag by an opaque
 * [String] id (a `HomeRowKind` name, or an app `componentKey`). The pure, unit-tested
 * decision logic lives in `core:model` (`HomeRowDropResolver` / `FolderDropResolver`);
 * this layer only tracks the pointer, decides hover, and calls back.
 */

/** A registered drop target's window-space bounds + id. */
data class DropTargetBounds(val id: String, val bounds: Rect)

/** Immutable drag runtime state. [deltaPx] is pointer movement since the lift. */
data class DragUiState(
    val draggingId: String? = null,
    val originWindowPx: Offset = Offset.Zero,
    val pointerWindowPx: Offset = Offset.Zero,
    val hoveredTargetId: String? = null,
    /** True once the pointer has moved past the long-press threshold (a real drag). */
    val didMove: Boolean = false,
) {
    val isDragging: Boolean get() = draggingId != null

    /** Where to draw the preview: the finger, offset by the grab point. */
    val previewOffsetPx: Offset get() = pointerWindowPx - originWindowPx
}

/**
 * Per-surface drag controller. A home screen / drawer owns one; drags never cross
 * surfaces. Hover is resolved by hit-testing registered [DropTargetBounds] against the
 * live pointer position, so a target only needs `Modifier.dropTarget(id, controller)`.
 */
class DragController internal constructor() {
    /** Publicly readable; mutated only through the modifiers in this file. */
    var state by mutableStateOf(DragUiState())
        internal set

    private val targets = mutableMapOf<String, Rect>()
    
    // P7.2: throttle drag state updates to 16ms (1 frame at 60 FPS) to reduce recomposition storm
    private var lastUpdateTimeMs = 0L
    private val throttleIntervalMs = 16L

    internal fun registerTarget(id: String, bounds: Rect) {
        targets[id] = bounds
    }

    internal fun unregisterTarget(id: String) {
        targets.remove(id)
    }

    internal fun begin(id: String, originWindow: Offset) {
        state = DragUiState(draggingId = id, originWindowPx = originWindow, pointerWindowPx = originWindow)
        lastUpdateTimeMs = System.currentTimeMillis()
    }

    internal fun drag(newPointerWindow: Offset) {
        // P7.2: throttle to 16ms to avoid recomposition storm during scroll/swipe
        val currentTimeMs = System.currentTimeMillis()
        if (currentTimeMs - lastUpdateTimeMs < throttleIntervalMs) {
            return  // Skip this update; pointer moved too soon after last update
        }
        lastUpdateTimeMs = currentTimeMs
        
        val hovered = targets.entries.firstOrNull { (_, r) -> r.contains(newPointerWindow) }?.key
        val moved = state.didMove || (newPointerWindow - state.originWindowPx).getDistance() > 8f
        state = state.copy(pointerWindowPx = newPointerWindow, hoveredTargetId = hovered, didMove = moved)
    }

    internal fun end() {
        state = DragUiState()
    }
}

@Composable
fun rememberDragController(): DragController = remember { DragController() }

/** Provides the active [DragController] so descendants can register themselves. */
val LocalDragController = staticCompositionLocalOf<DragController?> { null }

/**
 * Registers this node as a **drag source**. Long-press-lifts (haptic) then tracks the
 * pointer; [onDrop] fires with the hovered drop target id (or null) when the finger
 * lifts. A null target = cancel (snap back).
 *
 * This modifier owns the whole gesture for the node (do **not** also add a
 * `clickable`/`combinedClickable`, which would fight for the press):
 *  - long-press with **movement** -> drag, [onDrop] on release;
 *  - long-press with **no movement** -> [onLongPress] (e.g. open a context menu);
 *  - a **normal tap** (no long-press) -> [onTap] (e.g. launch the app).
 */
fun Modifier.dragSource(
    id: String,
    controller: DragController,
    onTap: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    onDrop: (targetId: String?, pointerWindowPx: Offset) -> Unit,
): Modifier = composed {
    val haptics = LocalHapticFeedback.current
    var sourceWindow by remember { mutableStateOf(Offset.Zero) }
    val currentOnDrop by rememberUpdatedState(onDrop)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    val currentOnTap by rememberUpdatedState(onTap)

    this
        .onGloballyPositioned { sourceWindow = it.boundsInWindow().topLeft }
        .pointerInput(id, controller) {
            awaitPointerEventScope {
                while (true) {
                    // Wait for a press, then distinguish a real tap, pre-long-press
                    // movement, and a long-press. `awaitLongPressOrCancellation` treats
                    // both movement and release as cancellation, which used to make the
                    // first few pixels of a grid scroll launch the app.
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val press = PressInteraction.Press(down.position)
                    interactionSource?.tryEmit(press)
                    val releasedBeforeLongPress = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: continue
                            // A scroll parent or the pointer itself moved past slop. This
                            // is neither a tap nor a drag yet; let the parent consume it.
                            // Check this before UP: Android can deliver the final UP with
                            // the accumulated position change, and treating that event as
                            // a tap re-opens the old "swipe launches the tile" regression.
                            val movedPastSlop =
                                (change.position - down.position).getDistance() > viewConfiguration.touchSlop
                            if (change.isConsumed || movedPastSlop) {
                                return@withTimeoutOrNull false
                            }
                            if (change.changedToUpIgnoreConsumed()) return@withTimeoutOrNull true
                        }
                    }
                    if (releasedBeforeLongPress == true) {
                        // Released before long-press without movement -> a tap.
                        interactionSource?.tryEmit(PressInteraction.Release(press))
                        currentOnTap?.invoke()
                        continue
                    }
                    if (releasedBeforeLongPress == false) {
                        // Movement before long-press -> cancel completely. In particular,
                        // never launch an app as a side effect of starting a scroll.
                        interactionSource?.tryEmit(PressInteraction.Cancel(press))
                        continue
                    }
                    // Timeout elapsed while the pointer stayed within touch slop: a real
                    // long-press. Track drag movement until release.
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    controller.begin(id, down.position + sourceWindow)
                    var pointer = down.position
                    while (true) {
                        val event = awaitPointerEvent()
                        val ch = event.changes.firstOrNull { it.id == down.id } ?: continue
                        if (ch.changedToUpIgnoreConsumed()) {
                            val didMove = controller.state.didMove
                            val finalPointer = controller.state.pointerWindowPx
                            val hovered = controller.state.hoveredTargetId
                            controller.end()
                            interactionSource?.tryEmit(PressInteraction.Release(press))
                            if (didMove) currentOnDrop(hovered, finalPointer)
                            else currentOnLongPress?.invoke()
                            break
                        }
                        if (ch.positionChanged()) {
                            ch.consume()
                            pointer = ch.position
                            controller.drag(pointer + sourceWindow)
                        }
                    }
                }
            }
        }
}

/**
 * Registers this node as a **drop target** with [targetId]. Its window-space bounds are
 * reported to [controller] so hover can be hit-tested during a drag. The registration is
 * dropped when the node leaves composition.
 */
fun Modifier.dropTarget(
    targetId: String,
    controller: DragController,
): Modifier = composed {
    androidx.compose.runtime.DisposableEffect(targetId, controller) {
        onDispose { controller.unregisterTarget(targetId) }
    }
    this.onGloballyPositioned { controller.registerTarget(targetId, it.boundsInWindow()) }
}

/**
 * The floating preview layer: renders [content] at the drag position while a drag is
 * active. Place once per surface, as a direct child of the surface's root `Box`.
 *
 * @param windowToLocal converts a window-space pointer into this layer's coordinate
 *   space (pass `{ it - rootInWindow }` from the surface).
 */
@Composable
fun BoxScope.DragPreviewLayer(
    controller: DragController,
    windowOrigin: Offset,
    content: @Composable (draggingId: String) -> Unit,
) {
    val state = controller.state
    val id = state.draggingId ?: return
    val liftScale by animateFloatAsState(
        targetValue = Dimens.dragLiftScale,
        animationSpec = MotionTokens.dragLift(),
        label = "dragLiftScale",
    )
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (state.pointerWindowPx.x - windowOrigin.x).roundToInt(),
                    (state.pointerWindowPx.y - windowOrigin.y).roundToInt(),
                )
            }
            .scale(liftScale)
            .softShadow(TileShadow),
    ) {
        content(id)
    }
}

/**
 * A 3px accent insertion line (row reorder). `accent` (#8A5F43) across the row width.
 */
@Composable
fun DragInsertionLine(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.dragInsertionThickness)
            .background(MaterialTheme.softColors.accent),
    )
}

/**
 * The drag preview for a **home row**: a compact cream label pill (a full-height row
 * would be unwieldy under the finger). See the spec, assumption K4.
 */
@Composable
fun DragRowChip(label: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.softColors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Dimens.dragRowChipRadius))
            .background(colors.tileWarm)
            .padding(horizontal = Dimens.dragRowChipPaddingX, vertical = Dimens.dragRowChipPaddingY),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
    }
}

/** Dims the in-place original while it is being dragged (spec: ~35% alpha). */
fun Modifier.dragSourceAlpha(isDragging: Boolean): Modifier =
    if (isDragging) this.alpha(Dimens.dragSourceAlpha) else this

/** Accent ring + slight scale on a hovered drop target (a folder tile). */
@Composable
fun DragHoverRing(
    hovered: Boolean,
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = MaterialTheme.softColors
    Box(
        modifier = modifier
            .scale(if (hovered) Dimens.dragHoverScale else 1f)
            .clip(shape),
        contentAlignment = Alignment.Center,
    ) {
        content()
        if (hovered) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape)
                    .background(colors.accent.copy(alpha = 0.18f)),
            )
        }
    }
}
