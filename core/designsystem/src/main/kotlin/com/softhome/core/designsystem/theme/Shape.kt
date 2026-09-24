package com.softhome.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shape tokens.
 * Source: design/homeApp.pen radii --
 *   card r22-24, settings row r18, tag r12, pill = height/2, icon tile ? 30% of size.
 */
val SoftShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),       // tags
    medium = RoundedCornerShape(18.dp),      // settings rows / icon grid gap-adjacent
    large = RoundedCornerShape(24.dp),       // cards (weather, feature)
    extraLarge = RoundedCornerShape(32.dp),  // sheets / panels
)

/** Squircle: icon tiles use ~30% of the tile size (62?19, 104?30, 512?154). */
fun iconShape(size: Dp): RoundedCornerShape = RoundedCornerShape(size * 0.30f)

val PillShape = RoundedCornerShape(percent = 50)
val SearchPillShape = PillShape
val ToggleShape = PillShape
