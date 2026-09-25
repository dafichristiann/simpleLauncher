package com.softhome.feature.appdrawer

import java.util.Locale

/**
 * Alphabet index bucketing. Design node bxlfq shows A C E G M P S W # at 11sp #6E675E.
 * We compute the ordered set of present initial letters plus '#' for non-letters.
 */
object AlphabetIndex {

    private const val OTHER = '#'

    /** The compact rail mirrors the launcher reference and always shows every bucket. */
    val allBuckets: List<Char> = ('A'..'Z').toList() + OTHER

    /** Initial letter bucket for a label: A-Z, or '#' for anything else. */
    fun bucketOf(label: String): Char {
        val c = label.trim().firstOrNull()
            ?.toString()
            ?.uppercase(Locale.ROOT)
            ?.firstOrNull()
            ?: return OTHER
        return if (c in 'A'..'Z') c else OTHER
    }

    /** Ordered present buckets: A..Z then '#'. */
    fun lettersPresentIn(labels: List<String>): List<Char> {
        val present = labels.map(::bucketOf).toSet()
        val letters = ('A'..'Z').filter { it in present }
        return if (OTHER in present) letters + OTHER else letters
    }

    /** Insertion index for a bucket, used to scroll the grid to a letter. */
    fun firstIndexFor(labels: List<String>, bucket: Char): Int {
        val wanted = bucket.toString().uppercase(Locale.ROOT).firstOrNull() ?: OTHER
        val idx = labels.indexOfFirst { bucketOf(it) == wanted }
        return if (idx < 0) 0 else idx
    }

    /**
     * Find the first index in the actual rendered grid. Folder cells remain part of the
     * index because they occupy real LazyGrid positions, while their member apps do not
     * create standalone alphabet buckets.
     */
    fun firstRenderedIndexFor(cells: List<DrawerCell>, bucket: Char): Int {
        val wanted = bucket.toString().uppercase(Locale.ROOT).firstOrNull() ?: OTHER
        val candidates = cells.mapIndexedNotNull { index, cell ->
            (cell as? DrawerCell.AppEntry)?.let { index to bucketOf(it.entry.app.label) }
        }
        val exact = candidates.firstOrNull { (_, bucketValue) -> bucketValue == wanted }
        if (exact != null) return exact.first

        // Empty buckets remain safe: jump to the closest rendered bucket in label order
        // instead of always jumping to item 0 (which was the visible alphabet bug).
        val wantedRank = bucketRank(wanted)
        return candidates.firstOrNull { (_, bucketValue) -> bucketRank(bucketValue) > wantedRank }
            ?.first
            ?: candidates.lastOrNull()?.first
            ?: 0
    }

    private fun bucketRank(bucket: Char): Int = when (bucket) {
        in 'A'..'Z' -> bucket - 'A'
        else -> allBuckets.lastIndex
    }

    /** LazyGrid target for a bucket: align to the row containing its first app. */
    fun firstRenderedRowStartFor(
        cells: List<DrawerCell>,
        bucket: Char,
        columns: Int,
    ): Int {
        val itemIndex = firstRenderedIndexFor(cells, bucket)
        val safeColumns = columns.coerceAtLeast(1)
        return (itemIndex / safeColumns) * safeColumns
    }
}
