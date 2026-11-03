package io.kailand.feature.island

import io.kailand.core.contract.ArrivalEvidence
import io.kailand.core.contract.ArrivalResult
import io.kailand.core.contract.DataSource
import io.kailand.core.contract.GameAction
import io.kailand.core.contract.GameSnapshot
import io.kailand.core.contract.MutationResult
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.*
import org.junit.Test

class RegionArrivalTest {
    private val d03 = IslandTestData.arrival("D03")
    private val d04 = IslandTestData.arrival("D04")
    private val d05 = IslandTestData.arrival("D05")

    @Test fun d03CommitsGiftsDiscoveryOriginalAndPendingTogether() = withService { service, store ->
        val result = service.completeArrival(d03) as ArrivalResult.Applied
        val expected = IslandTestData.expected("D03")
        val gifts = expected.getValue("grantedItemIds").jsonArray.map { it.jsonPrimitive.content }
        val receipt = result.receipt
        assertEquals(gifts, receipt.grantedItemIds)
        assertEquals(expected.getValue("discoveredRegionId").jsonPrimitive.content, receipt.discoveredRegionId)
        assertEquals(expected.getValue("growthDelta").jsonPrimitive.int, receipt.growthAfter - receipt.growthBefore)
        assertEquals(d03.journeyId, receipt.journeyId)
        assertEquals(d03.source, receipt.source)
        assertEquals(d03.kind, receipt.kind)
        assertEquals(d03.regionId, receipt.regionId)
        assertEquals(d03.completedAtEpochMs, receipt.completedAtEpochMs)
        assertEquals(d03.elapsedSeconds, receipt.elapsedSeconds)
        assertEquals(d03.distanceMeters, receipt.distanceMeters)
        assertEquals(1, store.commits.size)
        val saved = store.commits.single()
        assertEquals(gifts.toSet(), saved.ownedItemIds)
        assertEquals(setOf("daejeon"), saved.discoveredRegionIds)
        assertEquals(receipt, saved.processedReceipts.getValue(d03.journeyId).toContract())
        assertEquals(d03.journeyId, saved.pendingReceiptId)
        assertEquals(saved.toSnapshot(), service.state.value)
    }

    @Test fun d04ReturnsOriginalWithoutNewEffects() = withService { service, store ->
        val original = (service.completeArrival(d03) as ArrivalResult.Applied).receipt
        val before = store.save
        val replay = service.completeArrival(d04) as ArrivalResult.AlreadyApplied
        assertEquals(original, replay.receipt) // Original gift list is intentionally retained.
        assertEquals(before, store.save)
        assertEquals(1, store.commits.size)
    }

    @Test fun d05NewJourneyRevisitStoresEmptyRewardReceipt() = withService { service, store ->
        val original = (service.completeArrival(d03) as ArrivalResult.Applied).receipt
        assertTrue(service.completeArrival(d04) is ArrivalResult.AlreadyApplied)
        val before = service.state.value
        val receipt = (service.completeArrival(d05) as ArrivalResult.Applied).receipt
        assertEquals(IslandTestData.expected("D05").getValue("grantedItemIds").jsonArray.size, receipt.grantedItemIds.size)
        assertNull(receipt.discoveredRegionId)
        assertEquals(0, receipt.growthAfter - receipt.growthBefore)
        assertEquals(before.ownedItemIds, service.state.value.ownedItemIds)
        assertEquals(before.discoveredRegionIds, service.state.value.discoveredRegionIds)
        assertEquals(d05.journeyId, service.state.value.pendingReceipt?.journeyId)
        assertEquals(2, store.save!!.processedReceipts.size)
        assertEquals(original, store.save!!.processedReceipts.getValue(d03.journeyId).toContract())
        assertEquals(original, (service.completeArrival(d03) as ArrivalResult.AlreadyApplied).receipt)
        assertEquals(d05.journeyId, service.state.value.pendingReceipt?.journeyId)
    }

