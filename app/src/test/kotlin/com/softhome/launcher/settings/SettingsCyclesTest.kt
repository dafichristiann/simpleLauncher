package com.softhome.launcher.settings

import com.google.common.truth.Truth.assertThat
import com.softhome.core.model.SpacingScale
import com.softhome.core.model.ThemeMode
import org.junit.Test

/** P3 (G): the Appearance section's 3-way cycle controls. */
class SettingsCyclesTest {

    @Test
    fun `theme cycles light - dark - system`() {
        assertThat(SettingsCycles.nextTheme(ThemeMode.Light)).isEqualTo(ThemeMode.Dark)
        assertThat(SettingsCycles.nextTheme(ThemeMode.Dark)).isEqualTo(ThemeMode.System)
        assertThat(SettingsCycles.nextTheme(ThemeMode.System)).isEqualTo(ThemeMode.Light)
    }

    @Test
    fun `spacing cycles compact - normal - roomy`() {
        assertThat(SettingsCycles.nextSpacing(SpacingScale.Compact)).isEqualTo(SpacingScale.Normal)
        assertThat(SettingsCycles.nextSpacing(SpacingScale.Normal)).isEqualTo(SpacingScale.Roomy)
        assertThat(SettingsCycles.nextSpacing(SpacingScale.Roomy)).isEqualTo(SpacingScale.Compact)
    }

    @Test
    fun `columns cycle 4 5 6 and wrap`() {
        assertThat(SettingsCycles.nextColumns(4)).isEqualTo(5)
        assertThat(SettingsCycles.nextColumns(5)).isEqualTo(6)
        assertThat(SettingsCycles.nextColumns(6)).isEqualTo(4)
    }
}
