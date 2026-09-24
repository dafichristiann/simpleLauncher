package com.softhome.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * SOFT / HOME color tokens.
 *
 * Source of truth: design/homeApp.pen (Pen canvas v2.18) + docs/02-DESIGN-SYSTEM.md
 *  - background / surface / card / tile / text values are read from the
 *    `.pen` frames `znb90` (Warm Right Rail home), `L7ZAp` (full system board)
 *    and `TpzL1` (Warm App Drawer).
 *  - The dark palette is a DERIVED warm adaptation (no dark palette exists in the
 *    `.pen`); see docs/04-ASSUMPTIONS.md #5.
 *
 * Rule: no screen hardcodes a color. Every screen consumes [SoftColors].
 */

// --- Light palette (verbatim from .pen) ---
val SoftBackground = Color(0xFFEDE6D8) // canvas bg
val SoftSurface = Color(0xFFE8DFD0)    // phone screen bg + cream icon stroke
val SoftCard = Color(0xFFF6F0E7)       // weather card / search pill / menu / settings rows
val SoftCardAlt = Color(0xFFF5EFE6)    // recommendation cards
val SoftAccentWash = Color(0xFFD8C8B6) // muted section wash
val SoftTile = Color(0xFF2B2B2B)       // icon tiles (icon set), CTA, toggles
val SoftTileAlt = Color(0xFF2F2F2F)    // home-grid icon tiles
val SoftTextTitle = Color(0xFF1A1A1A)  // headings, clock
val SoftTextBody = Color(0xFF625B52)   // paragraphs
val SoftTextMuted = Color(0xFF81796D)  // meta / secondary
val SoftIndexLetter = Color(0xFF6E675E) // alphabet index rail
val SoftOnDark = Color(0xFFF7F0E5)     // label on charcoal
val SoftIconStroke = Color(0xFFE8DFD0) // cream line-art on charcoal tiles
val SoftAccent = Color(0xFF8A5F43)     // eyebrow / section numbers
val SoftAccentDeep = Color(0xFF7A553D)
val SoftCodeAccent = Color(0xFFCBB39D)
val SoftStatusText = Color(0xFF3A3A3A) // status row + small dark icon fill

// --- Warm dark palette (derived -- docs/02) ---
val DarkBackground = Color(0xFF1F1D1A)
val DarkSurface = Color(0xFF26231F)
val DarkCard = Color(0xFF2E2A25)
val DarkCardAlt = Color(0xFF332E28)
val DarkAccentWash = Color(0xFF3A342C)
val DarkTile = Color(0xFFEDE6D8)       // inverted: cream tile
val DarkTextTitle = Color(0xFFF7F0E5)
val DarkTextBody = Color(0xFFC9BFB0)
val DarkTextMuted = Color(0xFF9C9284)
val DarkIndexLetter = Color(0xFF9C9284)
val DarkOnDark = Color(0xFF2B2B2B)
val DarkIconStroke = Color(0xFF2B2B2B) // dark stroke on cream tile
val DarkAccent = Color(0xFFC89A78)
val DarkStatusText = Color(0xFFCFC4B4)

// --- Warm dark adaptations for the Warm Right Rail semantic fields ---
val DarkRailBg = Color(0xFF2E3134)       // dark editorial rail (q0n2Wd)
val DarkDivider = Color(0xFF343638)      // dark dividers (QzH19 / l3vDi / oNpkj / XLDj6)
val DarkTileWarm = Color(0xFF33302B)
val DarkCategoryWash = Color(0xFF3A342C)
val DarkDrawerBg = Color(0xFF26231F)
val DarkDrawerStroke = Color(0xFF4A443B)
val DarkProgressTrack = Color(0xFF676866) // dark progress track (Tl7UC)
val DarkIndexActive = Color(0xFFC89A78)

