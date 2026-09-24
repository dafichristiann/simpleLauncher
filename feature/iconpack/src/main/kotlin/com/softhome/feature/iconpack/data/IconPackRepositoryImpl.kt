package com.softhome.feature.iconpack.data

import android.content.Context
import com.softhome.core.common.DispatcherProvider
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
) : IconPackRepository {

    private val _activePack = MutableStateFlow<IconPack?>(null)
    override val activePack: StateFlow<IconPack?> = _activePack.asStateFlow()

    init {
        CoroutineScope(dispatchers.io).launch {
            try {
                val packsDir = File(context.filesDir, "iconpacks")
                val packZip = packsDir.listFiles { f -> f.extension == "zip" }
                    ?.maxByOrNull { f -> f.lastModified() }
                if (packZip != null && packZip.exists()) {
                    val result = loadFromZip(packZip)
                    if (result is IconPackParseResult.Success) {
                        _activePack.value = result.pack
                        android.util.Log.d("SOFTHOME_PIPELINE", "[REPO] Auto-restored active icon pack: ${result.pack.name} with ${result.pack.iconCount} entries")
                    }
                }
            } catch (t: Throwable) {
                android.util.Log.e("SOFTHOME_PIPELINE", "[REPO] Failed to auto-restore icon pack: ${t.message}")
            }
        }
    }

    override fun importZipStream(stream: InputStream, displayName: String): Flow<ImportProgress> =
        importer.importZip(stream, displayName).onSuccessActivate()

    override fun importZip(file: File): Flow<ImportProgress> =
        importer.importZipFile(file, file.name, copyIntoStore = true).onSuccessActivate()

    override suspend fun scanInstalled(): List<DiscoveredIconPack> =
        withContext(dispatchers.io) { scanner.scan() }

    override suspend fun applyInstalled(discovered: DiscoveredIconPack): IconPackParseResult =
        withContext(dispatchers.io) {
            val result = scanner.parse(discovered)
            if (result is IconPackParseResult.Success) _activePack.value = result.pack
            result
        }

    override suspend fun loadFromZip(file: File): IconPackParseResult = withContext(dispatchers.io) {
        val packId = file.nameWithoutExtension.lowercase().replace(" ", "_")
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
    }

    override suspend fun clearActive() {
        loader.clear()
        _activePack.value = null
    }

    /** Activates the pack as soon as the import reports success, clearing stale cache. */
    private fun Flow<ImportProgress>.onSuccessActivate(): Flow<ImportProgress> = flow {
        collect { progress ->
            if (progress is ImportProgress.Done) {
                loader.clear()
                _activePack.value = progress.pack
            }
            emit(progress)
        }
    }.flowOn(dispatchers.io)
}
