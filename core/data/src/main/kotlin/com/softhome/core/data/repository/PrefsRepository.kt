package com.softhome.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.softhome.core.model.GridConfig
import com.softhome.core.model.HomeRowKind
import com.softhome.core.model.HomeRowLogic
import com.softhome.core.model.HomeRowPref
import com.softhome.core.model.IconOverride
import com.softhome.core.model.DrawerIconTokenName
import com.softhome.core.model.LauncherPrefs
import com.softhome.core.model.RailOrderLogic
import com.softhome.core.model.RailConfigLogic
import com.softhome.core.model.RailItemId
import com.softhome.core.model.RailItemIdCodec
import com.softhome.core.model.SpacingScale
import com.softhome.core.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton

private val Context.launcherDataStore: DataStore<Preferences> by preferencesDataStore(name = "soft_home_prefs")

/** Persisted launcher preferences (docs/01-ARCHITECTURE.md -- DataStore). */
interface PrefsRepository {
    val prefs: Flow<LauncherPrefs>
    suspend fun setGrid(grid: GridConfig)
    suspend fun setActiveIconPack(packId: String?)
    suspend fun setMaskUnsupported(mask: Boolean)
    suspend fun setDarkTheme(mode: ThemeMode)
    suspend fun setShowBadges(show: Boolean)
    /** P3 (G/Widgets): persist home row visibility + order. */
    suspend fun setHomeRows(rows: List<HomeRowPref>)
    /** P3 (G/Appearance): persist the spacing preset (Q3). */
    suspend fun setSpacing(scale: SpacingScale)
    /** P3 (Q2): persist the drawer hidden-apps set. */
    suspend fun setHiddenApps(keys: Set<String>)
    /** P3 (Q2): hide a single app from the drawer. */
    suspend fun hideApp(componentKey: String)
    /** P3 (Q2): restore a hidden app. */
    suspend fun unhideApp(componentKey: String)
    /** P4b: set (or clear, with null) the explicit icon override for one app. */
    suspend fun setIconOverride(componentKey: String, override: IconOverride?)
    /** P4d: replace ALL persisted prefs in a single write (used by backup restore). */
    suspend fun applyAll(prefs: LauncherPrefs)
    /**
     * Rewrite only the fields that carry a per-app `componentKey` (hidden set, icon
     * overrides, rail items) in a single write. Used by the dead-reference prune when an
     * app is uninstalled; other fields are left untouched.
     */
    suspend fun applyPruned(prefs: LauncherPrefs)
    /** P7: persist the user-defined right-rail order. */
    suspend fun setRailOrder(order: List<String>) { }
    /** Unified rail configuration; this is the canonical write path. */
    suspend fun setRailItems(items: List<RailItemId>) { }
}

@Singleton
class PrefsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : PrefsRepository {

    private object Keys {
        val COLUMNS = intPreferencesKey("grid_columns")
        val ROWS = intPreferencesKey("grid_rows")
        val ICON_SCALE = intPreferencesKey("grid_icon_scale_x100")
        val SHOW_LABELS = booleanPreferencesKey("grid_show_labels")
        val ICON_PACK = stringPreferencesKey("active_icon_pack")
        val MASK = booleanPreferencesKey("mask_unsupported")
        val THEME = stringPreferencesKey("theme_mode")
        val BADGES = booleanPreferencesKey("show_badges")
        // --- P3 ---
        val HOME_ROWS = stringPreferencesKey("home_rows")
        val SPACING = stringPreferencesKey("spacing_scale")
        val HIDDEN_APPS = stringSetPreferencesKey("hidden_apps")
        // --- P4b ---
        val ICON_OVERRIDES = stringPreferencesKey("icon_overrides_json")
        val RAIL_ORDER = stringPreferencesKey("rail_order")
        val RAIL_ITEMS_V2 = stringPreferencesKey("rail_items_v2")
    }

    /**
     * P2: one-shot guard so the legacy home-row migration is applied (and persisted) at
     * most once per repository instance. A `@Singleton` repo means once per process.
     */
    private val legacyRowMigrationDone = java.util.concurrent.atomic.AtomicBoolean(false)
    private val legacyRailMigrationDone = java.util.concurrent.atomic.AtomicBoolean(false)

