package com.softhome.feature.appdrawer

import com.google.common.truth.Truth.assertThat
import com.softhome.core.model.AppInfo
import com.softhome.core.model.Folder
import com.softhome.core.model.IconSource
import com.softhome.core.model.ResolvedIcon
import org.junit.Test

class FolderCellsTest {

    private fun entry(key: String): DrawerEntry {
        val pkg = key.substringBefore('/')
        val cls = key.substringAfter('/')
        return DrawerEntry(
            app = AppInfo(packageName = pkg, className = cls, label = cls),
            symbolName = "AppWindow",
            colorToken = com.softhome.feature.iconpack.domain.DrawerIconColor.Token.Neutral,
            resolved = ResolvedIcon(
                source = IconSource.System,
                drawableName = null,
                symbolName = "AppWindow",
                componentKey = key,
                packageName = pkg,
                className = cls,
            ),
        )
    }

    @Test
    fun `preview entries follow the folder's own app order`() {
        val folder = Folder("f1", "Tools", listOf("com.c/Three", "com.a/One"))
        val members = listOf(entry("com.a/One"), entry("com.c/Three"), entry("com.b/Two"))
        val preview = FoldersPreview.entriesFor(folder, members)
        assertThat(preview.map { it.app.componentKey })
            .containsExactly("com.c/Three", "com.a/One").inOrder()
    }

    @Test
    fun `preview caps at four entries`() {
        val folder = Folder("f1", "Big", listOf("a/A", "b/B", "c/C", "d/D", "e/E"))
        val members = listOf("a/A", "b/B", "c/C", "d/D", "e/E").map(::entry)
        assertThat(FoldersPreview.entriesFor(folder, members)).hasSize(4)
    }

    @Test
    fun `preview skips folder keys with no matching entry`() {
        val folder = Folder("f1", "Tools", listOf("com.gone/Gone", "com.a/One"))
        val members = listOf(entry("com.a/One"))
        val preview = FoldersPreview.entriesFor(folder, members)
        assertThat(preview.map { it.app.componentKey }).containsExactly("com.a/One")
    }

    @Test
    fun `preview of empty folder is empty`() {
        assertThat(FoldersPreview.entriesFor(Folder("f1", "Empty"), emptyList())).isEmpty()
    }
}
