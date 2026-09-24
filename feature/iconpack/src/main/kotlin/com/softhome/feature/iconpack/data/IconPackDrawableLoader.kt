package com.softhome.feature.iconpack.data

import android.content.Context
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import com.softhome.core.model.IconPack
import com.softhome.core.model.IconPackSource
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves a pack `drawable` name to a real Android [Drawable] (P1.5 - this replaces
 * the P1 placeholder glyph with actual icon-pack artwork).
 *
 * Handles all three [IconPackSource] kinds:
 *  - [IconPackSource.Zip]: streams the PNG/WEBP/vector out of the archive.
 *  - [IconPackSource.InstalledPack]: resolves the resource inside the pack APK.
 *  - [IconPackSource.Assets]: opens `assets/<dir>/<name>.png`.
 *
 * Every path is defensive: a missing/malformed entry returns null so the caller can
 * fall through to auto-mask (docs/08 ?5), and never crashes the grid.
 */
@Singleton
class IconPackDrawableLoader @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    // Small LRU of decoded drawables, keyed by "packId|drawableName". Icons are tiny
    // (128-512px) so a modest count is plenty and avoids re-decoding per recomposition.
    private val cache = LruCache<String, Drawable>(256)

    /** Decode [drawableName] for [pack]; null when unavailable (caller masks). */
    fun load(pack: IconPack, drawableName: String): Drawable? {
        val key = "${pack.id}|$drawableName"
        cache.get(key)?.let {
            android.util.Log.d("SOFTHOME_PIPELINE", "[LOADER] Cache hit: pack=${pack.id} drawable=$drawableName")
            return it
        }
        val drawable = try {
            when (val src = pack.source) {
                is IconPackSource.Zip -> loadFromZip(src.zipPath, drawableName)
                is IconPackSource.InstalledPack -> loadFromPackage(src.packageName, drawableName)
                is IconPackSource.Assets -> loadFromAssets(src.assetDir, drawableName)
            }
        } catch (t: Throwable) {
            android.util.Log.e("SOFTHOME_PIPELINE", "[LOADER] Threw for pack=${pack.id} drawable=$drawableName: ${t.message}", t)
            null
        }
        android.util.Log.d(
            "SOFTHOME_PIPELINE",
            "[LOADER] Resolved: packId=${pack.id} source=${pack.source} drawableName=$drawableName -> " +
                (drawable?.let { "${it.javaClass.simpleName} intrinsic=${it.intrinsicWidth}x${it.intrinsicHeight}" } ?: "NULL"),
        )
        if (drawable != null) cache.put(key, drawable)
        return drawable
    }

    /** True when the pack can currently produce a drawable for [drawableName]. */
    fun canLoad(pack: IconPack, drawableName: String): Boolean = load(pack, drawableName) != null

    fun clear() = cache.evictAll()

    // --- Zip ---------------------------------------------------------------------

    private fun loadFromZip(zipPath: String, name: String): Drawable? {
        val file = File(zipPath)
        if (!file.exists()) return null
        return ZipFile(file).use { zip ->
            val entry = findDrawableEntry(zip, name) ?: return null
            zip.getInputStream(entry).use { stream ->
                val bmp = BitmapFactory.decodeStream(stream) ?: return null
                BitmapDrawable(context.resources, bmp)
            }
        }
    }

    /** Locate the drawable file for [name] (any drawable-density bucket) in the zip. */
    private fun findDrawableEntry(zip: ZipFile, name: String): java.util.zip.ZipEntry? {
        val exts = listOf("png", "webp", "jpg", "jpeg", "xml")
        val names = exts.map { "$name.$it" }
        // Prefer the highest-density drawable bucket when several exist.
        val candidates = zip.entries().asSequence()
            .filter { e -> names.any { e.name.endsWith("/$it") || e.name == it } }
            .toList()
        android.util.Log.d("SOFTHOME_PIPELINE", "[LOADER] findEntry name=$name candidates=${candidates.joinToString { it.name }}")
        if (candidates.isEmpty()) return null
        return candidates.firstOrNull { it.name.contains("drawable-xxxhdpi") }
            ?: candidates.firstOrNull { it.name.contains("drawable-xxhdpi") }
            ?: candidates.firstOrNull { it.name.contains("drawable-xhdpi") }
            ?: candidates.firstOrNull { it.name.contains("drawable-hdpi") }
            ?: candidates.firstOrNull { it.name.contains("drawable-webp") }
            ?: candidates.first()
    }

    // --- Installed pack APK ------------------------------------------------------

    private fun loadFromPackage(packageName: String, name: String): Drawable? {
        val pm = context.packageManager
        val res: Resources = try {
            pm.getResourcesForApplication(packageName)
        } catch (_: Throwable) {
            return null
        }
        val id = res.getIdentifier(name, "drawable", packageName)
        if (id == 0) return null
        return try {
            res.getDrawable(id, null)
        } catch (_: Throwable) {
            null
        }
    }

    // --- Bundled assets ----------------------------------------------------------

    private fun loadFromAssets(assetDir: String, name: String): Drawable? {
        val exts = listOf("png", "webp", "jpg")
        for (ext in exts) {
            val path = "$assetDir/$name.$ext"
            val bmp = try {
                context.assets.open(path).use { stream -> BitmapFactory.decodeStream(stream) }
            } catch (_: Throwable) {
                null
            }
            if (bmp != null) return BitmapDrawable(context.resources, bmp)
        }
        return null
    }
}