    @Test fun arrivalFailureAfterCalculationChangesNeitherStorageNorPublishedState() = withService { service, store ->
        val before = service.state.value
        val beforeSave = store.save
        val observed = mutableListOf<GameSnapshot>()
        val collector = CoroutineScope(Dispatchers.Unconfined).launch { service.state.collect { observed += it } }
        try {
            store.failWrite = true
            assertTrue(service.completeArrival(d03) is ArrivalResult.StorageFailure)
            assertSame(before, service.state.value)
            assertEquals(beforeSave, store.save)
            assertEquals(listOf(before), observed)
            assertTrue(store.commits.isEmpty())
            store.failWrite = false
            assertTrue(service.completeArrival(d03) is ArrivalResult.Applied)
            assertEquals(1, store.commits.size)
        } finally { collector.cancel() }
    }

    @Test fun regeneratedServiceReturnsOriginalEvenIfReplayPayloadChanged() = withService { service, store ->
        val original = (service.completeArrival(d03) as ArrivalResult.Applied).receipt
        assertEquals(MutationResult.Success, service.acknowledgeReceipt(d03.journeyId))
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val recreated = BGameService(store, scope) { IslandTestData.rewards }
            val result = recreated.completeArrival(d03.copy(completedAtEpochMs = d03.completedAtEpochMs + 1,
                elapsedSeconds = null, distanceMeters = null)) as ArrivalResult.AlreadyApplied
            assertEquals(original, result.receipt)
            assertNull(recreated.state.value.pendingReceipt)
            assertEquals(2, store.commits.size)
        } finally { scope.cancel() }
    }

    @Test fun staleOrUnknownAcknowledgeCannotClearNewPending() = withService { service, store ->
        service.completeArrival(d03)
        service.completeArrival(d05)
        val before = store.save
        assertTrue(service.acknowledgeReceipt(d03.journeyId) is MutationResult.Rejected)
        assertTrue(service.acknowledgeReceipt("unknown") is MutationResult.Rejected)
        assertEquals(before, store.save)
        assertEquals(d05.journeyId, service.state.value.pendingReceipt?.journeyId)
        assertEquals(MutationResult.Success, service.acknowledgeReceipt(d05.journeyId))
        assertNull(service.state.value.pendingReceipt)
        assertEquals(2, store.save!!.processedReceipts.size)
        val commits = store.commits.size
        assertEquals(MutationResult.Success, service.acknowledgeReceipt(d05.journeyId))
        assertTrue(service.acknowledgeReceipt("unknown") is MutationResult.Rejected)
        assertEquals(commits, store.commits.size)
    }

    @Test fun acknowledgementWriteFailureRetainsPendingAndOriginal() = withService { service, store ->
        service.completeArrival(d03)
        val before = store.save
        val snapshot = service.state.value
        store.failWrite = true
        assertTrue(service.acknowledgeReceipt(d03.journeyId) is MutationResult.StorageFailure)
        assertEquals(before, store.save)
        assertSame(snapshot, service.state.value)
        store.failWrite = false
        assertEquals(MutationResult.Success, service.acknowledgeReceipt(d03.journeyId))
        assertTrue(service.completeArrival(d03) is ArrivalResult.AlreadyApplied)
        assertNull(service.state.value.pendingReceipt)
    }

    @Test fun pleosAndNullMeasurementsArePreservedWithoutDailyGrowth() = withService(
        GameSave(dailyGrowth = 2, lastDailyGrantDate = "2026-11-01"),
    ) { service, store ->
        val input = d03.copy(source = DataSource.PLEOS, evidence = ArrivalEvidence.PLEOS_FINAL_DESTINATION,
            elapsedSeconds = null, distanceMeters = null)
        val receipt = (service.completeArrival(input) as ArrivalResult.Applied).receipt
        assertEquals(DataSource.PLEOS, receipt.source)
        assertNull(receipt.elapsedSeconds)
        assertNull(receipt.distanceMeters)
        assertEquals(2, receipt.growthBefore)
        assertEquals(2, receipt.growthAfter)
        assertEquals("2026-11-01", store.save!!.lastDailyGrantDate)
    }

    @Test fun invalidInputAndUnimplementedFeaturesNeverWrite() = withService { service, store ->
        val invalid = listOf(
            d03.copy(journeyId = "invalid"), d03.copy(routeRevision = -1),
            d03.copy(source = DataSource.PLEOS), d03.copy(evidence = ArrivalEvidence.PLEOS_FINAL_DESTINATION),
            d03.copy(completedAtEpochMs = -1), d03.copy(elapsedSeconds = -1),
            d03.copy(distanceMeters = -1.0), d03.copy(distanceMeters = Double.NaN),
            d03.copy(distanceMeters = Double.POSITIVE_INFINITY), d03.copy(regionId = "missing"),
            d03.copy(regionId = ""), IslandTestData.arrival("D01"),
        )
        invalid.forEach { assertTrue("Must reject $it", service.completeArrival(it) is ArrivalResult.Rejected) }
        assertTrue(service.apply(GameAction.SelectCharacter("momo")) is MutationResult.Rejected)
        assertNull(store.save)
        assertTrue(store.commits.isEmpty())
    }

    @Test fun nullRegionRecordsCompletionWithoutInventingRewards() = withService { service, store ->
        val receipt = (service.completeArrival(d03.copy(regionId = null)) as ArrivalResult.Applied).receipt
        assertNull(receipt.regionId)
        assertNull(receipt.discoveredRegionId)
        assertTrue(receipt.grantedItemIds.isEmpty())
        assertTrue(store.save!!.discoveredRegionIds.isEmpty())
        assertTrue(store.save!!.ownedItemIds.isEmpty())
    }

    @Test fun concurrentDuplicatesCommitOnlyOneReward() = withService { service, store ->
        val results = coroutineScope {
            (1..16).map { async(Dispatchers.Default) { service.completeArrival(d03) } }.awaitAll()
        }
        assertEquals(1, results.count { it is ArrivalResult.Applied })
        assertEquals(15, results.count { it is ArrivalResult.AlreadyApplied })
        assertEquals(1, store.commits.size)
        val original = (results.first { it is ArrivalResult.Applied } as ArrivalResult.Applied).receipt
        results.filterIsInstance<ArrivalResult.AlreadyApplied>().forEach { assertEquals(original, it.receipt) }
    }

    @Test fun corruptHistoricalReceiptBlocksReadAndEveryWrite() = withService { service, store ->
        service.completeArrival(d03)
        service.acknowledgeReceipt(d03.journeyId)
        val original = store.save!!
        val receipt = original.processedReceipts.getValue(d03.journeyId)
        store.save = original.copy(processedReceipts = mapOf(d03.journeyId to receipt.copy(source = "BROKEN")))
        val corrupt = store.save
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val recreated = BGameService(store, scope) { IslandTestData.rewards }
            assertEquals(GameReadiness.READ_ERROR, recreated.readiness.value)
            assertTrue(recreated.completeArrival(d05) is ArrivalResult.StorageFailure)
            assertTrue(recreated.acknowledgeReceipt(d03.journeyId) is MutationResult.StorageFailure)
            assertTrue(recreated.resetDemoProgress() is MutationResult.StorageFailure)
            assertEquals(corrupt, store.save)
        } finally { scope.cancel() }
    }

    private fun withService(
        initial: GameSave? = null,
        block: suspend (BGameService, RecordingStore) -> Unit,
    ): Unit = runBlocking {
        val store = RecordingStore(initial)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try { block(BGameService(store, scope) { IslandTestData.rewards }, store) }
        finally { scope.cancel() }
    }

    private class RecordingStore(var save: GameSave?) : GameSaveStore {
        var failWrite = false
        val commits = mutableListOf<GameSave>()
        override suspend fun read(): GameSave? = save
        override suspend fun update(transform: (GameSave) -> GameSave): GameSave {
            val current = save ?: GameSave()
            val next = transform(current)
            next.toSnapshot()
            if (failWrite) throw IOException("Simulated failure after calculation, before commit")
            if (next != current) {
                save = GameSaveCodec.decode(GameSaveCodec.encode(next))
                commits += save!!
            }
            return next
        }
    }
}
