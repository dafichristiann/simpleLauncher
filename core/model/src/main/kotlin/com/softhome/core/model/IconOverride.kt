package com.softhome.core.model

/**
 * P4b: a user's explicit icon choice for one app, stored per `componentKey`.
 *
 * Absence from [LauncherPrefs.iconOverrides] means **automatic** -- the P3.5 hybrid
 * renderer decides (a real pack drawable if the active pack maps this app, otherwise a
 * category-colored lucide glyph). An override only exists when the user opened the
 * "Edit Icon" editor and saved a choice.
 *
 * This replaces the earlier `Map<componentKey, packId>` shape, which could not express
 * *which* drawable/glyph/color was chosen (the resolver re-derived the pack's default
 * entry, so the override was visually a no-op). See the P4b spec section 1.
 */
sealed interface IconOverride {

    /**
     * Use a specific drawable from the **active** pack (P4b-7). [drawableName] is the
     * pack resource name; the renderer decodes it through `IconPackDrawableLoader`.
     */
    data class Pack(val drawableName: String) : IconOverride

    /**
     * Use a specific lucide [symbolName] tinted with [colorToken] (P4b-8). This is the
     * depth offered for an app that is **not** in the active pack (the P3.5 hybrid's
     * non-pack branch, now user-controlled).
     */
    data class Glyph(val symbolName: String, val colorToken: DrawerIconTokenName) : IconOverride
}

/**
 * The drawer glyph color slot, named in `core:model` so a persisted override never
 * depends on the `feature:iconpack` module. Mirrors `DrawerIconColor.Token` 1:1 (the
 * P3.5 palette from design/homeApp.pen frame `TpzL1`); the UI maps a name to a
 * light/dark `SoftColors` field.
 */
enum class DrawerIconTokenName {
    Communication,
    Social,
    Productivity,
    Media,
    Travel,
    Finance,
    Neutral,
}
