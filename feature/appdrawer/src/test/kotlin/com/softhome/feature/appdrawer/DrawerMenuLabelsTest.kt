package com.softhome.feature.appdrawer

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * P3 (F3): the drawer app long-press menu exposes the expected rows, in order.
 * The interactive behaviour (menu opens on long-press, Uninstall greyed for system
 * apps) is covered by AppContextMenuTest + the Phase 7 device screenshots.
 */
class DrawerMenuLabelsTest {

    @Test
    fun `menu rows are in the designed order`() {
        assertThat(DrawerMenuLabels.ALL).containsExactly(
            "Open", "App Info", "Edit Icon", "Remove", "Uninstall", "Shortcuts",
        ).inOrder()
    }
}
