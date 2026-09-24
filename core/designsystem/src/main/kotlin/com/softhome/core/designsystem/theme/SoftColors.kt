package com.softhome.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic color surface consumed by every screen.
 * Maps the raw tokens in Color.kt to roles (docs/02-DESIGN-SYSTEM.md ?semantic).
 */
@Immutable
data class SoftColors(
    val background: Color,
    val surface: Color,
    val card: Color,
    val cardAlt: Color,
    val accentWash: Color,
    val tile: Color,
    val onTile: Color,          // line-art stroke on a tile
    val textPrimary: Color,
    val textBody: Color,
    val textMuted: Color,
    val indexLetter: Color,
    val accent: Color,
    val statusText: Color,
    val primaryAction: Color,   // CTA background
    val onPrimaryAction: Color,
    // --- Warm Right Rail semantic fields ---
    val railBg: Color,          // right rail background
    val divider: Color,         // row divider line
    val tileWarm: Color,        // icon tile / album circle bg
    val categoryWash: Color,    // panel / storyboard card bg
    val drawerBg: Color,        // app drawer screen background
    val drawerStroke: Color,    // drawer search pill border
    val progressTrack: Color,   // music progress track
    val indexActive: Color,     // alphabet rail active letter
    // --- P3 semantic fields (system UI + settings) ---
    val destructive: Color,     // uninstall / remove emphasis (warm clay, no red)
    val disabled: Color,        // greyed rows (system app, unavailable action)
    val isLight: Boolean,
) {
    /** Shadow color used behind tiles/cards in the current theme. */
    val shadow: Color get() = if (isLight) ShadowCard else Color(0x00000000)
}

val LightSoftColors = SoftColors(
    background = SoftBackground,
    surface = SoftSurface,
    card = SoftCard,
    cardAlt = SoftCardAlt,
    accentWash = SoftAccentWash,
    tile = SoftTileAlt,
    onTile = SoftIconStroke,
    textPrimary = SoftTextTitle,
    textBody = SoftTextBody,
    textMuted = SoftTextMuted,
    indexLetter = SoftIndexLetter,
    accent = SoftAccent,
    statusText = SoftStatusText,
    primaryAction = SoftTile,
    onPrimaryAction = SoftOnDark,
    railBg = SoftRailBg,
    divider = SoftDivider,
    tileWarm = SoftTileWarm,
    categoryWash = SoftCategoryWash,
    drawerBg = SoftDrawerBg,
    drawerStroke = SoftDrawerStroke,
    progressTrack = SoftProgressTrack,
    indexActive = SoftIndexActive,
    destructive = SoftDestructive,
    disabled = SoftDisabled,
    isLight = true,
)

val DarkSoftColors = SoftColors(
    background = DarkBackground,
    surface = DarkSurface,
    card = DarkCard,
    cardAlt = DarkCardAlt,
    accentWash = DarkAccentWash,
    tile = DarkTile,
    onTile = DarkIconStroke,
    textPrimary = DarkTextTitle,
    textBody = DarkTextBody,
    textMuted = DarkTextMuted,
    indexLetter = DarkIndexLetter,
    accent = DarkAccent,
    statusText = DarkStatusText,
    primaryAction = DarkTile,
    onPrimaryAction = DarkOnDark,
    railBg = DarkRailBg,
    divider = DarkDivider,
    tileWarm = DarkTileWarm,
    categoryWash = DarkCategoryWash,
    drawerBg = DarkDrawerBg,
    drawerStroke = DarkDrawerStroke,
    progressTrack = DarkProgressTrack,
    indexActive = DarkIndexActive,
    destructive = DarkDestructive,
    disabled = DarkDisabled,
    isLight = false,
)

val LocalSoftColors = staticCompositionLocalOf { LightSoftColors }
