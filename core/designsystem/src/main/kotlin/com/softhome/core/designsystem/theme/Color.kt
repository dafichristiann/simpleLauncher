package com.softhome.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * SOFT / HOME color tokens.
 *
 * Source of truth: design/homeApp.pen (Pen canvas v2.18) + docs/02-DESIGN-SYSTEM.md
 *  - background / surface / card / tile / text values are read from the
 *    `.pen` frames `znb90` (Warm Right Rail home), `L7ZAp` (full system board)
 *    and `TpzL1` (Warm App Drawer).
 *  - The **dark palette is the `KkPN3` "Dark Editorial" frame** (P4c): a concrete,
 *    named palette in the file (cool/neutral), replacing the earlier DERIVED warm-dark.
 *    See docs/04-ASSUMPTIONS.md section M. A few surfaces the frame does not show
 *    (menu/popup/drawer cards) are derived from the same neutral family.
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

// --- Dark palette (P4c: the KkPN3 "Dark Editorial" named palette) ---
// Verbatim from `design/homeApp.pen` frame `KkPN3` (Pen v2.18), except where noted.
// The frame defines: screen bg #18191A, rail #2E3134, divider #343638, primary text
// #F2EEE7, soft text #E2DDD5, day #D2CBC1, month #918F8B, artist #C8C0B6, track
// #817F7B, placeholder #A8A29A, album #E3DED6, album mark #454648, track #676866.
val DarkBackground = Color(0xFF18191A)  // KkPN3 screen bg (verbatim)
val DarkSurface = Color(0xFF18191A)     // KkPN3 screen bg (surfaces sit on it)
val DarkCard = Color(0xFF24262A)        // derived: card/menu on the neutral family
val DarkCardAlt = Color(0xFF2A2C30)     // derived: alt card
val DarkAccentWash = Color(0xFF2E3134)  // KkPN3 rail tint (reused as a wash)
val DarkTile = Color(0xFFF2EEE7)        // inverted: light tile on dark
val DarkTextTitle = Color(0xFFF2EEE7)   // KkPN3 primary text (verbatim)
val DarkTextBody = Color(0xFFD2CBC1)    // KkPN3 "day" soft text (verbatim)
val DarkTextMuted = Color(0xFF918F8B)   // KkPN3 "month" muted (verbatim)
val DarkIndexLetter = Color(0xFF918F8B)
val DarkOnDark = Color(0xFF18191A)
val DarkIconStroke = Color(0xFF18191A)  // dark stroke on the light inverted tile
val DarkAccent = Color(0xFFC98B6E)      // warm accent kept legible on the neutral dark
val DarkStatusText = Color(0xFFE2DDD5)  // KkPN3 soft text (verbatim)

// --- Dark adaptations for the Warm Right Rail semantic fields (KkPN3) ---
val DarkRailBg = Color(0xFF2E3134)       // KkPN3 dark rail (verbatim)
val DarkDivider = Color(0xFF343638)      // KkPN3 divider (verbatim)
val DarkTileWarm = Color(0xFF2E3134)     // derived: tile warm on dark (rail family)
val DarkCategoryWash = Color(0xFF2A2C30) // derived: panel/wash on dark
val DarkDrawerBg = Color(0xFF1F2124)     // derived: drawer bg (slightly lifted from bg)
val DarkDrawerStroke = Color(0xFF3E4145) // derived: drawer search pill border
val DarkProgressTrack = Color(0xFF676866) // KkPN3 progress track (verbatim)
val DarkIndexActive = Color(0xFFC98B6E)  // accent (legible on dark)

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

// Dark adaptations (the .pen has no dark drawer palette; P4c aligns them to the
// KkPN3 neutral dark family so the drawer reads consistently in dark mode).
val DarkDrawerTileCream = Color(0xFF2E3134)
val DarkDrawerTileSelected = Color(0xFF8A6440)
val DarkDrawerIconOnSelected = Color(0xFFF5EFE6)
val DarkDrawerIconCommunication = Color(0xFF7FA79B)
val DarkDrawerIconSocial = Color(0xFF8FAFA8)
val DarkDrawerIconProductivity = Color(0xFFC89A78)
val DarkDrawerIconMedia = Color(0xFFD9A972)
val DarkDrawerIconTravel = Color(0xFFC98B6E)
val DarkDrawerIconFinance = Color(0xFF7FA6B2)
val DarkDrawerIconNeutral = Color(0xFFD2CBC1)

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
