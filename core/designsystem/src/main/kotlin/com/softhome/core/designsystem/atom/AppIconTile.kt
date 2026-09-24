package com.softhome.core.designsystem.atom

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.softhome.core.designsystem.theme.SoftShadow
import com.softhome.core.designsystem.theme.ShadowBadgeGlow
import com.softhome.core.designsystem.theme.TileShadow
import com.softhome.core.designsystem.theme.iconShape
import com.softhome.core.designsystem.theme.softColors
import com.softhome.core.designsystem.theme.softShadow

/**
 * The signature SOFT tile: a charcoal squircle (~30% radius) holding either a
 * cream line-art symbol, a decoded icon-pack drawable, or a masked real app icon.
 *
 * Sources in design/homeApp.pen:
 *   - home tile  S1eRI  62x62 r19 #2F2F2F, symbol 26 cream, shadow b8
 *   - drawer tile R0mbF 104x104 r30 #2B2B2B, symbol 44 cream
 *   - icon set    lk7jo 512 r154, symbol 250 cream  (symbol ratio 250/512 = 0.49)
 *
 * Rendering precedence, highest first:
 *  1. [fullBleedPainter] - an already-composited icon bitmap (auto-mask / system),
 *     drawn edge-to-edge inside the squircle with no extra tint or inset.
 *  2. [painter]          - a decoded pack drawable, tinted to the cream stroke and
 *     inset to the design's symbol ratio (glyphs, not full-bleed artwork).
 *  3. [symbol]           - the category-glyph fallback (mask heuristic).
 */
@Composable
fun AppIconTile(
    size: Dp,
    modifier: Modifier = Modifier,
    symbol: LineIcon? = null,
    painter: Painter? = null,
    fullBleedPainter: Painter? = null,
    contentDescription: String? = null,
) {
    val colors = MaterialTheme.softColors
    val tileColor = colors.tile
    val stroke = colors.onTile

    Box(
        modifier = modifier
            .size(size)
            .softShadow(TileShadow, iconShape(size))
            .background(tileColor, iconShape(size)),
        contentAlignment = Alignment.Center,
    ) {
        when {
            fullBleedPainter != null -> {
                // Pre-composited icon (already has its own charcoal backdrop + tint).
                Image(
                    painter = fullBleedPainter,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(iconShape(size)),
                )
            }
            painter != null -> {
                Image(
                    painter = painter,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(stroke),
                    modifier = Modifier.size(size * ICON_SYMBOL_RATIO),
                )
            }
            symbol != null -> {
                LineIconImage(
                    icon = symbol,
                    size = size * ICON_GLYPH_RATIO,
                    tint = stroke,
                    contentDescription = contentDescription,
                )
            }
        }
    }
}

/** Design icon-set ratio: symbol 250 / tile 512 = 0.49. Used for decoded drawables. */
const val ICON_SYMBOL_RATIO = 0.49f

/** Line-art glyph ratio used for the fallback category symbol. */
const val ICON_GLYPH_RATIO = 0.42f

/**
 * P3.5: a **color-parameterized** icon tile for the app drawer ("Unique Icon Grid",
 * design/homeApp.pen frame `TpzL1`).
 *
 * Unlike [AppIconTile] -- which is monochrome by construction (charcoal squircle +
 * cream mark) -- this atom takes an explicit [background] and an **optional**
 * [painterTint]:
 *  - [painterTint] `null` renders the painter's own colors (a real icon-pack drawable),
 *  - a non-null [painterTint] recolors it,
 *  - [symbolTint] colors the category glyph.
 *
 * Rendering precedence mirrors [AppIconTile]: full-bleed painter > painter > symbol.
 * The monochrome [AppIconTile] path is left unchanged for every non-drawer surface.
 */
@Composable
fun DrawerIconTile(
    size: Dp,
    background: Color,
    modifier: Modifier = Modifier,
    symbol: LineIcon? = null,
    symbolTint: Color = Color.Unspecified,
    painter: Painter? = null,
    painterTint: Color? = null,
    fullBleedPainter: Painter? = null,
    contentDescription: String? = null,
) {
    Box(
        modifier = modifier
            .size(size)
            .softShadow(TileShadow, iconShape(size))
            .background(background, iconShape(size)),
        contentAlignment = Alignment.Center,
    ) {
        when {
            fullBleedPainter != null -> {
                Image(
                    painter = fullBleedPainter,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(iconShape(size)),
                )
            }
            painter != null -> {
                Image(
                    painter = painter,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Fit,
                    colorFilter = painterTint?.let { ColorFilter.tint(it) },
                    modifier = Modifier.size(size * ICON_SYMBOL_RATIO),
                )
            }
            symbol != null -> {
                LineIconImage(
                    icon = symbol,
                    size = size * ICON_GLYPH_RATIO,
                    tint = symbolTint,
                    contentDescription = contentDescription,
                )
            }
        }
    }
}

/**
 * Cream "one quiet signal" notification badge (design node ha6OA / docs/08 ?4):
 * 15% dot, cream, soft glow, NO red and NO count.
 */
@Composable
fun CreamBadgeDot(
    tileSize: Dp,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.softColors
    Box(
        modifier = modifier
            .size(tileSize * 0.15f)
            .softShadow(
                SoftShadow(elevation = 8.dp, color = ShadowBadgeGlow, shape = CircleShape),
                CircleShape,
            )
            .background(colors.surface, CircleShape),
    )
}
