package com.softhome.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * P4a: pure drop-resolution tests for the two drop targets. These are the "adapters"
 * that make the shared gesture engine safe — all index / membership math is verified
 * here, on the JVM, before any Compose code runs.
 */
class DragDropResolverTest {

    // --- Home row reorder -----------------------------------------------------

    private val defaultRows = HomeRowLogic.default()

    @Test
    fun `reorder moves a row earlier`() {
        val result = HomeRowDropResolver.reorder(defaultRows, HomeRowKind.Weather, 0)
        assertThat(result.map { it.kind }).containsExactly(
            HomeRowKind.Weather,
            HomeRowKind.Time,
            HomeRowKind.Date,
            HomeRowKind.Search,
            HomeRowKind.Calendar,
            HomeRowKind.BatteryStorage,
            HomeRowKind.Notes,
        ).inOrder()
    }

    @Test
    fun `reorder moves a row later`() {
        val result = HomeRowDropResolver.reorder(defaultRows, HomeRowKind.Time, 3)
        assertThat(result.map { it.kind }).containsExactly(
            HomeRowKind.Date,
            HomeRowKind.Weather,
            HomeRowKind.Search,
            HomeRowKind.Time,
            HomeRowKind.Calendar,
            HomeRowKind.BatteryStorage,
            HomeRowKind.Notes,
        ).inOrder()
    }

    @Test
    fun `reorder clamps past the end`() {
        val result = HomeRowDropResolver.reorder(defaultRows, HomeRowKind.Time, 999)
        assertThat(result.map { it.kind }).containsExactly(
            HomeRowKind.Date,
            HomeRowKind.Weather,
            HomeRowKind.Search,
            HomeRowKind.Calendar,
            HomeRowKind.BatteryStorage,
            HomeRowKind.Notes,
            HomeRowKind.Time,
        ).inOrder()
    }

    @Test
    fun `reorder to the same index is a no-op`() {
        assertThat(HomeRowDropResolver.reorder(defaultRows, HomeRowKind.Notes, 6))
            .isEqualTo(defaultRows)
    }

    @Test
    fun `reorder preserves visibility and count`() {
        val hidden = HomeRowLogic.toggle(defaultRows, HomeRowKind.Search)
        val result = HomeRowDropResolver.reorder(hidden, HomeRowKind.Notes, 0)
        assertThat(result).hasSize(defaultRows.size)
        assertThat(result.first { it.kind == HomeRowKind.Search }.visible).isFalse()
        assertThat(result.first().kind).isEqualTo(HomeRowKind.Notes)
    }

    @Test
    fun `reorder of an unknown kind is a no-op`() {
        // A list without Notes: dragging Notes resolves to no change.
        val noNotes = defaultRows.filterNot { it.kind == HomeRowKind.Notes }
        assertThat(HomeRowDropResolver.reorder(noNotes, HomeRowKind.Notes, 0)).isEqualTo(noNotes)
    }

    // --- App -> folder --------------------------------------------------------

    @Test
    fun `assign adds the app to the folder`() {
        val folders = listOf(Folder("f1", "Tools", listOf("com.a/One")))
        val result = FolderDropResolver.assign(folders, "f1", "com.b/Two")
        assertThat(result.first().apps).containsExactly("com.a/One", "com.b/Two").inOrder()
    }

    @Test
    fun `assign to an unknown folder is a no-op`() {
        val folders = listOf(Folder("f1", "Tools", listOf("com.a/One")))
        assertThat(FolderDropResolver.assign(folders, "nope", "com.b/Two")).isEqualTo(folders)
    }

    @Test
    fun `assign an existing member does not duplicate`() {
        val folders = listOf(Folder("f1", "Tools", listOf("com.a/One")))
        val result = FolderDropResolver.assign(folders, "f1", "com.a/One")
        assertThat(result.first().apps).containsExactly("com.a/One")
    }

    @Test
    fun `createWith builds a new folder containing only the app`() {
        val result = FolderDropResolver.createWith(emptyList(), "com.a/One", "f9", "New folder")
        assertThat(result).hasSize(1)
        assertThat(result.first().id).isEqualTo("f9")
        assertThat(result.first().name).isEqualTo("New folder")
        assertThat(result.first().apps).containsExactly("com.a/One")
    }

    @Test
    fun `createWith appends after existing folders`() {
        val folders = listOf(Folder("f1", "Tools", listOf("com.a/One")))
        val result = FolderDropResolver.createWith(folders, "com.b/Two", "f9", "New folder")
        assertThat(result.map { it.id }).containsExactly("f1", "f9").inOrder()
    }

    @Test
    fun `removeFrom pulls the app out`() {
        val folders = listOf(Folder("f1", "Tools", listOf("com.a/One", "com.b/Two")))
        val result = FolderDropResolver.removeFrom(folders, "f1", "com.a/One")
        assertThat(result.first().apps).containsExactly("com.b/Two")
    }

    @Test
    fun `removeFrom an unknown folder is a no-op`() {
        val folders = listOf(Folder("f1", "Tools", listOf("com.a/One")))
        assertThat(FolderDropResolver.removeFrom(folders, "nope", "com.a/One")).isEqualTo(folders)
    }
}
