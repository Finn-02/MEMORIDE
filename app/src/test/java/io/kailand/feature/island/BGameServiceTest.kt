package io.kailand.feature.island

import io.kailand.core.contract.ArrivalCommand
import io.kailand.core.contract.ArrivalEvidence
import io.kailand.core.contract.ArrivalResult
import io.kailand.core.contract.DataSource
import io.kailand.core.contract.GameAction
import io.kailand.core.contract.JourneyKind
import io.kailand.core.contract.MutationResult
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BGameServiceTest {
    private val sampleArrival = ArrivalCommand(
        journeyId = "00000000-0000-4000-8000-000000000003",
        source = DataSource.DEMO,
        kind = JourneyKind.REGION,
        regionId = "daejeon",
        routeRevision = 0,
        evidence = ArrivalEvidence.DEMO_FINAL_DESTINATION,
        completedAtEpochMs = 1793408400000L,
    )

    @Test fun freshStateIsReadyWithoutWriting() = runBlocking {
        val store = FakeStore()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val service = BGameService(store, scope)
            assertEquals(GameReadiness.READY, service.readiness.value)
            assertEquals("tori", service.state.value.avatar.characterId)
            assertEquals(0, service.state.value.dailyGrowth)
            assertTrue(service.state.value.ownedItemIds.isEmpty())
            assertEquals(0, store.writes)
        } finally { scope.cancel() }
    }

    @Test fun loadingBlocksAllPermanentMutations() = runBlocking {
        val pending = CompletableDeferred<GameSave?>()
        val store = FakeStore(pending = pending)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val service = BGameService(store, scope)
            assertEquals(GameReadiness.LOADING, service.readiness.value)
            assertTrue(service.completeArrival(sampleArrival) is ArrivalResult.StorageFailure)
            assertTrue(service.apply(GameAction.SelectCharacter("momo")) is MutationResult.StorageFailure)
            assertTrue(service.acknowledgeReceipt(sampleArrival.journeyId) is MutationResult.StorageFailure)
            assertTrue(service.resetDemoProgress() is MutationResult.StorageFailure)
            assertEquals(0, store.writes)
            pending.complete(GameSave())
            assertEquals(GameReadiness.READY, service.readiness.value)
        } finally { scope.cancel() }
    }

    @Test fun unreadableSaveBlocksAllPermanentMutations() = runBlocking {
        val store = FakeStore(failRead = true)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val service = BGameService(store, scope)
            assertEquals(GameReadiness.READ_ERROR, service.readiness.value)
            assertTrue(service.completeArrival(sampleArrival) is ArrivalResult.StorageFailure)
            assertTrue(service.apply(GameAction.SelectCharacter("momo")) is MutationResult.StorageFailure)
            assertTrue(service.acknowledgeReceipt(sampleArrival.journeyId) is MutationResult.StorageFailure)
            assertTrue(service.resetDemoProgress() is MutationResult.StorageFailure)
            assertEquals(0, store.writes)
        } finally { scope.cancel() }
    }

    @Test fun corruptSchemaNeverReplacesSave() = runBlocking {
        val store = FakeStore(save = GameSave(schemaVersion = 99))
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val service = BGameService(store, scope)
            assertEquals(GameReadiness.READ_ERROR, service.readiness.value)
            assertTrue(service.resetDemoProgress() is MutationResult.StorageFailure)
            assertEquals(99, store.save?.schemaVersion)
            assertEquals(0, store.writes)
        } finally { scope.cancel() }
    }

    @Test fun restoresSnapshotFromPersistedState() = runBlocking {
        val store = FakeStore(save = GameSave(characterId = "momo", dailyGrowth = 2,
            ownedItemIds = setOf("daejeon_bread"), discoveredRegionIds = setOf("daejeon")))
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val service = BGameService(store, scope)
            assertEquals(GameReadiness.READY, service.readiness.value)
            assertEquals("momo", service.state.value.avatar.characterId)
            assertEquals(2, service.state.value.dailyGrowth)
            assertEquals(setOf("daejeon"), service.state.value.discoveredRegionIds)
        } finally { scope.cancel() }
    }

    @Test fun failedWriteDoesNotPublishSuccessOrChangeSnapshot() = runBlocking {
        val store = FakeStore(save = GameSave(dailyGrowth = 2), failWrite = true)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val service = BGameService(store, scope)
            assertTrue(service.resetDemoProgress() is MutationResult.StorageFailure)
            assertEquals(2, service.state.value.dailyGrowth)
            assertEquals(2, store.save?.dailyGrowth)
            assertEquals(0, store.writes)
        } finally { scope.cancel() }
    }

    @Test fun resetAfterReadyIsPersisted() = runBlocking {
        val store = FakeStore(save = GameSave(characterId = "lulu", dailyGrowth = 3))
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val service = BGameService(store, scope)
            assertEquals(MutationResult.Success, service.resetDemoProgress())
            assertEquals(0, service.state.value.dailyGrowth)
            assertEquals("tori", store.save?.characterId)
            assertEquals(1, store.writes)
        } finally { scope.cancel() }
    }

    @Test fun jsonRoundTripAndUnsupportedSchema() {
        val original = GameSave(characterId = "momo", dailyGrowth = 1,
            decorationSlots = mapOf("slot_left" to "daejeon_sign"))
        assertEquals(original, GameSaveCodec.decode(GameSaveCodec.encode(original)))
        var rejected = false
        try { GameSaveCodec.decode(GameSaveCodec.encode(GameSave(schemaVersion = 2))) }
        catch (_: IllegalStateException) { rejected = true }
        assertTrue(rejected)
    }

    private class FakeStore(
        var save: GameSave? = null,
        private val pending: CompletableDeferred<GameSave?>? = null,
        private val failRead: Boolean = false,
        private val failWrite: Boolean = false,
    ) : GameSaveStore {
        var writes = 0
        override suspend fun read(): GameSave? {
            if (failRead) throw IOException("test read failure")
            return pending?.await() ?: save
        }
        override suspend fun update(transform: (GameSave) -> GameSave): GameSave {
            if (failWrite) throw IOException("test write failure")
            val updated = transform(save ?: GameSave())
            updated.toSnapshot()
            save = updated
            writes += 1
            return updated
        }
    }
}
