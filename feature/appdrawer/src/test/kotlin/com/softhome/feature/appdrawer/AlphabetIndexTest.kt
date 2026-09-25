package com.softhome.feature.appdrawer

import com.google.common.truth.Truth.assertThat
import com.softhome.core.model.AppInfo
import com.softhome.core.model.Folder
import com.softhome.core.model.IconSource
import com.softhome.core.model.ResolvedIcon
import org.junit.Test

class AlphabetIndexTest {

    @Test
    fun `bucket is first letter uppercased`() {
        assertThat(AlphabetIndex.bucketOf("camera")).isEqualTo('C')
        assertThat(AlphabetIndex.bucketOf("Zebra")).isEqualTo('Z')
    }

    @Test
    fun `non letter start becomes hash`() {
        assertThat(AlphabetIndex.bucketOf("3D Mark")).isEqualTo('#')
        assertThat(AlphabetIndex.bucketOf("")).isEqualTo('#')
    }

    @Test
    fun `present letters are ordered and de-duplicated, hash last`() {
        val labels = listOf("Chrome", "Camera", "Alarm", "Zebra", "3D Mark", "Contacts")
        assertThat(AlphabetIndex.lettersPresentIn(labels))
            .containsExactly('A', 'C', 'Z', '#').inOrder()
    }

    @Test
    fun `firstIndexFor finds the bucket`() {
        val labels = listOf("Alarm", "Camera", "Zebra")
        assertThat(AlphabetIndex.firstIndexFor(labels, 'C')).isEqualTo(1)
        assertThat(AlphabetIndex.firstIndexFor(labels, 'Z')).isEqualTo(2)
        assertThat(AlphabetIndex.firstIndexFor(labels, 'X')).isEqualTo(0)
    }

    @Test
    fun `bucket normalization is locale stable and empty stays safe`() {
        assertThat(AlphabetIndex.bucketOf("  chrome")).isEqualTo('C')
        assertThat(AlphabetIndex.bucketOf("Журнал")).isEqualTo('#')
        assertThat(AlphabetIndex.firstIndexFor(emptyList(), 'Z')).isEqualTo(0)
    }

    @Test
    fun `rendered cell index includes folders but ignores folder members`() {
        val cells = listOf(
            DrawerCell.FolderCell(Folder("tools", "Tools", listOf("pkg/inside")), emptyList()),
            appCell("Alpha"),
            appCell("beta"),
        )
        assertThat(AlphabetIndex.firstRenderedIndexFor(cells, 'A')).isEqualTo(1)
        assertThat(AlphabetIndex.firstRenderedIndexFor(cells, 'B')).isEqualTo(2)
        assertThat(AlphabetIndex.firstRenderedIndexFor(cells, 'Z')).isEqualTo(2)
        assertThat(AlphabetIndex.firstRenderedRowStartFor(cells, 'B', columns = 2)).isEqualTo(2)
        assertThat(AlphabetIndex.firstRenderedRowStartFor(cells, 'B', columns = 0)).isEqualTo(2)
    }

    @Test
    fun `all buckets keep stable A to Z hash presentation`() {
        assertThat(AlphabetIndex.allBuckets).hasSize(27)
        assertThat(AlphabetIndex.allBuckets.first()).isEqualTo('A')
        assertThat(AlphabetIndex.allBuckets[25]).isEqualTo('Z')
        assertThat(AlphabetIndex.allBuckets.last()).isEqualTo('#')
    }

    private fun appCell(label: String): DrawerCell = DrawerCell.AppEntry(
        DrawerEntry(
            app = AppInfo("pkg.${label.lowercase()}", "Main", label),
            resolved = ResolvedIcon(
                source = IconSource.System,
                symbolName = "AppWindow",
                componentKey = "pkg.${label.lowercase()}/Main",
                packageName = "pkg.${label.lowercase()}",
                className = "Main",
            ),
            symbolName = "AppWindow",
            colorToken = com.softhome.feature.iconpack.domain.DrawerIconColor.Token.Neutral,
        ),
    )
}
