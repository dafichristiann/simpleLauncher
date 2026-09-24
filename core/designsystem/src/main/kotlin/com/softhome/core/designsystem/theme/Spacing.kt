package com.softhome.core.designsystem.theme

import androidx.compose.ui.unit.dp

/**
 * Spacing scale. Source: docs/02-DESIGN-SYSTEM.md + design-specified paddings.
 * Card padding 24, weather inner 18, home screen padding 28/24/24/24.
 */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 14.dp   // home icon grid vertical gap (from .pen ECmUn gap=14)
    val xl = 16.dp
    val gridGap = 18.dp // home icon grid column gap (SfSp2 gap=18)
    val xxl = 24.dp
    val drawerGap = 26.dp // drawer grid gap (q5mzR gap=26)
    val xxxl = 28.dp
    val huge = 36.dp

    // --- Warm Right Rail -- from .pen znb90 ---
    val railGap = 22.dp   // hrsLU rail icon gap
    val rowBand = 164.dp  // divider-to-divider distance (y 128 -> 226 -> 322 -> 424)
}
