package io.kailand.app

import io.kailand.core.contract.*
import io.kailand.feature.island.*
import java.io.File
import java.io.IOException
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

/** Exercise the new routing boundary with B's actual reducer and transactional service. */
class ProductNavigationIntegrationTest {
    private val catalog = File("../content/catalog.json").readText(Charsets.UTF_8)

    @Test fun selectionWaitsForDiskPublicationBeforeEnteringIslandAndRejectsDuplicate() = runBlocking {
        val p = Pipeline()
        try {
            p.store.pause = true
            val pending = async(start = CoroutineStart.UNDISPATCHED) { p.select() }
            p.store.committed.await()
            assertEquals(ProductRoute.CHARACTER, p.nav.state.value.route)
            assertEquals("tori", p.game.state.value.avatar.characterId)
            assertTrue(p.nav.busy.value)
            assertTrue(p.select() is MutationResult.Rejected)
            assertFalse(p.nav.open(p.nav.state.value, ProductRoute.ISLAND))
            p.store.release.complete(Unit)
            assertEquals(MutationResult.Success, pending.await())
            assertEquals("momo", p.game.state.value.avatar.characterId)
            assertEquals(ProductRoute.ISLAND, p.nav.state.value.route)
            assertEquals(1, p.store.updates)
        } finally { p.scope.cancel() }
    }

    @Test fun selectionFailureStaysOnCharacterAndRetrySavesOnce() = runBlocking {
        val p = Pipeline()
        try {
            p.store.fail = true
            assertTrue(p.select() is MutationResult.StorageFailure)
            assertEquals(ProductRoute.CHARACTER, p.nav.state.value.route)
            assertEquals("tori", p.game.state.value.avatar.characterId)
            assertEquals(MutationResult.Success, p.select())
            assertEquals(ProductRoute.ISLAND, p.nav.state.value.route)
            assertEquals("momo", p.store.save.characterId)
        } finally { p.scope.cancel() }
    }

    @Test fun admittedSelectionPersistsButCannotRouteAfterGateOwnerForegroundOrGenerationChanges() = runBlocking {
        for (change in listOf("gate", "foreground", "owner", "generation")) {
            val p = Pipeline()
            try {
                p.store.pause = true
                val pending = async(start = CoroutineStart.UNDISPATCHED) { p.select() }
                p.store.committed.await()
                when (change) {
                    "gate" -> p.context = p.context.copy(gate = InteractionGate(GateState.UNKNOWN))
                    "foreground" -> p.context = p.context.copy(foreground = false)
                    "owner" -> p.owner = false
                    "generation" -> p.generation = 1
                }
                p.store.release.complete(Unit)
                assertTrue(change, pending.await() is MutationResult.Rejected)
                assertEquals(change, ProductRoute.CHARACTER, p.nav.state.value.route)
                assertEquals("momo", p.game.state.value.avatar.characterId)
            } finally { p.scope.cancel() }
        }
    }

    @Test fun staleIslandCallbacksCannotWriteOrNavigateAfterWorldRoundTrip() = runBlocking {
        val p = Pipeline()
        try {
            p.select()
            val oldIsland = p.nav.state.value
            assertTrue(p.nav.open(oldIsland, ProductRoute.WORLD))
            assertTrue(p.nav.open(p.nav.state.value, ProductRoute.ISLAND))
            assertFalse(p.nav.open(oldIsland, ProductRoute.INVENTORY))
            assertTrue(p.commands.apply(0, GameAction.EquipOutfit(null)) {
                p.nav.current(oldIsland)
            } is MutationResult.Rejected)
            assertEquals(1, p.store.updates)
        } finally { p.scope.cancel() }
    }

    @Test fun pendingReceiptPreemptsColdStartAndBothTargetsAwaitSuccessfulAck() = runBlocking {
        for (target in listOf(ProductRoute.ISLAND, ProductRoute.CHARACTER)) {
            val p = Pipeline()
            try {
                val receipt = p.arrive()
                p.nav.presentReceipt(receipt.journeyId)
                val screen = p.nav.state.value
                assertEquals(ProductRoute.BRIEFING, screen.route)
                assertFalse(p.nav.open(screen, target))
                p.store.fail = true
                val closer = ReceiptCloseCoordinator(p.game, p.commands, 0) { true }
                assertTrue(closer.close(receipt, { p.nav.current(screen) }) {
                    p.nav.acknowledged(screen, receipt.journeyId, target)
                } is MutationResult.StorageFailure)
                assertEquals(receipt, p.game.state.value.pendingReceipt)
                assertEquals(ProductRoute.BRIEFING, p.nav.state.value.route)
                assertEquals(MutationResult.Success, closer.close(receipt, { p.nav.current(screen) }) {
                    p.nav.acknowledged(screen, receipt.journeyId, target)
                })
                assertEquals(target, p.nav.state.value.route)
                assertNull(p.game.state.value.pendingReceipt)
                assertEquals(1, p.store.save.processedReceipts.size)
            } finally { p.scope.cancel() }
        }
    }

