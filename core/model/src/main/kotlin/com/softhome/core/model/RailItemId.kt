package com.softhome.core.model

/** Stable identities for entries shown in the home right rail. */
enum class RailShortcutId {
    Sparkles,
    CircleDot,
    MessageCircle,
    Send,
    Camera,
    Wind,
    PanelLeft,
    Phone,
}

sealed interface RailItemId {
    val storageId: String

    data class System(val shortcut: RailShortcutId) : RailItemId {
        override val storageId: String = "system:${shortcut.name}"
    }

    data class App(val componentKey: String) : RailItemId {
        override val storageId: String = "app:$componentKey"
    }
}

object RailItemIdCodec {
    fun parse(raw: String?): RailItemId? {
        if (raw.isNullOrBlank()) return null
        val separator = raw.indexOf(':')
        if (separator <= 0 || separator == raw.lastIndex) return null
        return when (raw.substring(0, separator)) {
            "system" -> RailShortcutId.entries
                .firstOrNull { it.name == raw.substring(separator + 1) }
                ?.let(RailItemId::System)
            "app" -> raw.substring(separator + 1).trim()
                .takeIf { it.isNotBlank() }
                ?.let(RailItemId::App)
            else -> null
        }
    }

    fun encode(items: List<RailItemId>): String = items.joinToString(",") { it.storageId }

    fun decode(raw: String?): List<RailItemId> = raw.orEmpty()
        .split(',')
        .mapNotNull(::parse)
}
