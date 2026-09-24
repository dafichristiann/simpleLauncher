package com.softhome.feature.iconpack.data

import com.softhome.core.model.DiscoveredIconPack
import com.softhome.core.model.IconPack
import com.softhome.core.model.IconPackParseResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.io.InputStream

/**
 * Loads, imports and caches the active icon pack (P1.5).
 *
 * Two import paths, per the standard Android icon-pack schema:
 *  - [importZipStream] / [importZip]: a `.zip` the user picks (APP-agnostic).
 *  - [scanInstalled] / [applyInstalled]: a pack APK already installed on the device.
 *
 * All failures are graceful: nothing throws across this boundary, the active pack is
 * only replaced on a successful parse, and the caller can always fall back to mask.
 */
interface IconPackRepository {
    val activePack: StateFlow<IconPack?>

    /** Import a pack from a user-picked stream; emits progress and activates on success. */
    fun importZipStream(stream: InputStream, displayName: String): Flow<ImportProgress>

    /** Import a pack from an existing zip on disk (idempotent); activates on success. */
    fun importZip(file: File): Flow<ImportProgress>

    /** Icon packs already installed on the device (query on IO). */
    suspend fun scanInstalled(): List<DiscoveredIconPack>

    /** Parse + activate a discovered installed pack. */
    suspend fun applyInstalled(discovered: DiscoveredIconPack): IconPackParseResult

    /** Load a pack from a zip synchronously (kept from P1; used by tests/seed). */
    suspend fun loadFromZip(file: File): IconPackParseResult

    suspend fun setActive(pack: IconPack?)
    suspend fun clearActive()
}
