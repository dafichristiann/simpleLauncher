package com.softhome.feature.iconpack.data

import android.content.Context
import com.softhome.core.common.DispatcherProvider
import com.softhome.core.model.IconPack
import com.softhome.core.model.IconPackParseResult
import com.softhome.core.model.IconPackSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Progress of an icon-pack import, surfaced to the UI so a pack with hundreds of
 * drawables never blocks the frame (docs/09 decision).
 */
sealed interface ImportProgress {
    data class Validating(val fileName: String) : ImportProgress
    data class Indexing(val current: Int, val total: Int) : ImportProgress
    data class Done(val pack: IconPack) : ImportProgress
    data class Failed(val reason: String) : ImportProgress

    val fraction: Float?
        get() = when (this) {
            is Indexing -> if (total == 0) null else current.toFloat() / total
            is Done -> 1f
            else -> null
        }
}

/**
 * Imports an icon pack from a `.zip` chosen by the user (P1.5).
 *
 * Steps (all on IO, emitting progress):
 *  1. Validate: readable zip + a drawable folder + an appfilter (soft requirement).
 *  2. Copy into app-private storage so a later URI-permission loss can't break it.
 *  3. Parse `appfilter.xml` (malformed -> NoAppFilter, never a crash).
 *  4. Verify referenced drawables actually exist; a pack with an appfilter but zero
 *     decodable drawables is reported invalid.
 *
 * Any failure is a [ImportProgress.Failed]; the repository keeps the previous pack and
 * the launcher falls back to auto-mask.
 */
@Singleton
class IconPackImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val parser: AppFilterParser,
    private val loader: IconPackDrawableLoader,
    private val dispatchers: DispatcherProvider,
) {

    private val packsDir: File
        get() = File(context.filesDir, "iconpacks").apply { if (!exists()) mkdirs() }

    /** Import from a user-picked stream; copied to a temp file first, then parsed. */
    fun importZip(sourceStream: InputStream, displayName: String): Flow<ImportProgress> = flow {
        emit(ImportProgress.Validating(displayName))
        val temp = File(context.cacheDir, "import_${System.currentTimeMillis()}.zip")
        try {
            sourceStream.use { input -> temp.outputStream().use { input.copyTo(it) } }
        } catch (t: Throwable) {
            temp.delete()
            emit(ImportProgress.Failed(t.message ?: "Could not read the selected file"))
            return@flow
        }
        emitAll(importZipFile(temp, displayName, copyIntoStore = true, deleteAfter = true, storeAsName = displayName))
    }.flowOn(dispatchers.io)

    /** Import an existing `.zip` already on disk. */
    fun importZipFile(
        file: File,
        displayName: String,
        copyIntoStore: Boolean,
        deleteAfter: Boolean = false,
        storeAsName: String = file.name,
    ): Flow<ImportProgress> = flow {
        if (!file.exists() || !file.isFile) {
            emit(ImportProgress.Failed("File not found")); return@flow
        }

        // 1. structural validation + locate appfilter
        val names: List<String>
        val appFilterEntry: String?
        try {
            ZipFile(file).use { zip ->
                names = zip.entries().asSequence().map { it.name }.toList()
            }
        } catch (_: Throwable) {
            if (deleteAfter) file.delete()
            emit(ImportProgress.Failed("Not a valid .zip archive")); return@flow
        }
        val hasDrawableDir = names.any {
            it.startsWith("res/drawable") || it.startsWith("drawable") || it.contains("/drawable")
        }
        if (!hasDrawableDir) {
            if (deleteAfter) file.delete()
            emit(ImportProgress.Failed("Missing a drawable folder - not an icon pack"))
            return@flow
        }
        appFilterEntry = names.firstOrNull {
            it.equals("appfilter.xml", true) || it.endsWith("/appfilter.xml", true)
        }

        // 2. persist a stable copy
        val stored: File = if (copyIntoStore) {
            val dest = File(packsDir, sanitize(storeAsName))
            try {
                file.copyTo(dest, overwrite = true)
            } catch (_: Throwable) {
                if (deleteAfter) file.delete()
                emit(ImportProgress.Failed("Could not store the pack")); return@flow
            }
            if (deleteAfter) file.delete()
            dest
        } else file

        // 3. parse appfilter
        val packId = displayName.substringBeforeLast('.').lowercase().replace(Regex("[^a-z0-9_]+"), "_")
        val packName = displayName.substringBeforeLast('.').ifBlank { displayName }
        val parseResult: IconPackParseResult = if (appFilterEntry == null) {
            IconPackParseResult.NoAppFilter(
                IconPack.invalid(packId, packName, stored.absolutePath).copy(
                    source = IconPackSource.Zip(stored.absolutePath),
                    sourceKind = IconPack.SourceKind.Zip,
                    iconCount = names.size,
                ),
            )
        } else {
            parseFromZip(stored, appFilterEntry, packId, packName)
        }

        val pack = when (parseResult) {
            is IconPackParseResult.Success -> parseResult.pack
            is IconPackParseResult.NoAppFilter -> parseResult.pack
            is IconPackParseResult.Invalid -> {
                emit(ImportProgress.Failed(parseResult.reason)); return@flow
            }
        }

        // 4. verify drawables exist, emitting batched progress
        val drawables = pack.entries.values.toSet()
        if (drawables.isEmpty()) {
            emit(ImportProgress.Done(pack)); return@flow
        }
        val list = drawables.toList()
        var ok = 0
        list.forEachIndexed { i, drawableName ->
            if (loader.canLoad(pack, drawableName)) ok++
            if (i % 25 == 0 || i == list.lastIndex) emit(ImportProgress.Indexing(i + 1, list.size))
        }
        if (ok == 0) {
            emit(ImportProgress.Failed("Pack has an appfilter but no readable icons"))
            return@flow
        }
        emit(ImportProgress.Done(pack.copy(drawableCount = ok)))
    }.flowOn(dispatchers.io)

    private fun parseFromZip(zipFile: File, entry: String, packId: String, packName: String): IconPackParseResult =
        try {
            ZipFile(zipFile).use { z ->
                val parsed = z.getInputStream(z.getEntry(entry)).use {
                    parser.parse(it, packId, packName, zipFile.absolutePath)
                }
                when (parsed) {
                    is IconPackParseResult.Success -> IconPackParseResult.Success(
                        parsed.pack.copy(
                            source = IconPackSource.Zip(zipFile.absolutePath),
                            sourceKind = IconPack.SourceKind.Zip,
                        ),
                    )
                    else -> parsed
                }
            }
        } catch (t: Throwable) {
            IconPackParseResult.Invalid(t.message ?: "Malformed appfilter")
        }

    private fun sanitize(name: String): String = name.replace(Regex("[^A-Za-z0-9._-]+"), "_")
}
