package com.softhome.core.data.repository

import com.softhome.core.model.BackupDocument
import com.softhome.core.model.Folder
import com.softhome.core.model.GridConfig
import com.softhome.core.model.HomeRowLogic
import com.softhome.core.model.LauncherPrefs
import com.softhome.core.model.SpacingScale
import com.softhome.core.model.ThemeMode
import org.json.JSONArray
import org.json.JSONObject

/** P4d: typed outcome of decoding a backup file. Never an exception (P4d-4). */
sealed interface BackupDecodeResult {
    data class Ok(val document: BackupDocument) : BackupDecodeResult
    /** JSON is readable but is not a SOFT / HOME backup (wrong/absent app marker). */
    data object NotABackup : BackupDecodeResult
    /** A SOFT / HOME backup, but written by a newer schema than this build understands. */
    data class UnsupportedVersion(val version: Int) : BackupDecodeResult
    /** JSON is missing required sections or is not parseable at all. */
    data object Malformed : BackupDecodeResult
}

/**
 * P4d: pure codec for the whole launcher state ([BackupDocument]).
 *
 * Format (schema v1):
 * ```json
 * {
 *   "app": "SOFT_HOME",
 *   "version": 1,
 *   "exportedAt": 1758800000000,
 *   "prefs": { "grid": {...}, "activeIconPackId": "...", "maskUnsupportedApps": true,
 *              "darkTheme": "System", "showNotificationBadges": true,
 *              "homeRows": "Time:1,Date:1,...", "spacing": "Normal",
 *              "hiddenApps": ["pkg/Cls"], "iconOverrides": "{...}" },
 *   "folders": "[{\"id\":\"f1\",\"name\":\"Games\",\"apps\":[\"pkg/Cls\"]}]",
 *   "notes": "buy milk"
 * }
 * ```
 *
 * (`folders` and `prefs.homeRows`/`prefs.iconOverrides` are nested **strings** produced by
 * their own codecs, so the on-disk sub-shapes have a single source of truth.)
 *
 * It **reuses the existing item codecs** (`HomeRowsCodec`, `IconOverridesCodec`,
 * `FoldersCodec`) so each sub-shape has exactly one encoder and cannot drift from the
 * DataStore format.
 *
 * [decode] is **total**: it never throws and never returns a half-populated document --
 * a bad file surfaces as [BackupDecodeResult.Malformed] / [BackupDecodeResult.NotABackup]
 * / [BackupDecodeResult.UnsupportedVersion], and the caller must leave state untouched.
 *
 * `org.json` is a JVM-available Android API (used the same way by the sibling codecs), so
 * this object stays unit-testable without a device.
 */
object BackupCodec {

    private const val APP = "app"
    private const val VERSION = "version"
    private const val EXPORTED_AT = "exportedAt"
    private const val PREFS = "prefs"
    private const val FOLDERS = "folders"
    private const val NOTES = "notes"

    private const val GRID = "grid"
    private const val COLUMNS = "columns"
    private const val ROWS = "rows"
    private const val ICON_SCALE = "iconScale"
    private const val SHOW_LABELS = "showLabels"
    private const val ACTIVE_PACK = "activeIconPackId"
    private const val MASK = "maskUnsupportedApps"
    private const val THEME = "darkTheme"
    private const val BADGES = "showNotificationBadges"
    private const val HOME_ROWS = "homeRows"
    private const val SPACING = "spacing"
    private const val HIDDEN = "hiddenApps"
    private const val OVERRIDES = "iconOverrides"

