package io.kailand.feature.island

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.kailand.core.contract.ArrivalResult
import io.kailand.core.contract.GameAction
import io.kailand.core.contract.MutationResult
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertArrayEquals
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

    @Test fun gameActionsPersistAcrossRealFileAndServiceRecreation() = runBlocking {
        val file = File(temp.root, "actions.preferences_pb")

        suspend fun <T> session(block: suspend (BGameService, PreferencesGameSaveStore) -> T): T {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            try {
                val backend = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
                val store = PreferencesGameSaveStore(backend)
                val service = BGameService(store, scope) { IslandTestData.rewards }
                assertEquals(GameReadiness.READY, service.readiness.first { it != GameReadiness.LOADING })
                return block(service, store)
            } finally { scope.coroutineContext[Job]?.cancelAndJoin() }
        }

        val expected = session { service, store ->
            assertTrue(service.completeArrival(IslandTestData.arrival("D01")) is ArrivalResult.Applied)
            assertTrue(service.completeArrival(IslandTestData.arrival("D03")) is ArrivalResult.Applied)
            val rewarded = store.read()!!
            val actions = listOf(GameAction.SelectCharacter("momo"), GameAction.EquipOutfit("daejeon_hat"),
                GameAction.PlaceDecoration("slot_left", "daejeon_sign"))
            for (action in actions) assertEquals(MutationResult.Success, service.apply(action))
            val changed = rewarded.copy(characterId = "momo", equippedOutfitId = "daejeon_hat",
                decorationSlots = mapOf("slot_left" to "daejeon_sign"))
            assertEquals(changed, store.read())
            val bytes = file.readBytes()
            for (action in actions + GameAction.Eat("daejeon_bread")) {
                assertEquals(MutationResult.Success, service.apply(action))
            }
            assertTrue(service.apply(GameAction.PlaceDecoration("slot_right", "daejeon_sign")) is MutationResult.Rejected)
            assertArrayEquals(bytes, file.readBytes())
            changed
        }

        session { service, store ->
            assertEquals(expected, store.read())
            assertEquals(expected.toSnapshot(), service.state.value)
            val receipt = expected.processedReceipts.getValue(IslandTestData.arrival("D03").journeyId).toContract()
            assertEquals(ArrivalResult.AlreadyApplied(receipt), service.completeArrival(IslandTestData.arrival("D04")))
            assertEquals(expected, store.read())
        }
    }

    @Test fun dailyDatesAndD06NullOriginalsSurviveRealFileRecreation() = runBlocking {
        val file = File(temp.root, "daily.preferences_pb")
        val d01 = IslandTestData.arrival("D01")
        val d02 = IslandTestData.arrival("D02")
        val d06 = IslandTestData.arrival("D06")
        val nextDay = d01.copy(journeyId = "00000000-0000-4000-8000-000000000888",
            completedAtEpochMs = d01.completedAtEpochMs + 86_400_000)

        suspend fun <T> session(block: suspend (BGameService, PreferencesGameSaveStore) -> T): T {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            try {
                val backend = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
                val store = PreferencesGameSaveStore(backend)
                val service = BGameService(store, scope) { IslandTestData.rewards }
                assertEquals(GameReadiness.READY, service.readiness.first { it != GameReadiness.LOADING })
                return block(service, store)
            } finally { scope.coroutineContext[Job]?.cancelAndJoin() }
        }

        val (daily, wonju) = session { service, store ->
            val daily = (service.completeArrival(d01) as ArrivalResult.Applied).receipt
            val savedDaily = store.read()!!
            assertEquals(1, savedDaily.dailyGrowth)
            assertEquals("2026-10-31", savedDaily.lastDailyGrantDate)
            assertEquals(daily, savedDaily.processedReceipts.getValue(d01.journeyId).toContract())
            assertEquals(d01.journeyId, savedDaily.pendingReceiptId)
            val wonju = (service.completeArrival(d06) as ArrivalResult.Applied).receipt
            assertNull(wonju.elapsedSeconds)
            assertNull(wonju.distanceMeters)
            assertEquals(1, wonju.growthBefore)
            assertEquals(1, wonju.growthAfter)
            val saved = store.read()!!
            assertEquals(wonju.grantedItemIds.toSet(), saved.ownedItemIds)
            assertEquals(setOf("wonju"), saved.discoveredRegionIds)
            assertEquals(wonju, saved.processedReceipts.getValue(d06.journeyId).toContract())
            assertEquals(MutationResult.Success, service.acknowledgeReceipt(d06.journeyId))
            daily to wonju
        }

        session { service, store ->
            assertNull(service.state.value.pendingReceipt)
            assertEquals(daily, (service.completeArrival(d01.copy(completedAtEpochMs = nextDay.completedAtEpochMs))
                as ArrivalResult.AlreadyApplied).receipt)
            assertEquals(wonju, (service.completeArrival(d06.copy(elapsedSeconds = 999, distanceMeters = 999.0))
                as ArrivalResult.AlreadyApplied).receipt)
            assertNull(service.state.value.pendingReceipt)
            val sameDate = (service.completeArrival(d02) as ArrivalResult.Applied).receipt
            assertEquals(1, sameDate.growthBefore)
            assertEquals(1, sameDate.growthAfter)
            assertEquals(2, (service.completeArrival(nextDay) as ArrivalResult.Applied).receipt.growthAfter)
            assertEquals("2026-11-01", store.read()!!.lastDailyGrantDate)
            assertTrue(service.acknowledgeReceipt(d02.journeyId) is MutationResult.Rejected)
            assertEquals(nextDay.journeyId, store.read()!!.pendingReceiptId)
        }

        session { service, store ->
            assertEquals(2, service.state.value.dailyGrowth)
            assertEquals(nextDay.journeyId, service.state.value.pendingReceipt?.journeyId)
            val olderDayNewId = d01.copy(journeyId = "00000000-0000-4000-8000-000000000889")
            val receipt = (service.completeArrival(olderDayNewId) as ArrivalResult.Applied).receipt
            assertEquals(2, receipt.growthBefore)
            assertEquals(2, receipt.growthAfter)
            val beforeReplay = store.read()
            assertEquals(daily, (service.completeArrival(d01) as ArrivalResult.AlreadyApplied).receipt)
            assertEquals(wonju, (service.completeArrival(d06) as ArrivalResult.AlreadyApplied).receipt)
            assertEquals(beforeReplay, store.read())
            assertEquals(olderDayNewId.journeyId, service.state.value.pendingReceipt?.journeyId)
        }
    }

    @Test fun d03D04D05AndAcknowledgementSurviveRealFileRecreation() = runBlocking {
        val file = File(temp.root, "arrivals.preferences_pb")
        val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val d03 = IslandTestData.arrival("D03")
        val d04 = IslandTestData.arrival("D04")
        val d05 = IslandTestData.arrival("D05")
        val original = try {
            val backend = PreferenceDataStoreFactory.create(scope = firstScope, produceFile = { file })
            val store = PreferencesGameSaveStore(backend)
            val service = BGameService(store, firstScope) { IslandTestData.rewards }
            assertEquals(GameReadiness.READY, service.readiness.first { it != GameReadiness.LOADING })
            val receipt = (service.completeArrival(d03) as ArrivalResult.Applied).receipt
            val saved = store.read()!!
            assertEquals(receipt.grantedItemIds.toSet(), saved.ownedItemIds)
            assertEquals(setOf("daejeon"), saved.discoveredRegionIds)
            assertEquals(d03.journeyId, saved.pendingReceiptId)
            assertEquals(receipt, saved.processedReceipts.getValue(d03.journeyId).toContract())
            receipt
        } finally { firstScope.coroutineContext[Job]?.cancelAndJoin() }

        val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val backend = PreferenceDataStoreFactory.create(scope = secondScope, produceFile = { file })
            val store = PreferencesGameSaveStore(backend)
            val service = BGameService(store, secondScope) { IslandTestData.rewards }
            assertEquals(GameReadiness.READY, service.readiness.first { it != GameReadiness.LOADING })
            assertEquals(original, service.state.value.pendingReceipt)
            assertEquals(original, (service.completeArrival(d04) as ArrivalResult.AlreadyApplied).receipt)
            val revisit = (service.completeArrival(d05) as ArrivalResult.Applied).receipt
            assertTrue(revisit.grantedItemIds.isEmpty())
            assertNull(revisit.discoveredRegionId)
            assertEquals(0, revisit.growthAfter)
            assertTrue(service.acknowledgeReceipt(d03.journeyId) is MutationResult.Rejected)
            assertEquals(d05.journeyId, store.read()!!.pendingReceiptId)
            assertEquals(MutationResult.Success, service.acknowledgeReceipt(d05.journeyId))
        } finally { secondScope.coroutineContext[Job]?.cancelAndJoin() }

        val thirdScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val backend = PreferenceDataStoreFactory.create(scope = thirdScope, produceFile = { file })
            val store = PreferencesGameSaveStore(backend)
            val service = BGameService(store, thirdScope) { IslandTestData.rewards }
            assertEquals(GameReadiness.READY, service.readiness.first { it != GameReadiness.LOADING })
            assertNull(service.state.value.pendingReceipt)
            assertEquals(original, (service.completeArrival(d04) as ArrivalResult.AlreadyApplied).receipt)
            assertNull(service.state.value.pendingReceipt)
            assertEquals(2, store.read()!!.processedReceipts.size)
        } finally { thirdScope.coroutineContext[Job]?.cancelAndJoin() }
    }

    @Test fun malformedOrMismatchedHistoricalSaveCannotBeOverwritten() = runBlocking {
        val d03 = IslandTestData.arrival("D03")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val file = File(temp.root, "corrupt.preferences_pb")
            val backend = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
            val store = PreferencesGameSaveStore(backend)
            val service = BGameService(store, scope) { IslandTestData.rewards }
            assertEquals(GameReadiness.READY, service.readiness.first { it != GameReadiness.LOADING })
            service.completeArrival(d03)
            val saved = store.read()!!
            val receipt = saved.processedReceipts.getValue(d03.journeyId)
            val key = stringPreferencesKey("game_save_json_v1")
            val corruptInputs = listOf(
                "{broken json",
                GameSaveCodec.encode(saved.copy(lastDailyGrantDate = "not-a-date")),
                GameSaveCodec.encode(saved.copy(pendingReceiptId = null,
                    processedReceipts = mapOf("wrong-id" to receipt))),
                GameSaveCodec.encode(saved.copy(pendingReceiptId = null,
                    processedReceipts = mapOf(d03.journeyId to receipt.copy(source = "BROKEN")))),
            )
            for (invalid in corruptInputs) {
                backend.edit { it[key] = invalid }
                val previous = service.state.value
                assertTrue(service.apply(GameAction.SelectCharacter("momo")) is MutationResult.StorageFailure)
                assertEquals(previous, service.state.value)
                val recreated = BGameService(store, scope) { IslandTestData.rewards }
                assertEquals(GameReadiness.READ_ERROR, recreated.readiness.first { it != GameReadiness.LOADING })
                assertTrue(recreated.completeArrival(d03) is ArrivalResult.StorageFailure)
                var rejected = false
                try { store.update { GameSave() } } catch (_: Exception) { rejected = true }
                assertTrue(rejected)
                assertEquals(invalid, backend.data.first()[key])
            }
        } finally { scope.coroutineContext[Job]?.cancelAndJoin() }
    }

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
