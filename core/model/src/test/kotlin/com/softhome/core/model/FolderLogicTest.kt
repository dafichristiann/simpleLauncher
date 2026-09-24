package com.softhome.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FolderLogicTest {

    private val apps = listOf(
        "com.a/One",
        "com.b/Two",
        "com.c/Three",
        "com.d/Four",
        "com.e/Five",
    )

    @Test
    fun `preview takes first four apps`() {
        val folder = Folder(id = "f1", name = "Tools", apps = apps)
        assertThat(FolderLogic.previewKeys(folder)).containsExactly(
            "com.a/One", "com.b/Two", "com.c/Three", "com.d/Four",
        ).inOrder()
    }

    @Test
    fun `preview of small folder returns all`() {
        val folder = Folder(id = "f1", name = "Tiny", apps = listOf("com.a/One"))
        assertThat(FolderLogic.previewKeys(folder)).containsExactly("com.a/One")
    }

    @Test
    fun `isInsideFolder detects membership`() {
        val folders = listOf(Folder("f1", "Tools", listOf("com.a/One")))
        assertThat(FolderLogic.isInsideFolder(folders, "com.a/One")).isTrue()
        assertThat(FolderLogic.isInsideFolder(folders, "com.b/Two")).isFalse()
    }

    @Test
    fun `drawer grid leads with folders then non-nested apps`() {
        val folders = listOf(Folder("f1", "Tools", listOf("com.a/One", "com.b/Two")))
        val items = FolderLogic.drawerGridItems(apps, folders)
        assertThat(items).containsExactly(
            DrawerGridItem.FolderItem("f1"),
            DrawerGridItem.App("com.c/Three"),
            DrawerGridItem.App("com.d/Four"),
            DrawerGridItem.App("com.e/Five"),
        ).inOrder()
    }

    @Test
    fun `drawer grid with no folders is all apps in order`() {
        val items = FolderLogic.drawerGridItems(apps, emptyList())
        assertThat(items).containsExactlyElementsIn(apps.map { DrawerGridItem.App(it) }).inOrder()
    }

    @Test
    fun `rename keeps new name and trims`() {
        val f = Folder("f1", "Old", listOf("com.a/One"))
        assertThat(FolderLogic.rename(f, "  New  ").name).isEqualTo("New")
    }

    @Test
    fun `rename ignores blank name`() {
        val f = Folder("f1", "Old", listOf("com.a/One"))
        assertThat(FolderLogic.rename(f, "   ").name).isEqualTo("Old")
    }

    @Test
    fun `addApp appends and de-duplicates`() {
        val f = Folder("f1", "Tools", listOf("com.a/One"))
        assertThat(FolderLogic.addApp(f, "com.b/Two").apps)
            .containsExactly("com.a/One", "com.b/Two").inOrder()
        assertThat(FolderLogic.addApp(f, "com.a/One").apps).containsExactly("com.a/One")
    }

    @Test
    fun `removeApp drops the key`() {
        val f = Folder("f1", "Tools", listOf("com.a/One", "com.b/Two"))
        assertThat(FolderLogic.removeApp(f, "com.a/One").apps).containsExactly("com.b/Two")
    }

    @Test
    fun `createFolder trims name and de-dupes keys`() {
        val f = FolderLogic.createFolder("f9", "  New  ", listOf("com.a/One", "com.a/One"))
        assertThat(f.id).isEqualTo("f9")
        assertThat(f.name).isEqualTo("New")
        assertThat(f.apps).containsExactly("com.a/One")
    }

    @Test
    fun `createFolder falls back to default name when blank`() {
        val f = FolderLogic.createFolder("f9", "   ", listOf("com.a/One"))
        assertThat(f.name).isEqualTo("Folder")
    }
}
