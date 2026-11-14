package io.kailand.app

import io.kailand.core.contract.*
import io.kailand.feature.demo.*
import io.kailand.feature.island.*
import io.kailand.feature.visit.*
import java.io.File
import java.io.IOException
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class GameSimulationRuntimeTest {
    private val catalogJson = File("../content/catalog.json").readText(Charsets.UTF_8)
    private val catalog = decodeCatalog(catalogJson)
    private val plans = DemoScenarios.fromJson(File("../fixtures/scenarios.json").readText(Charsets.UTF_8))
    private val parked = InteractionGate(GateState.PARKED_ALLOWED)

    @Test fun runnerOwnResetAcknowledgementPreservesD01AndAllDependentSteps() = runBlocking {
        val p = Pipeline()
        try {
            assertTrue(p.runtime.runner.selectScenario("D01"))
            assertTrue(p.runtime.runner.resetConfirmed())
            assertEquals("D01", p.runtime.runner.state.value.selected?.id)
            assertEquals(DemoStep.PREPARE, p.runtime.runner.state.value.step)
            assertEquals(GameCommandSession(1), p.commands.session.value)
            fun verify(id: String, before: Int, after: Int, discovered: String? = null): ArrivalReceipt {
                val plan = plans.single { it.id == id }
                val snapshot = p.game.state.value
                val receipt = checkNotNull(snapshot.pendingReceipt)
                assertEquals(id, p.runtime.runner.state.value.selected?.id)
                assertEquals(DemoStep.DONE, p.runtime.runner.state.value.step)
                assertEquals(plan.kind, receipt.kind)
                assertEquals(DataSource.DEMO, receipt.source)
                assertEquals(plan.regionId, receipt.regionId)
                assertEquals(plan.completedAtEpochMs, receipt.completedAtEpochMs)
                assertEquals(plan.elapsedSeconds, receipt.elapsedSeconds)
                assertEquals(plan.distanceMeters, receipt.distanceMeters)
                assertEquals(before, receipt.growthBefore)
                assertEquals(after, receipt.growthAfter)
                assertEquals(after, snapshot.dailyGrowth)
                assertEquals(discovered, receipt.discoveredRegionId)
                val gifts = catalog.regions.singleOrNull { it.id == discovered }?.itemIds.orEmpty()
                assertEquals(gifts, receipt.grantedItemIds)
                assertEquals(receipt, p.store.save.processedReceipts.getValue(receipt.journeyId).toContract())
                return receipt
            }

            p.finishSelected()
            val firstDaily = verify("D01", 0, 0)
            p.runScenario("D02")
            val secondDaily = verify("D02", 0, 0)
            assertNotEquals(firstDaily.journeyId, secondDaily.journeyId)
            assertEquals(setOf(firstDaily.journeyId, secondDaily.journeyId), p.store.save.processedReceipts.keys)
            assertNull(p.store.save.lastDailyGrantDate)
            assertTrue(p.game.state.value.ownedItemIds.isEmpty())

            p.runScenario("D03")
            val original = verify("D03", 0, 1, "daejeon")
            assertEquals(GameCommandSession(2), p.commands.session.value)
            assertEquals(3, original.grantedItemIds.size)
            val discovered = p.store.save
            assertEquals(setOf(original.journeyId), discovered.processedReceipts.keys)
            p.runScenario("D04")
            assertEquals(DemoStep.DONE, p.runtime.runner.state.value.step)
            assertEquals(original, p.game.state.value.pendingReceipt)
            assertEquals(discovered, p.store.save)

            p.runScenario("D05")
            val revisit = verify("D05", 1, 1)
            assertNotEquals(original.journeyId, revisit.journeyId)
            assertEquals(setOf(original.journeyId, revisit.journeyId), p.store.save.processedReceipts.keys)
            assertEquals(original.grantedItemIds.toSet(), p.game.state.value.ownedItemIds)

            p.runScenario("D06")
            val wonju = verify("D06", 0, 1, "wonju")
            assertEquals(GameCommandSession(3), p.commands.session.value)
            assertNull(wonju.elapsedSeconds)
            assertNull(wonju.distanceMeters)
            assertEquals(3, wonju.grantedItemIds.size)
            assertEquals(wonju.grantedItemIds.toSet(), p.game.state.value.ownedItemIds)
            assertEquals(setOf("wonju"), p.game.state.value.discoveredRegionIds)
            assertEquals(setOf(wonju.journeyId), p.store.save.processedReceipts.keys)
        } finally { p.scope.cancel() }
    }

    @Test fun committedFixtureMismatchRechecksWithoutRetryingStorage() = runBlocking {
        val outdated = plans.map { if (it.id == "D01") it.copy(expectedGrowth = 1) else it }
        val p = Pipeline(outdated)
        try {
            val runner = p.runtime.runner
            assertTrue(runner.selectScenario("D01"))
            assertTrue(runner.resetConfirmed())
            repeat(3) { assertTrue(runner.advance()) }
            assertEquals(DemoStep.COMPLETE, runner.state.value.step)

            assertFalse(runner.advance())
            val committed = p.store.save
            val receipt = checkNotNull(p.game.state.value.pendingReceipt)
            assertEquals(JourneyPhase.ARRIVED, p.runtime.snapshot.phase)
            assertEquals(0, receipt.growthAfter)
            assertEquals(receipt, committed.processedReceipts.getValue(receipt.journeyId).toContract())
            assertEquals(DemoStep.VERIFY_SAVED, runner.state.value.step)
            val verificationMessage = runner.state.value.message
            assertTrue(verificationMessage.orEmpty().contains("시연 기대값"))

            p.store.beforeNextUpdate = { fail("Verification must not produce another write") }
            assertFalse(runner.advance())
            assertEquals(DemoStep.VERIFY_SAVED, runner.state.value.step)
            assertEquals(verificationMessage, runner.state.value.message)
            assertEquals(committed, p.store.save)
            assertEquals(receipt, p.game.state.value.pendingReceipt)
        } finally { p.scope.cancel() }
    }

    @Test fun u20ResetDrainsRunnerWriteWithoutSelfJoinAndRejectsLateArrivalAndVisit() = runBlocking {
        withTimeout(5000) {
            val p = Pipeline()
            try {
                assertTrue(p.runtime.runner.selectScenario("D03"))
                assertTrue(p.runtime.runner.resetConfirmed())
                val oldGeneration = p.commands.session.value.generation
                p.runtime.setScreens(demo = true, visit = true)
                val token = (p.runtime.visit.request() as VisitRequestResult.Created).request.token
                val lateTimer = p.timer.actions.last()
                repeat(3) { assertTrue(p.runtime.runner.advance()); yield() }
                val id = p.runtime.snapshot.journeyId!!
                val entered = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
                p.store.beforeNextUpdate = { entered.complete(Unit); release.await() }
                val operation = p.scope.async { p.runtime.runner.advance() }
                entered.await()
                val reset = async(start = CoroutineStart.UNDISPATCHED) { p.runtime.runner.resetConfirmed() }
                assertFalse(reset.isCompleted)
                release.complete(Unit)
                assertTrue(reset.await())
                operation.join() // Cancelled by Runner.beginReset, never joined from its own reset job.
                assertEquals(GameSnapshot(AvatarPreview("tori", null), 0, emptySet(), emptySet(), emptyMap(), null), p.game.state.value)
                assertTrue(p.store.save.processedReceipts.isEmpty())
                val stale = ArrivalCommand(id, DataSource.DEMO, JourneyKind.REGION, "daejeon", 0,
                    ArrivalEvidence.DEMO_FINAL_DESTINATION, 1793408400000L)
                assertTrue(p.commands.completeArrival(oldGeneration, stale) is ArrivalResult.Rejected)
                p.clock.now = 20_000
                lateTimer()
                assertFalse(p.runtime.visit.accept(token))
                assertEquals(VisitPhase.IDLE, p.runtime.visit.state.value.phase)
            } finally { p.scope.cancel() }
        }
    }

    @Test fun u20QueuedOldActionCannotWriteAfterResetEvenWithSameDefaultSave() = runBlocking {
        withTimeout(5000) {
            val p = Pipeline()
            try {
                val entered = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
                p.store.beforeNextUpdate = { entered.complete(Unit); release.await() }
                val first = async(start = CoroutineStart.UNDISPATCHED) {
                    p.commands.apply(0, GameAction.SelectCharacter("momo"))
                }
                entered.await()
                val reset = async(start = CoroutineStart.UNDISPATCHED) { p.runtime.runner.resetConfirmed() }
                val late = async(start = CoroutineStart.UNDISPATCHED) {
                    p.commands.apply(0, GameAction.SelectCharacter("lulu"))
                }
                release.complete(Unit)
                assertEquals(MutationResult.Success, first.await())
                assertTrue(reset.await())
                assertTrue(late.await() is MutationResult.Rejected)
                assertEquals(AvatarPreview("tori", null), p.game.state.value.avatar)
                assertEquals(GameCommandSession(1), p.commands.session.value)
                assertTrue(p.runtime.runner.resetConfirmed()) // Same stored values, still a new generation.
                assertEquals(GameCommandSession(2), p.commands.session.value)
            } finally { p.scope.cancel() }
        }
    }

    @Test fun resetFailureBlocksRunnerAndVisitUntilExplicitRetry() = runBlocking {
        val p = Pipeline()
        try {
            p.runtime.setScreens(demo = true, visit = true)
            p.store.failNext = true
            assertTrue(p.runtime.runner.selectScenario("D01"))
            assertFalse(p.runtime.runner.resetConfirmed())
            assertEquals(GameCommandPhase.RESET_FAILED, p.commands.session.value.phase)
            assertTrue(p.runtime.runner.state.value.resetBlocked)
            assertFalse(p.runtime.runner.state.value.busy)
            assertTrue(p.runtime.visit.request() is VisitRequestResult.Rejected)
            assertTrue(p.runtime.journey.requestDestination(p.destination, JourneyKind.REGION) is RequestResult.Failed)
            assertTrue(p.runtime.runner.resetConfirmed())
            assertEquals(GameCommandPhase.ACTIVE, p.commands.session.value.phase)
            assertFalse(p.runtime.runner.state.value.resetBlocked)
            assertEquals("D01", p.runtime.runner.state.value.selected?.id)
            assertEquals(VisitPhase.IDLE, p.runtime.visit.state.value.phase)
        } finally { p.scope.cancel() }
    }

    @Test fun modeChangePreservesStoredReceiptAndInvalidatesJourneyAndVisitTokens() = runBlocking {
        val p = Pipeline()
        try {
            assertEquals(RequestResult.Accepted, p.runtime.journey.requestDestination(p.destination, JourneyKind.REGION))
            assertEquals(RequestResult.Accepted, p.runtime.demo.dispatch(DemoInput.Complete(1234, null, null)))
            val saved = p.game.state.value
            p.runtime.setScreens(demo = true, visit = true)
            val token = (p.runtime.visit.request() as VisitRequestResult.Created).request.token
            assertTrue(p.runtime.visit.accept(token))
            assertEquals(MutationResult.Success, p.changeMode(DataSource.PLEOS))
            assertEquals(saved, p.game.state.value)
            assertEquals(JourneyPhase.IDLE, p.runtime.snapshot.phase)
            assertFalse(p.runtime.visit.accept(token))
            assertEquals(VisitCancelReason.MODE_CHANGED, p.runtime.visit.state.value.cancelReason)
            assertTrue(p.runtime.journey.requestDestination(p.destination, JourneyKind.REGION) is RequestResult.Failed)
            assertTrue(p.commands.apply(0, GameAction.SelectCharacter("momo")) is MutationResult.Rejected)
            assertEquals(MutationResult.Success, p.changeMode(DataSource.DEMO))
            assertEquals(saved, p.game.state.value)
            assertEquals(GameCommandSession(2), p.commands.session.value)
            assertEquals(RequestResult.Accepted, p.runtime.journey.requestDestination(p.destination, JourneyKind.REGION))
        } finally { p.scope.cancel() }
    }

    @Test fun modeFailureDoesNotResetGameOrCommitTargetAndCanBeRetried() = runBlocking {
        val p = Pipeline()
        try {
            val before = p.store.save
            assertTrue(p.commands.changeMode(0, { p.host }, { p.host = false }, { p.mode = DataSource.PLEOS }) is MutationResult.Rejected)
            assertEquals(GameCommandPhase.MODE_FAILED, p.commands.session.value.phase)
            assertEquals(DataSource.DEMO, p.mode)
            assertEquals(before, p.store.save)
            assertTrue(p.commands.apply(1, GameAction.SelectCharacter("momo")) is MutationResult.Rejected)
            p.host = true
            assertEquals(MutationResult.Success, p.changeMode(DataSource.PLEOS))
            assertEquals(before, p.store.save)
            assertEquals(GameCommandSession(2), p.commands.session.value)
        } finally { p.scope.cancel() }
    }

    @Test fun actualRestrictionAndBackgroundStopSimulationWithoutChangingGame() = runBlocking {
        val p = Pipeline()
        try {
            p.runtime.setScreens(demo = true, visit = true)
            val request = (p.runtime.visit.request() as VisitRequestResult.Created).request
            assertTrue(p.runtime.visit.accept(request.token))
            val before = p.game.state.value
            p.host = false
            p.runtime.updateHost(GameCommandContext(InteractionGate(GateState.RESTRICTED), false, p.mode))
            assertFalse(p.runtime.runner.state.value.interactionAllowed)
            assertEquals(VisitPhase.CANCELLED, p.runtime.visit.state.value.phase)
            assertFalse(p.runtime.visit.accept(request.token))
            assertEquals(before, p.game.state.value)
            assertTrue(p.runtime.demo.dispatch(DemoInput.StartTravel) is RequestResult.Failed)
        } finally { p.scope.cancel() }
    }

    @Test fun expiredOrWithdrawnVisitNeverBecomesVisitorOrReward() = runBlocking {
        val p = Pipeline()
        try {
            p.runtime.setScreens(demo = false, visit = true)
            val before = p.game.state.value
            val first = (p.runtime.visit.request() as VisitRequestResult.Created).request
            p.clock.now = 15_000
            p.timer.actions.last()()
            assertEquals(VisitPhase.EXPIRED, p.runtime.visit.state.value.phase)
            assertFalse(p.runtime.visit.accept(first.token))
            assertTrue(p.runtime.visit.restartDemo())
            val second = (p.runtime.visit.request() as VisitRequestResult.Created).request
            assertTrue(p.runtime.visit.withdraw(second.token))
            assertFalse(p.runtime.visit.accept(second.token))
            assertEquals(before, p.game.state.value)
        } finally { p.scope.cancel() }
    }

    @Test fun cancelledResetScreenAfterCommitStillFinishesSnapshotGenerationRunnerAndVisit() = runBlocking {
        withTimeout(5000) {
            val p = Pipeline()
            try {
                assertEquals(RequestResult.Accepted, p.runtime.journey.requestDestination(p.destination, JourneyKind.REGION))
                assertEquals(RequestResult.Accepted, p.runtime.demo.dispatch(DemoInput.Complete(1234, null, null)))
                val entered = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
                p.store.afterNextCommit = { entered.complete(Unit); release.await() }
                val screen = launch(start = CoroutineStart.UNDISPATCHED) { p.runtime.runner.resetConfirmed() }
                entered.await()
                assertEquals(GameSave(), p.store.save)
                assertNotNull(p.game.state.value.pendingReceipt)
                p.host = false
                p.runtime.updateHost(GameCommandContext(InteractionGate(GateState.UNKNOWN), false))
                screen.cancel()
                release.complete(Unit); screen.join()
                assertEquals(p.store.save.toSnapshot(), p.game.state.value)
                assertEquals(GameCommandSession(1), p.commands.session.value)
                assertFalse(p.runtime.runner.state.value.resetBlocked)
                assertFalse(p.runtime.runner.state.value.busy)
                assertEquals(VisitPhase.IDLE, p.runtime.visit.state.value.phase)
                p.host = true
                p.runtime.updateHost(GameCommandContext(parked, true))
                assertTrue(p.runtime.runner.resetConfirmed())
                assertEquals(GameCommandSession(2), p.commands.session.value)
            } finally { p.scope.cancel() }
        }
    }

    private inner class Pipeline(scenarios: List<DemoScenario> = plans) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val store = Store()
        val game = BGameService(store, scope) { RegionRewards.fromJson(catalogJson) }
        var host = true
        var mode = DataSource.DEMO
        val clock = Clock()
        val timer = Timer()
        lateinit var runtime: GameSimulationRuntime
        val commands = GameCommandCoordinator(game, { game.readiness.value == GameReadiness.READY }) {
            GameCommandContext(parked, host, mode, runtime.snapshot)
        }
        val destination = catalog.regions.single { it.id == "wonju" }.let {
            DestinationSpec(it.id, it.name, it.latitude, it.longitude)
        }
        init {
            runtime = GameSimulationRuntime(scope, game, commands, catalog, scenarios, { host }, { mode }, clock, timer)
            runtime.updateHost(GameCommandContext(parked, true, mode))
            runtime.setScreens(demo = true, visit = false)
        }
        suspend fun finishSelected() {
            repeat(8) {
                if (runtime.runner.state.value.step == DemoStep.DONE) return
                val advanced = runtime.runner.advance()
                val state = runtime.runner.state.value
                assertTrue("step=${state.step}, message=${state.message}", advanced)
                yield()
            }
            fail("Runner did not finish")
        }
        suspend fun runScenario(id: String) {
            assertTrue(runtime.runner.selectScenario(id))
            if (runtime.runner.state.value.step == DemoStep.RESET) assertTrue(runtime.runner.resetConfirmed())
            finishSelected()
        }
        suspend fun changeMode(target: DataSource) = commands.changeMode(commands.session.value.generation, { host },
            { runtime.stopForModeChange(target) }, {
                mode = target
                runtime.updateHost(GameCommandContext(parked, true, mode))
            })
    }
    private class Clock : VisitClock { var now = 0L; override fun nowMs() = now }
    private class Timer : VisitScheduler {
        val actions = mutableListOf<() -> Unit>()
        override fun schedule(delayMs: Long, action: () -> Unit): VisitCancellation {
            actions += action
            return VisitCancellation {} // Deliberately allow stale wakes; token checks must still reject them.
        }
    }
    private class Store : GameSaveStore {
        var save = GameSave()
        var failNext = false
        var beforeNextUpdate: (suspend () -> Unit)? = null
        var afterNextCommit: (suspend () -> Unit)? = null
        override suspend fun read() = save
        override suspend fun update(transform: (GameSave) -> GameSave): GameSave {
            val before = beforeNextUpdate; beforeNextUpdate = null; before?.invoke()
            if (failNext) { failNext = false; throw IOException("simulated storage failure") }
            val result = transform(save).also { save = it }
            val after = afterNextCommit; afterNextCommit = null; after?.invoke()
            return result
        }
    }
}
