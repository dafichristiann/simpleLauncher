package com.softhome.core.data.repository

import android.content.Context
import com.softhome.core.common.DispatcherProvider
import com.softhome.core.model.BackupDocument
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

/** P4d: typed outcome of an export/import. Never throws across this boundary. */
sealed interface BackupResult {
    data object Success : BackupResult

    /** A human-readable, user-facing reason (already action-able copy is chosen by the UI). */
    data class Failure(val reason: FailureReason) : BackupResult

    enum class FailureReason {
        /** The picked file is not a SOFT / HOME backup. */
        NotABackup,

        /** The backup was written by a newer schema than this build understands. */
        UnsupportedVersion,

        /** The file could not be parsed / read / written. */
        Malformed,

        /** A storage error (open/read/write failed). */
        IoError,
    }
}

/**
 * P4d: export/import the whole launcher user state as one JSON document (see the P4d spec).
 *
 * Both methods are **total and non-destructive on failure**: [import] validates before it
 * writes anything, and a bad file leaves the current state exactly as it was (P4d-4).
 */
interface BackupRepository {
    /** Snapshot the current state and write it to [out]. */
    suspend fun export(out: OutputStream): BackupResult

    /** Read [input], validate, and (only if valid) replace all state. */
    suspend fun import(input: InputStream): BackupResult
}

@Singleton
class BackupRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefsRepository: PrefsRepository,
    private val folderRepository: FolderRepository,
    private val notesRepository: NotesRepository,
    private val dispatchers: DispatcherProvider,
) : BackupRepository {

    override suspend fun export(out: OutputStream): BackupResult = withContext(dispatchers.io) {
        try {
            val document = BackupDocument(
                exportedAtEpochMs = System.currentTimeMillis(),
                prefs = prefsRepository.prefs.first(),
                folders = folderRepository.folders.first(),
                notes = notesRepository.notes.first().body,
            )
            out.write(BackupCodec.encode(document).toByteArray(Charsets.UTF_8))
            out.flush()
            BackupResult.Success
        } catch (t: Throwable) {
            BackupResult.Failure(BackupResult.FailureReason.IoError)
        }
    }

    override suspend fun import(input: InputStream): BackupResult = withContext(dispatchers.io) {
        // 1. Read + decode FIRST -- nothing is written until the document is valid.
        val json = try {
            input.use { it.readBytes().toString(Charsets.UTF_8) }
        } catch (t: Throwable) {
            return@withContext BackupResult.Failure(BackupResult.FailureReason.IoError)
        }

        when (val decoded = BackupCodec.decode(json)) {
            is BackupDecodeResult.NotABackup ->
                return@withContext BackupResult.Failure(BackupResult.FailureReason.NotABackup)

            is BackupDecodeResult.UnsupportedVersion ->
                return@withContext BackupResult.Failure(BackupResult.FailureReason.UnsupportedVersion)

            is BackupDecodeResult.Malformed ->
                return@withContext BackupResult.Failure(BackupResult.FailureReason.Malformed)

            is BackupDecodeResult.Ok -> {
                // 2. Apply. Prefs in one write; folders + notes in one each.
                try {
                    prefsRepository.applyAll(decoded.document.prefs)
                    folderRepository.save(decoded.document.folders)
                    notesRepository.setBody(decoded.document.notes)
                } catch (t: Throwable) {
                    return@withContext BackupResult.Failure(BackupResult.FailureReason.IoError)
                }
            }
        }
        BackupResult.Success
    }
}
