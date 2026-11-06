package io.kailand.app

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.kailand.app.journey.*
import io.kailand.core.contract.*
import io.kailand.feature.island.*
import java.io.File
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** A's actual confirmation -> command mutex -> B DataStore -> pending/ack path. No vehicle fake. */
class ManualJourneyIntegrationTest {
    @get:Rule val temp = TemporaryFolder()
    private val json = File("../content/catalog.json").readText(Charsets.UTF_8)
    private val catalog = decodeCatalog(json)
    private val target = catalog.regions.single { it.id == "wonju" }.let {
        DestinationSpec(it.id, it.name, it.latitude, it.longitude)
    }

    @Test fun confirmationStoresOriginalNullsThenBriefingAcknowledgesAndNextJourneyCanStart() = runBlocking {
        val p = pipeline()
        try {
            val token = p.question()
            assertNull(p.game.state.value.pendingReceipt)
            assertEquals(RequestResult.Accepted, p.manual.choose(token, ManualJourneyChoice.ARRIVED) { true })
            val receipt = checkNotNull(p.game.state.value.pendingReceipt)
            assertEquals(DataSource.USER_CONFIRMED, receipt.source)
            assertEquals(token.journeyId, receipt.journeyId)
            assertEquals(1234L, receipt.completedAtEpochMs)
            assertNull(receipt.distanceMeters); assertNull(receipt.elapsedSeconds)
            assertEquals(3, receipt.grantedItemIds.size)
            var navigated = false
            val closer = ReceiptCloseCoordinator(p.game, p.commands, 0) { p.allowed() }
            assertEquals(MutationResult.Success, closer.close(receipt) {
                p.manual.finishAcknowledged(receipt.journeyId); navigated = true
            })
            assertTrue(navigated); assertNull(p.game.state.value.pendingReceipt)
            assertEquals(ManualJourneyPhase.IDLE, p.manual.state.value.phase)
            assertEquals(RequestResult.Accepted, p.manual.begin(target, JourneyKind.REGION) { true })
            assertNotEquals(token.journeyId, p.manual.state.value.journeyId)
        } finally { p.close() }
    }

    @Test fun saveFailureAndRetryAcrossClockChangeKeepExactlyTheFirstCommand() = runBlocking {
        val p = pipeline()
        try {
            val token = p.question()
            p.store.failNext = true
            assertTrue(p.manual.choose(token, ManualJourneyChoice.ARRIVED) { true } is RequestResult.Failed)
            assertNull(p.game.state.value.pendingReceipt)
            assertTrue(p.game.state.value.ownedItemIds.isEmpty())
            val draft = p.manual.state.value.confirmedDraft
            p.clock = 999999L
            p.foreground = false; p.manual.updateHost()
            assertTrue(p.manual.retry { true } is RequestResult.Failed)
            p.foreground = true; p.manual.updateHost()
            assertEquals(RequestResult.Accepted, p.manual.retry { true })
            assertSame(draft, p.manual.state.value.confirmedDraft)
            assertEquals(1234L, p.game.state.value.pendingReceipt!!.completedAtEpochMs)
            assertEquals(1, p.store.read()!!.processedReceipts.size)
        } finally { p.close() }
    }

    @Test fun changedDestinationAndOldQuestionCannotSaveOrAlterRewards() = runBlocking {
        val p = pipeline()
        try {
            val old = p.question()
            val other = target.copy(label = "같은 지역 새 목적지", latitude = target.latitude + .01)
            assertEquals(RequestResult.Accepted, p.manual.begin(other, JourneyKind.REGION) { true })
            assertTrue(p.manual.choose(old, ManualJourneyChoice.ARRIVED) { true } is RequestResult.Failed)
            assertNull(p.game.state.value.pendingReceipt)
            assertTrue(p.manual.requestReview { true })
            val fresh = p.manual.state.value.confirmation!!.token
            assertEquals(old.revision + 1, fresh.revision)
            assertEquals(RequestResult.Accepted, p.manual.choose(fresh, ManualJourneyChoice.ARRIVED) { true })
            assertEquals(other, p.manual.snapshot.destination)
        } finally { p.close() }
    }

