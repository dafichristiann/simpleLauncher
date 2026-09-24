package com.softhome.feature.iconpack.data

import android.content.Context
import com.softhome.core.common.DispatcherProvider
import com.softhome.core.data.repository.PrefsRepository
import com.softhome.core.model.DiscoveredIconPack
import com.softhome.core.model.IconPack
import com.softhome.core.model.IconPackParseResult
import com.softhome.core.model.IconPackSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IconPackRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val parser: AppFilterParser,
    private val importer: IconPackImporter,
    private val scanner: InstalledIconPackScanner,
    private val loader: IconPackDrawableLoader,
    private val dispatchers: DispatcherProvider,
    private val prefsRepository: PrefsRepository,
) : IconPackRepository {

    private val _activePack = MutableStateFlow<IconPack?>(null)
    override val activePack: StateFlow<IconPack?> = _activePack.asStateFlow()

    init {
        CoroutineScope(dispatchers.io).launch { restoreActivePack() }
    }

    /**
     * Cold-start rehydration (P3-5, closes docs/04 #36): re-parse the pack the user
     * last chose (persisted id). Falls back to the newest imported zip, then to
     * auto-mask (null) -- never crashes, and clears a dead id so it doesn't stick.
     */
    private suspend fun restoreActivePack() {
        try {
            val storedId = prefsRepository.prefs.first().activeIconPackId
            if (!storedId.isNullOrBlank() && rehydrate(storedId)) return

            // No usable stored id: legacy behaviour -- newest imported zip, if any.
            val packsDir = File(context.filesDir, "iconpacks")
            val packZip = packsDir.listFiles { f -> f.extension == "zip" }
                ?.maxByOrNull { f -> f.lastModified() }
            if (packZip != null && packZip.exists()) {
                val result = loadFromZip(packZip)
                if (result is IconPackParseResult.Success) {
                    _activePack.value = result.pack
                    persistPackId(result.pack.id)
                }
            } else if (!storedId.isNullOrBlank()) {
                // Stored id could not be resolved -> clear it (missing/corrupt pack).
                prefsRepository.setActiveIconPack(null)
            }
        } catch (t: Throwable) {
            android.util.Log.e("SOFTHOME_PIPELINE", "[REPO] Failed to restore icon pack: ${t.message}")
        }
    }

    /** Resolve a stored pack id to a re-parsed [IconPack]; true on success. */
    private suspend fun rehydrate(storedId: String): Boolean {
        // (a) an imported zip whose derived id matches;
        val packsDir = File(context.filesDir, "iconpacks")
        val match = packsDir.listFiles { f -> f.extension == "zip" }
            ?.firstOrNull { IconPackRef.idForZip(it) == storedId }
        if (match != null) {
            val result = loadFromZip(match)
            if (result is IconPackParseResult.Success) {
                _activePack.value = result.pack
                return true
            }
        }
        // (b) an installed icon pack whose id or package name matches.
        val discovered = scanner.scan().firstOrNull {
            it.id == storedId || it.packageName == storedId
        }
        if (discovered != null) {
            val result = scanner.parse(discovered)
            if (result is IconPackParseResult.Success) {
                _activePack.value = result.pack
                return true
            }
        }
        return false
    }

    private suspend fun persistPackId(id: String?) = prefsRepository.setActiveIconPack(id)

    override fun importZipStream(stream: InputStream, displayName: String): Flow<ImportProgress> =
        importer.importZip(stream, displayName).onSuccessActivate()

    override fun importZip(file: File): Flow<ImportProgress> =
        importer.importZipFile(file, file.name, copyIntoStore = true).onSuccessActivate()

    override suspend fun scanInstalled(): List<DiscoveredIconPack> =
        withContext(dispatchers.io) { scanner.scan() }

    override suspend fun applyInstalled(discovered: DiscoveredIconPack): IconPackParseResult =
        withContext(dispatchers.io) {
            val result = scanner.parse(discovered)
            if (result is IconPackParseResult.Success) {
                _activePack.value = result.pack
                persistPackId(result.pack.id)
            }
            result
        }

    override suspend fun loadFromZip(file: File): IconPackParseResult = withContext(dispatchers.io) {
        val packId = IconPackRef.idForZip(file)
        val packName = file.nameWithoutExtension
        try {
            ZipFile(file).use { zip ->
                val entry = zip.getEntry("appfilter.xml")
                    ?: zip.entries().asSequence().firstOrNull {
                        it.name.equals("appfilter.xml", ignoreCase = true) || it.name.endsWith("/appfilter.xml")
                    }
                if (entry == null) {
                    return@withContext IconPackParseResult.NoAppFilter(
                        IconPack.invalid(packId, packName, file.absolutePath),
                    )
                }
                zip.getInputStream(entry).use { stream ->
                    when (val r = parser.parse(stream, packId, packName, file.absolutePath)) {
                        is IconPackParseResult.Success -> IconPackParseResult.Success(
                            r.pack.copy(
                                source = IconPackSource.Zip(file.absolutePath),
                                sourceKind = IconPack.SourceKind.Zip,
                            ),
                        )
                        else -> r
                    }
                }
            }
        } catch (t: Throwable) {
            IconPackParseResult.Invalid(t.message ?: "Unreadable icon pack")
        }
    }

    override suspend fun setActive(pack: IconPack?) {
        _activePack.value = pack
        persistPackId(pack?.id)
    }

    override suspend fun clearActive() {
        loader.clear()
        _activePack.value = null
        persistPackId(null)
    }

    /** Activates the pack as soon as the import reports success, clearing stale cache. */
    private fun Flow<ImportProgress>.onSuccessActivate(): Flow<ImportProgress> = flow {
        collect { progress ->
            if (progress is ImportProgress.Done) {
                loader.clear()
                _activePack.value = progress.pack
                persistPackId(progress.pack.id)
            }
            emit(progress)
        }
    }.flowOn(dispatchers.io)
}

/**
 * Pure helper deriving a stable pack id from a zip file -- kept in one place so the
 * importer, the loader and cold-start rehydration agree on the id (P3-5).
 */
object IconPackRef {
    /** Mirrors the id the importer derives: filename without extension, lowercased. */
    fun idForZip(file: File): String =
        file.nameWithoutExtension.lowercase().replace(" ", "_")
}
