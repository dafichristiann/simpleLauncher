package com.softhome.launcher

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.softhome.core.common.DefaultDispatcherProvider
import com.softhome.core.data.repository.AppRepository
import com.softhome.core.data.repository.BackupRepositoryImpl
import com.softhome.core.data.repository.FolderRepositoryImpl
import com.softhome.core.data.repository.NotesRepositoryImpl
import com.softhome.core.data.repository.PrefsRepositoryImpl
import com.softhome.core.designsystem.theme.SoftHomeTheme
import com.softhome.feature.iconpack.data.IconPackRepositoryImpl
import com.softhome.launcher.settings.SettingsPanel
import com.softhome.launcher.settings.SettingsViewModel
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * P4d evidence: renders the real settings panel (with the new "Backup & restore" section)
 * inside a phone-sized frame and writes a PNG to the device, so verification can pull a
 * real screenshot of the section without scripting the SAF picker over adb.
 *
 * File lands in `context.getExternalFilesDir(null)/p4d-settings-backup.png`.
 */
class BackupSettingsScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun capture_settings_with_backup_section() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val prefs = PrefsRepositoryImpl(context)
        val folders = FolderRepositoryImpl(context)
        val notes = NotesRepositoryImpl(context)
        val backup = BackupRepositoryImpl(context, prefs, folders, notes, DefaultDispatcherProvider())
        val parser = com.softhome.feature.iconpack.data.AppFilterParser()
        val loader = com.softhome.feature.iconpack.data.IconPackDrawableLoader(context)
        val iconPack = IconPackRepositoryImpl(
            context = context,
            parser = parser,
            importer = com.softhome.feature.iconpack.data.IconPackImporter(context, parser, loader, DefaultDispatcherProvider()),
            scanner = com.softhome.feature.iconpack.data.InstalledIconPackScanner(context, parser),
            loader = loader,
            dispatchers = DefaultDispatcherProvider(),
            prefsRepository = prefs,
        )
        val appRepository = object : AppRepository {
            override suspend fun getInstalledApps(): List<com.softhome.core.model.AppInfo> = emptyList()
            override fun launchApp(app: com.softhome.core.model.AppInfo) = Unit
        }
        val vm = SettingsViewModel(prefs, iconPack, appRepository, backup, folders, notes)

        composeRule.setContent {
            SoftHomeTheme {
                Box(Modifier.size(390.dp, 720.dp)) {
                    SettingsPanel(
                        viewModel = vm,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        composeRule.waitForIdle()

        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        val file = File(dir, "p4d-settings-backup.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        // Also copy to a public, teardown-proof location so verification can pull it.
        runCatching {
            val public = File("/sdcard/Download/p4d-settings-backup.png")
            file.copyTo(public, overwrite = true)
            android.util.Log.i("P4D_SCREENSHOT", "copied to ${public.absolutePath}")
        }
    }
}
