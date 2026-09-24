package com.softhome.feature.iconpack.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.softhome.core.common.DispatcherProvider
import com.softhome.core.data.repository.PrefsRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * P3-5: the active icon pack must survive process death (closes docs/04 #36).
 *
 * A "restart" is proxied by constructing a NEW [IconPackRepositoryImpl] over the same
 * app-private storage + DataStore file. The repo rehydrates the pack whose id was
 * persisted; a stored id with no matching pack is cleared (falls back to auto-mask).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class IconPackPersistenceTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private val dispatchers = object : DispatcherProvider {
        override val io = Dispatchers.Unconfined
        override val default = Dispatchers.Unconfined
        override val main = Dispatchers.Unconfined
    }

    private fun prefs() = PrefsRepositoryImpl(context)

    private fun repo(p: PrefsRepositoryImpl) = IconPackRepositoryImpl(
        context = context,
        parser = AppFilterParser(),
        importer = IconPackImporter(context, AppFilterParser(), IconPackDrawableLoader(context), dispatchers),
        scanner = InstalledIconPackScanner(context, AppFilterParser()),
        loader = IconPackDrawableLoader(context),
        dispatchers = dispatchers,
        prefsRepository = p,
    )

    private val appFilter = """
        <resources>
          <item component="ComponentInfo{com.a/.Main}" drawable="pack_a"/>
        </resources>
    """.trimIndent().toByteArray()

    private fun png(): ByteArray {
        val bmp = android.graphics.Bitmap.createBitmap(2, 2, android.graphics.Bitmap.Config.ARGB_8888)
        bmp.eraseColor(android.graphics.Color.WHITE)
        val out = java.io.ByteArrayOutputStream()
        bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
        return out.toByteArray()
    }

    /** Writes a valid pack zip into app-private `filesDir/iconpacks/` under [name]. */
    private fun storedPack(name: String): File {
        val dir = File(context.filesDir, "iconpacks").apply { mkdirs() }
        val f = File(dir, name)
        ZipOutputStream(f.outputStream()).use { zos ->
            listOf(
                "appfilter.xml" to appFilter,
                "res/drawable-xxhdpi/pack_a.png" to png(),
            ).forEach { (n, bytes) ->
                zos.putNextEntry(ZipEntry(n)); zos.write(bytes); zos.closeEntry()
            }
        }
        return f
    }

    @Test
    fun `persisted pack id rehydrates on a fresh repository`() {
        val stored = storedPack("mypack.zip")
        val id = IconPackRef.idForZip(stored)

        runBlocking { prefs().setActiveIconPack(id) }

        // Fresh repository == proxy for a cold start.
        val repo = repo(prefs())
        val pack = runBlocking { repo.activePack.first { it != null } }
        assertThat(pack).isNotNull()
        assertThat(pack!!.id).isEqualTo(id)
    }

    @Test
    fun `stored id with no matching pack is cleared and falls back to null`() {
        // Ensure no pack zips linger from other tests.
        File(context.filesDir, "iconpacks").listFiles()?.forEach { it.delete() }
        val p = prefs()
        runBlocking { p.setActiveIconPack("ghost_pack") }

        val repo = repo(p)
        // Give the init coroutine a turn, then assert the id was cleared.
        runBlocking {
            repo.activePack.first()  // seed
            kotlinx.coroutines.delay(50)
        }
        assertThat(runBlocking { prefs().prefs.first().activeIconPackId }).isNull()
        assertThat(repo.activePack.value).isNull()
    }

    @Test
    fun `id prefix matches importer convention`() {
        assertThat(IconPackRef.idForZip(File("/x/My Pack.zip"))).isEqualTo("my_pack")
    }
}
