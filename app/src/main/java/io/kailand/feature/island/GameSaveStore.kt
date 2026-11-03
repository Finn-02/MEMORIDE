package io.kailand.feature.island

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** One JSON value per DataStore transaction. B owns the only writer. */
internal interface GameSaveStore {
    suspend fun read(): GameSave?
    suspend fun update(transform: (GameSave) -> GameSave): GameSave
}

internal object GameSaveCodec {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = false }

    fun encode(save: GameSave): String = json.encodeToString(save)

    fun decode(text: String): GameSave {
        val save = json.decodeFromString<GameSave>(text)
        // Validate before any mutation. Never silently overwrite unknown/corrupt saves.
        save.toSnapshot()
        return save
    }
}

/** The Context delegate ensures one Preferences DataStore for this file per process. */
private val Context.memorideGameDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "memoride_game")

internal class PreferencesGameSaveStore(
    private val dataStore: DataStore<Preferences>,
) : GameSaveStore {
    // Production uses the existing one-per-process Context delegate. Tests may
    // inject a file-backed DataStore and safely close it before recreating it.
    constructor(context: Context) : this(context.applicationContext.memorideGameDataStore)

    private val saveKey = stringPreferencesKey("game_save_json_v1")

    override suspend fun read(): GameSave? =
        dataStore.data.first()[saveKey]?.let(GameSaveCodec::decode)

    override suspend fun update(transform: (GameSave) -> GameSave): GameSave {
        var committed: GameSave? = null
        dataStore.edit { preferences ->
            val current = preferences[saveKey]?.let(GameSaveCodec::decode) ?: GameSave()
            val next = transform(current)
            next.toSnapshot() // Validate the new state before persisting it.
            preferences[saveKey] = GameSaveCodec.encode(next)
            committed = next
        }
        return checkNotNull(committed)
    }
}
