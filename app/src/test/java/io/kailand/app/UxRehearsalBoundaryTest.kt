package io.kailand.app

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.kailand.app.journey.*
import io.kailand.core.contract.*
import io.kailand.feature.demo.*
import io.kailand.feature.island.*
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

/** Actual B file-backed transaction with the debug-only failure decorator and manual runtime. */
class UxRehearsalBoundaryTest {
    private val json = File("../content/catalog.json").readText(Charsets.UTF_8)
    private val catalog = decodeCatalog(json)
    private val wonju = catalog.regions.single { it.id == "wonju" }.let {
        DestinationSpec(it.id, it.name, it.latitude, it.longitude)
    }

    @Test fun precommitFailureLeavesFileUnchangedAndManualRetryPreservesEntireOriginal() = runBlocking {
        val p = Pipeline()
        try {
            p.ready()
            p.commands.apply(0, GameAction.SelectCharacter("momo"))
            val before = p.game.state.value
            val diskBefore = p.file.readBytes()
            p.probe.failNextArrival = true
            p.begin()
            assertTrue(p.arrive() is RequestResult.Failed)
            assertEquals(before, p.game.state.value)
            assertEquals(before, p.probe.confirmedFailure)
            assertArrayEquals(diskBefore, p.file.readBytes())
            assertTrue(p.probe.committed.processedReceipts.isEmpty())
            assertEquals(RequestResult.Accepted, p.runtime.retry { p.allowed })
            assertEquals(2, p.attempts.size)
            assertEquals(p.attempts[0], p.attempts[1])
            assertEquals(1, p.probe.committed.processedReceipts.size)
            val receipt = checkNotNull(p.game.state.value.pendingReceipt)
            assertEquals(p.attempts[0].journeyId, receipt.journeyId)
            assertEquals(p.attempts[0].completedAtEpochMs, receipt.completedAtEpochMs)
            assertEquals(DataSource.USER_CONFIRMED, receipt.source)
            assertNull(receipt.elapsedSeconds); assertNull(receipt.distanceMeters)
            val request = UxDemoRequest(UxDemoPreset.SAVE_RETRY, "save-failure-request")
            val evidence = UxDemoEvidence(request, "file-backed-retry", true, DataSource.USER_CONFIRMED,
                before, 0, false, before, p.game.state.value, 1, receipt,
                saveFailure = UxDemoSaveFailure(p.attempts[0], p.attempts[1], checkNotNull(p.probe.confirmedFailure)),
                finished = true)
            assertEquals(UxDemoVerdict.PASS, checkUxDemoResult(request, evidence, catalog).verdict)
        } finally { p.close() }
    }

    @Test fun staleGateRejectsConfirmationWithoutConsumingSingleFailure() = runBlocking {
        val p = Pipeline()
        try {
            p.ready(); p.begin()
            p.probe.failNextArrival = true
            p.allowed = false
            assertTrue(p.arrive() is RequestResult.Failed)
            assertTrue(p.probe.failNextArrival)
            assertNull(p.probe.confirmedFailure)
            assertTrue(p.attempts.isEmpty())
            p.allowed = true
            p.runtime.updateHost()
            p.runtime.requestReview { p.allowed }
            assertTrue(p.arrive() is RequestResult.Failed)
            assertFalse(p.probe.failNextArrival)
            assertNotNull(p.probe.confirmedFailure)
            assertEquals(RequestResult.Accepted, p.runtime.retry { p.allowed })
        } finally { p.close() }
    }

    @Test fun characterWriteDoesNotConsumeArrivalFaultAndSecondStoreRemainsUntouched() = runBlocking {
        val p = Pipeline(); val other = Pipeline()
        try {
            p.ready(); other.ready()
            p.probe.failNextArrival = true
            assertEquals(MutationResult.Success, p.commands.apply(0, GameAction.SelectCharacter("lulu")))
            assertTrue(p.probe.failNextArrival)
            p.begin(); p.arrive(); p.runtime.retry { true }
            assertEquals("tori", other.game.state.value.avatar.characterId)
            assertTrue(other.probe.committed.processedReceipts.isEmpty())
            assertFalse(other.file.exists())
        } finally { p.close(); other.close() }
    }

