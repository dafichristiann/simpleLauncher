package com.softhome.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * P4a: pure drag **state** tests. The engine is Android-free, so hover gating, begin,
 * move and end are all covered here without a device.
 */
class DragAndDropStateTest {

    private val rowDrag = DragKind.HomeRow(HomeRowKind.Weather)

    @Test
    fun `starts idle`() {
        val s = DragAndDropState()
        assertThat(s.isDragging).isFalse()
        assertThat(s.dragging).isNull()
        assertThat(s.hoveredTargetId).isNull()
    }

    @Test
    fun `begin sets the dragged kind and clears hover`() {
        val s = DragAndDropState().begin(rowDrag, FloatPair(10f, 20f))
        assertThat(s.isDragging).isTrue()
        assertThat(s.dragging).isEqualTo(rowDrag)
        assertThat(s.originPx).isEqualTo(FloatPair(10f, 20f))
        assertThat(s.deltaPx).isEqualTo(FloatPair.Zero)
        assertThat(s.hoveredTargetId).isNull()
    }

    @Test
    fun `move records the delta`() {
        val s = DragAndDropState().begin(rowDrag, FloatPair.Zero).move(FloatPair(5f, -8f))
        assertThat(s.deltaPx).isEqualTo(FloatPair(5f, -8f))
    }

    @Test
    fun `hover sets and clears the target`() {
        val s = DragAndDropState().begin(rowDrag, FloatPair.Zero).hover("folder:1")
        assertThat(s.hoveredTargetId).isEqualTo("folder:1")
        assertThat(s.hover(null).hoveredTargetId).isNull()
    }

    @Test
    fun `end resets to idle`() {
        val s = DragAndDropState()
            .begin(rowDrag, FloatPair(1f, 1f))
            .move(FloatPair(1f, 1f))
            .hover("x")
            .end()
        assertThat(s).isEqualTo(DragAndDropState())
    }
}
