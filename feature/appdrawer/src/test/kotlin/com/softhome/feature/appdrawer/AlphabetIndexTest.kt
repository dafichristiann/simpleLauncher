package com.softhome.feature.appdrawer

import com.google.common.truth.Truth.assertThat
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
}