    @Test fun dismissContinueChangeAndCancelNeverWriteADepartureAsArrival() = runBlocking {
        for (choice in listOf(ManualJourneyChoice.CONTINUE, ManualJourneyChoice.CHANGE_DESTINATION, ManualJourneyChoice.CANCEL)) {
            val p = pipeline()
            try {
                assertEquals(RequestResult.Accepted, p.manual.choose(p.question(), choice) { true })
                assertNull(p.store.read()); assertNull(p.game.state.value.pendingReceipt)
            } finally { p.close() }
        }
    }

    @Test fun unknownGateBackgroundAndReplacedOwnerRejectEvenPreviouslyVisibleQuestion() = runBlocking {
        val p = pipeline()
        try {
            for (block in listOf<() -> Unit>(
                { p.gate = InteractionGate(GateState.UNKNOWN) },
                { p.gate = InteractionGate(GateState.RESTRICTED) },
                { p.foreground = false }, { p.ownerCurrent = false },
            )) {
                p.gate = InteractionGate(GateState.PARKED_ALLOWED); p.foreground = true; p.ownerCurrent = true
                p.manual.updateHost()
                val token = p.question()
                block(); p.manual.updateHost()
                assertTrue(p.manual.choose(token, ManualJourneyChoice.ARRIVED) { true } is RequestResult.Failed)
                assertNull(p.store.read())
            }
        } finally { p.close() }
    }

    @Test fun leavingScreenReissuesQuestionAndRejectsItsOldCallback() = runBlocking {
        val p = pipeline()
        try {
            val old = p.question()
            p.manual.leaveScreen()
            assertTrue(p.manual.choose(old, ManualJourneyChoice.ARRIVED) { false } is RequestResult.Failed)
            p.manual.updateHost()
            assertNotEquals(old, p.manual.state.value.confirmation?.token)
            assertTrue(p.manual.choose(old, ManualJourneyChoice.ARRIVED) { true } is RequestResult.Failed)
            assertNull(p.store.read())
        } finally { p.close() }
    }

    @Test fun queuedSaveRechecksScreenOwnerAndGateAfterAcquiringCommandLock() = runBlocking {
        val p = pipeline()
        try {
            val token = p.question()
            val entered = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
            p.store.beforeUpdate = { entered.complete(Unit); release.await() }
            val first = async(start = CoroutineStart.UNDISPATCHED) { p.commands.apply(0, GameAction.SelectCharacter("momo")) }
            entered.await()
            var screenCurrent = true
            val queued = async(start = CoroutineStart.UNDISPATCHED) {
                p.manual.choose(token, ManualJourneyChoice.ARRIVED) { screenCurrent }
            }
            screenCurrent = false; p.ownerCurrent = false; p.gate = InteractionGate(GateState.UNKNOWN)
            release.complete(Unit)
            assertEquals(MutationResult.Success, first.await())
            assertTrue(queued.await() is RequestResult.Failed)
            assertNull(p.game.state.value.pendingReceipt)
            assertTrue(p.store.read()!!.processedReceipts.isEmpty())
        } finally { p.close() }
    }

    @Test fun modeChangeAndSubsequentResetRejectOldDraftAndDelayedRetry() = runBlocking {
        val p = pipeline()
        try {
            p.store.failNext = true
            assertTrue(p.manual.choose(p.question(), ManualJourneyChoice.ARRIVED) { true } is RequestResult.Failed)
            assertEquals(MutationResult.Success, p.commands.changeMode(0, { true },
                stopOldWork = { p.manual.invalidate() }, commitMode = { p.mode = DataSource.DEMO }))
            assertEquals(MutationResult.Success, p.commands.resetDemo(1) {})
            assertTrue(p.manual.retry { true } is RequestResult.Failed)
            assertTrue(p.game.state.value.ownedItemIds.isEmpty())
            assertNull(p.game.state.value.pendingReceipt)
            assertEquals(2, p.commands.session.value.generation)
        } finally { p.close() }
    }

