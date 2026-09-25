package com.softhome.feature.iconpack.domain

import com.softhome.core.model.AppCategory
import com.softhome.core.model.DrawerIconTokenName

/**
 * P3.5: the drawer icon **color** rule (pure -- no Android, no Compose).
 *
 * The drawer (design/homeApp.pen frame `TpzL1`, "Unique Icon Grid") renders a cream
 * tile with a **per-category colored** lucide glyph for apps that have no active-pack
 * artwork. This object decides *which* palette token a given app uses, so the UI and
 * the tests share one source of truth (no hex lives in the screen).
 *
 * Mapping is from the raw `ApplicationInfo.category` Int (mirrored in
 * [AppCategory]) to a [DrawerIconToken]; the UI layer resolves the token to a
 * light/dark `SoftColors` field. See the P3.5 spec section 3.2.
 *
 * Precedence (highest first):
 *  1. selected tile  -> handled by the caller (`selected = true`), not here.
 *  2. category       -> [tokenFor] table below.
 *  3. communication heuristic -> a phone/mail/calendar glyph with an unknown category
 *     still reads as "communication" rather than a grey fallback.
 *  4. neutral        -> the catch-all.
 */
object DrawerIconColor {

    /** A palette slot in the drawer icon system. */
    enum class Token {
        Communication,
        Social,
        Productivity,
        Media,
        Travel,
        Finance,
        Neutral,

        /** P4: the "Browser & Search" slot (the .pen tints Chrome with the blue). */
        Browser,
    }

    /**
     * Glyph names (from [IconMasker.symbolFor]) that read as "communication" when the
     * app carries no usable category. Matches the TpzL1 `$icon-communication` group.
     */
    private val COMMUNICATION_GLYPHS = setOf(
        "MessageCircle",
        "Phone",
        "PhoneCall",
        "Mail",
        "CalendarDays",
    )

    /**
     * Resolve the color token for an app's drawer glyph.
     *
     * @param category raw `ApplicationInfo.category` (null / unknown allowed).
     * @param symbolName the category glyph name from [IconMasker.symbolFor].
     */
    fun tokenFor(category: Int?, symbolName: String): Token = when (category) {
        AppCategory.SOCIAL -> Token.Social
        AppCategory.IMAGE -> Token.Social
        AppCategory.PRODUCTIVITY -> Token.Productivity
        AppCategory.GAME, AppCategory.AUDIO, AppCategory.VIDEO -> Token.Media
        AppCategory.NEWS -> Token.Finance
        AppCategory.MAPS -> Token.Travel
        else -> {
            // Unknown / uncategorized: fall back to a glyph heuristic so a phone or
            // mail app is not a grey blob, then neutral.
            if (symbolName in COMMUNICATION_GLYPHS) Token.Communication else Token.Neutral
        }
    }

    /**
     * P4b: the persisted-model mirror of [Token]. The editor stores
     * [DrawerIconTokenName] in `core:model`, so the two enums must stay 1:1 — this
     * mapping is the single bridge (tested by `IconEditorTest`).
     */
    fun nameOf(token: Token): DrawerIconTokenName = when (token) {
        Token.Communication -> DrawerIconTokenName.Communication
        Token.Social -> DrawerIconTokenName.Social
        Token.Productivity -> DrawerIconTokenName.Productivity
        Token.Media -> DrawerIconTokenName.Media
        Token.Travel -> DrawerIconTokenName.Travel
        Token.Finance -> DrawerIconTokenName.Finance
        Token.Neutral -> DrawerIconTokenName.Neutral
        Token.Browser -> DrawerIconTokenName.Browser
    }

    /**
     * P4: the [DrawerIconMap] color slot -> the palette [Token]. When the per-package map
     * knows the app, its **design** color wins (the tiles show, e.g., Chrome = blue); this
     * is the single bridge between the persisted-model enum and the palette enum.
     */
    fun tokenForName(name: DrawerIconTokenName): Token = when (name) {
        DrawerIconTokenName.Communication -> Token.Communication
        DrawerIconTokenName.Social -> Token.Social
        DrawerIconTokenName.Productivity -> Token.Productivity
        DrawerIconTokenName.Media -> Token.Media
        DrawerIconTokenName.Travel -> Token.Travel
        DrawerIconTokenName.Finance -> Token.Finance
        DrawerIconTokenName.Neutral -> Token.Neutral
        DrawerIconTokenName.Browser -> Token.Browser
    }
}
