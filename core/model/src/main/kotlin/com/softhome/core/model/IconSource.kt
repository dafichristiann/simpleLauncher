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

    /**
     * P4b: a **user-chosen** glyph in a **user-chosen** color (non-pack apps). Produced
     * by an [IconOverride.Glyph]; the drawer renders [symbolName] tinted with the
     * resolved [colorToken] instead of the category-derived color.
     */
    data class Glyph(val symbolName: String, val colorToken: DrawerIconTokenName) : IconSource
}

/**
 * The fully-resolved render intent for one tile, produced by the ViewModel and
 * consumed by [com.softhome.feature.iconpack.ui.AppIcon] (P1.5) and
 * [com.softhome.feature.iconpack.ui.DrawerAppIcon] (P3.5/P4b).
 *
 * [drawableName] is set only for [IconSource.Override]/[IconSource.FromPack]/[IconSource.Glyph]
 * when a pack drawable is chosen; the UI decodes it from the *active* pack.
 * [symbolName] is the fallback glyph, and (P4b) the chosen glyph when
 * [source] is [IconSource.Glyph]. [overrideColorToken] is set only for a P4b glyph
 * override, so the drawer tints the glyph with the user's chosen color instead of the
 * category-derived one.
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
    /** P4b: a user-chosen glyph color token (non-pack override), or null for the
     *  category-derived color. */
    val overrideColorToken: DrawerIconTokenName? = null,
)
