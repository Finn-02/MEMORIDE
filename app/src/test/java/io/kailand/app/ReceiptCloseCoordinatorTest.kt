package io.kailand.app

import io.kailand.core.contract.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Test

class ReceiptCloseCoordinatorTest {
    private val receipt = ArrivalReceipt("id", DataSource.DEMO, JourneyKind.DAILY, null,
        1234, null, null, 0, 1, null, emptyList())
    private var current = true
    private val game = Game()
    private val commands = GameCommandCoordinator(game, { true }) {
        GameCommandContext(InteractionGate(GateState.PARKED_ALLOWED), true)
    }
    private val closer = ReceiptCloseCoordinator(game, commands, 0) { current }

    @Test fun pendingCanDisappearBeforeAwaitReturnsWithoutDisposingDisplayedReceipt() = runBlocking {
        val release = CompletableDeferred<Unit>()
        game.afterClear = { release.await() }
        var navigated = false
        val close = async(start = CoroutineStart.UNDISPATCHED) { closer.close(receipt) { navigated = true } }
        assertNull(game.state.value.pendingReceipt)
        assertEquals(receipt, closer.heldReceipt.value)
        assertFalse(navigated)
        assertTrue(closer.close(receipt) {} is MutationResult.Rejected)
        assertEquals(receipt, closer.heldReceipt.value)
        release.complete(Unit)
        assertEquals(MutationResult.Success, close.await())
        assertTrue(navigated); assertNull(closer.heldReceipt.value)
    }

    @Test fun screenDisposalWhileAcknowledgingCannotNavigate() = runBlocking {
        game.afterClear = { current = false }
        var navigated = false
        assertTrue(closer.close(receipt) { navigated = true } is MutationResult.Rejected)
        assertFalse(navigated); assertNull(closer.heldReceipt.value)
    }

    @Test fun newerPendingWinsOverOldAcknowledgementAndRemainsVisible() = runBlocking {
        val newer = receipt.copy(journeyId = "new")
        game.afterClear = { game.state.value = game.state.value.copy(pendingReceipt = newer) }
        var navigated = false
        assertTrue(closer.close(receipt) { navigated = true } is MutationResult.Rejected)
        assertFalse(navigated); assertEquals(newer, game.state.value.pendingReceipt)
    }

    @Test fun cancellationReleasesHeldReceiptAndDoesNotNavigate() = runBlocking {
        game.afterClear = { throw CancellationException("screen left") }
        var navigated = false
        try { closer.close(receipt) { navigated = true }; fail("Must propagate cancellation") }
        catch (_: CancellationException) { }
        assertFalse(navigated); assertNull(closer.heldReceipt.value)
    }

    private inner class Game : GameService {
        override val state = MutableStateFlow(GameSnapshot(AvatarPreview("tori", null), 1,
            emptySet(), emptySet(), emptyMap(), receipt))
        var afterClear: suspend () -> Unit = {}
        override suspend fun acknowledgeReceipt(journeyId: String): MutationResult {
            state.value = state.value.copy(pendingReceipt = null)
            afterClear()
            return MutationResult.Success
        }
        override suspend fun apply(action: GameAction) = error("Not used")
        override suspend fun completeArrival(command: ArrivalCommand) = error("Not used")
        override suspend fun resetDemoProgress() = error("Not used")
    }
}
