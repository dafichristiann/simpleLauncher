package com.softhome.feature.appdrawer

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * P3 (F3): the drawer app long-press menu exposes the expected rows, in order.
 * The labels are string resources (localizable); this test locks their **order and
 * identity** without needing a Context. The interactive behaviour (menu opens on
 * long-press, Uninstall greyed for system apps) is covered by AppContextMenuTest +
 * the Phase 7 device screenshots.
 */
class DrawerMenuLabelsTest {

    @Test
    fun `menu rows are in the designed order`() {
        assertThat(DrawerMenuLabels.ALL).containsExactly(
            R.string.drawer_menu_open,
            R.string.drawer_menu_app_info,
            R.string.drawer_menu_edit_icon,
            R.string.drawer_menu_remove,
            R.string.drawer_menu_uninstall,
            R.string.drawer_menu_shortcuts,
        ).inOrder()
    }
}