    /**
     * P2 (Q1 = apply-once): if the backing store still holds the **legacy all-visible**
     * home-row shape, rewrite it to the new lean default exactly once. Runs as a prefix of
     * the [prefs] flow, before the first value is delivered, so the first emission already
     * reflects the migration. A real user choice (any hidden row / reorder) is never the
     * legacy shape, so it is left untouched.
     */
    private suspend fun migrateLegacyHomeRowsOnce() {
        if (!legacyRowMigrationDone.compareAndSet(false, true)) return
        context.launcherDataStore.edit { p ->
            val stored = HomeRowsCodec.decode(p[Keys.HOME_ROWS])
            val newRows = HomeRowLogic.migrateLegacy(stored)
            if (newRows != null) {
                p[Keys.HOME_ROWS] = HomeRowsCodec.encode(HomeRowLogic.sanitize(newRows))
            }
        }
    }

    private suspend fun migrateLegacyRailOnce() {
        if (!legacyRailMigrationDone.compareAndSet(false, true)) return
        context.launcherDataStore.edit { p ->
            if (!p.contains(Keys.RAIL_ITEMS_V2)) {
                val migrated = RailConfigLogic.fromLegacyShortcutNames(
                    p[Keys.RAIL_ORDER]?.split(',').orEmpty(),
                )
                p[Keys.RAIL_ITEMS_V2] = RailItemIdCodec.encode(migrated)
            }
        }
    }

    override val prefs: Flow<LauncherPrefs> = context.launcherDataStore.data
        .onStart {
            migrateLegacyHomeRowsOnce()
            migrateLegacyRailOnce()
        }
        .map { p ->
            val default = GridConfig.Default
            LauncherPrefs(
                grid = GridConfig(
                    columns = p[Keys.COLUMNS] ?: default.columns,
                    rows = p[Keys.ROWS] ?: default.rows,
                    iconScale = (p[Keys.ICON_SCALE] ?: (default.iconScale * 100).toInt()) / 100f,
                    showLabels = p[Keys.SHOW_LABELS] ?: default.showLabels,
                ),
                activeIconPackId = p[Keys.ICON_PACK],
                maskUnsupportedApps = p[Keys.MASK] ?: true,
                darkTheme = runCatching { ThemeMode.valueOf(p[Keys.THEME] ?: ThemeMode.System.name) }
                    .getOrDefault(ThemeMode.System),
                showNotificationBadges = p[Keys.BADGES] ?: true,
                // The one-shot migration (onStart, above) has already rewritten a legacy
                // all-visible list in the store; reading it here yields the new default.
                homeRows = HomeRowLogic.sanitize(HomeRowsCodec.decode(p[Keys.HOME_ROWS])),
                spacing = runCatching { SpacingScale.valueOf(p[Keys.SPACING] ?: SpacingScale.Normal.name) }
                    .getOrDefault(SpacingScale.Normal),
                hiddenApps = p[Keys.HIDDEN_APPS] ?: emptySet(),
                iconOverrides = IconOverridesCodec.decode(p[Keys.ICON_OVERRIDES]),
                railOrder = legacyShortcutNames(
                    RailConfigLogic.sanitize(
                        if (p.contains(Keys.RAIL_ITEMS_V2)) {
                            RailItemIdCodec.decode(p[Keys.RAIL_ITEMS_V2])
                        } else {
                            RailConfigLogic.fromLegacyShortcutNames(p[Keys.RAIL_ORDER]?.split(','))
                        },
                    ),
                ),
                railItems = RailConfigLogic.sanitize(
                    if (p.contains(Keys.RAIL_ITEMS_V2)) {
                        RailItemIdCodec.decode(p[Keys.RAIL_ITEMS_V2])
                    } else {
                        RailConfigLogic.fromLegacyShortcutNames(p[Keys.RAIL_ORDER]?.split(','))
                    },
                ),
            )
        }

    override suspend fun setGrid(grid: GridConfig) {
        context.launcherDataStore.edit { p ->
            p[Keys.COLUMNS] = grid.columns
            p[Keys.ROWS] = grid.rows
            p[Keys.ICON_SCALE] = (grid.iconScale * 100).toInt()
            p[Keys.SHOW_LABELS] = grid.showLabels
        }
    }

    override suspend fun setActiveIconPack(packId: String?) {
        context.launcherDataStore.edit { p ->
            if (packId == null) p.remove(Keys.ICON_PACK) else p[Keys.ICON_PACK] = packId
        }
    }

    override suspend fun setMaskUnsupported(mask: Boolean) {
        context.launcherDataStore.edit { it[Keys.MASK] = mask }
    }

    override suspend fun setDarkTheme(mode: ThemeMode) {
        context.launcherDataStore.edit { it[Keys.THEME] = mode.name }
    }

    override suspend fun setShowBadges(show: Boolean) {
        context.launcherDataStore.edit { it[Keys.BADGES] = show }
    }