    @Test fun failedAckKeepsPendingAndServiceRecreationRestoresOriginalWithoutAnActiveJourney() = runBlocking {
        val p = pipeline()
        val file = p.file
        try {
            p.manual.choose(p.question(), ManualJourneyChoice.ARRIVED) { true }
            val receipt = p.game.state.value.pendingReceipt!!
            p.store.failNext = true
            var routed = false
            val closer = ReceiptCloseCoordinator(p.game, p.commands, 0) { p.allowed() }
            assertTrue(closer.close(receipt) { routed = true } is MutationResult.StorageFailure)
            assertFalse(routed); assertEquals(receipt, p.game.state.value.pendingReceipt)
            p.close()
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            try {
                val backend = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
                val game = BGameService(PreferencesGameSaveStore(backend), scope) { RegionRewards.fromJson(json) }
                assertEquals(GameReadiness.READY, game.readiness.first { it != GameReadiness.LOADING })
                assertEquals(receipt, game.state.value.pendingReceipt)
                val commands = GameCommandCoordinator(game, { true }) {
                    GameCommandContext(InteractionGate(GateState.PARKED_ALLOWED), true, DataSource.USER_CONFIRMED)
                }
                assertEquals(MutationResult.Success, ReceiptCloseCoordinator(game, commands, 0) { true }.close(receipt) { routed = true })
                assertTrue(routed); assertNull(game.state.value.pendingReceipt)
                assertEquals(3, game.state.value.ownedItemIds.size)
            } finally { scope.coroutineContext[Job]!!.cancelAndJoin() }
        } finally { p.close() }
    }

    @Test fun duplicateClickAndRetryAfterSuccessCannotReopenAcknowledgedReceipt() = runBlocking {
        val p = pipeline()
        try {
            val token = p.question()
            assertEquals(RequestResult.Accepted, p.manual.choose(token, ManualJourneyChoice.ARRIVED) { true })
            val receipt = p.game.state.value.pendingReceipt!!
            assertTrue(p.manual.choose(token, ManualJourneyChoice.ARRIVED) { true } is RequestResult.Failed)
            val closer = ReceiptCloseCoordinator(p.game, p.commands, 0) { true }
            assertEquals(MutationResult.Success, closer.close(receipt) {})
            val saved = p.store.read()
            assertTrue(p.manual.retry { true } is RequestResult.Failed)
            assertEquals(saved, p.store.read()); assertNull(p.game.state.value.pendingReceipt)
            assertEquals(1, saved!!.processedReceipts.size)
        } finally { p.close() }
    }

    @Test fun cancellationAfterAdmissionStillPublishesCommittedReceiptForLaterBriefing() = runBlocking {
        val p = pipeline()
        try {
            val token = p.question()
            val entered = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
            p.store.beforeUpdate = { entered.complete(Unit); release.await() }
            val click = launch(start = CoroutineStart.UNDISPATCHED) {
                p.manual.choose(token, ManualJourneyChoice.ARRIVED) { true }
            }
            entered.await()
            click.cancel(); p.foreground = false; p.manual.updateHost()
            release.complete(Unit); click.join()
            withTimeout(3000) { p.manual.busy.first { !it } }
            val receipt = checkNotNull(p.game.state.value.pendingReceipt)
            assertEquals(token.journeyId, receipt.journeyId)
            assertEquals(DataSource.USER_CONFIRMED, receipt.source)
            assertEquals(1, p.store.read()!!.processedReceipts.size)
        } finally { p.close() }
    }

