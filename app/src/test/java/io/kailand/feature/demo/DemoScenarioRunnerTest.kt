package io.kailand.feature.demo

import io.kailand.core.contract.*
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class DemoScenarioRunnerTest {
    private class Fixture {
        private val root = generateSequence(File(checkNotNull(System.getProperty("user.dir")))) { it.parentFile }
            .first { File(it, "content/catalog.json").isFile }
        val text = File(root, "fixtures/scenarios.json").readText(Charsets.UTF_8)
        val scenarios = DemoScenarios.fromJson(text)
        private val json = Json.parseToJsonElement(File(root, "content/catalog.json").readText(Charsets.UTF_8)).jsonObject
        private fun JsonObject.text(k: String) = getValue(k).jsonPrimitive.content
        val catalog = Catalog(json.text("contentVersion"), json.text("description"), emptyList(), emptyMap(), emptyMap(),
            json.getValue("regions").jsonArray.map { v -> v.jsonObject.let {
                RegionDefinition(it.text("id"), it.text("name"), it.text("theme"), it.getValue("latitude").jsonPrimitive.double,
                    it.getValue("longitude").jsonPrimitive.double, it.getValue("itemIds").jsonArray.map { i -> i.jsonPrimitive.content })
            } }, emptyList(), GameRules(3, 1, "Asia/Seoul", emptyList()))
        private val emptyGame = GameSnapshot(AvatarPreview("tori", null), 0, emptySet(), emptySet(), emptyMap(), null)
        var game = emptyGame
        var gameAvailable = true
        var gameReadFails = false
        var daily: DestinationSpec? = catalog.regions.first().let { DestinationSpec(null, it.name, it.latitude, it.longitude) }
        val journeyState = MutableStateFlow(JourneySnapshot(null, DataSource.DEMO, JourneyKind.REGION, JourneyPhase.IDLE, 0, null))
        val requests = mutableListOf<DestinationSpec>()
        val inputs = mutableListOf<DemoInput>()
        var resetCalls = 0
        var retries = 0
        var counter = 0
        var lastComplete: DemoInput.Complete? = null
        var lastReceipt: ArrivalReceipt? = null
        var requestHook: (suspend (DestinationSpec, JourneyKind) -> RequestResult)? = null
        var dispatchHook: (suspend (DemoInput) -> RequestResult)? = null
        var resetHook: (suspend () -> RequestResult)? = null
        var retryHook: (suspend () -> RequestResult)? = null
        val journey = object : JourneyPort {
            override val state = journeyState
            override suspend fun requestDestination(destination: DestinationSpec, kind: JourneyKind): RequestResult {
                requests.add(destination)
                requestHook?.let { return it(destination, kind) }
                journeyState.value = JourneySnapshot("A-runtime-${++counter}", DataSource.DEMO, kind,
                    JourneyPhase.READY, 0, destination)
                return RequestResult.Accepted
            }
            override suspend fun cancel(): RequestResult = error("Runner must use the DEMO port")
            override suspend fun retryArrival(): RequestResult {
                retries++
                retryHook?.let { return it() }
                return save(checkNotNull(lastComplete))
            }
        }
        val demo = object : DemoControlPort {
            override suspend fun dispatch(input: DemoInput): RequestResult {
                inputs.add(input)
                if (input is DemoInput.Complete) lastComplete = input
                dispatchHook?.let { return it(input) }
                return when (input) {
                    DemoInput.StartTravel -> { journeyState.value = journeyState.value.copy(phase = JourneyPhase.TRAVELLING); RequestResult.Accepted }
                    is DemoInput.Complete -> save(input)
                    DemoInput.RepeatLastArrival -> { game = game.copy(pendingReceipt = lastReceipt); RequestResult.Accepted }
                    DemoInput.Cancel -> { journeyState.value = journeyState.value.copy(phase = JourneyPhase.CANCELLED); RequestResult.Accepted }
                }
            }
        }
        val runner = DemoScenarioRunner(scenarios, catalog, journey, demo, {
            check(!gameReadFails) { "snapshot read failed" }
            game.takeIf { gameAvailable }
        }, { daily }, {
            resetCalls++
            resetHook?.invoke() ?: run {
                game = emptyGame
                lastReceipt = null
                journeyState.value = journeyState.value.copy(journeyId = null, phase = JourneyPhase.IDLE)
                RequestResult.Accepted
            }
        })
        val allowed = DemoContext(InteractionGate(GateState.PARKED_ALLOWED), true, true)
        init { runner.updateContext(allowed) }
        val state get() = runner.state.value
        fun plan(id: String) = scenarios.single { it.id == id }
        fun save(input: DemoInput.Complete): RequestResult {
            val plan = checkNotNull(state.selected)
            val receipt = ArrivalReceipt(checkNotNull(journeyState.value.journeyId), DataSource.DEMO, plan.kind, plan.regionId,
                input.completedAtEpochMs, input.elapsedSeconds, input.distanceMeters, game.dailyGrowth,
                game.dailyGrowth + plan.expectedGrowth, plan.expectedDiscovery, plan.expectedItems.toList())
            game = game.copy(dailyGrowth = game.dailyGrowth + plan.expectedGrowth,
                ownedItemIds = game.ownedItemIds + plan.expectedItems,
                discoveredRegionIds = game.discoveredRegionIds + setOfNotNull(plan.expectedDiscovery), pendingReceipt = receipt)
            lastReceipt = receipt
            journeyState.value = journeyState.value.copy(phase = JourneyPhase.ARRIVED)
            return RequestResult.Accepted
        }
        suspend fun readyToComplete(id: String = "D03") {
            assertTrue(runner.selectScenario(id))
            if (plan(id).resetBefore) assertTrue(runner.resetConfirmed())
            assertTrue(runner.advance())
            assertEquals(DemoStep.WAIT_READY, state.step)
            assertTrue(runner.advance())
            assertEquals(DemoStep.START, state.step)
            assertTrue(runner.advance())
            assertEquals(DemoStep.COMPLETE, state.step)
        }
    }

    @Test fun fixturesSupplyPlansWithoutRuntimeUuidOrNullMeasurementFabrication() {
        val f = Fixture()
        assertEquals(6, f.scenarios.size)
        assertEquals("D03", f.plan("D04").replayOf)
        assertNull(f.plan("D06").elapsedSeconds)
        assertNull(f.plan("D06").distanceMeters)
        assertFalse(f.scenarios.toString().contains("00000000-0000-4000"))
        try { DemoScenarios.fromJson(f.text.replace("\"DEMO\"", "\"PLEOS\"")); fail() } catch (_: IllegalArgumentException) { }
    }

    @Test fun blockedOrInactiveAndPleosContextsNeverDispatchDemoCommands() = runBlocking {
        for (context in listOf(DemoContext(), DemoContext(InteractionGate(GateState.RESTRICTED), true, true),
            DemoContext(InteractionGate(GateState.PARKED_ALLOWED), false, true),
            DemoContext(InteractionGate(GateState.PARKED_ALLOWED), true, false),
            DemoContext(InteractionGate(GateState.PARKED_ALLOWED), true, true, DataSource.PLEOS))) {
            val f = Fixture()
            f.runner.updateContext(context)
            assertFalse(f.runner.selectScenario("D03"))
            assertFalse(f.runner.advance())
            assertFalse(f.runner.resetConfirmed())
            assertTrue(f.inputs.isEmpty()); assertTrue(f.requests.isEmpty()); assertEquals(0, f.resetCalls)
        }
    }

    @Test fun manualFlowSeparatesResetRequestReadyStartCompleteAndSavedResult() = runBlocking {
        val f = Fixture()
        assertTrue(f.runner.selectScenario("D03"))
        assertEquals(DemoStep.RESET, f.state.step)
        assertFalse(f.runner.advance())
        assertEquals(0, f.resetCalls)
        assertTrue(f.runner.resetConfirmed())
        assertTrue(f.runner.advance())
        assertEquals(DemoStep.WAIT_READY, f.state.step)
        assertNull(f.game.pendingReceipt)
        assertTrue(f.inputs.isEmpty())
        assertTrue(f.runner.advance())
        assertTrue(f.runner.advance())
        assertNull(f.game.pendingReceipt)
        assertTrue(f.runner.advance())
        assertEquals(DemoStep.DONE, f.state.step)
        assertEquals(f.plan("D03").expectedItems, f.game.ownedItemIds)
        assertTrue(f.game.pendingReceipt!!.journeyId.startsWith("A-runtime-"))
        assertEquals(listOf(DemoInput.StartTravel, f.lastComplete), f.inputs)
        assertFalse(f.runner.advance())
    }

    @Test fun acceptedDestinationWaitsForMatchingNewReadyInsteadOfAssumingSuccess() = runBlocking {
        val f = Fixture()
        f.runner.selectScenario("D03"); f.runner.resetConfirmed()
        f.requestHook = { destination, kind ->
            f.journeyState.value = JourneySnapshot("A-pending", DataSource.DEMO, kind, JourneyPhase.PREPARING, 0, destination)
            RequestResult.Accepted
        }
        assertTrue(f.runner.advance())
        assertFalse(f.runner.advance())
        assertEquals(DemoStep.WAIT_READY, f.state.step)
        f.journeyState.value = f.journeyState.value.copy(phase = JourneyPhase.READY)
        assertTrue(f.runner.advance())
        assertEquals("A-pending", f.state.journeyId)
        assertTrue(f.inputs.isEmpty())
    }

    @Test fun staleReadyFromBeforeRequestIsRejected() = runBlocking {
        val f = Fixture()
        f.runner.selectScenario("D03"); f.runner.resetConfirmed()
        val region = f.catalog.regions.first()
        f.journeyState.value = JourneySnapshot("old-ready", DataSource.DEMO, JourneyKind.REGION, JourneyPhase.READY, 0,
            DestinationSpec(region.id, region.name, region.latitude, region.longitude))
        f.requestHook = { _, _ -> RequestResult.Accepted }
        f.runner.advance()
        assertFalse(f.runner.advance())
        assertTrue(f.inputs.isEmpty())
    }

    @Test fun readinessUsesSubmittedDestinationEvenIfCandidateChangesWhileWaiting() = runBlocking {
        val f = Fixture()
        f.runner.selectScenario("D01"); f.runner.resetConfirmed()
        assertTrue(f.runner.advance())
        f.daily = f.daily!!.copy(label = "다른 목적지", latitude = 0.0)
        assertTrue(f.runner.advance())
        assertEquals(DemoStep.START, f.state.step)
    }

    @Test fun replayRequiresD03AndActuallyDispatchesSameArrivalWithoutNewDestination() = runBlocking {
        val f = Fixture()
        assertFalse(f.runner.selectScenario("D04"))
        assertFalse(f.runner.selectScenario("D02"))
        f.readyToComplete(); f.runner.advance()
        val before = f.game
        assertTrue(f.runner.selectScenario("D04"))
        assertEquals(DemoStep.REPEAT, f.state.step)
        assertTrue(f.runner.advance())
        assertEquals(DemoInput.RepeatLastArrival, f.inputs.last())
        assertEquals(before, f.game)
        assertEquals(1, f.requests.size)
        assertEquals(DemoStep.DONE, f.state.step)
    }

    @Test fun missingMeasurementsRemainNullWhenForwardedToA() = runBlocking {
        val f = Fixture()
        f.readyToComplete("D06"); assertTrue(f.runner.advance())
        assertNull(f.lastComplete!!.elapsedSeconds)
        assertNull(f.lastComplete!!.distanceMeters)
        assertNull(f.game.pendingReceipt!!.elapsedSeconds)
    }

    @Test fun dailyThenAdditionalDailyAndRegionRevisitFollowFixtureDependencies() = runBlocking {
        val f = Fixture()
        f.readyToComplete("D01"); assertTrue(f.runner.advance())
        assertEquals(0, f.game.dailyGrowth)
        f.readyToComplete("D02"); assertTrue(f.runner.advance())
        assertEquals(0, f.game.dailyGrowth)
        assertEquals(2, f.requests.size)
        f.readyToComplete("D03"); assertTrue(f.runner.advance())
        val original = f.game
        assertEquals(1, original.dailyGrowth)
        assertEquals(3, original.ownedItemIds.size)
        f.runner.selectScenario("D04"); assertTrue(f.runner.advance())
        assertEquals(original, f.game)
        f.readyToComplete("D05"); assertTrue(f.runner.advance())
        assertEquals(1, f.game.dailyGrowth)
        assertEquals(original.ownedItemIds, f.game.ownedItemIds)
        assertTrue(f.game.pendingReceipt!!.grantedItemIds.isEmpty())
        assertEquals(4, f.requests.size) // Replay creates neither a destination request nor a new journey.
    }

    @Test fun storageFailureRetriesOriginalArrivalAndDoesNotSendCompleteAgain() = runBlocking {
        val f = Fixture()
        f.readyToComplete()
        val id = f.state.journeyId
        f.dispatchHook = { RequestResult.Failed("storage failed") }
        assertFalse(f.runner.advance())
        assertEquals(DemoStep.RETRY_ARRIVAL, f.state.step)
        assertEquals("storage failed", f.state.message)
        assertNull(f.game.pendingReceipt)
        assertTrue(f.runner.advance())
        assertEquals(DemoStep.DONE, f.state.step)
        assertEquals(1, f.retries)
        assertEquals(id, f.game.pendingReceipt!!.journeyId)
        assertEquals(1, f.inputs.count { it is DemoInput.Complete })
    }

    @Test fun acceptedSaveWithUnavailableSnapshotOnlyRechecksTheResult() = runBlocking {
        for (throwsOnRead in listOf(false, true)) {
            val f = Fixture()
            f.readyToComplete()
            f.dispatchHook = { input ->
                f.save(input as DemoInput.Complete).also {
                    f.gameReadFails = throwsOnRead
                    f.gameAvailable = false
                }
            }
            assertFalse(f.runner.advance())
            val committed = f.game
            val sent = f.inputs.toList()
            assertEquals(DemoStep.VERIFY_SAVED, f.state.step)
            assertEquals(f.plan("D03").expectedGrowth, committed.dailyGrowth)
            assertEquals(3, committed.ownedItemIds.size)
            assertNotNull(committed.pendingReceipt)

            assertFalse(f.runner.advance())
            assertEquals(DemoStep.VERIFY_SAVED, f.state.step)
            f.gameAvailable = true
            f.gameReadFails = false
            assertTrue(f.runner.advance())
            assertEquals(DemoStep.DONE, f.state.step)
            assertEquals(committed, f.game)
            assertEquals(sent, f.inputs)
            assertEquals(0, f.retries)
        }
    }

    @Test fun acceptedWithoutStoredReceiptRemainsUnverifiedWithoutRedispatch() = runBlocking {
        val f = Fixture()
        f.readyToComplete()
        val before = f.game
        f.dispatchHook = {
            f.journeyState.value = f.journeyState.value.copy(phase = JourneyPhase.ARRIVED)
            RequestResult.Accepted
        }
        assertFalse(f.runner.advance())
        val sent = f.inputs.toList()
        repeat(3) {
            assertFalse(f.runner.advance())
            assertEquals(DemoStep.VERIFY_SAVED, f.state.step)
            assertEquals(before, f.game)
            assertNull(f.game.pendingReceipt)
            assertEquals(sent, f.inputs)
            assertEquals(0, f.retries)
        }
    }

    @Test fun replayVerificationRechecksWithoutDispatchingReplayAgain() = runBlocking {
        val f = Fixture()
        f.readyToComplete("D03")
        assertTrue(f.runner.advance())
        val original = f.game
        assertTrue(f.runner.selectScenario("D04"))
        f.dispatchHook = { input ->
            assertEquals(DemoInput.RepeatLastArrival, input)
            f.gameAvailable = false
            RequestResult.Accepted
        }
        assertFalse(f.runner.advance())
        assertEquals(DemoStep.VERIFY_SAVED, f.state.step)
        f.gameAvailable = true
        assertTrue(f.runner.advance())
        assertEquals(DemoStep.DONE, f.state.step)
        assertEquals(original, f.game)
        assertEquals(1, f.inputs.count { it == DemoInput.RepeatLastArrival })
        assertEquals(0, f.retries)
    }

    @Test fun leavingForegroundInvalidatesSavedVerificationWithoutCancellingTheArrival() = runBlocking {
        val f = Fixture()
        f.readyToComplete()
        f.dispatchHook = { input ->
            f.save(input as DemoInput.Complete).also { f.gameAvailable = false }
        }
        assertFalse(f.runner.advance())
        assertEquals(DemoStep.VERIFY_SAVED, f.state.step)
        val committed = f.game
        val sent = f.inputs.toList()
        assertFalse(f.runner.cancelJourney())

        f.runner.updateContext(f.allowed.copy(foreground = false))
        assertFalse(f.runner.advance())
        f.runner.updateContext(f.allowed)
        f.gameAvailable = true
        assertEquals(DemoStep.CHOOSE, f.state.step)
        assertFalse(f.runner.advance())
        assertEquals(committed, f.game)
        assertEquals(sent, f.inputs)
        assertEquals(0, f.retries)
    }

    @Test fun wrongReceiptOrUnexpectedGameEffectsDoNotPassVerification() = runBlocking {
        for (wrongReceipt in listOf(true, false)) {
            val f = Fixture()
            f.readyToComplete()
            f.dispatchHook = { input ->
                f.save(input as DemoInput.Complete)
                f.game = if (wrongReceipt) f.game.copy(pendingReceipt = f.game.pendingReceipt!!.copy(journeyId = "old"))
                    else f.game.copy(ownedItemIds = f.game.ownedItemIds + "unexpected")
                RequestResult.Accepted
            }
            assertFalse(f.runner.advance())
            assertEquals(DemoStep.VERIFY_SAVED, f.state.step)
            val mismatched = f.game
            assertFalse(f.runner.advance())
            assertEquals(DemoStep.VERIFY_SAVED, f.state.step)
            assertEquals(mismatched, f.game)
            assertEquals(0, f.retries)
        }
    }

    @Test fun pendingCommandDisablesDuplicatesAndWaitsForAcknowledgement() = runBlocking {
        val f = Fixture()
        f.readyToComplete()
        val release = CompletableDeferred<Unit>()
        f.dispatchHook = { input -> release.await(); f.save(input as DemoInput.Complete) }
        val work = async(start = CoroutineStart.UNDISPATCHED) { f.runner.advance() }
        assertTrue(f.state.busy)
        assertFalse(f.runner.advance())
        assertEquals(1, f.inputs.count { it is DemoInput.Complete })
        assertNotEquals(DemoStep.DONE, f.state.step)
        release.complete(Unit)
        assertTrue(work.await())
        assertEquals(DemoStep.DONE, f.state.step)
    }

    @Test fun modeChangeAndLateNonCooperativeCallbackCannotResumeRunner() = runBlocking {
        val f = Fixture()
        f.readyToComplete()
        val release = CompletableDeferred<Unit>()
        f.dispatchHook = { withContext(NonCancellable) { release.await() }; RequestResult.Accepted }
        val work = async(start = CoroutineStart.UNDISPATCHED) { f.runner.advance() }
        f.runner.updateContext(f.allowed.copy(mode = DataSource.PLEOS))
        f.runner.updateContext(f.allowed)
        release.complete(Unit)
        assertFalse(work.await())
        assertEquals(DemoStep.CHOOSE, f.state.step)
        assertNull(f.state.selected)
        assertFalse(f.runner.selectScenario("D04"))
        assertFalse(f.state.busy)
    }

    @Test fun confirmedResetDuringAwaitCancelsWithoutJoiningItsOwnCaller() = runBlocking {
        val f = Fixture()
        f.readyToComplete()
        val release = CompletableDeferred<Unit>()
        f.dispatchHook = { withContext(NonCancellable) { release.await() }; RequestResult.Accepted }
        val work = async(start = CoroutineStart.UNDISPATCHED) { f.runner.advance() }
        assertTrue(withTimeout(2_000) { f.runner.resetConfirmed() })
        assertEquals(2, f.resetCalls)
        assertEquals(DemoStep.CHOOSE, f.state.step)
        release.complete(Unit)
        assertFalse(work.await())
        assertNull(f.game.pendingReceipt)
        assertFalse(f.state.resetBlocked)
    }

    @Test fun resetFailureKeepsInputBlockedUntilExplicitSuccessfulRetry() = runBlocking {
        val f = Fixture()
        f.runner.selectScenario("D03")
        f.resetHook = { RequestResult.Failed("disk error") }
        assertFalse(f.runner.resetConfirmed())
        assertTrue(f.state.resetBlocked)
        assertFalse(f.runner.selectScenario("D06"))
        assertFalse(f.runner.advance())
        f.resetHook = null
        assertTrue(f.runner.resetConfirmed())
        assertEquals(DemoStep.PREPARE, f.state.step)
        assertTrue(f.runner.advance())
    }

    @Test fun resetIdentitySurvivesModeChangeButOldResetCannotUnlockNewReset() {
        val f = Fixture()
        val old = f.runner.beginReset()
        f.runner.updateContext(f.allowed.copy(mode = DataSource.PLEOS))
        val current = f.runner.beginReset()
        f.runner.updateContext(f.allowed)
        assertFalse(f.runner.finishReset(old, true))
        assertTrue(f.state.resetBlocked)
        assertTrue(f.runner.finishReset(current, true))
        assertTrue(f.state.interactionAllowed)
    }

    @Test fun confirmedResetAckAfterModeSwitchUnlocksWithoutResumingOldScenario() = runBlocking {
        val f = Fixture()
        f.runner.selectScenario("D03")
        val release = CompletableDeferred<Unit>()
        f.resetHook = { release.await(); RequestResult.Accepted }
        val work = async(start = CoroutineStart.UNDISPATCHED) { f.runner.resetConfirmed() }
        f.runner.updateContext(f.allowed.copy(mode = DataSource.PLEOS))
        assertFalse(f.state.interactionAllowed)
        release.complete(Unit)
        assertTrue(work.await())
        assertFalse(f.state.resetBlocked)
        assertFalse(f.state.interactionAllowed)
        f.runner.updateContext(f.allowed)
        assertEquals(DemoStep.CHOOSE, f.state.step)
        assertNull(f.state.selected)
        assertFalse(f.runner.advance())
    }

    @Test fun realPortSourceCannotBeOverriddenByDemoContext() = runBlocking {
        val f = Fixture()
        f.journeyState.value = f.journeyState.value.copy(source = DataSource.PLEOS)
        assertFalse(f.runner.selectScenario("D03"))
        assertFalse(f.runner.resetConfirmed())
        assertFalse(f.runner.advance())
        assertFalse(f.runner.cancelJourney())
        assertTrue(f.inputs.isEmpty()); assertTrue(f.requests.isEmpty()); assertEquals(0, f.resetCalls)
    }

    @Test fun absentDailyDestinationDoesNotInventCoordinatesOrDispatch() = runBlocking {
        val f = Fixture()
        f.runner.selectScenario("D01"); f.runner.resetConfirmed()
        f.daily = null
        assertFalse(f.runner.advance())
        assertTrue(f.requests.isEmpty())
        assertEquals(DemoStep.PREPARE, f.state.step)
    }

    @Test fun missingGameLoadBlocksCompleteBeforeAnyWriteRequest() = runBlocking {
        val f = Fixture()
        f.readyToComplete()
        f.gameAvailable = false
        assertFalse(f.runner.advance())
        assertEquals(0, f.inputs.count { it is DemoInput.Complete })
        f.gameAvailable = true
        assertTrue(f.runner.advance())
    }

    @Test fun leavingScreenInvalidatesSelectionAndCannotAutoResume() = runBlocking {
        val f = Fixture()
        f.readyToComplete()
        f.runner.leaveScreen()
        f.runner.updateContext(f.allowed)
        assertEquals(DemoStep.CHOOSE, f.state.step)
        assertFalse(f.runner.advance())
        assertEquals(0, f.inputs.count { it is DemoInput.Complete })
    }

    @Test fun manualCancellationUsesOnlyDemoPortAndNeverProducesArrival() = runBlocking {
        val f = Fixture()
        f.readyToComplete()
        assertTrue(f.runner.cancelJourney())
        assertEquals(DemoInput.Cancel, f.inputs.last())
        assertEquals(JourneyPhase.CANCELLED, f.journeyState.value.phase)
        assertNull(f.game.pendingReceipt)
        assertEquals(DemoStep.CHOOSE, f.state.step)
        assertFalse(f.runner.advance())
        assertFalse(f.runner.cancelJourney())
    }
}