    override suspend fun setHomeRows(rows: List<HomeRowPref>) {
        val encoded = HomeRowsCodec.encode(rows)
        context.launcherDataStore.edit { it[Keys.HOME_ROWS] = encoded }
    }

    override suspend fun setSpacing(scale: SpacingScale) {
        context.launcherDataStore.edit { it[Keys.SPACING] = scale.name }
    }

    override suspend fun setHiddenApps(keys: Set<String>) {
        context.launcherDataStore.edit { it[Keys.HIDDEN_APPS] = keys }
    }

    override suspend fun hideApp(componentKey: String) {
        context.launcherDataStore.edit { p ->
            p[Keys.HIDDEN_APPS] = (p[Keys.HIDDEN_APPS] ?: emptySet()) + componentKey
        }
    }

    override suspend fun unhideApp(componentKey: String) {
        context.launcherDataStore.edit { p ->
            p[Keys.HIDDEN_APPS] = (p[Keys.HIDDEN_APPS] ?: emptySet()) - componentKey
        }
    }

    override suspend fun setIconOverride(componentKey: String, override: IconOverride?) {
        context.launcherDataStore.edit { p ->
            val current = IconOverridesCodec.decode(p[Keys.ICON_OVERRIDES]).toMutableMap()
            if (override == null) current.remove(componentKey)
            else current[componentKey] = override
            if (current.isEmpty()) {
                p.remove(Keys.ICON_OVERRIDES)
            } else {
                p[Keys.ICON_OVERRIDES] = IconOverridesCodec.encode(current)
            }
        }
    }

    override suspend fun setRailOrder(order: List<String>) {
        setRailItems(RailConfigLogic.fromLegacyShortcutNames(order))
    }

    override suspend fun setRailItems(items: List<RailItemId>) {
        val sanitized = RailConfigLogic.sanitize(items)
        context.launcherDataStore.edit { p ->
            p[Keys.RAIL_ITEMS_V2] = RailItemIdCodec.encode(sanitized)
            // Keep the old key as a readable compatibility view for one upgrade window.
            p[Keys.RAIL_ORDER] = legacyShortcutNames(sanitized).joinToString(",")
        }
    }

    /**
     * P4d: write every pref key in **one** `edit` block, so a restore produces a single
     * DataStore write (and one reactive `prefs` emission) rather than N. Mirrors the
     * encode used by the read path exactly, so encode/decode stay symmetric.
     */
    override suspend fun applyAll(prefs: LauncherPrefs) {
        context.launcherDataStore.edit { p ->
            p[Keys.COLUMNS] = prefs.grid.columns
            p[Keys.ROWS] = prefs.grid.rows
            p[Keys.ICON_SCALE] = (prefs.grid.iconScale * 100).toInt()
            p[Keys.SHOW_LABELS] = prefs.grid.showLabels

            val packId = prefs.activeIconPackId
            if (packId == null) p.remove(Keys.ICON_PACK)
            else p[Keys.ICON_PACK] = packId

            p[Keys.MASK] = prefs.maskUnsupportedApps
            p[Keys.THEME] = prefs.darkTheme.name
            p[Keys.BADGES] = prefs.showNotificationBadges
            p[Keys.HOME_ROWS] = HomeRowsCodec.encode(HomeRowLogic.sanitize(prefs.homeRows))
            p[Keys.SPACING] = prefs.spacing.name
            p[Keys.HIDDEN_APPS] = prefs.hiddenApps

            if (prefs.iconOverrides.isEmpty()) p.remove(Keys.ICON_OVERRIDES)
            else p[Keys.ICON_OVERRIDES] = IconOverridesCodec.encode(prefs.iconOverrides)
            val railItems = RailConfigLogic.sanitize(
                if (prefs.railItems != RailConfigLogic.DEFAULT_ITEMS) prefs.railItems
                else RailConfigLogic.fromLegacyShortcutNames(prefs.railOrder),
            )
            p[Keys.RAIL_ITEMS_V2] = RailItemIdCodec.encode(railItems)
            p[Keys.RAIL_ORDER] = legacyShortcutNames(railItems).joinToString(",")
        }
    }

