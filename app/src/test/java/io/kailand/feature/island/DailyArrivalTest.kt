package io.kailand.feature.island

import io.kailand.core.contract.ArrivalCommand
import io.kailand.core.contract.ArrivalEvidence
import io.kailand.core.contract.ArrivalResult
import io.kailand.core.contract.DataSource
import io.kailand.core.contract.GameSnapshot
import io.kailand.core.contract.MutationResult
import java.io.IOException
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
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

class DailyArrivalTest {
    private val d01 = IslandTestData.arrival("D01")
    private val d02 = IslandTestData.arrival("D02")

    @Test fun d01StoresGrowthDateOriginalAndPendingInOneCommit() = withService { service, store ->
        val receipt = (service.completeArrival(d01) as ArrivalResult.Applied).receipt
        assertEquals(IslandTestData.expected("D01").getValue("growthDelta").jsonPrimitive.int,
            receipt.growthAfter - receipt.growthBefore)
        assertEquals(0, receipt.growthBefore)
        assertEquals(1, receipt.growthAfter)
        assertEquals(d01.journeyId, receipt.journeyId)
        assertEquals(d01.kind, receipt.kind)
        assertEquals(d01.source, receipt.source)
        assertEquals(d01.completedAtEpochMs, receipt.completedAtEpochMs)
        assertEquals(d01.elapsedSeconds, receipt.elapsedSeconds)
        assertEquals(d01.distanceMeters, receipt.distanceMeters)
        assertNull(receipt.regionId)
        assertNull(receipt.discoveredRegionId)
        assertTrue(receipt.grantedItemIds.isEmpty())
        val saved = store.commits.single()
        assertEquals(1, saved.dailyGrowth)
        assertEquals("2026-10-31", saved.lastDailyGrantDate)
        assertEquals(receipt, saved.processedReceipts.getValue(d01.journeyId).toContract())
        assertEquals(d01.journeyId, saved.pendingReceiptId)
        assertEquals(saved.toSnapshot(), service.state.value)
    }

    @Test fun d02SameDateIsAppliedWithNoAdditionalGrowth() = withService { service, store ->
        val original = (service.completeArrival(d01) as ArrivalResult.Applied).receipt
        val receipt = (service.completeArrival(d02) as ArrivalResult.Applied).receipt
        assertEquals(IslandTestData.expected("D02").getValue("growthDelta").jsonPrimitive.int,
            receipt.growthAfter - receipt.growthBefore)
        assertEquals(1, receipt.growthBefore)
        assertEquals(1, receipt.growthAfter)
        assertEquals(2, store.commits.size)
        assertEquals("2026-10-31", store.save!!.lastDailyGrantDate)
        assertEquals(2, store.save!!.processedReceipts.size)
        assertEquals(d02.journeyId, service.state.value.pendingReceipt?.journeyId)
        assertEquals(original, (service.completeArrival(d01) as ArrivalResult.AlreadyApplied).receipt)
        assertEquals(d02.journeyId, service.state.value.pendingReceipt?.journeyId)
        assertEquals(2, store.commits.size)
    }

    @Test fun distinctDatesGrowToCatalogCapAndKeepReceiptsAtCap() = withService { service, store ->
        assertEquals(3, IslandTestData.rewards.dailyGrowthCap)
        val expectedGrowth = listOf(1, 2, 3, 3)
        expectedGrowth.forEachIndexed { index, growth ->
            val input = daily(100 + index, "2026-11-0${index + 1}T01:00:00Z")
            val receipt = (service.completeArrival(input) as ArrivalResult.Applied).receipt
            assertEquals(growth, receipt.growthAfter)
            assertTrue(receipt.grantedItemIds.isEmpty())
            assertEquals(growth, service.state.value.dailyGrowth)
            assertEquals(input.journeyId, service.state.value.pendingReceipt?.journeyId)
        }
        assertEquals(4, store.save!!.processedReceipts.size)
        assertEquals("2026-11-03", store.save!!.lastDailyGrantDate) // Last actual grant, not capped completion.
        assertTrue(store.save!!.ownedItemIds.isEmpty())
        assertTrue(store.save!!.discoveredRegionIds.isEmpty())
    }

    @Test fun seoulMidnightAndUtcMidnightUseSeoulDates() = withService { service, store ->
        val before = daily(110, "2026-11-03T14:59:59.999Z")
        val midnight = daily(111, "2026-11-03T15:00:00Z")
        val utcMidnight = daily(112, "2026-11-04T00:00:00Z")
        assertEquals(1, (service.completeArrival(before) as ArrivalResult.Applied).receipt.growthAfter)
        assertEquals("2026-11-03", store.save!!.lastDailyGrantDate)
        assertEquals(2, (service.completeArrival(midnight) as ArrivalResult.Applied).receipt.growthAfter)
        assertEquals("2026-11-04", store.save!!.lastDailyGrantDate)
        val noSecondGrant = (service.completeArrival(utcMidnight) as ArrivalResult.Applied).receipt
        assertEquals(2, noSecondGrant.growthBefore)
        assertEquals(2, noSecondGrant.growthAfter)
    }

