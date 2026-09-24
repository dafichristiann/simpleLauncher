package com.softhome.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GridConfigTest {

    @Test
    fun `default matches design four column home grid`() {
        assertThat(GridConfig.Default.columns).isEqualTo(4)
        assertThat(GridConfig.Default.showLabels).isFalse()
    }

    @Test
    fun `nova preset matches the design guide note`() {
        assertThat(GridConfig.NovaPreset.columns).isEqualTo(5)
        assertThat(GridConfig.NovaPreset.rows).isEqualTo(6)
    }

    @Test
    fun `rejects out of range columns`() {
        assertThrows { GridConfig(columns = 9) }
        assertThrows { GridConfig(columns = 1) }
    }

    @Test
    fun `rejects out of range icon scale`() {
        assertThrows { GridConfig(iconScale = 3f) }
        assertThrows { GridConfig(iconScale = 0.1f) }
    }

    private fun assertThrows(block: () -> Unit) {
        try {
            block(); throw AssertionError("Expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            // expected
        }
    }
}
