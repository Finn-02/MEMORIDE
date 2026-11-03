package io.kailand.feature.island

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Exercises the real Preferences DataStore backend on a JVM temporary file.
 *  This is not an Android device/app-restart or Pleos test.
 */
class GameSaveDataStoreTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun realDataStoreFileSurvivesStoreRecreation() = runBlocking {
        val file = File(temp.root, "game.preferences_pb")
        val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val firstDataStore = PreferenceDataStoreFactory.create(
                scope = firstScope,
                produceFile = { file },
            )
            val first = PreferencesGameSaveStore(firstDataStore)
            assertNull(first.read())
            val committed = first.update { original ->
                original.copy(
                    characterId = "momo",
                    dailyGrowth = 2,
                    ownedItemIds = setOf("daejeon_bread", "daejeon_hat"),
                    discoveredRegionIds = setOf("daejeon"),
                    equippedOutfitId = "daejeon_hat",
                    decorationSlots = mapOf("slot_left" to "daejeon_sign"),
                )
            }
            assertEquals("momo", committed.characterId)
            assertTrue(file.isFile)
        } finally {
            // Do not run two DataStore instances against the same file at once.
            firstScope.coroutineContext[Job]?.cancelAndJoin()
        }

        val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val secondDataStore = PreferenceDataStoreFactory.create(
                scope = secondScope,
                produceFile = { file },
            )
            val second = PreferencesGameSaveStore(secondDataStore)
            val restored = second.read()
            assertNotNull(restored)
            assertEquals("momo", restored?.characterId)
            assertEquals(2, restored?.dailyGrowth)
            assertEquals(setOf("daejeon_bread", "daejeon_hat"), restored?.ownedItemIds)
            assertEquals(setOf("daejeon"), restored?.discoveredRegionIds)
            assertEquals("daejeon_hat", restored?.equippedOutfitId)
            assertEquals(mapOf("slot_left" to "daejeon_sign"), restored?.decorationSlots)
        } finally {
            secondScope.coroutineContext[Job]?.cancelAndJoin()
        }
    }

    @Test fun unknownSaveSchemaIsNotOverwrittenByRealDataStoreUpdate() = runBlocking {
        val file = File(temp.root, "unsupported.preferences_pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val dataStore = PreferenceDataStoreFactory.create(
                scope = scope,
                produceFile = { file },
            )
            val key = stringPreferencesKey("game_save_json_v1")
            val invalid = GameSaveCodec.encode(GameSave(schemaVersion = 99))
            dataStore.edit { it[key] = invalid }
            val store = PreferencesGameSaveStore(dataStore)
            var readRejected = false
            try { store.read() } catch (_: IllegalStateException) { readRejected = true }
            assertTrue("Unsupported schema must fail on read", readRejected)

            var updateRejected = false
            try { store.update { GameSave() } }
            catch (_: IllegalStateException) { updateRejected = true }
            assertTrue("Unsupported schema must block overwriting", updateRejected)
            assertEquals(invalid, dataStore.data.first()[key])
        } finally {
            scope.coroutineContext[Job]?.cancelAndJoin()
        }
    }
}
