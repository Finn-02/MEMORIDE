package io.kailand.app

import io.kailand.core.contract.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Test

class GameCommandCoordinatorTest {
    private val destination = DestinationSpec("wonju", "원주", 37.34, 127.92)
    private val command = ArrivalCommand("journey", DataSource.DEMO, JourneyKind.REGION, "wonju", 2,
        ArrivalEvidence.DEMO_FINAL_DESTINATION, 1234, null, null)
    private val receipt = ArrivalReceipt("journey", DataSource.DEMO, JourneyKind.REGION, "wonju",
        1234, null, null, 0, 0, "wonju", listOf("gift"))
    private val action = GameAction.SelectCharacter("momo")
    private val parked = InteractionGate(GateState.PARKED_ALLOWED)
    private val journey = JourneySnapshot("journey", DataSource.DEMO, JourneyKind.REGION,
        JourneyPhase.ARRIVED, 2, destination)
    private var context = GameCommandContext(parked, true, DataSource.DEMO, journey)
    private var ready = true
    private val game = FakeGame()
    private val coordinator = GameCommandCoordinator(game, { ready }, { context })

    @Test fun unknownBackgroundAndLoadingNeverReachGame() = runBlocking {
        context = context.copy(gate = InteractionGate(GateState.UNKNOWN))
        assertTrue(coordinator.apply(0, action) is MutationResult.Rejected)
        context = context.copy(gate = parked, foreground = false)
        assertTrue(coordinator.completeArrival(0, command) is ArrivalResult.Rejected)
        context = context.copy(foreground = true); ready = false
        assertTrue(coordinator.apply(0, action) is MutationResult.Rejected)
        assertEquals(0, game.writes)
    }

    @Test fun arrivalMustMatchCurrentIdRevisionKindSourceAndPhase() = runBlocking {
        for (old in listOf(command.copy(journeyId = "old"), command.copy(routeRevision = 1),
            command.copy(kind = JourneyKind.DAILY), command.copy(source = DataSource.PLEOS),
            command.copy(regionId = "daejeon"))) {
            assertTrue(coordinator.completeArrival(0, old) is ArrivalResult.Rejected)
        }
        context = context.copy(journey = journey.copy(phase = JourneyPhase.READY))
        assertTrue(coordinator.completeArrival(0, command) is ArrivalResult.Rejected)
        context = context.copy(journey = journey)
        assertTrue(coordinator.completeArrival(0, command) is ArrivalResult.Applied)
        assertEquals(listOf(command), game.commands)
    }

    @Test fun retryPassesSameImmutableCommandIncludingTimestampAndNulls() = runBlocking {
        game.arrivalResult = ArrivalResult.StorageFailure("disk")
        assertTrue(coordinator.completeArrival(0, command) is ArrivalResult.StorageFailure)
        game.arrivalResult = ArrivalResult.Applied(receipt)
        assertTrue(coordinator.completeArrival(0, command) is ArrivalResult.Applied)
        assertEquals(listOf(command, command), game.commands)
        assertSame(game.commands[0], game.commands[1])
    }

    @Test fun queuedActionChecksGateAgainAfterGettingWriteLock() = runBlocking {
        val entered = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
        game.beforeApply = { entered.complete(Unit); release.await() }
        val first = async(start = CoroutineStart.UNDISPATCHED) { coordinator.apply(0, action) }
        entered.await()
        val queued = async(start = CoroutineStart.UNDISPATCHED) { coordinator.apply(0, action) }
        context = context.copy(foreground = false)
        release.complete(Unit)
        assertEquals(MutationResult.Success, first.await())
        assertTrue(queued.await() is MutationResult.Rejected)
        assertEquals(1, game.writes)
    }

    @Test fun suppliedUserConfirmedTypesCannotReachStorageBeforeB05Integration() = runBlocking {
        val before = game.state.value
        for (source in DataSource.entries) for (evidence in ArrivalEvidence.entries) {
            if (source != DataSource.USER_CONFIRMED && evidence != ArrivalEvidence.USER_CONFIRMED_DESTINATION) continue
            context = context.copy(mode = source, journey = journey.copy(source = source))
            assertTrue("$source / $evidence", coordinator.completeArrival(0,
                command.copy(source = source, evidence = evidence)) is ArrivalResult.Rejected)
        }
        assertTrue(game.commands.isEmpty())
        assertEquals(before, game.state.value)
    }

    @Test fun supplyingNewTypesPreservesExistingDemoAndPleosDispatch() = runBlocking {
        for ((source, evidence) in listOf(
            DataSource.DEMO to ArrivalEvidence.DEMO_FINAL_DESTINATION,
            DataSource.PLEOS to ArrivalEvidence.PLEOS_FINAL_DESTINATION,
        )) {
            context = context.copy(mode = source, journey = journey.copy(source = source))
            val original = command.copy(source = source, evidence = evidence)
            assertTrue(coordinator.completeArrival(0, original) is ArrivalResult.Applied)
            assertSame(original, game.commands.last())
        }
        assertEquals(2, game.commands.size)
    }

    @Test fun acknowledgementFailureKeepsPendingAndWrongIdCannotCloseIt() = runBlocking {
        game.state.value = game.state.value.copy(pendingReceipt = receipt)
        assertTrue(coordinator.acknowledge(0, "old") is MutationResult.Rejected)
        assertEquals(0, game.ackCalls)
        game.ackResult = MutationResult.StorageFailure("disk")
        assertTrue(coordinator.acknowledge(0, "journey") is MutationResult.StorageFailure)
        assertEquals(receipt, game.state.value.pendingReceipt)
        game.ackResult = MutationResult.Success
        assertEquals(MutationResult.Success, coordinator.acknowledge(0, "journey"))
        assertNull(game.state.value.pendingReceipt)
    }

