package com.softhome.core.model

/**
 * Quick notes (P2 / E5).
 *
 * Decision P2-2: the notes body is **user data** persisted in DataStore (survives
 * process death / restart), unlike the static music row which is a UI placeholder.
 */
data class Notes(
    val body: String = "",
) {
    /** One-line preview for the collapsed home row ("Nothing yet" when blank). */
    val preview: String
        get() = body.replace('\n', ' ').trim().ifBlank { NOTHING_YET }

    companion object {
        const val NOTHING_YET = "Nothing yet"
        const val FIELD_KEY = "quick_notes"
    }
}
