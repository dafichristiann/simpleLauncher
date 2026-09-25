package com.softhome.core.model

/** Pure rules for the unified, user-configurable home right rail. */
object RailConfigLogic {
    const val MAX_TOTAL_ITEMS = 8
    const val MAX_CONFIGURABLE_ITEMS = MAX_TOTAL_ITEMS - 1

    val LOCKED_SETTINGS = RailItemId.System(RailShortcutId.PanelLeft)
    val DEFAULT_ITEMS: List<RailItemId> = listOf(
        RailShortcutId.Sparkles,
        RailShortcutId.CircleDot,
        RailShortcutId.MessageCircle,
        RailShortcutId.Send,
        RailShortcutId.Camera,
        RailShortcutId.Wind,
        RailShortcutId.PanelLeft,
        RailShortcutId.Phone,
    ).map(RailItemId::System)

    fun sanitize(raw: List<RailItemId>?): List<RailItemId> {
        val source = raw.orEmpty()
        val seen = LinkedHashSet<RailItemId>()
        var settingsIndex: Int? = null
        var parsedAny = false
        source.forEach { item ->
            if (item == LOCKED_SETTINGS) {
                parsedAny = true
                if (settingsIndex == null) settingsIndex = seen.size
                return@forEach
            }
            if (item is RailItemId.App && item.componentKey.isBlank()) return@forEach
            parsedAny = true
            seen += item
        }
        val configurable = seen.take(MAX_CONFIGURABLE_ITEMS).toMutableList()
        val index = (settingsIndex ?: configurable.size).coerceIn(0, configurable.size)
        configurable.add(index, LOCKED_SETTINGS)
        return if (configurable.size == 1 && !parsedAny) DEFAULT_ITEMS
        else configurable
    }

    fun fromLegacyShortcutNames(raw: List<String>?): List<RailItemId> =
        RailOrderLogic.sanitize(raw).mapNotNull { name ->
            RailShortcutId.entries.firstOrNull { it.name == name }?.let(RailItemId::System)
        }.let(::sanitize)

    fun add(raw: List<RailItemId>?, item: RailItemId): List<RailItemId> {
        if (item == LOCKED_SETTINGS || sanitize(raw).contains(item)) return sanitize(raw)
        val current = sanitize(raw)
        if (current.count { it != LOCKED_SETTINGS } >= MAX_CONFIGURABLE_ITEMS) return current
        return sanitize(current + item)
    }

    fun remove(raw: List<RailItemId>?, item: RailItemId): List<RailItemId> =
        if (item == LOCKED_SETTINGS) sanitize(raw) else sanitize(raw).filterNot { it == item }

    fun move(raw: List<RailItemId>?, item: RailItemId, targetIndex: Int): List<RailItemId> {
        val current = sanitize(raw)
        if (item !in current) return current
        val without = current - item
        return without.toMutableList().apply {
            add(targetIndex.coerceIn(0, size), item)
        }.let(::sanitize)
    }
}
