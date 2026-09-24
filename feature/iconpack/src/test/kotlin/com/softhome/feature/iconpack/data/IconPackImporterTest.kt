package com.softhome.feature.iconpack.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.softhome.core.common.DispatcherProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * P1.5 importer tests. Exercises the real zip path (Robolectric gives us a Context):
 *  - a valid pack imports, reports progress and a Done carrying the parsed pack
 *  - a zip with no drawable folder fails gracefully
 *  - a non-zip / corrupt file fails gracefully
 *  - an appfilter with no readable icons fails gracefully
 *  - a malformed appfilter degrades to a pack that still masks (never a crash)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class IconPackImporterTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private val testDispatchers = object : DispatcherProvider {
        override val io = Dispatchers.Unconfined
        override val default = Dispatchers.Unconfined
        override val main = Dispatchers.Unconfined
    }

    private fun importer() = IconPackImporter(
        context = context,
        parser = AppFilterParser(),
        loader = IconPackDrawableLoader(context),
        dispatchers = testDispatchers,
    )

    private fun zipOf(vararg entries: Pair<String, ByteArray>): File {
        val f = File.createTempFile("pack_", ".zip", context.cacheDir)
        ZipOutputStream(f.outputStream()).use { zos ->
            entries.forEach { (name, bytes) ->
                zos.putNextEntry(ZipEntry(name))
                zos.write(bytes)
                zos.closeEntry()
            }
        }
        return f
    }

    private val appFilter = """
        <resources>
          <item component="ComponentInfo{com.a/.Main}" drawable="pack_a"/>
        </resources>
    """.trimIndent().toByteArray()

    @Test
    fun `valid pack imports and reports Done with the parsed pack`() = runTest {
        val zip = zipOf(
            "appfilter.xml" to appFilter,
            "res/drawable-xxhdpi/pack_a.png" to pngBytes(),
        )
        val progress = importer().importZipFile(zip, "MyPack.zip", copyIntoStore = false).toList()
        val done = progress.filterIsInstance<ImportProgress.Done>().firstOrNull()
        assertThat(done).isNotNull()
        assertThat(done!!.pack.entries["com.a/com.a.Main"]).isEqualTo("pack_a")
        assertThat(done.pack.sourceKind).isEqualTo(com.softhome.core.model.IconPack.SourceKind.Zip)
        // drawable verification progress is emitted for a pack with an appfilter.
        assertThat(progress.filterIsInstance<ImportProgress.Indexing>()).isNotEmpty()
    }

    @Test
    fun `zip without a drawable folder fails gracefully`() = runTest {
        val zip = zipOf("appfilter.xml" to appFilter, "readme.txt" to "nope".toByteArray())
        val progress = importer().importZipFile(zip, "Bad.zip", copyIntoStore = false).toList()
        assertThat(progress.filterIsInstance<ImportProgress.Failed>()).isNotEmpty()
        assertThat(progress.filterIsInstance<ImportProgress.Done>()).isEmpty()
    }

    @Test
    fun `non-zip file fails gracefully`() = runTest {
        val f = File.createTempFile("notzip_", ".zip", context.cacheDir)
        f.writeBytes("this is not a zip".toByteArray())
        val progress = importer().importZipFile(f, "Nope.zip", copyIntoStore = false).toList()
        assertThat(progress.filterIsInstance<ImportProgress.Failed>()).isNotEmpty()
    }

    @Test
    fun `missing file fails gracefully`() = runTest {
        val progress = importer()
            .importZipFile(File(context.cacheDir, "does_not_exist.zip"), "x.zip", copyIntoStore = false)
            .toList()
        assertThat(progress.single()).isInstanceOf(ImportProgress.Failed::class.java)
    }

    @Test
    fun `appfilter with no readable icons fails gracefully`() = runTest {
        // appfilter references pack_a, but no drawable for it exists -> invalid.
        val zip = zipOf(
            "appfilter.xml" to appFilter,
            "res/drawable-xxhdpi/other.png" to pngBytes(),
        )
        val progress = importer().importZipFile(zip, "Ghost.zip", copyIntoStore = false).toList()
        assertThat(progress.filterIsInstance<ImportProgress.Failed>()).isNotEmpty()
    }

    @Test
    fun `pack with no appfilter still imports and can mask`() = runTest {
        val zip = zipOf("res/drawable-xxhdpi/pack_a.png" to pngBytes())
        val progress = importer().importZipFile(zip, "MaskOnly.zip", copyIntoStore = false).toList()
        val done = progress.filterIsInstance<ImportProgress.Done>().firstOrNull()
        assertThat(done).isNotNull()
        assertThat(done!!.pack.hasAppFilter).isFalse()
    }

    @Test
    fun `malformed appfilter degrades to a non-crashing result`() = runTest {
        // A pack whose appfilter references a real drawable but is otherwise messy.
        val messy = "<resources><item component=\"ComponentInfo{com.a/.Main}\" drawable=\"pack_a\"".toByteArray()
        val zip = zipOf(
            "appfilter.xml" to messy,
            "res/drawable-xxhdpi/pack_a.png" to pngBytes(),
        )
        // Must not throw; either Done (lenient parse) or Failed - never an exception.
        val progress = importer().importZipFile(zip, "Messy.zip", copyIntoStore = false).toList()
        assertThat(progress).isNotEmpty()
    }

    @Test
    fun `import into store writes the pack into app private storage`() = runTest {
        val zip = zipOf(
            "appfilter.xml" to appFilter,
            "res/drawable-xxhdpi/pack_a.png" to pngBytes(),
        )
        val progress = importer().importZipFile(zip, "Stored.zip", copyIntoStore = true).toList()
        val done = progress.filterIsInstance<ImportProgress.Done>().firstOrNull()
        assertThat(done).isNotNull()
        // The pack's source path must point at the stored copy inside app storage.
        val sourcePath = (done!!.pack.source as com.softhome.core.model.IconPackSource.Zip).zipPath
        assertThat(File(sourcePath).exists()).isTrue()
        assertThat(sourcePath).contains("iconpacks")
    }

    /** A 2x2 valid PNG, so BitmapFactory.decodeStream succeeds. */
    private fun pngBytes(): ByteArray {
        val bmp = android.graphics.Bitmap.createBitmap(2, 2, android.graphics.Bitmap.Config.ARGB_8888)
        bmp.eraseColor(android.graphics.Color.WHITE)
        val out = java.io.ByteArrayOutputStream()
        bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
        return out.toByteArray()
    }
}
