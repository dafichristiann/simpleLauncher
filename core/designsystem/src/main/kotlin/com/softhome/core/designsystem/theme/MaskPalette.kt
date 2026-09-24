package com.softhome.core.designsystem.theme

import androidx.compose.ui.graphics.toArgb

/**
 * The two design tokens the icon compositor needs as raw ARGB ints (it paints with
 * `android.graphics`, off the Compose layer). Kept here so the icon pipeline never
 * hardcodes a hex value and stays in sync with [Color.kt] / [SoftColors].
 *
 *   - tile:  charcoal squircle backdrop (#2B2B2B, the icon-set tile)
 *   - onTile: cream monochrome mark (#E8DFD0, the icon-set symbol)
 */
object MaskPalette {
    val tileArgb: Int = SoftTile.toArgb()
    val onTileArgb: Int = SoftIconStroke.toArgb()
}
