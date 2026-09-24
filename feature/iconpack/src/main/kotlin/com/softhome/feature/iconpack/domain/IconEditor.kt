package com.softhome.feature.iconpack.domain

import com.softhome.core.model.DrawerIconTokenName
import com.softhome.core.model.IconOverride

/**
 * P4b: the pure editing model behind the "Edit Icon" editor (no Compose, no Android).
 *
 * The editor is a small state machine: it is seeded from an app's current situation
 * (its automatic glyph + color, the distinct drawables the active pack maps, and any
 * existing override) and it produces an [IconOverride] on save -- or `null` to reset.
 *
 * Keeping this pure means the "which mode / what is selected / is anything changed /
 * what gets persisted" logic is unit-tested without a device (see P4b spec §6).
 */
object IconEditor {

    /** Which picker the editor shows. */
    enum class Mode { Pack, Glyph }

    /**
     * The immutable editor state.
     *
     * @param componentKey the app being edited.
     * @param mode the active picker tab.
     * @param packDrawables the distinct drawables the active pack maps (may be empty
     *   when there is no pack); the pack picker lists these.
     * @param selectedDrawable the chosen pack drawable (Pack mode), or null.
     * @param selectedGlyph the chosen glyph (Glyph mode).
     * @param selectedColor the chosen glyph color token (Glyph mode).
     * @param initial the override that was already stored when the editor opened, used
     *   only for change-detection (the "Save enabled" gate).
     * @param automaticGlyph the app's automatic glyph (`IconMasker.symbolFor`), shown
     *   as the Glyph-mode starting point.
     */
    data class State(
        val componentKey: String,
        val mode: Mode,
        val packDrawables: List<String>,
        val selectedDrawable: String?,
        val selectedGlyph: String,
        val selectedColor: DrawerIconTokenName,
        val initial: IconOverride?,
        val automaticGlyph: String,
    ) {
        /** A pack app defaults to Pack mode; a non-pack app defaults to Glyph mode. */
        val canPickPack: Boolean get() = packDrawables.isNotEmpty()

        /** The override that would be saved for the current selection, or null to reset. */
        val currentOverride: IconOverride?
            get() = when (mode) {
                Mode.Pack -> selectedDrawable?.let { IconOverride.Pack(it) }
                Mode.Glyph -> IconOverride.Glyph(selectedGlyph, selectedColor)
            }

        /**
         * Save is enabled only when the selection differs from what was stored when the
         * editor opened (P4b spec §4.6): a no-op edit must not write.
         */
        val changed: Boolean get() = currentOverride != initial
    }

    /**
     * Seed the editor for [componentKey].
     *
     * @param packDrawables distinct drawables the active pack maps (empty when none).
     * @param automaticGlyph the app's `IconMasker.symbolFor` glyph.
     * @param automaticColor the app's category-derived color token (the P3.5 default).
     * @param existing the override already stored for this app, if any.
     */
    fun start(
        componentKey: String,
        packDrawables: List<String>,
        automaticGlyph: String,
        automaticColor: DrawerIconTokenName,
        existing: IconOverride?,
    ): State {
        // Mode: honour the type of an existing override; otherwise default by membership.
        val inPack = packDrawables.isNotEmpty()
        val mode = when {
            existing is IconOverride.Glyph -> Mode.Glyph
            existing is IconOverride.Pack -> Mode.Pack
            inPack -> Mode.Pack
            else -> Mode.Glyph
        }
        val selectedDrawable = when {
            existing is IconOverride.Pack -> existing.drawableName
            inPack -> packDrawables.first()
            else -> null
        }
        val selectedGlyph = (existing as? IconOverride.Glyph)?.symbolName ?: automaticGlyph
        val selectedColor = (existing as? IconOverride.Glyph)?.colorToken ?: automaticColor
        return State(
            componentKey = componentKey,
            mode = mode,
            packDrawables = packDrawables,
            selectedDrawable = selectedDrawable,
            selectedGlyph = selectedGlyph,
            selectedColor = selectedColor,
            initial = existing,
            automaticGlyph = automaticGlyph,
        )
    }

    fun selectMode(state: State, mode: Mode): State = state.copy(mode = mode)

    fun selectDrawable(state: State, drawable: String): State =
        state.copy(mode = Mode.Pack, selectedDrawable = drawable)

    fun selectGlyph(state: State, glyph: String): State =
        state.copy(mode = Mode.Glyph, selectedGlyph = glyph)

    fun selectColor(state: State, token: DrawerIconTokenName): State =
        state.copy(mode = Mode.Glyph, selectedColor = token)
}
