package com.softhome.feature.home

/**
 * The home interaction states (design/homeApp.pen board `L7ZAp`, storyboard cards
 * `Az7qs` / `m9OlxQ` / `T8AA1`), plus the P2 `Notes` state:
 *
 *  - [Idle]   "Home at rest"  -- clock + weather visible, rail minimal.
 *  - [Search] "rail expands"  -- search row focused; rail slides open.
 *  - [Notes]  (P2 / E5)       -- quick-notes row grows in-place into an editor.
 *
 * Triggered by tapping the corresponding row; tapping elsewhere / Back returns
 * to [Idle].
 */
enum class HomeState {
    Idle,
    Search,
    Notes;

    /** Whether the search row should show its focused affordance. */
    val isSearching: Boolean get() = this == Search

    /** Whether the notes row should be expanded into its editor (P2). */
    val isNotesOpen: Boolean get() = this == Notes

    /**
     * Pure transition: tapping a row selects its state, or returns to Idle when
     * the same row is tapped again.
     */
    fun onTapRow(row: HomeRowId): HomeState {
        val target = when (row) {
            HomeRowId.Search -> Search
            HomeRowId.Notes -> Notes
            else -> Idle
        }
        return if (this == target) Idle else target
    }

    fun reset(): HomeState = Idle
}

/** Identifiable home rows that participate in state changes. */
enum class HomeRowId {
    Time, Date, Weather, Search, Calendar, BatteryStorage, Notes,
}

