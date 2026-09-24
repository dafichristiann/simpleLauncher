package com.softhome.core.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.softhome.core.model.Folder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FolderRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `empty by default`() = runTest {
        val repo = FolderRepositoryImpl(context)
        repo.save(emptyList())
        assertThat(repo.folders.first()).isEmpty()
    }

    @Test
    fun `save then read round-trips`() = runTest {
        val repo = FolderRepositoryImpl(context)
        val folders = listOf(
            Folder("f1", "Tools", listOf("com.a/One", "com.b/Two")),
            Folder("f2", "Games", listOf("com.c/Three")),
        )
        repo.save(folders)
        assertThat(repo.folders.first()).isEqualTo(folders)
    }

    @Test
    fun `folders survive a fresh repository instance over the same file`() = runTest {
        FolderRepositoryImpl(context).save(listOf(Folder("f1", "Tools", listOf("com.a/One"))))
        val reopened = FolderRepositoryImpl(context)
        assertThat(reopened.folders.first()).containsExactly(
            Folder("f1", "Tools", listOf("com.a/One")),
        )
    }

    @Test
    fun `corrupt json decodes to empty list without throwing`() {
        assertThat(FoldersCodec.decode("{not json")).isEmpty()
        assertThat(FoldersCodec.decode("   ")).isEmpty()
        assertThat(FoldersCodec.decode(null)).isEmpty()
    }

    @Test
    fun `entries without an id are dropped`() {
        val json = """[{"name":"NoId","apps":[]},{"id":"f1","name":"Ok","apps":["x/y"]}]"""
        assertThat(FoldersCodec.decode(json)).containsExactly(Folder("f1", "Ok", listOf("x/y")))
    }

    @Test
    fun `blank app keys are ignored`() {
        val json = """[{"id":"f1","name":"Ok","apps":["x/y","","  "]}]"""
        assertThat(FoldersCodec.decode(json)).containsExactly(Folder("f1", "Ok", listOf("x/y")))
    }
}
