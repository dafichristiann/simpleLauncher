package com.softhome.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NotesTest {

    @Test
    fun `blank notes preview shows nothing yet`() {
        assertThat(Notes("").preview).isEqualTo("Nothing yet")
        assertThat(Notes("   ").preview).isEqualTo("Nothing yet")
    }

    @Test
    fun `preview collapses newlines to single spaces`() {
        assertThat(Notes("line one\nline two").preview).isEqualTo("line one line two")
    }

    @Test
    fun `preview trims surrounding whitespace`() {
        assertThat(Notes("  milk  ").preview).isEqualTo("milk")
    }
}