    @Test fun olderAcknowledgementAndReplayCannotReplaceNewPendingAfterFileRecreation() = runBlocking {
        withTimeout(15_000) {
            val p = pipeline()
            try {
                val oldToken = p.question()
                assertEquals(RequestResult.Accepted, p.manual.choose(oldToken, ManualJourneyChoice.ARRIVED) { true })
                val oldReceipt = checkNotNull(p.game.state.value.pendingReceipt)
                val closer = ReceiptCloseCoordinator(p.game, p.commands, 0) { p.allowed() }
                assertEquals(MutationResult.Success, closer.close(oldReceipt) {
                    p.manual.finishAcknowledged(oldReceipt.journeyId)
                })
                p.clock += 1000
                val freshToken = p.question()
                assertEquals(RequestResult.Accepted, p.manual.choose(freshToken, ManualJourneyChoice.ARRIVED) { true })
                val freshReceipt = checkNotNull(p.game.state.value.pendingReceipt)
                assertNotEquals(oldReceipt.journeyId, freshReceipt.journeyId)
                assertTrue(freshReceipt.grantedItemIds.isEmpty())
                val before = checkNotNull(p.store.read())
                var routes = 0
                assertTrue(closer.close(oldReceipt) { routes++ } is MutationResult.Rejected)
                assertEquals(0, routes)
                assertEquals(before, p.store.read())
                p.close()
                withRestoredGame(p.file) { game, store ->
                    assertEquals(freshReceipt, game.state.value.pendingReceipt)
                    val oldCommand = ArrivalCommand(oldReceipt.journeyId, DataSource.USER_CONFIRMED,
                        JourneyKind.REGION, oldReceipt.regionId, oldToken.revision,
                        ArrivalEvidence.USER_CONFIRMED_DESTINATION, oldReceipt.completedAtEpochMs)
                    // B's public duplicate boundary must preserve the newer pending result too.
                    assertEquals(ArrivalResult.AlreadyApplied(oldReceipt), game.completeArrival(oldCommand))
                    assertEquals(before, store.read())
                    assertEquals(2, store.read()!!.processedReceipts.size)
                    assertEquals(3, game.state.value.ownedItemIds.size)
                    val commands = GameCommandCoordinator(game, { true }) {
                        GameCommandContext(InteractionGate(GateState.PARKED_ALLOWED), true, DataSource.USER_CONFIRMED)
                    }
                    assertEquals(MutationResult.Success, ReceiptCloseCoordinator(game, commands, 0) { true }
                        .close(freshReceipt) { routes++ })
                    assertEquals(1, routes)
                    assertEquals(before.copy(pendingReceiptId = null), store.read())
                }
            } finally { p.close() }
        }
    }

    @Test fun failedDailySaveRetriedAfterKstMidnightUsesOriginalDateAndRestoresBothDays() = runBlocking {
        withTimeout(15_000) {
            val p = pipeline()
            try {
                p.clock = Instant.parse("2026-11-06T14:59:59Z").toEpochMilli()
                val token = p.question(JourneyKind.DAILY)
                p.store.failNext = true
                assertTrue(p.manual.choose(token, ManualJourneyChoice.ARRIVED) { true } is RequestResult.Failed)
                assertNull(p.store.read())
                p.clock = Instant.parse("2026-11-06T15:00:01Z").toEpochMilli()
                assertEquals(RequestResult.Accepted, p.manual.retry { true })
                val original = checkNotNull(p.game.state.value.pendingReceipt)
                assertEquals(Instant.parse("2026-11-06T14:59:59Z").toEpochMilli(), original.completedAtEpochMs)
                assertEquals("2026-11-06", p.store.read()!!.lastDailyGrantDate)
                assertEquals(1, p.game.state.value.dailyGrowth)
                assertNull(original.elapsedSeconds); assertNull(original.distanceMeters)
                val closer = ReceiptCloseCoordinator(p.game, p.commands, 0) { p.allowed() }
                assertEquals(MutationResult.Success, closer.close(original) {
                    p.manual.finishAcknowledged(original.journeyId)
                })
                val next = p.question(JourneyKind.DAILY)
                assertEquals(RequestResult.Accepted, p.manual.choose(next, ManualJourneyChoice.ARRIVED) { true })
                val second = checkNotNull(p.game.state.value.pendingReceipt)
                assertEquals(2, p.game.state.value.dailyGrowth)
                assertEquals("2026-11-07", p.store.read()!!.lastDailyGrantDate)
                assertEquals(MutationResult.Success, closer.close(second) {
                    p.manual.finishAcknowledged(second.journeyId)
                })
                // A different confirmation on that same KST day cannot grant daily growth again.
                val sameDay = p.question(JourneyKind.DAILY)
                assertEquals(RequestResult.Accepted, p.manual.choose(sameDay, ManualJourneyChoice.ARRIVED) { true })
                assertEquals(2, p.game.state.value.dailyGrowth)
                val expected = p.store.read()
                p.close()
                withRestoredGame(p.file) { game, store ->
                    assertEquals(expected, store.read())
                    assertEquals(3, store.read()!!.processedReceipts.size)
                    assertEquals(2, game.state.value.dailyGrowth)
                    assertTrue(game.state.value.ownedItemIds.isEmpty())
                    assertEquals(sameDay.journeyId, game.state.value.pendingReceipt!!.journeyId)
                }
            } finally { p.close() }
        }
    }

