package com.softhome.core.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.softhome.core.model.GridConfig
import com.softhome.core.model.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PrefsRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun newRepo(): PrefsRepositoryImpl = PrefsRepositoryImpl(context)

    /** Reset to defaults (DataStore file is shared across tests in one process). */
    private suspend fun reset(repo: PrefsRepositoryImpl) {
        repo.setGrid(GridConfig.Default)
        repo.setDarkTheme(ThemeMode.System)
        repo.setActiveIconPack(null)
        repo.setMaskUnsupported(true)
        repo.setShowBadges(true)
    }

    @Test
    fun `defaults round-trip`() = runTest {
        val repo = newRepo(); reset(repo)
        val prefs = repo.prefs.first()
        assertThat(prefs.grid.columns).isEqualTo(4)
        assertThat(prefs.maskUnsupportedApps).isTrue()
    }

    @Test
    fun `saving grid persists`() = runTest {
        val repo = newRepo(); reset(repo)
        repo.setGrid(GridConfig(columns = 5, rows = 6, iconScale = 1.2f, showLabels = true))
        val prefs = repo.prefs.first()
        assertThat(prefs.grid.columns).isEqualTo(5)
        assertThat(prefs.grid.rows).isEqualTo(6)
        assertThat(prefs.grid.showLabels).isTrue()
        assertThat(prefs.grid.iconScale).isWithin(0.001f).of(1.2f)
    }

    @Test
    fun `theme and pack persist`() = runTest {
        val repo = newRepo(); reset(repo)
        repo.setDarkTheme(ThemeMode.Dark)
        repo.setActiveIconPack("whicons")
        repo.setMaskUnsupported(false)
        val prefs = repo.prefs.first()
        assertThat(prefs.darkTheme).isEqualTo(ThemeMode.Dark)
        assertThat(prefs.activeIconPackId).isEqualTo("whicons")
        assertThat(prefs.maskUnsupportedApps).isFalse()
    }
}
