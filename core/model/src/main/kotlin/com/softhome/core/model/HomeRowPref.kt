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
        HomeRowKind.Calendar,
        HomeRowKind.BatteryStorage,
        HomeRowKind.Notes,
    )

    /**
     * P2 (2026-09-25): rows that are **hidden by default** on a fresh install, so the home
     * opens lean -- only Time -> Date -> Weather -> Search. These are NOT removed:
     * they stay reorderable/visible via Settings -> Widgets ([canHide] returns true).
     *
     * Locked rows (Time/Date/Weather) are always visible and are never in this set.
     */
    val DEFAULT_HIDDEN: Set<HomeRowKind> = setOf(
        HomeRowKind.Calendar,
        HomeRowKind.BatteryStorage,
        HomeRowKind.Notes,
    )

    /** The rows the home renders on a fresh install: the default order minus [DEFAULT_HIDDEN]. */
    fun default(): List<HomeRowPref> =
        DEFAULT_ORDER.map { HomeRowPref(it, visible = it !in DEFAULT_HIDDEN) }

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
     *  - missing kinds are appended (in default order): **hidden** if in [DEFAULT_HIDDEN],
     *    else visible (P2),
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
        // Append any known kinds the stored list was missing. P2: a missing kind that is
        // hidden-by-default (Calendar/Battery/Notes) is appended hidden, so an upgrade that
        // adds a row does not silently un-hide a row the user is meant to opt into.
        DEFAULT_ORDER.filterNot { it in seen }.forEach {
            cleaned += HomeRowPref(it, visible = it !in DEFAULT_HIDDEN)
        }
        return cleaned.ifEmpty { default() }
    }

    /**
     * P2 migration helper (pure). Existing installs persisted the **legacy all-visible
     * default** (every known kind, all visible). We can't distinguish "user deliberately
     * turned everything on" from the old default, so per the P2 decision (Q1 = apply-once)
     * this detects the exact legacy shape and maps it to the new [default] exactly once.
     *
     * @return the migrated list when [stored] is the legacy all-visible default, else null
     *   (meaning: leave the stored value untouched).
     */
    fun migrateLegacy(stored: List<HomeRowPref>?): List<HomeRowPref>? {
        if (stored.isNullOrEmpty()) return null
        // The legacy default is exactly: every known kind present, in DEFAULT_ORDER, all visible.
        if (stored.size != DEFAULT_ORDER.size) return null
        if (stored.map { it.kind } != DEFAULT_ORDER) return null
        if (stored.any { !it.visible }) return null
        return default()
    }
}
