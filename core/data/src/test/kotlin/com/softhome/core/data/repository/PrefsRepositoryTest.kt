package com.softhome.core.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.softhome.core.model.GridConfig
import com.softhome.core.model.HomeRowKind
import com.softhome.core.model.HomeRowLogic
import com.softhome.core.model.HomeRowPref
import com.softhome.core.model.SpacingScale
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
        repo.setHomeRows(HomeRowLogic.default())
        repo.setSpacing(SpacingScale.Normal)
        repo.setHiddenApps(emptySet())
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

    // --- P3 ---

    @Test
    fun `home rows default to all visible in order`() = runTest {
        val repo = newRepo(); reset(repo)
        val prefs = repo.prefs.first()
        assertThat(prefs.homeRows.map { it.kind }).isEqualTo(HomeRowLogic.DEFAULT_ORDER)
        assertThat(prefs.homeRows.all { it.visible }).isTrue()
    }

    @Test
    fun `home rows visibility and order round-trip`() = runTest {
        val repo = newRepo(); reset(repo)
        var rows = HomeRowLogic.default()
        rows = HomeRowLogic.toggle(rows, HomeRowKind.Notes)  // hide notes
        rows = HomeRowLogic.moveUp(rows, HomeRowKind.Music)  // reorder music
        repo.setHomeRows(rows)
        val got = repo.prefs.first().homeRows
        assertThat(got.first { it.kind == HomeRowKind.Notes }.visible).isFalse()
        // Music moved before Weather (it was after Search originally).
        assertThat(got.map { it.kind }.indexOf(HomeRowKind.Music))
            .isLessThan(HomeRowLogic.DEFAULT_ORDER.indexOf(HomeRowKind.Music))
    }

    @Test
    fun `locked row stays visible even if stored hidden`() = runTest {
        val repo = newRepo(); reset(repo)
        // Write a raw list that hides Time (locked).
        repo.setHomeRows(listOf(HomeRowPref(HomeRowKind.Time, visible = false)))
        val got = repo.prefs.first().homeRows
        assertThat(got.first { it.kind == HomeRowKind.Time }.visible).isTrue()
        // And all other rows are appended.
        assertThat(got.map { it.kind }).containsExactlyElementsIn(HomeRowLogic.DEFAULT_ORDER)
    }

    @Test
    fun `spacing preset persists`() = runTest {
        val repo = newRepo(); reset(repo)
        repo.setSpacing(SpacingScale.Roomy)
        assertThat(repo.prefs.first().spacing).isEqualTo(SpacingScale.Roomy)
    }

    @Test
    fun `hidden apps hide and unhide`() = runTest {
        val repo = newRepo(); reset(repo)
        repo.hideApp("com.a/One")
        repo.hideApp("com.b/Two")
        assertThat(repo.prefs.first().hiddenApps).containsExactly("com.a/One", "com.b/Two")
        repo.unhideApp("com.a/One")
        assertThat(repo.prefs.first().hiddenApps).containsExactly("com.b/Two")
    }

    // --- HomeRowsCodec (pure) ---

    @Test
    fun `codec round-trips and tolerates junk`() {
        val rows = listOf(
            HomeRowPref(HomeRowKind.Time, true),
            HomeRowPref(HomeRowKind.Notes, false),
        )
        assertThat(HomeRowsCodec.decode(HomeRowsCodec.encode(rows))).isEqualTo(rows)

        // Malformed segments (wrong field count) and blank tokens are dropped;
        // an unknown kind is dropped; a non-"1" flag decodes as visible=false.
        assertThat(HomeRowsCodec.decode("Time:1,Bogus:1,Weather,Notes:x,,Date:0"))
            .containsExactly(
                HomeRowPref(HomeRowKind.Time, true),
                HomeRowPref(HomeRowKind.Notes, false),
                HomeRowPref(HomeRowKind.Date, false),
            ).inOrder()
        assertThat(HomeRowsCodec.decode(null)).isEmpty()
        assertThat(HomeRowsCodec.decode("")).isEmpty()
    }
}
