package com.softhome.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.softhome.core.model.GridConfig
import com.softhome.core.model.LauncherPrefs
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
}
