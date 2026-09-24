package com.softhome.feature.iconpack.domain

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.LruCache
import com.softhome.core.designsystem.theme.MaskPalette
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Produces the auto-mask bitmap for an app that is *not* in the active pack (P1.5).
 *
 * Takes the app's real launcher icon from [PackageManager], composites it into the
 * charcoal squircle as a cream monochrome mark via [IconCompositor], and caches the
 * result. If the real icon is unusable (null, or charcoal-only / blob after compose)
 * it returns null and the UI falls back to the category glyph (docs/08 §3b), so a
 * tile is never a blank identical ring.
 */
@Singleton
class IconBitmapProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val cache = LruCache<String, Bitmap>(128)

    /**
     * Mask the real icon for [packageName]/[className] into a [sizePx] squircle.
     * Returns null when the system icon cannot be loaded or the composite has no
     * visible glyph (caller must fall back to the category symbol).
     */
    fun maskedIcon(packageName: String, className: String, sizePx: Int): Bitmap? {
        val key = "mask|$packageName/$className|$sizePx"
        cache.get(key)?.let {
            android.util.Log.d("SOFTHOME_PIPELINE", "[PROVIDER] Cache hit: key=$key")
            return it
        }
        val real = loadSystemIcon(packageName, className)
        android.util.Log.d(
            "SOFTHOME_PIPELINE",
            "[PROVIDER] AutoMask source: pkg=$packageName cls=$className " +
                "realIcon=${real?.javaClass?.name ?: "NULL (fallback to category glyph)"} " +
                "intrinsic=${real?.intrinsicWidth}x${real?.intrinsicHeight}",
        )
        if (real == null) return null
        val bmp = try {
            IconCompositor.mask(
                icon = real,
                sizePx = sizePx,
                tileColor = MaskPalette.tileArgb,
                tintColor = MaskPalette.onTileArgb,
                monochrome = true,
            )
        } catch (t: Throwable) {
            android.util.Log.e(
                "SOFTHOME_PIPELINE",
                "[PROVIDER] IconCompositor.mask failed for $packageName: ${t.message}",
                t,
            )
            null
        }
        if (bmp == null) return null
        if (!IconCompositor.hasVisibleGlyph(bmp, MaskPalette.tileArgb)) {
            android.util.Log.w(
                "SOFTHOME_PIPELINE",
                "[PROVIDER] Rejecting charcoal-only/blob mask for $packageName " +
                    "fingerprint=${IconCompositor.fingerprint(bmp)} — UI will use category glyph",
            )
            bmp.recycle()
            return null
        }
        android.util.Log.d(
            "SOFTHOME_PIPELINE",
            "[PROVIDER] Accepted mask for $packageName fingerprint=${IconCompositor.fingerprint(bmp)}",
        )
        cache.put(key, bmp)
        return bmp
    }

    /** The raw system icon as a bitmap (system fallback, last resort). */
    fun systemIcon(packageName: String, className: String, sizePx: Int): Bitmap? {
        val key = "sys|$packageName/$className|$sizePx"
        cache.get(key)?.let { return it }
        val drawable = loadSystemIcon(packageName, className) ?: return null
        val bmp = IconCompositor.toBitmap(drawable, sizePx)
        if (bmp != null) cache.put(key, bmp)
        return bmp
    }

    fun clear() = cache.evictAll()

    private fun loadSystemIcon(packageName: String, className: String): Drawable? {
        val pm = context.packageManager
        return try {
            val info = if (className.isNotBlank()) {
                pm.getActivityInfo(ComponentName(packageName, className), 0)
            } else {
                pm.getApplicationInfo(packageName, 0)
            }
            info.loadIcon(pm)
        } catch (_: Throwable) {
            try {
                pm.getApplicationInfo(packageName, 0).loadIcon(pm)
            } catch (_: Throwable) {
                null
            }
        }
    }
}
