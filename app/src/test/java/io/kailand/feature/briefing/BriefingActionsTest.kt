package io.kailand.feature.briefing

import io.kailand.core.contract.GateState
import io.kailand.core.contract.InteractionGate
import io.kailand.core.contract.MutationResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class BriefingActionsTest {
    private fun allowed() = BriefingActions(InteractionGate(GateState.PARKED_ALLOWED))

    @Test fun dismissAndItemsShareOneInFlightActionAndSuccessCannotRepeat() = runBlocking {
        val actions = allowed()
        val result = CompletableDeferred<MutationResult>()
        var calls = 0
        val first = launch(start = CoroutineStart.UNDISPATCHED) {
            actions.perform(BriefingAction.DISMISS) { calls++; result.await() }
        }
        actions.perform(BriefingAction.OPEN_ITEMS) { calls++; MutationResult.Success }
        actions.perform(BriefingAction.DISMISS) { calls++; MutationResult.Success }
        assertEquals(1, calls)
        assertEquals(BriefingAction.DISMISS, actions.state.value.pending)
        result.complete(MutationResult.Success)
        first.join()
        assertTrue(actions.state.value.completed)
        actions.perform(BriefingAction.OPEN_ITEMS) { calls++; MutationResult.Success }
        assertEquals(1, calls)
    }

    @Test fun eitherActionCanFailAndRetryWithoutLosingScreen() = runBlocking {
        for (action in BriefingAction.entries) {
            val actions = allowed()
            actions.perform(action) { MutationResult.StorageFailure("private storage detail") }
            assertEquals(action, actions.state.value.failed)
            assertNull(actions.state.value.pending)
            assertFalse(actions.state.value.completed)
            assertFalse(requireNotNull(actions.state.value.error).contains("private"))
            actions.perform(action) { MutationResult.Success }
            assertTrue(actions.state.value.completed)
            assertNull(actions.state.value.error)
        }
    }

    @Test fun rejectedAndUnexpectedFailureAreRetryable() = runBlocking {
        val actions = allowed()
        actions.perform(BriefingAction.OPEN_ITEMS) { MutationResult.Rejected("stale journey") }
        assertNotNull(actions.state.value.error)
        actions.perform(BriefingAction.DISMISS) { throw IllegalStateException("disk path") }
        assertEquals(BriefingAction.DISMISS, actions.state.value.failed)
        actions.perform(BriefingAction.DISMISS) { MutationResult.Success }
        assertTrue(actions.state.value.completed)
    }

    @Test fun restrictedAndUnknownNeverInvokeEitherCallback() = runBlocking {
        var calls = 0
        for (gate in listOf(GateState.UNKNOWN, GateState.RESTRICTED)) {
            val actions = BriefingActions(InteractionGate(gate))
            for (action in BriefingAction.entries) {
                actions.perform(action) { calls++; MutationResult.Success }
            }
            assertEquals(BriefingActionState(), actions.state.value)
        }
        assertEquals(0, calls)
    }

    @Test fun cancellationPropagatesAndReleasesActionWithoutError() = runBlocking {
        val actions = allowed()
        val waiting = CompletableDeferred<MutationResult>()
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            actions.perform(BriefingAction.DISMISS) { waiting.await() }
        }
        job.cancelAndJoin()
        assertTrue(job.isCancelled)
        assertEquals(BriefingActionState(), actions.state.value)
        actions.perform(BriefingAction.DISMISS) { MutationResult.Success }
        assertTrue(actions.state.value.completed)
    }

    @Test fun disposedReceiptOrGateCannotPublishLateFailureIntoNewSession() = runBlocking {
        val old = allowed()
        val waiting = CompletableDeferred<MutationResult>()
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            old.perform(BriefingAction.DISMISS) { waiting.await() }
        }
        old.deactivate()
        val current = allowed()
        waiting.complete(MutationResult.StorageFailure("late"))
        job.join()
        assertNull(old.state.value.error)
        assertEquals(BriefingActionState(), current.state.value)
        var staleCalls = 0
        old.perform(BriefingAction.OPEN_ITEMS) { staleCalls++; MutationResult.Success }
        assertEquals(0, staleCalls)
        current.perform(BriefingAction.OPEN_ITEMS) { MutationResult.Success }
        assertTrue(current.state.value.completed)
    }
}
