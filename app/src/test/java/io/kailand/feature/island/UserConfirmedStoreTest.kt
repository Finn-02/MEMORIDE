package io.kailand.feature.island

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.kailand.core.contract.ArrivalResult
import io.kailand.core.contract.DataSource
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
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class UserConfirmedStoreTest {
    @get:Rule val temp = TemporaryFolder()
    private val key = stringPreferencesKey("game_save_json_v1")

    @Test fun actualFileRecreationPreservesUserReceiptsPendingAndAcknowledgedIdempotency() = runBlocking {
        val file = File(temp.root, "user-confirmed.preferences_pb")
        val daily = UserConfirmedArrivalTest.confirmed("D01")
        val region = UserConfirmedArrivalTest.confirmed("D06")
        val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val originals = try {
            val backend = PreferenceDataStoreFactory.create(scope = firstScope, produceFile = { file })
            val store = PreferencesGameSaveStore(backend)
            val service = readyService(store, firstScope)
            val first = (service.completeArrival(daily) as ArrivalResult.Applied).receipt
            val last = (service.completeArrival(region) as ArrivalResult.Applied).receipt
            assertEquals(0, first.growthBefore)
            assertEquals(0, first.growthAfter)
            assertEquals(0, last.growthBefore)
            assertEquals(1, last.growthAfter)
            assertTrue(file.isFile)
            first to last
        } finally { firstScope.coroutineContext[Job]?.cancelAndJoin() }
        val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val backend = PreferenceDataStoreFactory.create(scope = secondScope, produceFile = { file })
            val store = PreferencesGameSaveStore(backend)
            val service = readyService(store, secondScope)
            assertEquals(originals.second, service.state.value.pendingReceipt)
            assertEquals(1, service.state.value.dailyGrowth)
            assertEquals(setOf("wonju"), service.state.value.discoveredRegionIds)
            assertEquals(IslandTestData.rewards.itemsFor("wonju").toSet(), service.state.value.ownedItemIds)
            assertEquals(originals.first, (service.completeArrival(daily) as ArrivalResult.AlreadyApplied).receipt)
            assertEquals(originals.second, service.state.value.pendingReceipt)
            assertTrue(service.acknowledgeReceipt(daily.journeyId) is MutationResult.Rejected)
            assertEquals(MutationResult.Success, service.acknowledgeReceipt(region.journeyId))
        } finally { secondScope.coroutineContext[Job]?.cancelAndJoin() }
        val thirdScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val backend = PreferenceDataStoreFactory.create(scope = thirdScope, produceFile = { file })
            val store = PreferencesGameSaveStore(backend)
            val service = readyService(store, thirdScope)
            val rawBefore = backend.data.first()[key]
            assertNull(service.state.value.pendingReceipt)
            assertEquals(originals.first, (service.completeArrival(daily) as ArrivalResult.AlreadyApplied).receipt)
            assertEquals(originals.second, (service.completeArrival(region) as ArrivalResult.AlreadyApplied).receipt)
            assertNull(service.state.value.pendingReceipt)
            assertEquals(rawBefore, backend.data.first()[key])
            val saved = store.read()!!
            assertEquals(1, saved.schemaVersion)
            assertEquals(2, saved.processedReceipts.size)
            saved.processedReceipts.values.forEach {
                assertEquals("USER_CONFIRMED", it.source)
                assertNull(it.elapsedSeconds)
                assertNull(it.distanceMeters)
            }
        } finally { thirdScope.coroutineContext[Job]?.cancelAndJoin() }
    }

    @Test fun schemaOneDemoAndPleosJsonRemainsReadableWithoutRewrite() = runBlocking {
        // Literal pre-UC schema/enum names, not a fixture encoded with the new serializer.
        val legacy = """{
            "schemaVersion":1,"dailyGrowth":1,"lastDailyGrantDate":"2026-10-31",
            "processedReceipts":{
              "00000000-0000-4000-8000-000000000001":{
                "journeyId":"00000000-0000-4000-8000-000000000001","source":"DEMO","kind":"DAILY",
                "regionId":null,"completedAtEpochMs":1793404800000,"elapsedSeconds":12,"distanceMeters":3.5,
                "growthBefore":0,"growthAfter":1,"discoveredRegionId":null,"grantedItemIds":[]},
              "00000000-0000-4000-8000-000000000002":{
                "journeyId":"00000000-0000-4000-8000-000000000002","source":"PLEOS","kind":"REGION",
                "regionId":null,"completedAtEpochMs":1793404800001,"elapsedSeconds":null,"distanceMeters":null,
                "growthBefore":1,"growthAfter":1,"discoveredRegionId":null,"grantedItemIds":[]}},
            "pendingReceiptId":"00000000-0000-4000-8000-000000000002"
        }""".trimIndent()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val file = File(temp.root, "legacy.preferences_pb")
            val backend = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
            backend.edit { it[key] = legacy }
            val store = PreferencesGameSaveStore(backend)
            val service = readyService(store, scope)
            val saved = store.read()!!
            assertEquals(DataSource.DEMO, saved.processedReceipts.values.first().toContract().source)
            assertEquals(12L, saved.processedReceipts.values.first().elapsedSeconds)
            assertEquals(3.5, saved.processedReceipts.values.first().distanceMeters!!, 0.0)
            assertEquals(DataSource.PLEOS, service.state.value.pendingReceipt!!.source)
            assertEquals(legacy, backend.data.first()[key])
            val original = saved.processedReceipts.values.first().toContract()
            val replay = IslandTestData.arrival("D01").copy(journeyId = original.journeyId)
            assertEquals(original, (service.completeArrival(replay) as ArrivalResult.AlreadyApplied).receipt)
            assertEquals(legacy, backend.data.first()[key])
            assertTrue(service.completeArrival(UserConfirmedArrivalTest.confirmed("D06")) is ArrivalResult.Applied)
            assertEquals(original, store.read()!!.processedReceipts.getValue(original.journeyId).toContract())
        } finally { scope.coroutineContext[Job]?.cancelAndJoin() }
    }

    @Test fun everyHistoricalUserReceiptRejectsMeasurementsAndUnknownSourcesWithoutOverwrite() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val file = File(temp.root, "invalid-history.preferences_pb")
            val backend = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
            val store = PreferencesGameSaveStore(backend)
            val live = readyService(store, scope)
            val old = UserConfirmedArrivalTest.confirmed("D03")
            live.completeArrival(old)
            live.completeArrival(UserConfirmedArrivalTest.confirmed("D06"))
            val saved = store.read()!!
            val receipt = saved.processedReceipts.getValue(old.journeyId)
            for (pending in listOf(saved.pendingReceiptId, null)) {
                for (corrupt in listOf(
                    receipt.copy(elapsedSeconds = 0), receipt.copy(distanceMeters = 0.0),
                    receipt.copy(elapsedSeconds = 1, distanceMeters = 1.0),
                    receipt.copy(source = "UNSUPPORTED_FUTURE_SOURCE"),
                )) {
                    val invalid = GameSaveCodec.encode(saved.copy(pendingReceiptId = pending,
                        processedReceipts = saved.processedReceipts + (old.journeyId to corrupt)))
                    backend.edit { it[key] = invalid }
                    val before = live.state.value
                    assertTrue(live.completeArrival(old) is ArrivalResult.StorageFailure)
                    assertTrue(live.apply(GameAction.SelectCharacter("momo")) is MutationResult.StorageFailure)
                    assertTrue(live.acknowledgeReceipt(old.journeyId) is MutationResult.StorageFailure)
                    assertEquals(before, live.state.value)
                    val recreated = BGameService(store, scope) { IslandTestData.rewards }
                    assertEquals(GameReadiness.READ_ERROR, recreated.readiness.first { it != GameReadiness.LOADING })
                    assertTrue(recreated.resetDemoProgress() is MutationResult.StorageFailure)
                    var overwriteBlocked = false
                    try { store.update { GameSave() } } catch (_: Exception) { overwriteBlocked = true }
                    assertTrue(overwriteBlocked)
                    assertEquals(invalid, backend.data.first()[key])
                }
            }
        } finally { scope.coroutineContext[Job]?.cancelAndJoin() }
    }

    private suspend fun readyService(store: GameSaveStore, scope: CoroutineScope): BGameService {
        val service = BGameService(store, scope) { IslandTestData.rewards }
        assertEquals(GameReadiness.READY, service.readiness.first { it != GameReadiness.LOADING })
        return service
    }
}
