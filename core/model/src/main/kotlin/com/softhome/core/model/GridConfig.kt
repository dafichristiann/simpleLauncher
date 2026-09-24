package com.softhome.core.model

/**
 * Home grid configuration.
 * Design default (from .pen ECmUn): 4 columns. The Nova note in the design
 * ("5x6, icon 110%, labels off") is offered as a preset, not the hard default --
 * see docs/04-ASSUMPTIONS.md gap #1.
 */
data class GridConfig(
    val columns: Int = 4,
    val rows: Int = 5,
    val iconScale: Float = 1.10f,
    val showLabels: Boolean = false,
) {
    init {
        require(columns in 3..7) { "columns must be 3..7, was $columns" }
        require(rows in 3..8) { "rows must be 3..8, was $rows" }
        require(iconScale in 0.7f..1.5f) { "iconScale must be 0.7..1.5, was $iconScale" }
    }

    companion object {
        val Default = GridConfig()
        /** The Nova-style preset named in the design guide. */
        val NovaPreset = GridConfig(columns = 5, rows = 6, iconScale = 1.10f, showLabels = false)
    }
}
