package com.softhome.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.softhome.core.model.Folder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

private val Context.folderDataStore: DataStore<Preferences> by preferencesDataStore(name = "soft_home_folders")

/**
 * Persisted home/drawer folders (P2 / D1-D2).
 *
 * Folders live in the **app drawer** (decision P2-1) and are stored as a JSON
 * array in DataStore. Corrupt JSON degrades to an empty list (never crashes).
 */
interface FolderRepository {
    val folders: Flow<List<Folder>>
    suspend fun save(folders: List<Folder>)
}

@Singleton
class FolderRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : FolderRepository {

    private object Keys {
        val FOLDERS = stringPreferencesKey("folders_json")
    }

    override val folders: Flow<List<Folder>> = context.folderDataStore.data.map { p ->
        FoldersCodec.decode(p[Keys.FOLDERS])
    }

    override suspend fun save(folders: List<Folder>) {
        val encoded = FoldersCodec.encode(folders)
        context.folderDataStore.edit { p -> p[Keys.FOLDERS] = encoded }
    }
}

/**
 * Pure (Android-free apart from org.json) folder list <-> JSON codec. Kept separate
 * so it can be unit-tested directly, including the corrupt-input path.
 */
object FoldersCodec {

    fun encode(folders: List<Folder>): String {
        val array = JSONArray()
        folders.forEach { folder ->
            val obj = JSONObject()
            obj.put("id", folder.id)
            obj.put("name", folder.name)
            obj.put("apps", JSONArray(folder.apps))
            array.put(obj)
        }
        return array.toString()
    }

    /** Decodes folders, returning an empty list on any malformed input (never throws). */
    fun decode(json: String?): List<Folder> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            buildList(array.length()) {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val appsArray = obj.optJSONArray("apps")
                    val apps = buildList {
                        if (appsArray != null) {
                            for (j in 0 until appsArray.length()) {
                                appsArray.optString(j).takeIf { it.isNotBlank() }?.let(::add)
                            }
                        }
                    }
                    add(
                        Folder(
                            id = obj.optString("id"),
                            name = obj.optString("name"),
                            apps = apps,
                        ),
                    )
                }
            }.filter { it.id.isNotBlank() }
        }.getOrDefault(emptyList())
    }
}