    /**
     * Rewrite only the per-app-keyed fields (hidden set, icon overrides, rail items) in a
     * single `edit` — used by the dead-reference prune when an app is uninstalled. All
     * other keys are intentionally left as-is.
     */
    override suspend fun applyPruned(prefs: LauncherPrefs) {
        context.launcherDataStore.edit { p ->
            if (prefs.hiddenApps.isEmpty()) p.remove(Keys.HIDDEN_APPS)
            else p[Keys.HIDDEN_APPS] = prefs.hiddenApps

            if (prefs.iconOverrides.isEmpty()) p.remove(Keys.ICON_OVERRIDES)
            else p[Keys.ICON_OVERRIDES] = IconOverridesCodec.encode(prefs.iconOverrides)

            val railItems = RailConfigLogic.sanitize(prefs.railItems)
            p[Keys.RAIL_ITEMS_V2] = RailItemIdCodec.encode(railItems)
            p[Keys.RAIL_ORDER] = legacyShortcutNames(railItems).joinToString(",")
        }
    }

    private fun legacyShortcutNames(items: List<RailItemId>): List<String> =
        items.mapNotNull { (it as? RailItemId.System)?.shortcut?.name }
}

/**
 * Pure (no Android) codec for the home row preference list.
 *
 * Format: `KIND:1,KIND:0,...` (1 = visible). Decoding is total: unknown tokens and
 * malformed segments are skipped; [HomeRowLogic.sanitize] then repairs the list.
 */
object HomeRowsCodec {

    fun encode(rows: List<HomeRowPref>): String =
        rows.joinToString(",") { "${it.kind.name}:${if (it.visible) 1 else 0}" }

    fun decode(raw: String?): List<HomeRowPref> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(',').mapNotNull { part ->
            val bits = part.split(':')
            if (bits.size != 2) return@mapNotNull null
            val kind = HomeRowKind.entries.firstOrNull { it.name == bits[0].trim() }
                ?: return@mapNotNull null
            HomeRowPref(kind, visible = bits[1].trim() == "1")
        }
    }
}

/**
 * P4b: pure (Android-free apart from org.json) codec for per-app [IconOverride]s.
 *
 * Format: a JSON object `{ "pkg/Class": {"type":"pack","name":"drawable"} }` or
 * `{ ..., "type":"glyph","symbol":"Phone","color":"Communication" }`. Decoding is
 * **total**: unknown `type`s, unknown color tokens, blank names, and malformed JSON are
 * dropped (never throws) -- so a bad/legacy value degrades to "automatic", not a crash.
 *
 * Migration (P4b-11): the previous storage shape was `componentKey -> packId`. Values
 * that are not the new object shape are skipped here; a stale old value simply yields
 * no override (the app falls back to the P3.5 hybrid). Nothing is lost that the old
 * value could actually express (the old value could not pick a drawable -- see spec §1).
 */
object IconOverridesCodec {

    private const val TYPE = "type"
    private const val TYPE_PACK = "pack"
    private const val TYPE_GLYPH = "glyph"
    private const val NAME = "name"
    private const val SYMBOL = "symbol"
    private const val COLOR = "color"

    fun encode(overrides: Map<String, IconOverride>): String {
        val obj = JSONObject()
        overrides.forEach { (key, override) ->
            if (key.isBlank()) return@forEach
            val value = JSONObject()
            when (override) {
                is IconOverride.Pack -> {
                    if (override.drawableName.isBlank()) return@forEach
                    value.put(TYPE, TYPE_PACK)
                    value.put(NAME, override.drawableName)
                }
                is IconOverride.Glyph -> {
                    if (override.symbolName.isBlank()) return@forEach
                    value.put(TYPE, TYPE_GLYPH)
                    value.put(SYMBOL, override.symbolName)
                    value.put(COLOR, override.colorToken.name)
                }
            }
            obj.put(key, value)
        }
        return obj.toString()
    }

    /** Decodes overrides, dropping anything unrecognised (never throws). */
    fun decode(json: String?): Map<String, IconOverride> {
        if (json.isNullOrBlank()) return emptyMap()
        return runCatching {
            val obj = JSONObject(json)
            buildMap {
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    if (key.isBlank()) continue
                    val value = obj.optJSONObject(key) ?: continue
                    when (value.optString(TYPE)) {
                        TYPE_PACK ->
                            value.optString(NAME).takeIf { it.isNotBlank() }
                                ?.let { put(key, IconOverride.Pack(it)) }

                        TYPE_GLYPH -> {
                            val symbol = value.optString(SYMBOL).takeIf { it.isNotBlank() }
                            val token = DrawerIconTokenName.entries
                                .firstOrNull { it.name == value.optString(COLOR) }
                            if (symbol != null && token != null) {
                                put(key, IconOverride.Glyph(symbol, token))
                            }
                        }

                        else -> Unit // unknown/legacy type -> no override
                    }
                }
            }
        }.getOrDefault(emptyMap())
    }
}
