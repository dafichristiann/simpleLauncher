package com.softhome.feature.iconpack.domain

import com.softhome.core.model.AppInfo
import androidx.compose.ui.unit.Dp

/**
 * Icon masking math + category heuristics.
 *
 * Spec: docs/08-ICONPACK-FORMAT.md ?3.
 *  - squircle radius = size * 0.30 (62?19, 104?30, 512?154)
 *  - unknown apps get a generic symbol; known categories get a matching lucide glyph.
 *
 * Pure Kotlin so the math can be unit-tested without a device.
 */
object IconMasker {

    const val RADIUS_RATIO = 0.30f
    const val STROKE_WIDTH_RATIO = 0.055f   // relative to tile size, for the line glyph

    /** Corner radius of the charcoal squircle for a given tile size. */
    fun radiusFor(size: Dp): Dp = size * RADIUS_RATIO

    /** Corner radius in raw pixels (unit-testable). */
    fun radiusPx(sizePx: Float): Float = sizePx * RADIUS_RATIO

    /**
     * Choose a fallback glyph resource name for an app when it has no pack entry.
     * Order matters: more specific checks first. Returns a LineIcon enum name.
     */
    fun symbolFor(app: AppInfo): String {
        val hay = (app.packageName + " " + app.className + " " + app.label).lowercase()
        return when {
            hay.containsAny("camera", "photo", "gcam") -> "Camera"
            hay.containsAny("phone", "dialer", "call") -> "Phone"
            hay.containsAny("message", "sms", "mms", "chat", "whatsapp", "telegram") -> "MessageCircle"
            hay.containsAny("mail", "gmail", "email", "inbox") -> "Mail"
            hay.containsAny("calendar", "agenda", "planner") -> "CalendarDays"
            hay.containsAny("clock", "alarm", "timer") -> "Clock"
            hay.containsAny("map", "geo", "nav", "location") -> "MapPin"
            hay.containsAny("browser", "chrome", "firefox", "web") -> "Compass"
            hay.containsAny("music", "audio", "player", "spotify", "podcast") -> "Music"
            hay.containsAny("video", "youtube", "movie", "netflix", "player.video") -> "Play"
            hay.containsAny("gallery", "photo", "images", "media") -> "Image"
            hay.containsAny("file", "document", "drive", "storage", "explorer") -> "Folder"
            hay.containsAny("setting", "preference", "config") -> "Settings"
            hay.containsAny("calc", "math") -> "Calculator"
            hay.containsAny("weather", "forecast") -> "CloudSun"
            hay.containsAny("note", "notepad", "memo") -> "PenLine"
            hay.containsAny("contact", "people", "dialer.contact") -> "PhoneCall"
            else -> "AppWindow"
        }
    }

    private fun String.containsAny(vararg needles: String) = needles.any { this.contains(it) }
}
