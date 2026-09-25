package com.softhome.core.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.softhome.core.common.DefaultDispatcherProvider
import com.softhome.core.model.DrawerIconTokenName
import com.softhome.core.model.Folder
import com.softhome.core.model.GridConfig
import com.softhome.core.model.HomeRowKind
import com.softhome.core.model.HomeRowLogic
import com.softhome.core.model.IconOverride
import com.softhome.core.model.SpacingScale
import com.softhome.core.model.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * P4d: end-to-end export/import against **real** DataStores (Robolectric), the same bar
 * as `PrefsRepositoryTest`. Proves the round-trip restores state, and that a bad file
 * leaves state untouched.
 */
@RunWith(RobolectricTestRunner::class)
class BackupRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun prefs() = PrefsRepositoryImpl(context)
    private fun folders() = FolderRepositoryImpl(context)
    private fun notes() = NotesRepositoryImpl(context)

    private fun repo(): BackupRepositoryImpl = BackupRepositoryImpl(
        context = context,
        prefsRepository = prefs(),
        folderRepository = folders(),
        notesRepository = notes(),
        dispatchers = DefaultDispatcherProvider(),
    )

    /** Put every store back to a known empty-ish state (files are shared per process). */
    private suspend fun reset() {
        prefs().applyAll(
            com.softhome.core.model.LauncherPrefs(
                iconOverrides = emptyMap(),
                hiddenApps = emptySet(),
            ),
        )
        folders().save(emptyList())
        notes().setBody("")
    }

    @Test
    fun `export then import restores state`() = runTest {
        reset()

        // 1. Mutate a representative slice of every store.
        prefs().setGrid(GridConfig(columns = 6, rows = 7, iconScale = 1.3f, showLabels = true))
        prefs().setDarkTheme(ThemeMode.Dark)
        prefs().setSpacing(SpacingScale.Roomy)
        prefs().setActiveIconPack("supa_pack")
        prefs().hideApp("com.hidden/One")
        prefs().setIconOverride("com.a/Main", IconOverride.Glyph("Phone", DrawerIconTokenName.Media))
        prefs().setHomeRows(HomeRowLogic.toggle(HomeRowLogic.default(), HomeRowKind.Notes))
        folders().save(listOf(Folder("f1", "Games", listOf("com.g/A"))))
        notes().setBody("remember the milk")

        // 2. Export to a byte buffer.
        val out = ByteArrayOutputStream()
        assertThat(repo().export(out)).isEqualTo(BackupResult.Success)
        val bytes = out.toByteArray()
        assertThat(bytes).isNotEmpty()

        // 3. Wipe everything.
        reset()
        assertThat(prefs().prefs.first().grid.columns).isEqualTo(4)
        assertThat(folders().folders.first()).isEmpty()
        assertThat(notes().notes.first().body).isEmpty()

        // 4. Import the buffer back.
        val result = repo().import(ByteArrayInputStream(bytes))
        assertThat(result).isEqualTo(BackupResult.Success)

        // 5. Everything came back.
        val restored = prefs().prefs.first()
        assertThat(restored.grid.columns).isEqualTo(6)
        assertThat(restored.grid.showLabels).isTrue()
        assertThat(restored.darkTheme).isEqualTo(ThemeMode.Dark)
        assertThat(restored.spacing).isEqualTo(SpacingScale.Roomy)
        assertThat(restored.activeIconPackId).isEqualTo("supa_pack")
        assertThat(restored.hiddenApps).containsExactly("com.hidden/One")
        assertThat(restored.iconOverrides["com.a/Main"])
            .isEqualTo(IconOverride.Glyph("Phone", DrawerIconTokenName.Media))
        assertThat(restored.homeRows.first { it.kind == HomeRowKind.Notes }.visible).isTrue()
        assertThat(folders().folders.first()).isEqualTo(listOf(Folder("f1", "Games", listOf("com.g/A"))))
        assertThat(notes().notes.first().body).isEqualTo("remember the milk")
    }

    @Test
    fun `importing a non-backup file fails and leaves state untouched`() = runTest {
        reset()
        prefs().setDarkTheme(ThemeMode.Dark)
        val before = prefs().prefs.first()

        val result = repo().import(ByteArrayInputStream("{\"hello\":\"world\"}".toByteArray()))
        assertThat(result).isEqualTo(BackupResult.Failure(BackupResult.FailureReason.NotABackup))
        // Nothing changed.
        assertThat(prefs().prefs.first().darkTheme).isEqualTo(before.darkTheme)
    }

    @Test
    fun `importing garbage fails as malformed and leaves state untouched`() = runTest {
        reset()
        prefs().setSpacing(SpacingScale.Compact)
        val result = repo().import(ByteArrayInputStream("!!! not json".toByteArray()))
        assertThat(result).isEqualTo(BackupResult.Failure(BackupResult.FailureReason.Malformed))
        assertThat(prefs().prefs.first().spacing).isEqualTo(SpacingScale.Compact)
    }
}
