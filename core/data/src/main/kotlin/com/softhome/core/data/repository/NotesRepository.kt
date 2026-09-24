package com.softhome.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.softhome.core.model.Notes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.notesDataStore: DataStore<Preferences> by preferencesDataStore(name = "soft_home_notes")

/**
 * Persisted quick notes (P2 / E5).
 *
 * Decision P2-2: the notes body is **user data** and must survive process death /
 * restart, so it is stored in DataStore (not in-memory like the static music row).
 */
interface NotesRepository {
    val notes: Flow<Notes>
    suspend fun setBody(body: String)
}

@Singleton
class NotesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : NotesRepository {

    private object Keys {
        val BODY = stringPreferencesKey(Notes.FIELD_KEY)
    }

    override val notes: Flow<Notes> = context.notesDataStore.data.map { p ->
        Notes(body = p[Keys.BODY] ?: "")
    }

    override suspend fun setBody(body: String) {
        context.notesDataStore.edit { p -> p[Keys.BODY] = body }
    }
}
