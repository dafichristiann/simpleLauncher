package com.softhome.core.model

/**
 * Pure ordering and recovery rules for the home right rail.
 *
 * The persisted representation intentionally uses stable enum names so adding a
 * new rail action never invalidates an existing user's order. Unknown, duplicate,
 * or missing entries are repaired by [sanitize] and the original design order is
 * used as the fallback.
 */
object RailOrderLogic {
    val DEFAULT: List<String> = listOf(
        "Sparkles",
        "CircleDot",
        "MessageCircle",
        "Send",
        "Camera",
        "Wind",
        "PanelLeft",
        "Phone",
    )

    fun sanitize(raw: List<String>?): List<String> {
        val source = raw.orEmpty()
        val known = DEFAULT.toSet()
        val result = LinkedHashSet<String>(DEFAULT.size)
        source.forEach { name ->
            if (name in known) result += name
        }
        DEFAULT.forEach { name -> result += name }
        return result.toList()
    }

    /** [targetIndex] is the insertion position in the resulting list. */
    fun move(raw: List<String>?, item: String, targetIndex: Int): List<String> {
        val current = sanitize(raw)
        if (item !in current) return current
        val without = current - item
        val destination = targetIndex.coerceIn(0, without.size)
        return without.toMutableList().apply { add(destination, item) }
    }
}
