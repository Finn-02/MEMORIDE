package io.kailand.app.journey

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Separate A-owned metadata file; never reads/writes B's save or reward keys. */
class PreferencesNavigationDraftStore(private val data: DataStore<Preferences>) : NavigationDraftStore {
    @Serializable private data class Save(val schemaVersion: Int = 1, val draft: NavigationDraft?)
    private val key = stringPreferencesKey("latest_navigation_draft")
    override suspend fun read(): NavigationDraft? {
        val raw = data.data.first()[key] ?: return null
        val saved = Json.decodeFromString<Save>(raw)
        require(saved.schemaVersion == 1)
        return saved.draft
    }
    override suspend fun write(draft: NavigationDraft?) {
        data.edit { it[key] = Json.encodeToString(Save(draft = draft)) }
    }
}

fun createNavigationDraftStore(context: Context, scope: CoroutineScope): NavigationDraftStore =
    PreferencesNavigationDraftStore(PreferenceDataStoreFactory.create(scope = scope) {
        context.applicationContext.preferencesDataStoreFile("navigation_draft_v1")
    })
