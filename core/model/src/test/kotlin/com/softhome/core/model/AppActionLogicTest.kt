package com.softhome.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppActionLogicTest {

    @Test
    fun `app info and open are always enabled`() {
        assertThat(
            AppActionLogic.isEnabled(AppActionLogic.Action.Open, isSystemApp = true, isRemovable = false),
        ).isTrue()
        assertThat(
            AppActionLogic.isEnabled(AppActionLogic.Action.AppInfo, isSystemApp = true, isRemovable = false),
        ).isTrue()
    }

    @Test
    fun `uninstall disabled for system apps`() {
        assertThat(
            AppActionLogic.isEnabled(AppActionLogic.Action.Uninstall, isSystemApp = true, isRemovable = true),
        ).isFalse()
        assertThat(
            AppActionLogic.isEnabled(AppActionLogic.Action.Uninstall, isSystemApp = false, isRemovable = true),
        ).isTrue()
    }

    @Test
    fun `uninstall disabled when not removable`() {
        assertThat(
            AppActionLogic.isEnabled(AppActionLogic.Action.Uninstall, isSystemApp = false, isRemovable = false),
        ).isFalse()
    }

    @Test
    fun `remove and edit icon require the app to be in the drawer`() {
        assertThat(
            AppActionLogic.isEnabled(AppActionLogic.Action.Remove, isSystemApp = false, isRemovable = true, inDrawer = true),
        ).isTrue()
        assertThat(
            AppActionLogic.isEnabled(AppActionLogic.Action.Remove, isSystemApp = false, isRemovable = true, inDrawer = false),
        ).isFalse()
        assertThat(
            AppActionLogic.isEnabled(AppActionLogic.Action.EditIcon, isSystemApp = false, isRemovable = true, inDrawer = false),
        ).isFalse()
    }

    @Test
    fun `shortcuts requires the app to advertise shortcuts`() {
        assertThat(
            AppActionLogic.isEnabled(AppActionLogic.Action.Shortcuts, isSystemApp = false, isRemovable = true, hasShortcuts = true),
        ).isTrue()
        assertThat(
            AppActionLogic.isEnabled(AppActionLogic.Action.Shortcuts, isSystemApp = false, isRemovable = true, hasShortcuts = false),
        ).isFalse()
    }
}
