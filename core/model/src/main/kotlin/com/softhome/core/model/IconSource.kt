package com.softhome.core.model

/**
 * How an app's icon should be resolved for display. Ordered, short-circuiting
 * pipeline per docs/08-ICONPACK-FORMAT.md ?2:
 *   user override ? icon pack match ? auto-mask ? system fallback.
 */
sealed interface IconSource {
    /** User chose a specific icon for this app. */
    data class Override(val packId: String, val drawableName: String) : IconSource

    /** Active icon pack has an entry for this app. */
    data class FromPack(val packId: String, val drawableName: String) : IconSource

    /** Render the app's real icon inside the SOFT squircle (auto-mask). */
    data object AutoMask : IconSource

    /** Use the raw system icon (last resort). */
    data object System : IconSource
}

/**
 * The fully-resolved render intent for one tile, produced by the ViewModel and
 * consumed by [com.softhome.feature.iconpack.ui.AppIcon] (P1.5).
 *
 * [drawableName] is set only for [IconSource.Override]/[IconSource.FromPack]; the UI
 * decodes it from the *active* pack. [symbolName] is the fallback glyph used only when
 * a drawable is missing (so a corrupt entry degrades to a mask/glyph, never a blank).
 */
data class ResolvedIcon(
    val source: IconSource,
    /** Pack drawable to decode, when [source] is a pack/override match. */
    val drawableName: String? = null,
    /** Category glyph fallback (mask) - used when no drawable can be decoded. */
    val symbolName: String = "AppWindow",
    /** Component key to fetch the real system icon for auto-mask/system. */
    val componentKey: String,
    val packageName: String,
    val className: String,
)