    @Test fun modeChangeWaitsForAdmittedManualSaveThenResetPersistsWithoutRevivingOldWork() = runBlocking {
        withTimeout(15_000) {
            val p = pipeline()
            try {
                val token = p.question()
                val entered = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
                p.store.beforeUpdate = { entered.complete(Unit); release.await() }
                val saving = async(start = CoroutineStart.UNDISPATCHED) {
                    p.manual.choose(token, ManualJourneyChoice.ARRIVED) { true }
                }
                entered.await()
                val changing = async(start = CoroutineStart.UNDISPATCHED) {
                    p.commands.changeMode(0, { true }, stopOldWork = { p.manual.invalidate() },
                        commitMode = { p.mode = DataSource.DEMO })
                }
                assertFalse(changing.isCompleted)
                assertEquals(DataSource.USER_CONFIRMED, p.mode)
                release.complete(Unit)
                assertEquals(RequestResult.Accepted, saving.await())
                assertEquals(MutationResult.Success, changing.await())
                assertEquals(token.journeyId, p.game.state.value.pendingReceipt!!.journeyId)
                p.store.beforeUpdate = {}
                assertEquals(MutationResult.Success, p.commands.resetDemo(1) {})
                assertEquals(2L, p.commands.session.value.generation)
                assertTrue(p.manual.retry { true } is RequestResult.Failed)
                assertTrue(p.manual.choose(token, ManualJourneyChoice.ARRIVED) { true } is RequestResult.Failed)
                val expected = checkNotNull(p.store.read())
                assertTrue(expected.processedReceipts.isEmpty())
                assertTrue(p.game.state.value.ownedItemIds.isEmpty())
                assertNull(p.game.state.value.pendingReceipt)
                p.close()
                withRestoredGame(p.file) { game, store ->
                    assertEquals(expected, store.read())
                    assertTrue(game.state.value.ownedItemIds.isEmpty())
                    assertNull(game.state.value.pendingReceipt)
                }
            } finally { p.close() }
        }
    }

    private suspend fun <T> withRestoredGame(
        file: File, block: suspend (BGameService, PreferencesGameSaveStore) -> T,
    ): T {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val backend = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
            val store = PreferencesGameSaveStore(backend)
            val game = BGameService(store, scope) { RegionRewards.fromJson(json) }
            assertEquals(GameReadiness.READY, game.readiness.first { it != GameReadiness.LOADING })
            return block(game, store)
        } finally { scope.coroutineContext[Job]!!.cancelAndJoin() }
    }

    private suspend fun pipeline(): Pipeline = Pipeline().also {
        assertEquals(GameReadiness.READY, it.game.readiness.first { value -> value != GameReadiness.LOADING })
        it.manual.updateHost()
    }

    private inner class Pipeline {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val file = File(temp.newFolder(), "game.preferences_pb")
        val backend = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        val store = FailingStore(PreferencesGameSaveStore(backend))
        val game = BGameService(store, scope) { RegionRewards.fromJson(json) }
        var gate = InteractionGate(GateState.PARKED_ALLOWED)
        var foreground = true
        var ownerCurrent = true
        var mode = DataSource.USER_CONFIRMED
        var clock = 1234L
        val commands: GameCommandCoordinator = GameCommandCoordinator(game, { game.readiness.value == GameReadiness.READY }) {
            GameCommandContext(gate, foreground, mode, manual.snapshot)
        }
        val manual: ManualJourneyRuntime = ManualJourneyRuntime(scope, commands, 0, catalog.regions.map { it.id }.toSet(),
            { allowed() && mode == DataSource.USER_CONFIRMED }, now = { clock })
        fun allowed() = gate.canInteract && foreground && ownerCurrent
        fun question(kind: JourneyKind = JourneyKind.REGION): ManualConfirmationToken {
            assertEquals(RequestResult.Accepted, manual.begin(target, kind) { true })
            assertTrue(manual.requestReview { true })
            return manual.state.value.confirmation!!.token
        }
        suspend fun close() { scope.coroutineContext[Job]!!.cancelAndJoin() }
    }

    private class FailingStore(private val delegate: GameSaveStore) : GameSaveStore {
        var failNext = false
        var beforeUpdate: suspend () -> Unit = {}
        override suspend fun read() = delegate.read()
        override suspend fun update(transform: (GameSave) -> GameSave): GameSave {
            beforeUpdate()
            if (failNext) { failNext = false; throw IOException("Injected commit failure") }
            return delegate.update(transform)
        }
    }
}
