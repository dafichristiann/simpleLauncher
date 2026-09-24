package com.softhome.core.model

/**
 * P4d: the whole launcher user state in one versioned, portable document.
 *
 * A backup is a **single JSON document** (encoded by `BackupCodec` in `core:data`) that
 * captures every DataStore-backed preference -- the home rows, spacing, hidden apps,
 * per-app icon overrides, folders, quick notes, theme, and the active icon-pack **id**
 * (not its bytes; see the P4d spec section 2).
 *
 * Nothing here is derived from the device (installed apps, battery, wallpaper): those
 * are re-read on cold start and are deliberately **not** part of a backup.
 *
 * [schemaVersion] lets a future format evolve without breaking old files: a file whose
 * version is **newer** than [CURRENT_VERSION] is refused (graceful), never mis-read.
 */
data class BackupDocument(
    val schemaVersion: Int = CURRENT_VERSION,
    /** When the backup was written (epoch millis); informational only. */
    val exportedAtEpochMs: Long,
    /** The full typed launcher prefs (grid, pack id, theme, rows, spacing, hidden, overrides). */
    val prefs: LauncherPrefs,
    /** The drawer folders (P2). */
    val folders: List<Folder> = emptyList(),
    /** The quick-notes body (P2 / E5). */
    val notes: String = "",
) {
    companion object {
        const val CURRENT_VERSION = 1

        /**
         * Marker written into the file so a foreign/invalid JSON is rejected as
         * `NotABackup` rather than silently decoded into an empty state.
         */
        const val APP_MARKER = "SOFT_HOME"
    }
}
