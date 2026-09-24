package com.softhome.core.model

/**
 * Home row visibility + order model (P3 / G - Widgets section).
 *
 * After the Warm Right Rail redesign the home is a vertical list of full-width rows.
 * P3 lets the user choose **which rows show** and **in what order**, with a locked
 * minimum set so the home never renders empty.
 *
 * Locked decision P3-1: Time / Date / Weather are always visible. Everything else is
 * toggleable.
 */
enum class HomeRowKind {
    Time,
    Date,
    Weather,
    Search,
    Music,
    Calendar,
    BatteryStorage,
    Notes,
}

/** One home row's persisted preference: which row, and whether it is visible. */
data class HomeRowPref(
    val kind: HomeRowKind,
    val visible: Boolean,
)

/**
 * Pure logic for the home row list: defaults, the locked set, toggling, and reorder.
 * No Android dependency so it is fully unit-testable.
 */
object HomeRowLogic {

    /** Rows that can never be hidden (P3-1) -- guarantees a non-empty home. */
    val LOCKED: Set<HomeRowKind> = setOf(
        HomeRowKind.Time,
        HomeRowKind.Date,
        HomeRowKind.Weather,
    )

    /** The design's default row order (docs/03 E1-E5 + the redesign). */
    val DEFAULT_ORDER: List<HomeRowKind> = listOf(
        HomeRowKind.Time,
        HomeRowKind.Date,
        HomeRowKind.Weather,
        HomeRowKind.Search,
        HomeRowKind.Music,
        HomeRowKind.Calendar,
        HomeRowKind.BatteryStorage,
        HomeRowKind.Notes,
    )

    /** All rows visible, in the default order. */
    fun default(): List<HomeRowPref> =
        DEFAULT_ORDER.map { HomeRowPref(it, visible = true) }

    /** Whether [kind] may be hidden by the user. */
    fun canHide(kind: HomeRowKind): Boolean = kind !in LOCKED

    /**
     * Toggle a row's visibility. Locked rows are never hidden: toggling a locked row
     * is a no-op (it stays visible). Unknown kinds are ignored.
     */
    fun toggle(prefs: List<HomeRowPref>, kind: HomeRowKind): List<HomeRowPref> {
        if (!canHide(kind)) return prefs
        return prefs.map { if (it.kind == kind) it.copy(visible = !it.visible) else it }
    }

    /** Move [kind] one step earlier; clamped at the top. No-op if unknown. */
    fun moveUp(prefs: List<HomeRowPref>, kind: HomeRowKind): List<HomeRowPref> {
        val i = prefs.indexOfFirst { it.kind == kind }
        if (i <= 0) return prefs
        return prefs.toMutableList().apply { add(i - 1, removeAt(i)) }
    }

    /** Move [kind] one step later; clamped at the bottom. No-op if unknown. */
    fun moveDown(prefs: List<HomeRowPref>, kind: HomeRowKind): List<HomeRowPref> {
        val i = prefs.indexOfFirst { it.kind == kind }
        if (i < 0 || i >= prefs.size - 1) return prefs
        return prefs.toMutableList().apply { add(i + 1, removeAt(i)) }
    }

    /**
     * Reorder [kind] to [targetIndex] (P4a drag-and-drop). [targetIndex] is a position
     * in the **resulting** list (0..size-1); it is clamped, so dragging past the ends is
     * safe. No-op when the kind is unknown or already at [targetIndex]. The move keeps
     * visibility intact (locked rows stay visible; order is never locked — P4a-5).
     */
    fun move(prefs: List<HomeRowPref>, kind: HomeRowKind, targetIndex: Int): List<HomeRowPref> {
        val from = prefs.indexOfFirst { it.kind == kind }
        if (from < 0) return prefs
        val to = targetIndex.coerceIn(0, prefs.size - 1)
        if (from == to) return prefs
        val list = prefs.toMutableList()
        val moved = list.removeAt(from)
        list.add(to.coerceIn(0, list.size), moved)
        return list
    }

    /** The rows the home screen should render, in order, honoring visibility. */
    fun visibleInOrder(prefs: List<HomeRowPref>): List<HomeRowKind> =
        prefs.filter { it.visible || it.kind in LOCKED }.map { it.kind }

    /**
     * Repair a stored list so it is always usable:
     *  - unknown kinds are dropped,
     *  - missing kinds are appended (in default order),
     *  - locked rows are forced visible,
     *  - a blank/absent list falls back to [default].
     */
    fun sanitize(stored: List<HomeRowPref>?): List<HomeRowPref> {
        if (stored.isNullOrEmpty()) return default()
        val seen = LinkedHashSet<HomeRowKind>()
        val cleaned = ArrayList<HomeRowPref>(stored.size)
        for (p in stored) {
            if (p.kind in DEFAULT_ORDER && seen.add(p.kind)) {
                cleaned += p.copy(visible = p.visible || p.kind in LOCKED)
            }
        }
        // Append any known kinds the stored list was missing (default: visible).
        DEFAULT_ORDER.filterNot { it in seen }.forEach { cleaned += HomeRowPref(it, visible = true) }
        return cleaned.ifEmpty { default() }
    }
}
