package io.kailand.app.journey

import io.kailand.core.contract.*
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class DemoJourneyAdapterTest {
    private val destination = DestinationSpec("daejeon", "대전", 36.35, 127.38)
    private val wonju = DestinationSpec("wonju", "원주", 37.34, 127.92)
    private val completion = DemoInput.Complete(1793408400000L, 1200, 5000.0)

    @Test fun acceptedRequestDoesNotMeanReadyOrArrival() = runBlocking {
        val prepared = CompletableDeferred<Unit>()
        withAdapter(prepare = { prepared.await() }) { h ->
            assertEquals(JourneyPhase.IDLE, h.adapter.state.value.phase)
            assertEquals(RequestResult.Accepted, h.adapter.requestDestination(destination, JourneyKind.REGION))
            assertEquals(JourneyPhase.PREPARING, h.adapter.state.value.phase)
            assertTrue(h.adapter.dispatch(DemoInput.StartTravel) is RequestResult.Failed)
            assertTrue(h.calls.isEmpty())
            prepared.complete(Unit)
            h.awaitReady()
            assertTrue(h.calls.isEmpty())
        }
    }

    @Test fun normalTravelKeepsIdentityAndDemoEvidence() = runBlocking {
        withAdapter { h ->
            h.ready()
            val id = h.adapter.state.value.journeyId
            assertEquals(RequestResult.Accepted, h.adapter.dispatch(DemoInput.StartTravel))
            assertEquals(JourneyPhase.TRAVELLING, h.adapter.state.value.phase)
            assertEquals(RequestResult.Accepted, h.adapter.dispatch(completion))
            val command = h.calls.single()
            assertEquals(id, command.journeyId)
            assertEquals(DataSource.DEMO, command.source)
            assertEquals(ArrivalEvidence.DEMO_FINAL_DESTINATION, command.evidence)
            assertEquals(JourneyPhase.ARRIVED, h.adapter.state.value.phase)
            assertEquals(5000.0, h.adapter.state.value.distanceMeters)
            assertTrue(h.adapter.dispatch(completion) is RequestResult.Failed)
            assertEquals(1, h.calls.size)
        }
    }

    @Test fun completionFromReadyAndNullMeasurementsAreAllowed() = runBlocking {
        withAdapter { h ->
            h.ready(wonju)
            assertEquals(RequestResult.Accepted, h.adapter.dispatch(completion.copy(elapsedSeconds = null, distanceMeters = null)))
            assertNull(h.calls.single().elapsedSeconds)
            assertNull(h.calls.single().distanceMeters)
            assertNull(h.adapter.state.value.elapsedSeconds)
            assertNull(h.adapter.state.value.distanceMeters)
        }
    }

    @Test fun destinationChangeKeepsIdIncrementsRevisionAndDropsOldPreparation() = runBlocking {
        val first = CompletableDeferred<Unit>()
        withAdapter(prepare = { if (it.regionId == "daejeon") withContext(NonCancellable) { first.await() } }) { h ->
            h.adapter.requestDestination(destination, JourneyKind.REGION)
            val original = h.adapter.state.value
            h.ready(wonju)
            assertEquals(original.journeyId, h.adapter.state.value.journeyId)
            assertEquals(1, h.adapter.state.value.routeRevision)
            first.complete(Unit)
            yield()
            assertEquals(wonju, h.adapter.state.value.destination)
            assertEquals(JourneyPhase.READY, h.adapter.state.value.phase)
            h.adapter.dispatch(completion)
            assertEquals("wonju", h.calls.single().regionId)
            assertEquals(1, h.calls.single().routeRevision)
        }
    }

    @Test fun changingKindRequiresCancelAndThenNewIdentity() = runBlocking {
        withAdapter { h ->
            h.ready()
            val first = h.adapter.state.value
            assertTrue(h.adapter.requestDestination(destination, JourneyKind.DAILY) is RequestResult.Failed)
            assertEquals(first, h.adapter.state.value)
            assertEquals(RequestResult.Accepted, h.adapter.cancel())
            h.ready(kind = JourneyKind.DAILY)
            assertNotEquals(first.journeyId, h.adapter.state.value.journeyId)
            assertEquals(0, h.adapter.state.value.routeRevision)
            assertNull(h.adapter.state.value.destination?.regionId)
        }
    }

    @Test fun cancelledPreparationCannotBecomeReadyAndCannotSave() = runBlocking {
        val prepared = CompletableDeferred<Unit>()
        withAdapter(prepare = { withContext(NonCancellable) { prepared.await() } }) { h ->
            h.adapter.requestDestination(destination, JourneyKind.REGION)
            assertEquals(RequestResult.Accepted, h.adapter.dispatch(DemoInput.Cancel))
            assertEquals(RequestResult.Accepted, h.adapter.cancel())
            prepared.complete(Unit)
            yield()
            assertEquals(JourneyPhase.CANCELLED, h.adapter.state.value.phase)
            assertTrue(h.adapter.dispatch(completion) is RequestResult.Failed)
            assertTrue(h.calls.isEmpty())
        }
    }

    @Test fun restrictedInputIsBlockedButCleanupRemainsPossible() = runBlocking {
        withAdapter { h ->
            h.allowed = false
            assertTrue(h.adapter.requestDestination(destination, JourneyKind.REGION) is RequestResult.Failed)
            assertEquals(JourneyPhase.IDLE, h.adapter.state.value.phase)
            h.allowed = true
            h.ready()
            val before = h.adapter.state.value
            h.allowed = false
            for (input in listOf(DemoInput.StartTravel, completion, DemoInput.RepeatLastArrival)) {
                assertTrue(h.adapter.dispatch(input) is RequestResult.Failed)
            }
            assertTrue(h.adapter.retryArrival() is RequestResult.Failed)
            assertEquals(before, h.adapter.state.value)
            assertEquals(RequestResult.Accepted, h.adapter.cancel())
            assertTrue(h.calls.isEmpty())
        }
    }

    @Test fun completeWaitsForStorageAndDoesNotQueueCompetingInputs() = runBlocking {
        val written = CompletableDeferred<ArrivalResult>()
        withAdapter { h ->
            h.save = { written.await() }
            h.ready()
            val job = async(start = CoroutineStart.UNDISPATCHED) { h.adapter.dispatch(completion) }
            assertFalse(job.isCompleted)
            assertEquals(JourneyPhase.ARRIVED, h.adapter.state.value.phase)
            assertTrue(h.adapter.requestDestination(wonju, JourneyKind.REGION) is RequestResult.Failed)
            assertTrue(h.adapter.cancel() is RequestResult.Failed)
            assertTrue(h.adapter.dispatch(DemoInput.RepeatLastArrival) is RequestResult.Failed)
            // Closing the gate must not discard an already validated, in-flight arrival save.
            h.allowed = false
            written.complete(ArrivalResult.Applied(receipt(h.calls.single())))
            assertEquals(RequestResult.Accepted, job.await())
            assertEquals(1, h.calls.size)
        }
    }

    @Test fun storageFailureRetriesExactOriginalAndThenReplayRedelivers() = runBlocking {
        withAdapter { h ->
            h.ready()
            h.save = { ArrivalResult.StorageFailure("private path") }
            assertTrue(h.adapter.dispatch(completion) is RequestResult.Failed)
            assertEquals(JourneyPhase.ARRIVED, h.adapter.state.value.phase)
            assertFalse(h.adapter.state.value.errorMessage!!.contains("private"))
            val original = h.calls.single()
            h.save = { ArrivalResult.Applied(receipt(it)) }
            assertEquals(RequestResult.Accepted, h.adapter.retryArrival())
            assertSame(original, h.calls[1])
            h.save = { ArrivalResult.AlreadyApplied(receipt(it)) }
            assertEquals(RequestResult.Accepted, h.adapter.dispatch(DemoInput.RepeatLastArrival))
            assertSame(original, h.calls[2])
            assertNull(h.adapter.state.value.errorMessage)
            assertTrue(h.adapter.retryArrival() is RequestResult.Failed)
        }
    }

    @Test fun rejectedArrivalDoesNotClaimSuccessOrOfferStorageRetry() = runBlocking {
        withAdapter { h ->
            h.ready()
            h.save = { ArrivalResult.Rejected("not implemented") }
            assertTrue(h.adapter.dispatch(completion) is RequestResult.Failed)
            assertTrue(h.adapter.retryArrival() is RequestResult.Failed)
            assertTrue(h.adapter.dispatch(DemoInput.RepeatLastArrival) is RequestResult.Failed)
            assertEquals(1, h.calls.size)
        }
    }

    @Test fun freshJourneyAndInvalidationDropRetainedCommands() = runBlocking {
        withAdapter { h ->
            h.ready()
            h.adapter.dispatch(completion)
            val old = h.adapter.state.value.journeyId
            h.ready(wonju)
            assertNotEquals(old, h.adapter.state.value.journeyId)
            assertTrue(h.adapter.dispatch(DemoInput.RepeatLastArrival) is RequestResult.Failed)
            h.adapter.dispatch(completion)
            h.adapter.invalidate()
            assertNull(h.adapter.state.value.journeyId)
            assertEquals(JourneyPhase.IDLE, h.adapter.state.value.phase)
            assertTrue(h.adapter.dispatch(DemoInput.RepeatLastArrival) is RequestResult.Failed)
            assertTrue(h.adapter.retryArrival() is RequestResult.Failed)
        }
    }

    @Test fun invalidationWaitsForWriteAndThenClearsSession() = runBlocking {
        val written = CompletableDeferred<ArrivalResult>()
        withAdapter { h ->
            h.ready()
            h.save = { written.await() }
            val complete = async(start = CoroutineStart.UNDISPATCHED) { h.adapter.dispatch(completion) }
            val invalidation = async(start = CoroutineStart.UNDISPATCHED) { h.adapter.invalidate() }
            assertFalse(invalidation.isCompleted)
            written.complete(ArrivalResult.Applied(receipt(h.calls.single())))
            assertEquals(RequestResult.Accepted, complete.await())
            withTimeout(2000) { invalidation.await() }
            assertEquals(JourneyPhase.IDLE, h.adapter.state.value.phase)
        }
    }

    @Test fun cancelledSaveReleasesLockAndRetainsOriginalForIdempotentRetry() = runBlocking {
        withAdapter { h ->
            h.ready()
            val pending = CompletableDeferred<ArrivalResult>()
            h.save = { pending.await() }
            val job = launch(start = CoroutineStart.UNDISPATCHED) { h.adapter.dispatch(completion) }
            job.cancelAndJoin()
            h.save = { ArrivalResult.AlreadyApplied(receipt(it)) }
            assertEquals(RequestResult.Accepted, h.adapter.retryArrival())
            assertSame(h.calls.first(), h.calls.last())
        }
    }

    @Test fun preparationAndUnexpectedStorageExceptionsAreReported() = runBlocking {
        withAdapter(prepare = { throw IllegalStateException("internal details") }) { h ->
            assertEquals(RequestResult.Accepted, h.adapter.requestDestination(destination, JourneyKind.REGION))
            withTimeout(2000) { h.adapter.state.first { it.phase == JourneyPhase.ERROR } }
            assertTrue(h.calls.isEmpty())
            assertFalse(h.adapter.state.value.errorMessage!!.contains("internal"))
        }
        withAdapter { h ->
            h.ready()
            h.save = { throw IllegalStateException("disk path") }
            assertTrue(h.adapter.dispatch(completion) is RequestResult.Failed)
            h.save = { ArrivalResult.Applied(receipt(it)) }
            assertEquals(RequestResult.Accepted, h.adapter.retryArrival())
        }
    }

    @Test fun invalidInputsHaveNoArrivalOrStateEffects() = runBlocking {
        withAdapter { h ->
            for (bad in listOf(destination.copy(label = ""), destination.copy(regionId = "missing"),
                destination.copy(latitude = Double.NaN), destination.copy(longitude = 181.0))) {
                assertTrue(h.adapter.requestDestination(bad, JourneyKind.REGION) is RequestResult.Failed)
                assertEquals(JourneyPhase.IDLE, h.adapter.state.value.phase)
            }
            h.ready()
            val ready = h.adapter.state.value
            for (bad in listOf(completion.copy(completedAtEpochMs = -1), completion.copy(elapsedSeconds = -1),
                completion.copy(distanceMeters = -1.0), completion.copy(distanceMeters = Double.NaN),
                completion.copy(distanceMeters = Double.POSITIVE_INFINITY))) {
                assertTrue(h.adapter.dispatch(bad) is RequestResult.Failed)
                assertEquals(ready, h.adapter.state.value)
            }
            assertTrue(h.calls.isEmpty())
        }
    }

    @Test fun realZerosArePreservedAndForeignReceiptCannotReportSuccess() = runBlocking {
        withAdapter { h ->
            h.ready()
            h.save = { ArrivalResult.Applied(receipt(it).copy(journeyId = "foreign-receipt")) }
            assertTrue(h.adapter.dispatch(completion.copy(elapsedSeconds = 0, distanceMeters = 0.0)) is RequestResult.Failed)
            assertEquals(0L, h.calls.single().elapsedSeconds)
            assertEquals(0.0, h.calls.single().distanceMeters)
            assertEquals(0L, h.adapter.state.value.elapsedSeconds)
            assertEquals(0.0, h.adapter.state.value.distanceMeters)
            assertTrue(h.adapter.dispatch(DemoInput.RepeatLastArrival) is RequestResult.Failed)
            h.save = { ArrivalResult.Applied(receipt(it)) }
            assertEquals(RequestResult.Accepted, h.adapter.retryArrival())
            assertSame(h.calls.first(), h.calls.last())
        }
    }

    @Test fun fixtureTransportPreservesNullsAndUsesRuntimeIdsInsteadOfFixtureIds() = runBlocking {
        val root = generateSequence(File(checkNotNull(System.getProperty("user.dir")))) { it.parentFile }
            .first { File(it, "fixtures/scenarios.json").isFile }
        val scenarios = Json.parseToJsonElement(File(root, "fixtures/scenarios.json").readText(Charsets.UTF_8))
            .jsonObject.getValue("scenarios").jsonArray
        withAdapter { h ->
            for (element in scenarios) {
                val scenario = element.jsonObject
                if (scenario.getValue("id").jsonPrimitive.content == "D04") {
                    val original = h.calls.last()
                    h.adapter.dispatch(DemoInput.RepeatLastArrival)
                    assertSame(original, h.calls.last())
                    continue
                }
                val input = scenario.getValue("arrival").jsonObject
                val kind = JourneyKind.valueOf(input.getValue("kind").jsonPrimitive.content)
                val region = input.getValue("regionId").jsonPrimitive.contentOrNull
                h.ready(if (region == "wonju") wonju else destination, kind)
                h.adapter.dispatch(DemoInput.Complete(input.getValue("completedAtEpochMs").jsonPrimitive.long,
                    input.getValue("elapsedSeconds").jsonPrimitive.longOrNull,
                    input.getValue("distanceMeters").jsonPrimitive.doubleOrNull))
                val command = h.calls.last()
                assertNotEquals(input.getValue("journeyId").jsonPrimitive.content, command.journeyId)
                assertEquals(region, command.regionId)
                assertEquals(kind, command.kind)
                assertEquals(input.getValue("elapsedSeconds").jsonPrimitive.longOrNull, command.elapsedSeconds)
                assertEquals(input.getValue("distanceMeters").jsonPrimitive.doubleOrNull, command.distanceMeters)
            }
        }
    }

    private suspend fun withAdapter(
        prepare: suspend (DestinationSpec) -> Unit = {},
        block: suspend (Harness) -> Unit,
    ) {
        val h = Harness(prepare)
        try { block(h) } finally { h.scope.cancel() }
    }

    private inner class Harness(prepare: suspend (DestinationSpec) -> Unit) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        var allowed = true
        var counter = 100
        val calls = mutableListOf<ArrivalCommand>()
        var save: suspend (ArrivalCommand) -> ArrivalResult = { ArrivalResult.Applied(receipt(it)) }
        val adapter = DemoJourneyAdapter(scope, setOf("daejeon", "wonju", "sokcho"), { allowed },
            { calls += it; save(it) }, prepare,
            { "10000000-0000-4000-8000-${(++counter).toString().padStart(12, '0')}" })
        suspend fun ready(target: DestinationSpec = destination, kind: JourneyKind = JourneyKind.REGION) {
            assertEquals(RequestResult.Accepted, adapter.requestDestination(target, kind))
            awaitReady()
        }
        suspend fun awaitReady() = withTimeout(2000) { adapter.state.first { it.phase == JourneyPhase.READY } }
    }

    private fun receipt(c: ArrivalCommand) = ArrivalReceipt(c.journeyId, c.source, c.kind, c.regionId,
        c.completedAtEpochMs, c.elapsedSeconds, c.distanceMeters, 0, 0, null, emptyList())
}