    @Test fun queuedCallbackFromOldScreenCannotUseNewForegroundOwnersPermission() = runBlocking {
        val entered = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
        game.beforeApply = { entered.complete(Unit); release.await() }
        var originalScreenActive = true
        val first = async(start = CoroutineStart.UNDISPATCHED) { coordinator.apply(0, action) }
        entered.await()
        val queued = async(start = CoroutineStart.UNDISPATCHED) {
            coordinator.apply(0, action) { originalScreenActive }
        }
        originalScreenActive = false // A different activity/screen now has a valid parked gate.
        release.complete(Unit)
        assertEquals(MutationResult.Success, first.await())
        assertTrue(queued.await() is MutationResult.Rejected)
        assertEquals(1, game.writes)
    }

    @Test fun acknowledgementSuccessAfterBackgroundDoesNotAuthorizeNavigation() = runBlocking {
        game.state.value = game.state.value.copy(pendingReceipt = receipt)
        game.beforeAck = { context = context.copy(foreground = false) }
        assertTrue(coordinator.acknowledge(0, "journey") is MutationResult.Rejected)
        assertNull(game.state.value.pendingReceipt) // Persistence succeeded, routing must not.
    }

    @Test fun resetWaitsForExistingWriteAndNeverJoinsWorkUnderWriteLock() = runBlocking {
        withTimeout(3000) {
            val entered = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
            game.beforeApply = { entered.complete(Unit); release.await() }
            val first = async(start = CoroutineStart.UNDISPATCHED) { coordinator.apply(0, action) }
            entered.await()
            var stopped = false
            val reset = async(start = CoroutineStart.UNDISPATCHED) {
                coordinator.resetDemo(0) {
                    assertTrue(coordinator.apply(0, action) is MutationResult.Rejected)
                    stopped = true
                }
            }
            assertFalse(stopped)
            release.complete(Unit); first.await()
            assertEquals(MutationResult.Success, reset.await())
            assertTrue(stopped)
            assertEquals(GameCommandSession(1), coordinator.session.value)
            assertTrue(coordinator.apply(0, action) is MutationResult.Rejected)
            assertEquals(MutationResult.Success, coordinator.apply(1, action))
        }
    }

    @Test fun resetFailureBlocksInputsUntilExplicitFreshGenerationRetry() = runBlocking {
        game.resetResult = MutationResult.StorageFailure("disk")
        assertTrue(coordinator.resetDemo(0) {} is MutationResult.StorageFailure)
        assertEquals(GameCommandSession(1, GameCommandPhase.RESET_FAILED), coordinator.session.value)
        assertTrue(coordinator.apply(1, action) is MutationResult.Rejected)
        assertTrue(coordinator.resetDemo(0) {} is MutationResult.Rejected)
        game.resetResult = MutationResult.Success
        assertEquals(MutationResult.Success, coordinator.resetDemo(1) {})
        assertEquals(GameCommandSession(2), coordinator.session.value)
    }

    @Test fun cancelledCleanupLeavesResetBlockedWithoutCallingGameReset() = runBlocking {
        try {
            coordinator.resetDemo(0) { throw CancellationException("cleanup cancelled") }
            fail("Cancellation must propagate")
        } catch (_: CancellationException) { }
        assertEquals(GameCommandSession(1, GameCommandPhase.RESET_FAILED), coordinator.session.value)
        assertEquals(0, game.resetCalls)
        assertTrue(coordinator.apply(1, action) is MutationResult.Rejected)
    }

    @Test fun restrictionDuringCleanupPreventsResetWrite() = runBlocking {
        assertTrue(coordinator.resetDemo(0) { context = context.copy(foreground = false) } is MutationResult.Rejected)
        assertEquals(0, game.resetCalls)
        assertEquals(GameCommandPhase.RESET_FAILED, coordinator.session.value.phase)
    }

    @Test fun resetIsDemoOnlyAndConcurrentSecondResetCannotRun() = runBlocking {
        context = context.copy(mode = DataSource.PLEOS)
        assertTrue(coordinator.resetDemo(0) {} is MutationResult.Rejected)
        context = context.copy(mode = DataSource.DEMO)
        val release = CompletableDeferred<Unit>()
        val reset = async(start = CoroutineStart.UNDISPATCHED) { coordinator.resetDemo(0) { release.await() } }
        assertTrue(coordinator.resetDemo(1) {} is MutationResult.Rejected)
        release.complete(Unit)
        assertEquals(MutationResult.Success, reset.await())
        assertEquals(1, game.resetCalls)
    }

    private inner class FakeGame : GameService {
        override val state = MutableStateFlow(GameSnapshot(AvatarPreview("tori", null), 0,
            emptySet(), emptySet(), emptyMap(), null))
        var writes = 0
        var ackCalls = 0
        var resetCalls = 0
        val commands = mutableListOf<ArrivalCommand>()
        var beforeApply: suspend () -> Unit = {}
        var beforeAck: suspend () -> Unit = {}
        var arrivalResult: ArrivalResult = ArrivalResult.Applied(receipt)
        var ackResult: MutationResult = MutationResult.Success
        var resetResult: MutationResult = MutationResult.Success
        override suspend fun apply(action: GameAction): MutationResult {
            beforeApply(); writes++; return MutationResult.Success
        }
        override suspend fun completeArrival(command: ArrivalCommand): ArrivalResult {
            commands += command; return arrivalResult
        }
        override suspend fun acknowledgeReceipt(journeyId: String): MutationResult {
            ackCalls++; beforeAck()
            if (ackResult == MutationResult.Success) state.value = state.value.copy(pendingReceipt = null)
            return ackResult
        }
        override suspend fun resetDemoProgress(): MutationResult { resetCalls++; return resetResult }
    }
}
