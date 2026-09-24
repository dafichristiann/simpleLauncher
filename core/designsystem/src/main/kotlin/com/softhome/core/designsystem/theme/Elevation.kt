package com.softhome.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Soft-shadow tokens (outer only, no borders).
 * Source: design/homeApp.pen effect tokens + docs/02-DESIGN-SYSTEM.md#elevation.
 *  - tile     color #C7B9A8  dy 4  blur 8
 *  - card     color #CFC2B2  dy 7  blur 14
 *  - search   color #CFC2B2  dy 6  blur 12
 *  - recCard  color #D0C2B1  dy 8  blur 18
 *  - menu     color #BFAF9E  dy 8  blur 18
 *
 * Uses Compose's Modifier.shadow with explicit ambient/spot colors. Colored shadows
 * are honoured on API 28+; on API 26-27 the platform renders the default shadow
 * colour (documented in docs/04-ASSUMPTIONS.md). No borders anywhere.
 */
data class SoftShadow(
    val elevation: Dp,
    val color: Color,
    val shape: Shape = RoundedCornerShape(24.dp),
)

val TileShadow = SoftShadow(8.dp, ShadowTile, RoundedCornerShape(19.dp))
val CardShadow = SoftShadow(14.dp, ShadowCard, RoundedCornerShape(24.dp))
val SearchShadow = SoftShadow(12.dp, ShadowCard, RoundedCornerShape(percent = 50))
val RecCardShadow = SoftShadow(18.dp, ShadowRecCard, RoundedCornerShape(24.dp))
val MenuShadow = SoftShadow(18.dp, ShadowMenu, RoundedCornerShape(22.dp))

fun Modifier.softShadow(
    shadow: SoftShadow,
    shape: Shape = shadow.shape,
): Modifier = this.shadow(
    elevation = shadow.elevation,
    shape = shape,
    clip = false,
    ambientColor = shadow.color,
    spotColor = shadow.color,
)
