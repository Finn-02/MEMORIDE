package io.kailand.app.journey

import io.kailand.core.contract.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class GuardedDemoJourneyTest {
    private val destination = DestinationSpec("wonju", "원주", 37.34, 127.92)

    @Test fun disposedScreenCannotStartNewJourney() = runBlocking {
        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val adapter = DemoJourneyAdapter(appScope, setOf("wonju"), { true }, { error("No writes") })
            val guarded = GuardedDemoJourney(adapter, adapter, appScope) { false }
            assertTrue(guarded.requestDestination(destination, JourneyKind.REGION) is RequestResult.Failed)
            assertNull(adapter.state.value.journeyId)
        } finally { appScope.cancel() }
    }

    @Test fun briefingDisposesScreenButOriginalSaveAndReplayStateFinishInAppScope() = runBlocking {
        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val entered = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
            val commands = mutableListOf<ArrivalCommand>()
            val adapter = DemoJourneyAdapter(appScope, setOf("wonju"), { true }, { command ->
                commands += command
                entered.complete(Unit); release.await()
                ArrivalResult.Applied(ArrivalReceipt(command.journeyId, command.source, command.kind,
                    command.regionId, command.completedAtEpochMs, null, null, 0, 0, "wonju", emptyList()))
            }, prepare = {})
            val guarded = GuardedDemoJourney(adapter, adapter, appScope) { true }
            assertEquals(RequestResult.Accepted, guarded.requestDestination(destination, JourneyKind.REGION))
            val screen = launch(start = CoroutineStart.UNDISPATCHED) {
                guarded.dispatch(DemoInput.Complete(1234, null, null))
            }
            entered.await()
            screen.cancelAndJoin()
            release.complete(Unit)
            yield()
            assertEquals(RequestResult.Accepted, guarded.dispatch(DemoInput.RepeatLastArrival))
            assertEquals(2, commands.size)
            assertSame(commands[0], commands[1])
        } finally { appScope.cancel() }
    }
}
