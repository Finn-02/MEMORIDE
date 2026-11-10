package io.kailand.feature.island

import io.kailand.core.contract.ArrivalCommand
import io.kailand.core.contract.ArrivalEvidence
import io.kailand.core.contract.ArrivalResult
import io.kailand.core.contract.DataSource
import io.kailand.core.contract.GameAction
import io.kailand.core.contract.MutationResult
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class UserConfirmedArrivalTest {
    @Test fun exactlyThreeOfAllNineSourceEvidencePairsAreAccepted() = withService { service, store ->
        var accepted = 0
        var rejected = 0
        for (source in DataSource.entries) for (evidence in ArrivalEvidence.entries) {
            val command = confirmed("D03").copy(
                journeyId = id(source.ordinal * 3 + evidence.ordinal), source = source, evidence = evidence,
            )
            val before = store.save
            val snapshot = service.state.value
            val commits = store.commits
            val result = service.completeArrival(command)
            val matching = when (source) {
                DataSource.DEMO -> evidence == ArrivalEvidence.DEMO_FINAL_DESTINATION
                DataSource.PLEOS -> evidence == ArrivalEvidence.PLEOS_FINAL_DESTINATION
                DataSource.USER_CONFIRMED -> evidence == ArrivalEvidence.USER_CONFIRMED_DESTINATION
            }
            if (matching) {
                assertTrue("$source / $evidence", result is ArrivalResult.Applied)
                assertEquals(source, (result as ArrivalResult.Applied).receipt.source)
                accepted++
            } else {
                assertTrue("$source / $evidence", result is ArrivalResult.Rejected)
                assertEquals(before, store.save)
                assertEquals(snapshot, service.state.value)
                assertEquals(commits, store.commits)
                rejected++
            }
        }
        assertEquals(3, accepted)
        assertEquals(6, rejected)
    }

    @Test fun nonNullUserMeasurementsIncludingZeroAreRejectedBeforeReplay() = withService { service, store ->
        val command = confirmed("D03")
        val invalid = listOf(
            command.copy(elapsedSeconds = 0), command.copy(distanceMeters = 0.0),
            command.copy(elapsedSeconds = 1, distanceMeters = 1.0),
            command.copy(elapsedSeconds = -1), command.copy(distanceMeters = Double.NaN),
        )
        for (input in invalid) assertTrue(service.completeArrival(input) is ArrivalResult.Rejected)
        assertNull(store.save)
        val original = (service.completeArrival(command) as ArrivalResult.Applied).receipt
        val saved = store.save
        for (input in invalid) assertTrue(service.completeArrival(input) is ArrivalResult.Rejected)
        assertEquals(saved, store.save)
        assertEquals(original, (service.completeArrival(command) as ArrivalResult.AlreadyApplied).receipt)
    }

    @Test fun newDailyKeepsConfirmationTimeWithoutGrowthOrRegionRewards() = withService { service, store ->
        val times = listOf(
            "2026-11-05T14:59:59.999Z", // Nov 5, just before KST midnight.
            "2026-11-05T15:00:00Z",     // Nov 6, midnight.
            "2026-11-06T14:59:59.999Z", // Same KST date.
            "2026-11-06T15:00:00Z",     // Nov 7.
            "2026-11-07T15:00:00Z",     // Nov 8.
        )
        times.forEachIndexed { index, time ->
            val input = confirmed("D01").copy(journeyId = id(index),
                completedAtEpochMs = Instant.parse(time).toEpochMilli(), regionId = "daejeon")
            val receipt = (service.completeArrival(input) as ArrivalResult.Applied).receipt
            assertEquals(0, receipt.growthBefore)
            assertEquals(0, receipt.growthAfter)
            assertEquals(input.completedAtEpochMs, receipt.completedAtEpochMs)
            assertEquals(DataSource.USER_CONFIRMED, receipt.source)
            assertNull(receipt.regionId)
            assertNull(receipt.discoveredRegionId)
            assertNull(receipt.elapsedSeconds)
            assertNull(receipt.distanceMeters)
            assertTrue(receipt.grantedItemIds.isEmpty())
            assertTrue(store.save!!.ownedItemIds.isEmpty())
            assertTrue(store.save!!.discoveredRegionIds.isEmpty())
        }
        assertNull(store.save!!.lastDailyGrantDate)
    }

    @Test fun regionFirstVisitRevisitAndD06KeepCatalogRewardsAndNulls() = withService { service, store ->
        for ((scenario, region) in listOf("D03" to "daejeon", "D06" to "wonju")) {
            val input = confirmed(scenario)
            val receipt = (service.completeArrival(input) as ArrivalResult.Applied).receipt
            assertEquals(IslandTestData.rewards.itemsFor(region), receipt.grantedItemIds)
            assertEquals(3, receipt.grantedItemIds.size)
            assertEquals(region, receipt.discoveredRegionId)
            assertEquals(DataSource.USER_CONFIRMED, receipt.source)
            assertNull(receipt.elapsedSeconds)
            assertNull(receipt.distanceMeters)
            assertEquals(if (region == "daejeon") 0 else 1, receipt.growthBefore)
            assertEquals(1, receipt.growthAfter)
            assertEquals(receipt, store.save!!.processedReceipts.getValue(input.journeyId).toContract())
            val revisit = (service.completeArrival(input.copy(journeyId = id(region.length)))
                as ArrivalResult.Applied).receipt
            assertTrue(revisit.grantedItemIds.isEmpty())
            assertNull(revisit.discoveredRegionId)
            assertEquals(1, revisit.growthBefore)
            assertEquals(1, revisit.growthAfter)
        }
        assertEquals(setOf("daejeon", "wonju"), store.save!!.discoveredRegionIds)
        assertEquals(6, store.save!!.ownedItemIds.size)
    }

    @Test fun concurrentDuplicateCallsCommitOnceAndReturnOriginalReceipt() = withService { service, store ->
        val command = confirmed("D03")
        val results = coroutineScope {
            (1..16).map { async(Dispatchers.Default) { service.completeArrival(command) } }.awaitAll()
        }
        assertEquals(1, results.count { it is ArrivalResult.Applied })
        assertEquals(15, results.count { it is ArrivalResult.AlreadyApplied })
        val original = (results.first { it is ArrivalResult.Applied } as ArrivalResult.Applied).receipt
        results.filterIsInstance<ArrivalResult.AlreadyApplied>().forEach { assertEquals(original, it.receipt) }
        assertEquals(1, store.commits)
    }

    @Test fun failedSaveRetryKeepsFirstConfirmationAcrossKstMidnight() = withService { service, store ->
        val input = confirmed("D06").copy(completedAtEpochMs = Instant.parse("2026-11-05T14:59:59.999Z").toEpochMilli())
        val before = service.state.value
        store.failWrite = true
        assertTrue(service.completeArrival(input) is ArrivalResult.StorageFailure)
        assertEquals(before, service.state.value)
        assertNull(store.save)
        assertEquals(0, store.commits)
        store.failWrite = false
        // Retry exactly the first command, rather than replacing its ID or confirmation time.
        val original = (service.completeArrival(input) as ArrivalResult.Applied).receipt
        assertEquals(input.completedAtEpochMs, original.completedAtEpochMs)
        assertEquals(1, original.growthAfter)
        assertEquals("2026-11-05", store.save!!.lastDailyGrantDate)
        val next = input.copy(journeyId = id(99), completedAtEpochMs = Instant.parse("2026-11-05T15:00:00Z").toEpochMilli())
        assertEquals(2, (service.completeArrival(next) as ArrivalResult.Applied).receipt.growthAfter)
        assertEquals(original, (service.completeArrival(input) as ArrivalResult.AlreadyApplied).receipt)
        assertEquals(next.journeyId, store.save!!.pendingReceiptId)
    }

    @Test fun oldReplayPreservesLatestPendingAndAcknowledgedReplayNeverReopensIt() = withService { service, store ->
        val old = confirmed("D03")
        val original = (service.completeArrival(old) as ArrivalResult.Applied).receipt
        service.completeArrival(IslandTestData.arrival("D01"))
        val latest = confirmed("D06")
        val newest = (service.completeArrival(latest) as ArrivalResult.Applied).receipt
        val before = store.save
        assertEquals(original, (service.completeArrival(old.copy(completedAtEpochMs = latest.completedAtEpochMs))
            as ArrivalResult.AlreadyApplied).receipt)
        assertTrue(service.acknowledgeReceipt(old.journeyId) is MutationResult.Rejected)
        assertEquals(before, store.save)
        assertEquals(newest, service.state.value.pendingReceipt)
        assertEquals(MutationResult.Success, service.acknowledgeReceipt(latest.journeyId))
        val acknowledged = store.save
        assertEquals(original, (service.completeArrival(old) as ArrivalResult.AlreadyApplied).receipt)
        assertEquals(newest, (service.completeArrival(latest) as ArrivalResult.AlreadyApplied).receipt)
        assertEquals(acknowledged, store.save)
        assertNull(service.state.value.pendingReceipt)
    }

    @Test fun loadingAndReadErrorStillBlockUserConfirmedMutations() = runBlocking {
        val loaded = CompletableDeferred<GameSave?>()
        var writes = 0
        val store = object : GameSaveStore {
            override suspend fun read(): GameSave? = loaded.await()
            override suspend fun update(transform: (GameSave) -> GameSave): GameSave {
                writes++
                error("Must not write")
            }
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val service = BGameService(store, scope) { IslandTestData.rewards }
            assertEquals(GameReadiness.LOADING, service.readiness.value)
            assertTrue(service.completeArrival(confirmed("D03")) is ArrivalResult.StorageFailure)
            loaded.completeExceptionally(IOException("Read failed"))
            assertEquals(GameReadiness.READ_ERROR, service.readiness.first { it != GameReadiness.LOADING })
            assertTrue(service.completeArrival(confirmed("D03")) is ArrivalResult.StorageFailure)
            assertTrue(service.acknowledgeReceipt(confirmed("D03").journeyId) is MutationResult.StorageFailure)
            assertTrue(service.apply(GameAction.SelectCharacter("momo")) is MutationResult.StorageFailure)
            assertTrue(service.resetDemoProgress() is MutationResult.StorageFailure)
            assertEquals(0, writes)
        } finally { scope.cancel() }
    }

    private fun withService(block: suspend (BGameService, RecordingStore) -> Unit): Unit = runBlocking {
        val store = RecordingStore()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try { block(BGameService(store, scope) { IslandTestData.rewards }, store) }
        finally { scope.cancel() }
    }

    private class RecordingStore : GameSaveStore {
        var save: GameSave? = null
        var failWrite = false
        var commits = 0
        override suspend fun read(): GameSave? = save
        override suspend fun update(transform: (GameSave) -> GameSave): GameSave {
            val current = save ?: GameSave()
            val next = transform(current)
            next.toSnapshot()
            if (failWrite) throw IOException("Failure after calculation, before commit")
            if (current != next) {
                save = GameSaveCodec.decode(GameSaveCodec.encode(next))
                commits++
            }
            return next
        }
    }

    companion object {
        internal fun confirmed(scenario: String): ArrivalCommand = IslandTestData.arrival(scenario).copy(
            source = DataSource.USER_CONFIRMED, evidence = ArrivalEvidence.USER_CONFIRMED_DESTINATION,
            elapsedSeconds = null, distanceMeters = null,
        )
        private fun id(value: Int) = "00000000-0000-4000-8000-${value.toString().padStart(12, '0')}"
    }
}
