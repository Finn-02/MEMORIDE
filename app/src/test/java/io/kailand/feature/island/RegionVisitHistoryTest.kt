package io.kailand.feature.island

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.kailand.core.contract.ArrivalCommand
import io.kailand.core.contract.ArrivalResult
import io.kailand.core.contract.DataSource
import io.kailand.core.contract.MutationResult
import io.kailand.core.contract.RegionVisitHistory
import io.kailand.core.contract.RegionVisitSummary
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RegionVisitHistoryTest {
    @get:Rule val temp = TemporaryFolder()

    private val daejeon = IslandTestData.arrival("D03")
    private val wonju = IslandTestData.arrival("D06")
    private val emptyHistory = RegionVisitHistory.Available(emptyList())

    @Test fun committedDemoRegionCountsAsOneGameVisit() = withService { service, _ ->
        assertEquals(DataSource.DEMO, daejeon.source)
        assertTrue(service.completeArrival(daejeon) is ArrivalResult.Applied)
        assertEquals(historyOf(daejeon), service.regionVisitHistory.value)
    }

    @Test fun repeatVisitsUseMinAndMaxEpochRegardlessOfCommitOrder() = withService { service, _ ->
        val earlier = daejeon.copy(journeyId = wonju.journeyId, completedAtEpochMs = daejeon.completedAtEpochMs - 1)
        assertTrue(service.completeArrival(daejeon) is ArrivalResult.Applied)
        assertTrue(service.completeArrival(earlier) is ArrivalResult.Applied)
        assertEquals(
            RegionVisitHistory.Available(listOf(
                RegionVisitSummary("daejeon", 2, earlier.completedAtEpochMs, daejeon.completedAtEpochMs),
            )),
            service.regionVisitHistory.value,
        )
    }

    @Test fun distinctRegionsHaveSeparateSummaries() = withService { service, _ ->
        assertTrue(service.completeArrival(wonju) is ArrivalResult.Applied)
        assertTrue(service.completeArrival(daejeon) is ArrivalResult.Applied)
        assertEquals(
            RegionVisitHistory.Available(historyOf(daejeon).regions + historyOf(wonju).regions),
            service.regionVisitHistory.value,
        )
    }

    @Test fun replayUsesOriginalRegionAndTimestampWithoutAnotherVisit() = withService { service, store ->
        val applied = service.completeArrival(daejeon) as ArrivalResult.Applied
        val before = store.saved
        val replay = daejeon.copy(regionId = wonju.regionId, completedAtEpochMs = daejeon.completedAtEpochMs + 1)
        assertEquals(ArrivalResult.AlreadyApplied(applied.receipt), service.completeArrival(replay))
        assertEquals(historyOf(daejeon), service.regionVisitHistory.value)
        assertEquals(before, store.saved)
    }

    @Test fun dailyAndRegionWithoutRegisteredDestinationDoNotCount() = withService { service, _ ->
        val daily = IslandTestData.arrival("D01").copy(regionId = daejeon.regionId)
        assertTrue(service.completeArrival(daily) is ArrivalResult.Applied)
        assertTrue(service.completeArrival(wonju.copy(regionId = null)) is ArrivalResult.Applied)
        assertTrue(service.completeArrival(daejeon.copy(regionId = "unknown")) is ArrivalResult.Rejected)
        assertEquals(emptyHistory, service.regionVisitHistory.value)
    }

    @Test fun knownEmptySaveDoesNotInferVisitsFromDiscoveryIds() = withService(
        MemoryStore(GameSave(discoveredRegionIds = setOf("daejeon"))),
    ) { service, store ->
        assertEquals(GameReadiness.READY, service.readiness.value)
        assertEquals(emptyHistory, service.regionVisitHistory.value)
        assertEquals(0, store.writes)
    }

    @Test fun historyStaysLoadingUntilSaveReadCompletes() = runBlocking {
        val reading = CompletableDeferred<GameSave?>()
        withService(MemoryStore(read = { reading.await() })) { service, _ ->
            assertEquals(RegionVisitHistory.Loading, service.regionVisitHistory.value)
            reading.complete(null)
            assertEquals(emptyHistory, service.regionVisitHistory.first { it != RegionVisitHistory.Loading })
        }
    }

    @Test fun readFailureIsUnavailableAndDoesNotWrite() = withService(
        MemoryStore(read = { throw IOException("Read failed") }),
    ) { service, store ->
        assertEquals(GameReadiness.READ_ERROR, service.readiness.value)
        assertTrue(service.regionVisitHistory.value is RegionVisitHistory.Unavailable)
        assertEquals(0, store.writes)
    }

    @Test fun acknowledgeClearsPendingButKeepsHistory() = withService { service, store ->
        assertTrue(service.completeArrival(daejeon) is ArrivalResult.Applied)
        assertEquals(MutationResult.Success, service.acknowledgeReceipt(daejeon.journeyId))
        assertEquals(null, store.saved?.pendingReceiptId)
        assertEquals(historyOf(daejeon), service.regionVisitHistory.value)
    }

    @Test fun failedCommitAndResetKeepLastCommittedHistory() = withService { service, store ->
        assertTrue(service.completeArrival(daejeon) is ArrivalResult.Applied)
        val before = store.saved
        store.failWrite = true
        assertTrue(service.completeArrival(wonju) is ArrivalResult.StorageFailure)
        assertTrue(service.resetDemoProgress() is MutationResult.StorageFailure)
        assertEquals(historyOf(daejeon), service.regionVisitHistory.value)
        assertEquals(before, store.saved)
        store.failWrite = false
        assertEquals(MutationResult.Success, service.resetDemoProgress())
        assertEquals(emptyHistory, service.regionVisitHistory.value)
    }

    @Test fun existingSchemaOneDataStoreLoadsHistoryWithoutRewritingBytes() = runBlocking {
        val file = temp.root.resolve("legacy-history.preferences_pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val backend = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
            // An already-acknowledged REGION, a DAILY carrying a legacy region value, and
            // an unregistered historical destination. Only the registered REGION counts.
            val payload = """
                {
                  "schemaVersion":1,
                  "discoveredRegionIds":["daejeon","sokcho"],
                  "dailyGrowth":1,
                  "processedReceipts":{
                    "old-region":{"journeyId":"old-region","source":"PLEOS","kind":"REGION",
                      "regionId":"daejeon","completedAtEpochMs":1793468400000,
                      "elapsedSeconds":null,"distanceMeters":null,"growthBefore":1,"growthAfter":1,
                      "discoveredRegionId":"daejeon","grantedItemIds":[]},
                    "old-daily":{"journeyId":"old-daily","source":"DEMO","kind":"DAILY",
                      "regionId":"daejeon","completedAtEpochMs":1793468400001,
                      "elapsedSeconds":null,"distanceMeters":null,"growthBefore":0,"growthAfter":1,
                      "discoveredRegionId":null,"grantedItemIds":[]},
                    "old-unknown":{"journeyId":"old-unknown","source":"DEMO","kind":"REGION",
                      "regionId":"unregistered","completedAtEpochMs":1793468400002,
                      "elapsedSeconds":null,"distanceMeters":null,"growthBefore":1,"growthAfter":1,
                      "discoveredRegionId":null,"grantedItemIds":[]}
                  },
                  "pendingReceiptId":null
                }
            """.trimIndent()
            backend.edit { it[stringPreferencesKey("game_save_json_v1")] = payload }
            val bytes = file.readBytes()

            val service = BGameService(PreferencesGameSaveStore(backend), scope) { IslandTestData.rewards }
            assertEquals(GameReadiness.READY, service.readiness.first { it != GameReadiness.LOADING })
            assertEquals(
                RegionVisitHistory.Available(listOf(RegionVisitSummary("daejeon", 1, 1793468400000, 1793468400000))),
                service.regionVisitHistory.value,
            )
            assertArrayEquals(bytes, file.readBytes())
        } finally { scope.coroutineContext[Job]?.cancelAndJoin() }
    }

    private fun historyOf(command: ArrivalCommand): RegionVisitHistory.Available =
        RegionVisitHistory.Available(listOf(RegionVisitSummary(
            checkNotNull(command.regionId), 1, command.completedAtEpochMs, command.completedAtEpochMs,
        )))

    private fun withService(
        store: MemoryStore = MemoryStore(),
        block: suspend (BGameService, MemoryStore) -> Unit,
    ): Unit = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try { block(BGameService(store, scope) { IslandTestData.rewards }, store) }
        finally { scope.cancel() }
    }

    private class MemoryStore(
        var saved: GameSave? = null,
        private val read: (suspend () -> GameSave?)? = null,
    ) : GameSaveStore {
        var writes = 0
        var failWrite = false
        override suspend fun read(): GameSave? = read?.invoke() ?: saved
        override suspend fun update(transform: (GameSave) -> GameSave): GameSave {
            val next = transform(saved ?: GameSave())
            if (failWrite) throw IOException("Failed before commit")
            saved = next
            writes++
            return next
        }
    }
}