    @Test fun restrictionDuringAckPreservesCommittedRecordWithoutLateNavigation() = runBlocking {
        val p = Pipeline()
        try {
            val receipt = p.arrive()
            p.nav.presentReceipt(receipt.journeyId)
            val screen = p.nav.state.value
            val closer = ReceiptCloseCoordinator(p.game, p.commands, 0) { true }
            p.store.pause = true
            val pending = async(start = CoroutineStart.UNDISPATCHED) {
                closer.close(receipt, { p.nav.current(screen) }) {
                    p.nav.acknowledged(screen, receipt.journeyId, ProductRoute.CHARACTER)
                }
            }
            p.store.committed.await()
            p.context = p.context.copy(gate = InteractionGate(GateState.RESTRICTED))
            p.store.release.complete(Unit)
            assertTrue(pending.await() is MutationResult.Rejected)
            assertEquals(ProductRoute.BRIEFING, p.nav.state.value.route)
            assertNull(p.game.state.value.pendingReceipt)
            assertEquals(1, p.store.save.processedReceipts.size)
            p.nav.receiptAbsent()
            // Clearing a stale display fence itself never routes.
            assertEquals(ProductRoute.BRIEFING, p.nav.state.value.route)
        } finally { p.scope.cancel() }
    }

    @Test fun cancelledSelectionCannotNavigateAfterItsWriteCommits() = runBlocking {
        val p = Pipeline()
        try {
            p.store.pause = true
            val pending = launch(start = CoroutineStart.UNDISPATCHED) { p.select() }
            p.store.committed.await()
            pending.cancel()
            p.store.release.complete(Unit)
            pending.join()
            assertEquals("momo", p.game.state.value.avatar.characterId)
            assertEquals(ProductRoute.CHARACTER, p.nav.state.value.route)
            assertFalse(p.nav.busy.value)
        } finally { p.scope.cancel() }
    }

    @Test fun navigationReturnPrioritizesNewDestinationWithoutRepeatingSelectionOrHijackingWorld() = runBlocking {
        val p = Pipeline()
        try {
            p.nav.destinationObserved("destination:1")
            assertEquals(ProductRoute.CHARACTER, p.nav.state.value.route)
            p.select()
            p.nav.open(p.nav.state.value, ProductRoute.WORLD)
            p.nav.destinationObserved("destination:1")
            assertEquals(ProductRoute.ISLAND, p.nav.state.value.route)
            p.nav.open(p.nav.state.value, ProductRoute.WORLD)
            p.nav.destinationObserved("destination:1")
            assertEquals(ProductRoute.WORLD, p.nav.state.value.route)
            p.nav.destinationObserved("destination:2")
            assertEquals(ProductRoute.ISLAND, p.nav.state.value.route)
            assertEquals(ProductRoute.CHARACTER, ProductNavigation().state.value.route)
        } finally { p.scope.cancel() }
    }

    @Test fun departureFailureAndRetryNeverCreateJourneyReceiptOrReward() = runBlocking {
        val p = Pipeline()
        try {
            p.select()
            p.nav.depart(p.nav.state.value, { true }) { RequestResult.Failed("launcher unavailable") }
            assertEquals(ProductRoute.NAVIGATION_FAILURE, p.nav.state.value.route)
            p.nav.depart(p.nav.state.value, { true }) { RequestResult.Accepted }
            assertEquals(ProductRoute.ISLAND, p.nav.state.value.route)
            assertNull(p.game.state.value.pendingReceipt)
            assertTrue(p.store.save.processedReceipts.isEmpty())
            assertTrue(p.game.state.value.ownedItemIds.isEmpty())
            assertEquals(1, p.store.updates) // character selection only
        } finally { p.scope.cancel() }
    }

    @Test fun lateDepartureFailureCannotNavigateAfterOwnerChanges() = runBlocking {
        val p = Pipeline()
        try {
            p.select()
            val release = CompletableDeferred<RequestResult>()
            val pending = async(start = CoroutineStart.UNDISPATCHED) {
                p.nav.depart(p.nav.state.value, { p.owner }) { release.await() }
            }
            p.owner = false
            release.complete(RequestResult.Failed("late launcher error"))
            pending.await()
            assertEquals(ProductRoute.ISLAND, p.nav.state.value.route)
            assertFalse(p.nav.busy.value)
        } finally { p.scope.cancel() }
    }

    private inner class Pipeline {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val store = Store()
        val game = BGameService(store, scope) { RegionRewards.fromJson(catalog) }
        val nav = ProductNavigation()
        var owner = true
        var generation = 0L
        var context = GameCommandContext(InteractionGate(GateState.PARKED_ALLOWED), true)
        val commands = GameCommandCoordinator(game, { game.readiness.value == GameReadiness.READY }) { context }
        suspend fun select(): MutationResult {
            val screen = nav.state.value
            return nav.selectCharacter(screen, {
                owner && generation == 0L && commands.canRoute(0) { nav.current(screen) }
            }) { commands.apply(0, GameAction.SelectCharacter("momo")) { owner && nav.current(screen) } }
        }
        suspend fun arrive(): ArrivalReceipt {
            val command = ArrivalCommand("12345678-1234-1234-1234-123456789abc", DataSource.DEMO,
                JourneyKind.REGION, "wonju", 0, ArrivalEvidence.DEMO_FINAL_DESTINATION, 1793408400000)
            context = context.copy(journey = JourneySnapshot(command.journeyId, DataSource.DEMO,
                JourneyKind.REGION, JourneyPhase.ARRIVED, 0, DestinationSpec("wonju", "원주", 37.3422, 127.9202)))
            assertTrue(commands.completeArrival(0, command) is ArrivalResult.Applied)
            return checkNotNull(game.state.value.pendingReceipt)
        }
    }

    private class Store : GameSaveStore {
        var save = GameSave()
        var pause = false
        var fail = false
        var updates = 0
        val committed = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        override suspend fun read() = save
        override suspend fun update(transform: (GameSave) -> GameSave): GameSave {
            if (fail) { fail = false; throw IOException("isolated test storage failure") }
            save = transform(save)
            updates++
            if (pause) { pause = false; committed.complete(Unit); release.await() }
            return save
        }
    }
}
