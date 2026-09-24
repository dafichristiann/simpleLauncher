package com.softhome.launcher

import androidx.test.platform.app.InstrumentationRegistry
import com.softhome.core.common.DefaultDispatcherProvider
import com.softhome.core.data.repository.BackupRepositoryImpl
import com.softhome.core.data.repository.BackupResult
import com.softhome.core.data.repository.FolderRepositoryImpl
import com.softhome.core.data.repository.NotesRepositoryImpl
import com.softhome.core.data.repository.PrefsRepositoryImpl
import com.softhome.core.model.Folder
import com.softhome.core.model.GridConfig
import com.softhome.core.model.SpacingScale
import com.softhome.core.model.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * P4d end-to-end (on-device, real DataStore): export the whole launcher state to a real
 * file, wipe it, then restore from that file -- proving the full SAF-shaped round-trip
 * against the real Android DataStore, not a fake.
 */
class BackupRestoreEndToEndTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun prefs() = PrefsRepositoryImpl(context)
    private fun folders() = FolderRepositoryImpl(context)
    private fun notes() = NotesRepositoryImpl(context)

    private fun repo() = BackupRepositoryImpl(
        context = context,
        prefsRepository = prefs(),
        folderRepository = folders(),
        notesRepository = notes(),
        dispatchers = DefaultDispatcherProvider(),
    )

    @Test
    fun export_then_import_restores_state_from_a_real_file() = runBlocking<Unit> {
        // 1. Mutate state.
        prefs().setGrid(GridConfig(columns = 5, rows = 6, iconScale = 1.15f, showLabels = true))
        prefs().setDarkTheme(ThemeMode.Dark)
        prefs().setSpacing(SpacingScale.Roomy)
        folders().save(listOf(Folder("e2e", "E2E", listOf("com.x/A"))))
        notes().setBody("e2e note")

        // 2. Export to a real file.
        val file = File(context.cacheDir, "e2e-backup.json")
        FileOutputStream(file).use { assertEquals(BackupResult.Success, repo().export(it)) }
        assertTrue(file.length() > 0)

        // 3. Wipe.
        prefs().setGrid(GridConfig.Default)
        prefs().setDarkTheme(ThemeMode.System)
        prefs().setSpacing(SpacingScale.Normal)
        folders().save(emptyList())
        notes().setBody("")

        // 4. Restore from the file.
        FileInputStream(file).use { assertEquals(BackupResult.Success, repo().import(it)) }

        // 5. Assert restored.
        val p = prefs().prefs.first()
        assertEquals(5, p.grid.columns)
        assertEquals(ThemeMode.Dark, p.darkTheme)
        assertEquals(SpacingScale.Roomy, p.spacing)
        assertEquals(listOf(Folder("e2e", "E2E", listOf("com.x/A"))), folders().folders.first())
        assertEquals("e2e note", notes().notes.first().body)

        file.delete()
        Unit
    }
}