    @Test fun monthYearAndLeapDayBoundariesUseCalendarDates() {
        val boundaries = listOf(
            Triple("2026-01-31T14:59:59.999Z", "2026-01-31T15:00:00Z", "2026-02-01"),
            Triple("2026-12-31T14:59:59.999Z", "2026-12-31T15:00:00Z", "2027-01-01"),
            Triple("2028-02-28T14:59:59.999Z", "2028-02-28T15:00:00Z", "2028-02-29"),
        )
        boundaries.forEach { (before, after, date) ->
            withService { service, store ->
                service.completeArrival(daily(120, before))
                assertEquals(2, (service.completeArrival(daily(121, after)) as ArrivalResult.Applied).receipt.growthAfter)
                assertEquals(date, store.save!!.lastDailyGrantDate)
            }
        }
    }

    @Test fun outOfOrderDifferentIdsCannotGrantTwiceForAnEarlierDate() = withService { service, store ->
        service.completeArrival(daily(130, "2026-11-01T01:00:00Z"))
        service.completeArrival(daily(131, "2026-11-02T01:00:00Z"))
        val olderDate = (service.completeArrival(daily(132, "2026-11-01T02:00:00Z")) as ArrivalResult.Applied).receipt
        assertEquals(2, olderDate.growthBefore)
        assertEquals(2, olderDate.growthAfter)
        assertEquals("2026-11-02", store.save!!.lastDailyGrantDate)
        assertEquals(3, (service.completeArrival(daily(133, "2026-10-31T01:00:00Z")) as ArrivalResult.Applied).receipt.growthAfter)
    }

    @Test fun dailyNormalizesAnyRegionWithoutChangingRegionProgress() {
        listOf(null, "daejeon", "not-registered", "").forEach { regionId ->
            val initial = GameSave(ownedItemIds = setOf("wonju_potato"), discoveredRegionIds = setOf("wonju"))
            withService(initial) { service, store ->
                val receipt = (service.completeArrival(d01.copy(regionId = regionId)) as ArrivalResult.Applied).receipt
                assertNull(receipt.regionId)
                assertNull(receipt.discoveredRegionId)
                assertTrue(receipt.grantedItemIds.isEmpty())
                assertEquals(initial.ownedItemIds, store.save!!.ownedItemIds)
                assertEquals(initial.discoveredRegionIds, store.save!!.discoveredRegionIds)
                assertNull(store.save!!.processedReceipts.getValue(d01.journeyId).regionId)
            }
        }
    }

    @Test fun failedDailyCommitPublishesNothingAndRetryGrantsExactlyOnce() = withService { service, store ->
        val before = service.state.value
        val originalSave = store.save
        val observed = mutableListOf<GameSnapshot>()
        val collector = CoroutineScope(Dispatchers.Unconfined).launch { service.state.collect { observed += it } }
        try {
            store.failWrite = true
            assertTrue(service.completeArrival(d01) is ArrivalResult.StorageFailure)
            assertSame(before, service.state.value)
            assertEquals(originalSave, store.save)
            assertEquals(listOf(before), observed)
            assertTrue(store.commits.isEmpty())
            store.failWrite = false
            assertTrue(service.completeArrival(d01) is ArrivalResult.Applied)
            assertTrue(service.completeArrival(d01) is ArrivalResult.AlreadyApplied)
            assertEquals(1, store.commits.size)
            assertEquals(1, service.state.value.dailyGrowth)
        } finally { collector.cancel() }
    }

