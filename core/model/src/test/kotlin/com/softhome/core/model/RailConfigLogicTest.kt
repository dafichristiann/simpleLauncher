package com.softhome.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RailConfigLogicTest {
    @Test
    fun `default has seven configurable entries and locked settings`() {
        val items = RailConfigLogic.sanitize(emptyList())
        assertThat(items).hasSize(RailConfigLogic.MAX_TOTAL_ITEMS)
        assertThat(items.count { it != RailConfigLogic.LOCKED_SETTINGS })
            .isEqualTo(RailConfigLogic.MAX_CONFIGURABLE_ITEMS)
        assertThat(items.count { it == RailConfigLogic.LOCKED_SETTINGS }).isEqualTo(1)
    }

    @Test
    fun `legacy order maps to stable system ids`() {
        val items = RailConfigLogic.fromLegacyShortcutNames(listOf("Phone", "Camera"))
        assertThat(items.first()).isEqualTo(RailItemId.System(RailShortcutId.Phone))
        assertThat(items).contains(RailConfigLogic.LOCKED_SETTINGS)
    }

    @Test
    fun `settings cannot be removed and can be restored when missing`() {
        val onlySettings = RailConfigLogic.remove(
            listOf(RailConfigLogic.LOCKED_SETTINGS),
            RailConfigLogic.LOCKED_SETTINGS,
        )
        assertThat(onlySettings).containsExactly(RailConfigLogic.LOCKED_SETTINGS)

        val repaired = RailConfigLogic.sanitize(
            listOf(RailItemId.App("com.example/Main")),
        )
        assertThat(repaired).contains(RailConfigLogic.LOCKED_SETTINGS)
    }

    @Test
    fun `add caps configurable entries and rejects duplicates`() {
        val app = RailItemId.App("com.example/Main")
        val once = RailConfigLogic.add(RailConfigLogic.DEFAULT_ITEMS, app)
        assertThat(once).isEqualTo(RailConfigLogic.DEFAULT_ITEMS)

        val reduced = RailConfigLogic.remove(RailConfigLogic.DEFAULT_ITEMS, RailItemId.System(RailShortcutId.Phone))
        val added = RailConfigLogic.add(reduced, app)
        assertThat(added.count { it != RailConfigLogic.LOCKED_SETTINGS })
            .isEqualTo(RailConfigLogic.MAX_CONFIGURABLE_ITEMS)
        assertThat(RailConfigLogic.add(added, app)).isEqualTo(added)
    }

    @Test
    fun `codec round trips app and system ids`() {
        val values = listOf(
            RailItemId.App("com.example/Main"),
            RailItemId.System(RailShortcutId.Camera),
        )
        assertThat(RailItemIdCodec.decode(RailItemIdCodec.encode(values))).isEqualTo(values)
    }
}
