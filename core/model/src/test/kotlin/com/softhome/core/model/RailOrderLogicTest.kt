package com.softhome.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RailOrderLogicTest {
    @Test
    fun sanitize_repairs_unknown_duplicate_and_missing_entries() {
        val result = RailOrderLogic.sanitize(listOf("Camera", "Camera", "unknown"))
        assertThat(result).containsExactlyElementsIn(
            listOf("Camera") + RailOrderLogic.DEFAULT.filter { it != "Camera" },
        ).inOrder()
    }

    @Test
    fun move_inserts_item_at_clamped_position() {
        val result = RailOrderLogic.move(RailOrderLogic.DEFAULT, "Camera", 1)
        assertThat(result).containsExactly(
            "Sparkles", "Camera", "CircleDot", "MessageCircle", "Send", "Wind", "PanelLeft", "Phone",
        ).inOrder()
    }

    @Test
    fun move_supports_first_and_last_positions() {
        assertThat(RailOrderLogic.move(RailOrderLogic.DEFAULT, "Phone", 0).first())
            .isEqualTo("Phone")
        assertThat(RailOrderLogic.move(RailOrderLogic.DEFAULT, "Sparkles", Int.MAX_VALUE).last())
            .isEqualTo("Sparkles")
    }

    @Test
    fun empty_storage_falls_back_to_design_order() {
        assertThat(RailOrderLogic.sanitize(null)).containsExactlyElementsIn(RailOrderLogic.DEFAULT).inOrder()
    }
}
