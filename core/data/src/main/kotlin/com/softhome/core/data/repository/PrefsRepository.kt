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
import com.softhome.core.model.LauncherPrefs
import com.softhome.core.model.SpacingScale
import com.softhome.core.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
    }

    override val prefs: Flow<LauncherPrefs> = context.launcherDataStore.data.map { p ->
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
            homeRows = HomeRowLogic.sanitize(HomeRowsCodec.decode(p[Keys.HOME_ROWS])),
            spacing = runCatching { SpacingScale.valueOf(p[Keys.SPACING] ?: SpacingScale.Normal.name) }
                .getOrDefault(SpacingScale.Normal),
            hiddenApps = p[Keys.HIDDEN_APPS] ?: emptySet(),
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
