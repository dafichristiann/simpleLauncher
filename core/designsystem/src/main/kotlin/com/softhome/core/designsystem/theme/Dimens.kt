package com.softhome.core.designsystem.theme

import androidx.compose.ui.unit.dp

/**
 * Dimension tokens for launcher components.
 * Source: design/homeApp.pen frames `znb90` (Warm Right Rail home) and `TpzL1`
 * (Warm App Drawer).
 */
object Dimens {
    // --- Home (Warm Right Rail) -- from .pen znb90 / L7ZAp ---
    val railWidth = 72.dp             // hrsLU / B6633e rail width
    val railIcon = 20.dp              // rail icons 20x20
    val railPadTop = 58.dp            // hrsLU padding top
    val railPadBottom = 26.dp         // hrsLU padding bottom
    val homeRowPaddingX = 42.dp       // row content x (znb90 text x=42)
    val dividerHeight = 1.dp          // row divider thickness
    val searchRowIcon = 24.dp         // N7ICqS search row icon
    val albumArt = 64.dp              // mdsQP album artwork circle
    val musicControl = 18.dp          // play (largest control)
    val musicControlSmall = 16.dp     // skip-back / skip-forward
    val progressHeight = 5.dp         // pvQO0 progress bar
    val iconLibTile = 68.dp           // QBhow icon library tile

    // --- App drawer -- from .pen TpzL1 ---
    val drawerTileNew = 68.dp         // dBQmH tile
    val drawerTileRadius = 21.dp      // dBQmH cornerRadius (~30% of 68)
    val drawerTileSymbol = 24.dp      // glyph inside drawer tile
    val drawerLabelWidth = 76.dp      // J3Lb4 label box width
    val drawerSearchNewHeight = 56.dp // V7udUl search pill height
    val drawerSearchNewRadius = 28.dp // V7udUl radius (height / 2)

    // --- Shared ---
    val toggleWidth = 42.dp           // UPa9N
    val toggleHeight = 22.dp
    val toggleKnob = 16.dp

    // --- P2: widget rows + drawer folders (reuse Warm tokens where possible) ---
    val folderTile = 68.dp            // matches drawerTileNew (dBQmH)
    val folderTileRadius = 21.dp      // matches drawerTileRadius (~30% of 68)
    val folderMiniIcon = 22.dp        // mini icon inside the 2x2 preview
    val folderPopupRadius = 24.dp     // popup card (Shape.large)
    val widgetProgressHeight = 5.dp   // row progress bar (matches progressHeight / pvQO0)

    // --- P3: context menu + settings rows (tokens, not inline values) ---
    val menuRadius = 24.dp            // long-press context menu card (Shape.large)
    val menuRowMinHeight = 48.dp      // menu row tap target
    val menuIcon = 20.dp              // leading line icon in a menu row
    val menuScrim = 0.53f             // dim scrim alpha behind the menu (#87000000)
    val settingsRowRadius = 18.dp     // settings row (Shape.medium)
    val settingsRowMinHeight = 52.dp  // settings row tap target
    val settingsChevron = 18.dp       // trailing chevron glyph
    val moveButton = 28.dp            // inline up/down reorder button (Q5)

    // --- P4a: drag & drop (tokens, not inline values) ---
    val dragInsertionThickness = 3.dp // accent insertion line under the finger
    val dragInsertionHeight = 20.dp   // insertion line length cap (row-list width)
    val dragLiftScale = 1.05f         // lifted item scale (sibling of a soft shadow)
    val dragSourceAlpha = 0.35f       // the in-place original, dimmed while lifted
    val dragHoverScale = 1.08f        // a hovered folder tile grows slightly
    val dragRowChipRadius = 14.dp     // home-row drag preview (label pill)
    val dragRowChipPaddingX = 14.dp
    val dragRowChipPaddingY = 8.dp
    val dragRingWidth = 2.dp          // accent ring on a hovered drop target

    // --- P4b: icon editor (tokens, not inline values) ---
    val editorCardMaxWidth = 360.dp   // editor card cap (like the menu widthIn range)
    val editorPreviewTile = 72.dp     // live preview tile (slightly larger than the grid)
    val choiceTile = 48.dp            // one selectable drawable/glyph tile in the editor
    val choiceTileRadius = 14.dp
    val choiceIcon = 22.dp            // glyph inside a choice tile
    val swatch = 28.dp                // one color swatch (glyph mode)
    val swatchRadius = 14.dp          // ~half of `swatch` (a circle)
    val swatchRingWidth = 2.dp        // selected-swatch ring
}