    /** Encodes the document to a single JSON string (deterministic key order). */
    fun encode(doc: BackupDocument): String {
        val root = JSONObject()
        root.put(APP, BackupDocument.APP_MARKER)
        root.put(VERSION, doc.schemaVersion)
        root.put(EXPORTED_AT, doc.exportedAtEpochMs)

        val p = doc.prefs
        val prefs = JSONObject()
        val grid = JSONObject()
        grid.put(COLUMNS, p.grid.columns)
        grid.put(ROWS, p.grid.rows)
        grid.put(ICON_SCALE, p.grid.iconScale.toDouble())
        grid.put(SHOW_LABELS, p.grid.showLabels)
        prefs.put(GRID, grid)
        // null pack id: emit JSON null explicitly so the shape is stable.
        prefs.put(ACTIVE_PACK, p.activeIconPackId ?: JSONObject.NULL)
        prefs.put(MASK, p.maskUnsupportedApps)
        prefs.put(THEME, p.darkTheme.name)
        prefs.put(BADGES, p.showNotificationBadges)
        prefs.put(HOME_ROWS, HomeRowsCodec.encode(p.homeRows))
        prefs.put(SPACING, p.spacing.name)
        prefs.put(HIDDEN, JSONArray(p.hiddenApps.toList().sorted()))
        prefs.put(OVERRIDES, IconOverridesCodec.encode(p.iconOverrides))
        root.put(PREFS, prefs)

        root.put(FOLDERS, FoldersCodec.encode(doc.folders))
        root.put(NOTES, doc.notes)
        return root.toString()
    }

    /**
     * Decodes a backup, returning a typed result. Never throws (P4d-4). Extra/unknown
     * fields are ignored; missing optional fields fall back to their model defaults.
     */
    fun decode(json: String?): BackupDecodeResult {
        if (json.isNullOrBlank()) return BackupDecodeResult.Malformed
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return BackupDecodeResult.Malformed

        // 1. app marker: a foreign JSON object is NotABackup, not a silent empty state.
        if (root.optString(APP) != BackupDocument.APP_MARKER) return BackupDecodeResult.NotABackup

        // 2. version gate: refuse a file from a newer schema.
        val version = root.optInt(VERSION, -1)
        if (version <= 0) return BackupDecodeResult.Malformed
        if (version > BackupDocument.CURRENT_VERSION) {
            return BackupDecodeResult.UnsupportedVersion(version)
        }

        val prefsObj = root.optJSONObject(PREFS) ?: return BackupDecodeResult.Malformed

        val gridObj = prefsObj.optJSONObject(GRID)
        val default = GridConfig.Default
        val grid = runCatching {
            GridConfig(
                columns = gridObj?.optInt(COLUMNS, default.columns) ?: default.columns,
                rows = gridObj?.optInt(ROWS, default.rows) ?: default.rows,
                iconScale = gridObj?.optDouble(ICON_SCALE, default.iconScale.toDouble())
                    ?.toFloat() ?: default.iconScale,
                showLabels = gridObj?.optBoolean(SHOW_LABELS, default.showLabels) ?: default.showLabels,
            )
        }.getOrElse { default }

        val prefs = LauncherPrefs(
            grid = grid,
            activeIconPackId = if (prefsObj.isNull(ACTIVE_PACK)) {
                null
            } else {
                prefsObj.optString(ACTIVE_PACK).takeIf { it.isNotBlank() }
            },
            maskUnsupportedApps = prefsObj.optBoolean(MASK, true),
            darkTheme = enumOr(prefsObj.optString(THEME), ThemeMode.System, ThemeMode.entries),
            showNotificationBadges = prefsObj.optBoolean(BADGES, true),
            homeRows = HomeRowLogic.sanitize(HomeRowsCodec.decode(prefsObj.optString(HOME_ROWS))),
            spacing = enumOr(prefsObj.optString(SPACING), SpacingScale.Normal, SpacingScale.entries),
            hiddenApps = optStringSet(prefsObj.optJSONArray(HIDDEN)),
            iconOverrides = IconOverridesCodec.decode(prefsObj.optString(OVERRIDES)),
        )

        // Folders were encoded as a JSON *string* (reusing FoldersCodec); decode that string.
        val folders: List<Folder> = FoldersCodec.decode(root.optString(FOLDERS))

        val doc = BackupDocument(
            schemaVersion = version,
            exportedAtEpochMs = root.optLong(EXPORTED_AT, 0L),
            prefs = prefs,
            folders = folders,
            notes = root.optString(NOTES, ""),
        )
        return BackupDecodeResult.Ok(doc)
    }

    private fun optStringSet(array: JSONArray?): Set<String> {
        if (array == null) return emptySet()
        val out = LinkedHashSet<String>(array.length())
        for (i in 0 until array.length()) {
            array.optString(i).takeIf { it.isNotBlank() }?.let(out::add)
        }
        return out
    }

    private inline fun <reified E : Enum<E>> enumOr(name: String, fallback: E, all: List<E>): E =
        all.firstOrNull { it.name == name } ?: fallback
}
