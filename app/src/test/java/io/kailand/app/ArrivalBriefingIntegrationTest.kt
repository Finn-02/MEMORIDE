package io.kailand.app

import io.kailand.app.journey.DemoJourneyAdapter
import io.kailand.core.contract.*
import io.kailand.feature.island.*
import java.io.File
import java.io.IOException
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

/** A's orchestration with B's real reducer/service and an isolated in-memory transactional store. */
class ArrivalBriefingIntegrationTest {
    private val catalogJson = File("../content/catalog.json").readText(Charsets.UTF_8)
    private val catalog = decodeCatalog(catalogJson)
    private val destination = catalog.regions.single { it.id == "wonju" }.let {
        DestinationSpec(it.id, it.name, it.latitude, it.longitude)
    }
    private val completion = DemoInput.Complete(1793408400000L, null, null)

    @Test fun savedReceiptDrivesBriefingAckAndReplayWithoutNewRewardOrPending() = runBlocking {
        val p = Pipeline()
        try {
            p.arrive()
            val receipt = checkNotNull(p.game.state.value.pendingReceipt)
            assertEquals(DataSource.DEMO, receipt.source)
            assertEquals(3, receipt.grantedItemIds.size)
            assertNull(receipt.elapsedSeconds); assertNull(receipt.distanceMeters)
            var navigated = false
            assertEquals(MutationResult.Success, p.closer.close(receipt) { navigated = true })
            assertTrue(navigated); assertNull(p.game.state.value.pendingReceipt)
            assertEquals(RequestResult.Accepted, p.journey.dispatch(DemoInput.RepeatLastArrival))
            assertEquals(receipt, p.store.save.processedReceipts.getValue(receipt.journeyId).toContract())
            assertEquals(3, p.game.state.value.ownedItemIds.size)
            assertEquals(1, p.store.save.processedReceipts.size)
            assertNull(p.game.state.value.pendingReceipt)
        } finally { p.scope.cancel() }
    }

    @Test fun saveFailureRetryKeepsJourneyIdTimestampAndNulls() = runBlocking {
        val p = Pipeline()
        try {
            assertEquals(RequestResult.Accepted, p.journey.requestDestination(destination, JourneyKind.REGION))
            val id = p.journey.state.value.journeyId
            p.store.failNext = true
            assertTrue(p.journey.dispatch(completion) is RequestResult.Failed)
            assertNull(p.game.state.value.pendingReceipt)
            assertTrue(p.game.state.value.ownedItemIds.isEmpty())
            assertEquals(RequestResult.Accepted, p.journey.retryArrival())
            val receipt = checkNotNull(p.game.state.value.pendingReceipt)
            assertEquals(id, receipt.journeyId)
            assertEquals(completion.completedAtEpochMs, receipt.completedAtEpochMs)
            assertNull(receipt.elapsedSeconds); assertNull(receipt.distanceMeters)
            assertEquals(1, p.store.save.processedReceipts.size)
        } finally { p.scope.cancel() }
    }

    @Test fun failedAcknowledgementKeepsBriefingAndRetryDoesNotRecalculateReward() = runBlocking {
        val p = Pipeline()
        try {
            p.arrive()
            val receipt = checkNotNull(p.game.state.value.pendingReceipt)
            p.store.failNext = true
            var routed = false
            assertTrue(p.closer.close(receipt) { routed = true } is MutationResult.StorageFailure)
            assertFalse(routed)
            assertEquals(receipt, p.game.state.value.pendingReceipt)
            assertEquals(MutationResult.Success, p.closer.close(receipt) { routed = true })
            assertTrue(routed)
            assertEquals(3, p.game.state.value.ownedItemIds.size)
            assertEquals(1, p.store.save.processedReceipts.size)
        } finally { p.scope.cancel() }
    }

    @Test fun newServiceRestoresPendingReceiptAndCanAcknowledgeWithoutActiveJourney() = runBlocking {
        val p = Pipeline()
        val restartedScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            p.arrive()
            val receipt = checkNotNull(p.game.state.value.pendingReceipt)
            p.scope.cancel()
            val game = BGameService(p.store, restartedScope) { RegionRewards.fromJson(catalogJson) }
            assertEquals(GameReadiness.READY, game.readiness.value)
            assertEquals(receipt, game.state.value.pendingReceipt)
            val commands = GameCommandCoordinator(game, { true }) {
                GameCommandContext(InteractionGate(GateState.PARKED_ALLOWED), true)
            }
            val closer = ReceiptCloseCoordinator(game, commands, 0) { true }
            assertEquals(MutationResult.Success, closer.close(receipt) {})
            assertNull(game.state.value.pendingReceipt)
            assertEquals(3, game.state.value.ownedItemIds.size)
        } finally { p.scope.cancel(); restartedScope.cancel() }
    }

    @Test fun packagedCompletionMustRemainExplicitlyDemo() {
        val fixture = File("../fixtures/scenarios.json").readText(Charsets.UTF_8)
        assertEquals(1793408400000L, decodeDemoCompletion(fixture).completedAtEpochMs)
        try {
            decodeDemoCompletion(fixture.replace("\"DEMO\"", "\"PLEOS\""))
            fail("Real source must not become a DEMO completion")
        } catch (_: IllegalArgumentException) { }
    }

    private inner class Pipeline {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val store = Store()
        val game = BGameService(store, scope) { RegionRewards.fromJson(catalogJson) }
        lateinit var journey: DemoJourneyAdapter
        val commands = GameCommandCoordinator(game, { game.readiness.value == GameReadiness.READY }) {
            GameCommandContext(InteractionGate(GateState.PARKED_ALLOWED), true, DataSource.DEMO, journey.state.value)
        }
        val closer = ReceiptCloseCoordinator(game, commands, 0) { true }
        init {
            journey = DemoJourneyAdapter(scope, catalog.regions.map { it.id }.toSet(), { true },
                { commands.completeArrival(0, it) }, prepare = {})
        }
        suspend fun arrive() {
            assertEquals(RequestResult.Accepted, journey.requestDestination(destination, JourneyKind.REGION))
            assertEquals(RequestResult.Accepted, journey.dispatch(completion))
        }
    }

    private class Store : GameSaveStore {
        var save = GameSave()
        var failNext = false
        override suspend fun read() = save
        override suspend fun update(transform: (GameSave) -> GameSave): GameSave {
            if (failNext) { failNext = false; throw IOException("simulated storage failure") }
            return transform(save).also { save = it }
        }
    }
}
