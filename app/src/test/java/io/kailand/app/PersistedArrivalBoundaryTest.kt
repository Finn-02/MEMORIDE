package io.kailand.app

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.kailand.core.contract.*
import io.kailand.feature.island.*
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** A orchestration + B service + real temporary DataStore file; not an APK/device restart. */
class PersistedArrivalBoundaryTest {
    @get:Rule val temp = TemporaryFolder()
    private val catalog = File("../content/catalog.json").readText(Charsets.UTF_8)
    private val command = ArrivalCommand("12345678-1234-4234-8234-123456789abc", DataSource.DEMO,
        JourneyKind.REGION, "wonju", 0, ArrivalEvidence.DEMO_FINAL_DESTINATION, 1793408400000L)

    @Test fun admittedArrivalSurvivesRestrictionAndRestoresForOneAcknowledgement() = runBlocking {
        withTimeout(15_000) {
            val file = File(temp.root, "restricted.preferences_pb")
            val original = session(file) { p ->
                p.pauseNextCommit()
                val arrival = async(start = CoroutineStart.UNDISPATCHED) { p.commands.completeArrival(0, command) }
                p.committed.await()
                p.context = p.context.copy(gate = InteractionGate(GateState.RESTRICTED))
                p.release.complete(Unit)
                val receipt = (arrival.await() as ArrivalResult.Applied).receipt
                assertEquals(receipt, p.game.state.value.pendingReceipt)
                var routed = false
                assertTrue(p.closer.close(receipt) { routed = true } is MutationResult.Rejected)
                assertFalse(routed)
                assertEquals(receipt, p.store.read()!!.toSnapshot().pendingReceipt)
                receipt
            }
            session(file) { p ->
                assertEquals(original, p.game.state.value.pendingReceipt)
                assertNull(original.elapsedSeconds); assertNull(original.distanceMeters)
                val before = p.store.read()!!
                assertEquals(ArrivalResult.AlreadyApplied(original), p.commands.completeArrival(0, command))
                assertEquals(before, p.store.read())
                var routes = 0
                assertEquals(MutationResult.Success, p.closer.close(original) { routes++ })
                assertTrue(p.closer.close(original) { routes++ } is MutationResult.Rejected)
                assertEquals(1, routes)
                val after = p.store.read()!!
                assertEquals(before.copy(pendingReceiptId = null), after)
            }
            session(file) { p ->
                assertNull(p.game.state.value.pendingReceipt)
                val before = p.store.read()
                assertEquals(ArrivalResult.AlreadyApplied(original), p.commands.completeArrival(0, command))
                assertEquals(before, p.store.read())
                assertNull(p.game.state.value.pendingReceipt)
            }
        }
    }

    @Test fun cancelledAcknowledgementPublishesAndRestoresWithoutReopeningOrRewarding() = runBlocking {
        withTimeout(15_000) {
            val file = File(temp.root, "cancelled.preferences_pb")
            val expected = session(file) { p ->
                val receipt = (p.commands.completeArrival(0, command) as ArrivalResult.Applied).receipt
                val before = p.store.read()!!
                p.pauseNextCommit()
                var routed = false
                val screen = launch(start = CoroutineStart.UNDISPATCHED) {
                    p.closer.close(receipt) { routed = true }
                }
                p.committed.await()
                assertNull(p.store.read()!!.pendingReceiptId)
                assertEquals(receipt, p.game.state.value.pendingReceipt)
                screen.cancel()
                p.context = p.context.copy(foreground = false, gate = InteractionGate(GateState.UNKNOWN))
                p.release.complete(Unit)
                screen.join()
                assertFalse(routed)
                val saved = p.store.read()!!
                assertEquals(before.copy(pendingReceiptId = null), saved)
                assertEquals(saved.toSnapshot(), p.game.state.value)
                assertNull(p.closer.heldReceipt.value)
                saved
            }
            session(file) { p ->
                assertEquals(expected, p.store.read())
                assertEquals(expected.toSnapshot(), p.game.state.value)
                val receipt = expected.processedReceipts.getValue(command.journeyId).toContract()
                assertEquals(ArrivalResult.AlreadyApplied(receipt), p.commands.completeArrival(0, command))
                assertEquals(expected, p.store.read())
                assertNull(p.game.state.value.pendingReceipt)
            }
        }
    }

    private suspend fun <T> session(file: File, block: suspend (Pipeline) -> T): T {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val backend = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
            val p = Pipeline(PreferencesGameSaveStore(backend), scope)
            assertEquals(GameReadiness.READY, p.game.readiness.first { it != GameReadiness.LOADING })
            return block(p)
        } finally { scope.coroutineContext[Job]!!.cancelAndJoin() }
    }

    private inner class Pipeline(val store: PreferencesGameSaveStore, scope: CoroutineScope) {
        val committed = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        private var pause = false
        // Pause only after the real transaction, before B publishes its snapshot.
        private val boundary = object : GameSaveStore {
            override suspend fun read() = store.read()
            override suspend fun update(transform: (GameSave) -> GameSave): GameSave {
                val saved = store.update(transform)
                if (pause) { pause = false; committed.complete(Unit); release.await() }
                return saved
            }
        }
        val game = BGameService(boundary, scope) { RegionRewards.fromJson(catalog) }
        var context = GameCommandContext(InteractionGate(GateState.PARKED_ALLOWED), true,
            DataSource.DEMO, JourneySnapshot(command.journeyId, DataSource.DEMO, command.kind,
                JourneyPhase.ARRIVED, 0, DestinationSpec("wonju", "원주", 37.3422, 127.9202)))
        val commands = GameCommandCoordinator(game, { game.readiness.value == GameReadiness.READY }) { context }
        val closer = ReceiptCloseCoordinator(game, commands, 0) { true }
        fun pauseNextCommit() { pause = true }
    }
}
