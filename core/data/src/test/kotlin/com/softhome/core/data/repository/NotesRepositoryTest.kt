package com.softhome.core.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NotesRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `blank by default`() = runTest {
        val repo = NotesRepositoryImpl(context)
        repo.setBody("")
        assertThat(repo.notes.first().body).isEmpty()
    }

    @Test
    fun `write then read round-trips`() = runTest {
        val repo = NotesRepositoryImpl(context)
        repo.setBody("buy milk\ncall mom")
        assertThat(repo.notes.first().body).isEqualTo("buy milk\ncall mom")
    }

    @Test
    fun `value survives a fresh repository instance over the same file`() = runTest {
        // Proxy for process restart: a NEW repository instance reads the same DataStore file.
        NotesRepositoryImpl(context).setBody("persist me")
        val reopened = NotesRepositoryImpl(context)
        assertThat(reopened.notes.first().body).isEqualTo("persist me")
    }

    @Test
    fun `clearing the body persists as blank`() = runTest {
        val repo = NotesRepositoryImpl(context)
        repo.setBody("something")
        repo.setBody("")
        assertThat(repo.notes.first().body).isEmpty()
    }
}
