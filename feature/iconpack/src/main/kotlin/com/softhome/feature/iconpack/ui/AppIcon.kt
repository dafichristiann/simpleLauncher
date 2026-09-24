package com.softhome.feature.iconpack.ui

import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.softhome.core.designsystem.atom.AppIconTile
import com.softhome.core.designsystem.atom.CreamBadgeDot
import com.softhome.core.designsystem.atom.LineIcon
import com.softhome.core.model.IconPack
import com.softhome.core.model.IconSource
import com.softhome.core.model.ResolvedIcon
import com.softhome.feature.iconpack.data.IconPackDrawableLoader
import com.softhome.feature.iconpack.domain.IconBitmapProvider
import com.softhome.feature.iconpack.domain.IconCompositor

/**
 * Renders one app icon honoring its resolved icon source (P1.5 - real decode).
 *
 * Resolution, in order:
 *  - [IconSource.FromPack]/[IconSource.Override] -> decode the pack drawable and tint
 *    it to the cream stroke. If the drawable is missing/corrupt, fall through to mask.
 *  - [IconSource.AutoMask] (and pack-load fallthrough) -> composite the app's real icon
 *    into the charcoal squircle as a cream monochrome mark; if that fails, the
 *    category glyph.
 *  - [IconSource.System] -> the raw system icon, masked into the squircle when possible.
 *
 * The [symbolName] category glyph is always the last-resort placeholder, so a tile is
 * never blank (docs/08 §5).
 */
@Composable
fun AppIcon(
    resolved: ResolvedIcon,
    size: Dp,
    activePack: IconPack?,
    drawableLoader: IconPackDrawableLoader,
    bitmapProvider: IconBitmapProvider,
    modifier: Modifier = Modifier,
    showBadge: Boolean = false,
    contentDescription: String? = null,
) {
    val density = LocalDensity.current
    val sizePx = with(density) { size.roundToPx() }

    android.util.Log.d(
        "SOFTHOME_PIPELINE",
        "[TILE] Render tile: pkg=${resolved.packageName} cls=${resolved.className} " +
            "resolutionSource=${resolved.source} drawableName=${resolved.drawableName} " +
            "activePackId=${activePack?.id} appfilterEntry=${activePack?.entries?.get(resolved.componentKey)}",
    )

    // Decode the pack drawable once per (pack, entry, size). sizePx MUST be a key:
    // BoxWithConstraints can first compose with a tiny width; locking a 1px bitmap
    // forever produced charcoal-only tiles that looked identical across apps.
    val packPainter: Painter? = remember(activePack?.id, resolved.drawableName, resolved.source, sizePx) {
        val name = resolved.drawableName
        if (name == null || activePack == null) return@remember null
        if (resolved.source !is IconSource.FromPack && resolved.source !is IconSource.Override) {
            return@remember null
        }
        if (sizePx < 8) return@remember null
        val drawable = drawableLoader.load(activePack, name)
        android.util.Log.d(
            "SOFTHOME_PIPELINE",
            "[TILE] Pack load pkg=${resolved.packageName} drawable=$name -> " +
                (drawable?.let { "${it.javaClass.simpleName} ${it.intrinsicWidth}x${it.intrinsicHeight}" } ?: "NULL → fallthrough"),
        )
        drawable?.let {
            val bmp = IconCompositor.toBitmap(it, sizePx)
            if (bmp != null) {
                android.util.Log.d(
                    "SOFTHOME_PIPELINE",
                    "[TILE] Pack bitmap pkg=${resolved.packageName} fingerprint=${IconCompositor.fingerprint(bmp)}",
                )
                BitmapPainter(bmp.asImageBitmap())
            } else {
                painterFromDrawable(it)
            }
        }
    }

    // Compose the auto-mask (or system fallback) bitmap once per (component, size).
    // Also used when FromPack/Override failed to decode — never leave a charcoal hole.
    val maskedPainter: Painter? = remember(
        resolved.componentKey,
        sizePx,
        resolved.source,
        packPainter == null,
    ) {
        if (sizePx < 8) return@remember null
        val needsMask = when (resolved.source) {
            is IconSource.FromPack, is IconSource.Override -> packPainter == null
            IconSource.AutoMask, IconSource.System -> true
        }
        if (!needsMask) return@remember null
        android.util.Log.d(
            "SOFTHOME_PIPELINE",
            "[TILE] Mask fallthrough pkg=${resolved.packageName} source=${resolved.source}",
        )
        when (resolved.source) {
            IconSource.System -> bitmapProvider
                .systemIcon(resolved.packageName, resolved.className, sizePx)
                ?.let { BitmapPainter(it.asImageBitmap()) }
            else -> bitmapProvider
                .maskedIcon(resolved.packageName, resolved.className, sizePx)
                ?.let { BitmapPainter(it.asImageBitmap()) }
        }
    }

    Box(modifier = modifier) {
        AppIconTile(
            size = size,
            symbol = LineIcon.fromName(resolved.symbolName),
            painter = packPainter,
            fullBleedPainter = maskedPainter,
            contentDescription = contentDescription,
        )
        if (showBadge) {
            CreamBadgeDot(
                tileSize = size,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = size * 0.04f, end = size * 0.04f),
            )
        }
    }
}

/** Wrap a decoded [android.graphics.drawable.Drawable] as a Compose [Painter]. */
private fun painterFromDrawable(drawable: android.graphics.drawable.Drawable): Painter? =
    when (drawable) {
        is BitmapDrawable -> drawable.bitmap?.let { BitmapPainter(it.asImageBitmap()) }
        else -> null
    }