// --- Warm Right Rail tokens (from homeApp.pen frames znb90 / L7ZAp / TpzL1) ---
val SoftRailBg = Color(0xFFD8C8B6)         // right rail background (hrsLU / B6633e)
val SoftDivider = Color(0xFFD0C2B1)        // 1px row divider (MrrnQ / RDJW1 / LKAGP / FH9n4)
val SoftTileWarm = Color(0xFFF5EFE6)       // icon tile / album circle (mdsQP, drawer tiles)
val SoftCategoryWash = Color(0xFFE4D7C7)   // icon library + storyboard card bg (QBhow / Az7qs)
val SoftDrawerBg = Color(0xFFDCCDBA)       // drawer screen bg (TpzL1)
val SoftDrawerStroke = Color(0xFFC8B8A6)   // drawer search pill border (V7udUl)
val SoftProgressTrack = Color(0xFFB9AA98)  // music progress track (pvQO0)
val SoftIndexActive = Color(0xFFB06F52)    // alphabet rail active letter (fZMbk)
val SoftRailBgDark = Color(0xFF2E3134)     // dark editorial rail (q0n2Wd) -- dark reference only

// --- P3.5: drawer icon palette (from homeApp.pen frame TpzL1) ---
// The drawer renders cream tiles + per-category colored glyphs (not the monochrome
// charcoal squircle). See docs/superpowers/specs/2026-09-24-p35-drawer-icon-redesign-design.md.
val DrawerTileCream = Color(0xFFF6F0E7)      // $tile-cream   (default tile bg)
val DrawerTileSelected = Color(0xFFD19B62)   // $selected-tile (active/selected tile bg)
val DrawerIconOnSelected = Color(0xFFF5EFE6) // $selected-icon (glyph on a selected tile)
val DrawerIconCommunication = Color(0xFF5F7A72) // $icon-communication
val DrawerIconSocial = Color(0xFF6E8B86)        // $icon-social
val DrawerIconProductivity = Color(0xFF8A5F43)  // $icon-productivity (== SoftAccent)
val DrawerIconMedia = Color(0xFFD19B62)         // $icon-media       (== selected tile)
val DrawerIconTravel = Color(0xFFB06F52)        // $icon-travel      (== SoftIndexActive)
val DrawerIconFinance = Color(0xFF4D7C8A)       // $icon-finance
val DrawerIconNeutral = Color(0xFF625B52)       // $icon-neutral     (== SoftTextBody)

// Dark adaptations (the .pen has no dark drawer palette; warm-derived per docs/02).
val DarkDrawerTileCream = Color(0xFF33302B)
val DarkDrawerTileSelected = Color(0xFF8A6440)
val DarkDrawerIconOnSelected = Color(0xFFF5EFE6)
val DarkDrawerIconCommunication = Color(0xFF7FA79B)
val DarkDrawerIconSocial = Color(0xFF8FAFA8)
val DarkDrawerIconProductivity = Color(0xFFC89A78)
val DarkDrawerIconMedia = Color(0xFFD9A972)
val DarkDrawerIconTravel = Color(0xFFC98B6E)
val DarkDrawerIconFinance = Color(0xFF7FA6B2)
val DarkDrawerIconNeutral = Color(0xFFC9BFB0)

// --- P3 (System UI + Settings) semantic tokens ---
// The warm palette has no red by design (docs/00); "destructive" (uninstall) is a
// deep clay accent, and unavailable rows are a muted warm grey (disabled). See P3
// spec section 4.12.
val SoftDestructive = Color(0xFF9A4B33) // deep clay -- uninstall / remove emphasis
val SoftDisabled = Color(0xFFAFA493)    // muted warm grey -- greyed (system app) rows
val DarkDestructive = Color(0xFFD08A6A)
val DarkDisabled = Color(0xFF7C7468)

// --- Shadow colors (outer only, no borders) -- from .pen effect tokens ---
val ShadowMockup = Color(0xFFC7B9A8)
val ShadowTile = Color(0xFFC7B9A8)
val ShadowCard = Color(0xFFCFC2B2)
val ShadowRecCard = Color(0xFFD0C2B1)
val ShadowMenu = Color(0xFFBFAF9E)
val ShadowBadgeGlow = Color(0xFFF3EBDD)
