package io.kailand.app

import io.kailand.core.contract.*
import io.kailand.feature.island.*
import java.io.File
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

/** Pause AFTER the isolated store commits, BEFORE B receives the committed value. */
class AdmittedGameWriteTest {
    private val catalog = File("../content/catalog.json").readText(Charsets.UTF_8)
    private val command = ArrivalCommand("12345678-1234-1234-1234-123456789abc", DataSource.DEMO,
        JourneyKind.DAILY, null, 0, ArrivalEvidence.DEMO_FINAL_DESTINATION, 1234)

    @Test fun cancelledBriefingStillPublishesCommittedAcknowledgementWithoutRouting() = runBlocking {
        val p = Pipeline()
        try {
            p.arrive()
            val receipt = checkNotNull(p.game.state.value.pendingReceipt)
            val closer = ReceiptCloseCoordinator(p.game, p.commands, 0) { p.current }
            var routed = false
            p.store.pause = true
            val screen = launch(start = CoroutineStart.UNDISPATCHED) { closer.close(receipt) { routed = true } }
            p.store.committed.await()
            assertNull(p.store.save.pendingReceiptId)
            assertEquals(receipt, p.game.state.value.pendingReceipt)
            assertEquals(receipt, closer.heldReceipt.value)
            p.current = false
            screen.cancel()
            p.store.release.complete(Unit)
            screen.join()
            assertEquals(p.store.save.toSnapshot(), p.game.state.value)
            assertNull(p.game.state.value.pendingReceipt)
            assertNull(closer.heldReceipt.value)
            assertFalse(routed)
        } finally { p.scope.cancel() }
    }

    @Test fun cancelledInventoryWriteStillPublishesCommittedCharacter() = runBlocking {
        val p = Pipeline()
        try {
            p.store.pause = true
            val screen = launch(start = CoroutineStart.UNDISPATCHED) {
                p.commands.apply(0, GameAction.SelectCharacter("momo")) { p.current }
            }
            p.store.committed.await()
            assertEquals("momo", p.store.save.characterId)
            assertEquals("tori", p.game.state.value.avatar.characterId)
            p.current = false; screen.cancel()
            p.store.release.complete(Unit); screen.join()
            assertEquals(p.store.save.toSnapshot(), p.game.state.value)
        } finally { p.scope.cancel() }
    }

    @Test fun cancelledArrivalStillPublishesOriginalReceipt() = runBlocking {
        val p = Pipeline()
        try {
            p.store.pause = true
            val screen = launch(start = CoroutineStart.UNDISPATCHED) { p.commands.completeArrival(0, command) }
            p.store.committed.await()
            screen.cancel(); p.store.release.complete(Unit); screen.join()
            assertEquals(p.store.save.toSnapshot(), p.game.state.value)
            assertEquals(command.journeyId, p.game.state.value.pendingReceipt?.journeyId)
        } finally { p.scope.cancel() }
    }

    @Test fun latestGateAndScreenAreCheckedImmediatelyBeforeRouting() = runBlocking {
        for (change in listOf("gate", "foreground", "screen", "owner")) {
            val p = Pipeline()
            try {
                p.arrive()
                var screenCurrent = true
                val closer = ReceiptCloseCoordinator(p.game, p.commands, 0) { p.current }
                val receipt = checkNotNull(p.game.state.value.pendingReceipt)
                var routed = false
                p.store.pause = true
                val close = async(start = CoroutineStart.UNDISPATCHED) {
                    closer.close(receipt, { screenCurrent }) { routed = true }
                }
                p.store.committed.await()
                when (change) {
                    "gate" -> p.context = p.context.copy(gate = InteractionGate(GateState.UNKNOWN))
                    "foreground" -> p.context = p.context.copy(foreground = false)
                    "screen" -> screenCurrent = false
                    "owner" -> p.current = false
                }
                p.store.release.complete(Unit)
                assertTrue(change, close.await() is MutationResult.Rejected)
                assertFalse(change, routed)
                assertEquals(p.store.save.toSnapshot(), p.game.state.value)
            } finally { p.scope.cancel() }
        }
    }

    @Test fun gateChangeAfterCommandPostcheckIsCaughtByFinalRoutingCheck() = runBlocking {
        val p = Pipeline()
        try {
            p.arrive()
            var checks = 0
            val closer = ReceiptCloseCoordinator(p.game, p.commands, 0) {
                checks++
                // The command has already checked permission, just before returning Success.
                if (checks == 3) p.context = p.context.copy(gate = InteractionGate(GateState.RESTRICTED))
                true
            }
            var routed = false
            assertTrue(closer.close(checkNotNull(p.game.state.value.pendingReceipt)) { routed = true }
                is MutationResult.Rejected)
            assertFalse(routed)
            assertNull(p.game.state.value.pendingReceipt)
        } finally { p.scope.cancel() }
    }

    @Test fun cancelledCallerCannotRouteEvenBeforeScreenLeaseDisposal() = runBlocking {
        val p = Pipeline()
        try {
            p.arrive()
            val closer = ReceiptCloseCoordinator(p.game, p.commands, 0) { true }
            var routed = false
            p.store.pause = true
            val screen = launch(start = CoroutineStart.UNDISPATCHED) {
                closer.close(checkNotNull(p.game.state.value.pendingReceipt)) { routed = true }
            }
            p.store.committed.await()
            screen.cancel(); p.store.release.complete(Unit); screen.join()
            assertFalse(routed)
            assertEquals(p.store.save.toSnapshot(), p.game.state.value)
        } finally { p.scope.cancel() }
    }

    private inner class Pipeline {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val store = Store()
        val game = BGameService(store, scope) { RegionRewards.fromJson(catalog) }
        var current = true
        var context = GameCommandContext(InteractionGate(GateState.PARKED_ALLOWED), true,
            DataSource.DEMO, JourneySnapshot(command.journeyId, DataSource.DEMO, JourneyKind.DAILY,
                JourneyPhase.ARRIVED, 0, null))
        val commands = GameCommandCoordinator(game, { true }) { context }
        suspend fun arrive() { assertTrue(commands.completeArrival(0, command) is ArrivalResult.Applied) }
    }

    private class Store : GameSaveStore {
        var save = GameSave()
        var pause = false
        val committed = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        override suspend fun read() = save
        override suspend fun update(transform: (GameSave) -> GameSave): GameSave {
            val result = transform(save)
            save = result
            if (pause) { pause = false; committed.complete(Unit); release.await() }
            return result
        }
    }
}
