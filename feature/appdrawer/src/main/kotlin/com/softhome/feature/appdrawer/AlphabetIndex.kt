package com.softhome.feature.appdrawer

/**
 * Alphabet index bucketing. Design node bxlfq shows A C E G M P S W # at 11sp #6E675E.
 * We compute the ordered set of present initial letters plus '#' for non-letters.
 */
object AlphabetIndex {

    private const val OTHER = '#'

    /** Initial letter bucket for a label: A-Z, or '#' for anything else. */
    fun bucketOf(label: String): Char {
        val c = label.trim().firstOrNull()?.uppercaseChar() ?: return OTHER
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
        val idx = labels.indexOfFirst { bucketOf(it) == bucket }
        return if (idx < 0) 0 else idx
    }
}
