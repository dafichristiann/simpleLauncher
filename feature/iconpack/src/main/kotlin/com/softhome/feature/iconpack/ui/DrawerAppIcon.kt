package com.softhome.feature.iconpack.ui

import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.MaterialTheme
import com.softhome.core.designsystem.atom.DrawerIconTile
import com.softhome.core.designsystem.atom.LineIcon
import com.softhome.core.designsystem.theme.softColors
import com.softhome.core.model.IconPack
import com.softhome.core.model.IconSource
import com.softhome.core.model.ResolvedIcon
import com.softhome.feature.iconpack.data.IconPackDrawableLoader
import com.softhome.feature.iconpack.domain.DrawerIconColor
import com.softhome.feature.iconpack.domain.IconCompositor

/**
 * P3.5: renders one app icon for the **app drawer** grid in the color system
 * (design/homeApp.pen frame `TpzL1`, "Unique Icon Grid").
 *
 * Color rule (hybrid -- P3.5 decision 1):
 *  - An app with an active-pack drawable ([IconSource.FromPack]/[IconSource.Override])
 *    renders the **real drawable in its own colors** (no monochrome tint).
 *  - An app **not** in the pack renders a **category-colored lucide glyph**
 *    ([DrawerIconColor] -> a `SoftColors` drawer token).
 *
 * Auto-mask is deliberately **not** used here (decision 2): the drawer is always the
 * color system; the charcoal auto-mask stays the fallback for other surfaces.
 *
 * @param selected when true, the tile uses the amber `drawerTileSelected` background
 *   and the `drawerIconOnSelected` glyph (used for an open folder's members, and as
 *   the capability for a future "just installed" highlight).
 */
@Composable
fun DrawerAppIcon(
    resolved: ResolvedIcon,
    size: Dp,
    activePack: IconPack?,
    drawableLoader: IconPackDrawableLoader,
    modifier: Modifier = Modifier,
    category: Int? = null,
    selected: Boolean = false,
    contentDescription: String? = null,
) {
    val colors = MaterialTheme.softColors
    val density = LocalDensity.current
    val sizePx = with(density) { size.roundToPx() }

    // Decode the pack drawable once per (pack, entry, size). `sizePx` is a key so a
    // first compose with a tiny width never locks a 1px bitmap (see AppIcon.kt).
    val packPainter: Painter? = remember(activePack?.id, resolved.drawableName, resolved.source, sizePx) {
        val name = resolved.drawableName
        if (name == null || activePack == null) return@remember null
        if (resolved.source !is IconSource.FromPack && resolved.source !is IconSource.Override) {
            return@remember null
        }
        if (sizePx < 8) return@remember null
        val drawable = drawableLoader.load(activePack, name)
        drawable?.let {
            val bmp = IconCompositor.toBitmap(it, sizePx)
            if (bmp != null) BitmapPainter(bmp.asImageBitmap()) else painterFromDrawable(it)
        }
    }

    val background = if (selected) colors.drawerTileSelected else colors.drawerTileCream
    val overrideToken = resolved.overrideColorToken
    val symbolTint = when {
        selected -> colors.drawerIconOnSelected
        // P4b: a user-chosen glyph color, when the app carries a glyph override.
        overrideToken != null -> tokenColor(colors, overrideToken)
        else -> tokenColor(colors, DrawerIconColor.tokenFor(category, resolved.symbolName))
    }

    Box(modifier = modifier) {
        DrawerIconTile(
            size = size,
            background = background,
            symbol = LineIcon.fromName(resolved.symbolName),
            symbolTint = symbolTint,
            painter = packPainter,
            painterTint = null, // real pack artwork keeps its own colors
            contentDescription = contentDescription,
        )
    }
}

/** Map a [DrawerIconColor.Token] to the current theme's color field. */
internal fun tokenColor(
    colors: com.softhome.core.designsystem.theme.SoftColors,
    token: DrawerIconColor.Token,
): Color = when (token) {
    DrawerIconColor.Token.Communication -> colors.drawerIconCommunication
    DrawerIconColor.Token.Social -> colors.drawerIconSocial
    DrawerIconColor.Token.Productivity -> colors.drawerIconProductivity
    DrawerIconColor.Token.Media -> colors.drawerIconMedia
    DrawerIconColor.Token.Travel -> colors.drawerIconTravel
    DrawerIconColor.Token.Finance -> colors.drawerIconFinance
    DrawerIconColor.Token.Neutral -> colors.drawerIconNeutral
}

/**
 * P4b: resolve a persisted [com.softhome.core.model.DrawerIconTokenName] (a user icon
 * override's color) to the same theme field. Kept separate so the persisted model enum
 * never leaks into the P3.5 domain type.
 */
internal fun tokenColor(
    colors: com.softhome.core.designsystem.theme.SoftColors,
    token: com.softhome.core.model.DrawerIconTokenName,
): Color = when (token) {
    com.softhome.core.model.DrawerIconTokenName.Communication -> colors.drawerIconCommunication
    com.softhome.core.model.DrawerIconTokenName.Social -> colors.drawerIconSocial
    com.softhome.core.model.DrawerIconTokenName.Productivity -> colors.drawerIconProductivity
    com.softhome.core.model.DrawerIconTokenName.Media -> colors.drawerIconMedia
    com.softhome.core.model.DrawerIconTokenName.Travel -> colors.drawerIconTravel
    com.softhome.core.model.DrawerIconTokenName.Finance -> colors.drawerIconFinance
    com.softhome.core.model.DrawerIconTokenName.Neutral -> colors.drawerIconNeutral
}

private fun painterFromDrawable(drawable: android.graphics.drawable.Drawable): Painter? =
    when (drawable) {
        is BitmapDrawable -> drawable.bitmap?.let { BitmapPainter(it.asImageBitmap()) }
        else -> null
    }