    @Test fun serviceRecreationPreservesDailyLimitOriginalAndAcknowledgement() = withService { service, store ->
        val original = (service.completeArrival(d01) as ArrivalResult.Applied).receipt
        assertEquals(MutationResult.Success, service.acknowledgeReceipt(d01.journeyId))
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val recreated = BGameService(store, scope) { IslandTestData.rewards }
            val changedReplay = d01.copy(completedAtEpochMs = d01.completedAtEpochMs + 86_400_000,
                elapsedSeconds = null, distanceMeters = null)
            assertEquals(original, (recreated.completeArrival(changedReplay) as ArrivalResult.AlreadyApplied).receipt)
            assertNull(recreated.state.value.pendingReceipt)
            assertEquals(1, (recreated.completeArrival(d02) as ArrivalResult.Applied).receipt.growthAfter)
            assertTrue(recreated.acknowledgeReceipt(d01.journeyId) is MutationResult.Rejected)
            assertEquals(d02.journeyId, recreated.state.value.pendingReceipt?.journeyId)
            assertEquals(original, store.save!!.processedReceipts.getValue(d01.journeyId).toContract())
        } finally { scope.cancel() }
    }

    @Test fun d06PreservesWonjuOriginalAndNullMeasurementsAcrossRecreation() = withService { service, store ->
        val input = IslandTestData.arrival("D06")
        val original = (service.completeArrival(input) as ArrivalResult.Applied).receipt
        val expected = IslandTestData.expected("D06")
        assertEquals(expected.getValue("grantedItemIds").jsonArray.map { it.jsonPrimitive.content }, original.grantedItemIds)
        assertEquals(expected.getValue("discoveredRegionId").jsonPrimitive.content, original.discoveredRegionId)
        assertEquals(0, original.growthAfter - original.growthBefore)
        assertEquals(input.completedAtEpochMs, original.completedAtEpochMs)
        assertEquals(input.source, original.source)
        assertNull(original.elapsedSeconds)
        assertNull(original.distanceMeters)
        assertEquals(setOf("wonju"), store.save!!.discoveredRegionIds)
        assertNull(store.save!!.lastDailyGrantDate)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val recreated = BGameService(store, scope) { IslandTestData.rewards }
            assertEquals(original, recreated.state.value.pendingReceipt)
            assertEquals(original, (recreated.completeArrival(input.copy(elapsedSeconds = 99,
                distanceMeters = 123.0)) as ArrivalResult.AlreadyApplied).receipt)
        } finally { scope.cancel() }
    }

    @Test fun dailyKeepsSourceAndNullMeasurementsAndRejectsInvalidInputs() = withService { service, store ->
        val invalid = listOf(d01.copy(evidence = ArrivalEvidence.PLEOS_FINAL_DESTINATION),
            d01.copy(journeyId = "invalid"), d01.copy(routeRevision = -1),
            d01.copy(completedAtEpochMs = -1), d01.copy(elapsedSeconds = -1),
            d01.copy(distanceMeters = Double.NaN))
        invalid.forEach { assertTrue(service.completeArrival(it) is ArrivalResult.Rejected) }
        assertTrue(store.commits.isEmpty())
        // Contract-only test input; no SDK adapter or actual Pleos data is modified.
        val input = d01.copy(source = DataSource.PLEOS, evidence = ArrivalEvidence.PLEOS_FINAL_DESTINATION,
            elapsedSeconds = null, distanceMeters = null)
        val receipt = (service.completeArrival(input) as ArrivalResult.Applied).receipt
        assertEquals(DataSource.PLEOS, receipt.source)
        assertEquals(input.completedAtEpochMs, receipt.completedAtEpochMs)
        assertNull(receipt.elapsedSeconds)
        assertNull(receipt.distanceMeters)
    }

    @Test fun simultaneousDifferentJourneysOnOneDateGrantOnlyOnce() = withService { service, store ->
        val results = coroutineScope {
            (140..155).map { id -> async(Dispatchers.Default) {
                service.completeArrival(daily(id, "2026-11-03T01:00:00Z")) as ArrivalResult.Applied
            } }.awaitAll()
        }
        assertEquals(1, results.sumOf { it.receipt.growthAfter - it.receipt.growthBefore })
        assertEquals(1, service.state.value.dailyGrowth)
        assertEquals(16, store.save!!.processedReceipts.size)
    }

    @Test fun dailyBlockedWhileLoadingOrOnReadError() = runBlocking {
        val gate = CompletableDeferred<GameSave?>()
        val store = RecordingStore()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val loading = BGameService(object : GameSaveStore by store {
                override suspend fun read(): GameSave? = gate.await()
            }, scope) { IslandTestData.rewards }
            assertEquals(GameReadiness.LOADING, loading.readiness.value)
            assertTrue(loading.completeArrival(d01) is ArrivalResult.StorageFailure)
            assertTrue(loading.acknowledgeReceipt(d01.journeyId) is MutationResult.StorageFailure)
            gate.complete(GameSave())
            assertEquals(GameReadiness.READY, loading.readiness.value)
            val broken = BGameService(object : GameSaveStore by store {
                override suspend fun read(): GameSave? = throw IOException("Unreadable save")
            }, scope) { IslandTestData.rewards }
            assertEquals(GameReadiness.READ_ERROR, broken.readiness.value)
            assertTrue(broken.completeArrival(d01) is ArrivalResult.StorageFailure)
            assertTrue(broken.acknowledgeReceipt(d01.journeyId) is MutationResult.StorageFailure)
            assertTrue(store.commits.isEmpty())
        } finally { scope.cancel() }
    }

    private fun daily(id: Int, instant: String): ArrivalCommand = d01.copy(
        journeyId = UUID(0, id.toLong()).toString(), completedAtEpochMs = Instant.parse(instant).toEpochMilli(),
    )

    private fun withService(
        initial: GameSave? = null,
        block: suspend (BGameService, RecordingStore) -> Unit,
    ): Unit = runBlocking {
        val store = RecordingStore(initial)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try { block(BGameService(store, scope) { IslandTestData.rewards }, store) }
        finally { scope.cancel() }
    }

    private class RecordingStore(var save: GameSave? = null) : GameSaveStore {
        var failWrite = false
        val commits = mutableListOf<GameSave>()
        override suspend fun read(): GameSave? = save
        override suspend fun update(transform: (GameSave) -> GameSave): GameSave {
            val current = save ?: GameSave()
            val next = transform(current)
            next.toSnapshot()
            if (failWrite) throw IOException("Failure after calculation, before durable commit")
            if (next != current) {
                save = GameSaveCodec.decode(GameSaveCodec.encode(next))
                commits += save!!
            }
            return next
        }
    }
}