    @Test fun actualRegionGrowthAndVisitHistoryPassCChecker() = runBlocking {
        val p = Pipeline()
        try {
            p.ready()
            val before = p.game.state.value
            p.begin(); p.arrive()
            val after = p.game.state.value
            val receipt = checkNotNull(after.pendingReceipt)
            val request = UxDemoRequest(UxDemoPreset.NEW_REGION, "control-request")
            val evidence = UxDemoEvidence(request, "separate-file-test", true, DataSource.USER_CONFIRMED,
                before, 0, false, before, after, 1, receipt, finished = true)
            assertEquals(UxDemoVerdict.PASS, checkUxDemoPreparation(request, evidence, catalog).verdict)
            assertEquals(1, after.dailyGrowth)
            val history = p.game.regionVisitHistory.value as RegionVisitHistory.Available
            assertEquals(1, history.regions.single { it.regionId == "wonju" }.visitCount)
            assertEquals(receipt.completedAtEpochMs, history.regions.single().lastVisitedAtEpochMs)
            assertEquals(UxDemoVerdict.PASS, checkUxDemoResult(request, evidence, catalog).verdict)
        } finally { p.close() }
    }

    @Test fun sameKstDayRevisitThroughActualManualFlowPassesCCheckerWithoutSecondGrowth() = runBlocking {
        val p = Pipeline()
        try {
            p.ready()
            val seed = ArrivalCommand("00000000-0000-4000-8000-000000000001", DataSource.DEMO, JourneyKind.REGION,
                "wonju", 0, ArrivalEvidence.DEMO_FINAL_DESTINATION, 1794279600000)
            assertTrue(p.game.completeArrival(seed) is ArrivalResult.Applied)
            assertEquals(MutationResult.Success, p.game.acknowledgeReceipt(seed.journeyId))
            val before = p.game.state.value
            assertEquals(1, before.dailyGrowth)
            p.begin()
            assertEquals(RequestResult.Accepted, p.arrive())
            val after = p.game.state.value
            val receipt = checkNotNull(after.pendingReceipt)
            val request = UxDemoRequest(UxDemoPreset.REVISIT, "same-day-revisit")
            val history = p.game.regionVisitHistory.value as RegionVisitHistory.Available
            assertEquals(2, history.regions.single().visitCount)
            assertEquals(before.dailyGrowth, after.dailyGrowth)
            assertTrue(receipt.grantedItemIds.isEmpty())
            val evidence = UxDemoEvidence(request, "file-backed-revisit", true, DataSource.USER_CONFIRMED,
                before, 1, true, before, after, 2, receipt, finished = true)
            assertEquals(UxDemoVerdict.PASS, checkUxDemoResult(request, evidence, catalog).verdict)
        } finally { p.close() }
    }

    private inner class Pipeline {
        val dir = Files.createTempDirectory("memoride-c03-test").toFile()
        val file = File(dir, "game.preferences_pb")
        val diskScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val probe = RehearsalSaveProbe(PreferencesGameSaveStore(
            PreferenceDataStoreFactory.create(scope = diskScope) { file }))
        val game = BGameService(probe, scope) { RegionRewards.fromJson(json) }
        var allowed = true
        val attempts = mutableListOf<ArrivalCommand>()
        lateinit var runtime: ManualJourneyRuntime
        val commands = GameCommandCoordinator(game, { game.readiness.value == GameReadiness.READY }) {
            GameCommandContext(InteractionGate(if (allowed) GateState.PARKED_ALLOWED else GateState.UNKNOWN),
                allowed, DataSource.USER_CONFIRMED, if (::runtime.isInitialized) runtime.snapshot else null)
        }
        suspend fun ready() {
            assertEquals(GameReadiness.READY, game.readiness.first { it != GameReadiness.LOADING })
            runtime = ManualJourneyRuntime(scope, commands, 0, setOf("wonju"), { allowed },
                now = { 1794279600000 }, onAttempt = { attempts += it })
            runtime.updateHost()
        }
        fun begin() {
            assertEquals(RequestResult.Accepted, runtime.begin(wonju, JourneyKind.REGION) { allowed })
            assertTrue(runtime.requestReview { allowed })
        }
        suspend fun arrive(): RequestResult {
            val token = runtime.state.value.confirmation?.token
                ?: return RequestResult.Failed("No current question")
            return runtime.choose(token, ManualJourneyChoice.ARRIVED) { allowed }
        }
        suspend fun close() {
            scope.cancel(); diskScope.coroutineContext[Job]?.cancelAndJoin(); dir.deleteRecursively()
        }
    }
}
