package io.kailand.feature.visit

import io.kailand.core.contract.AvatarPreview
import io.kailand.core.contract.DataSource
import io.kailand.core.contract.GateState
import io.kailand.core.contract.InteractionGate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test

class VisitControllerTest {
    private class FakeClock : VisitClock {
        @Volatile var monotonic = 0L
        var epoch = 1_800_000_000_000L
        override fun nowMs() = monotonic
    }

    private class FakeScheduler(private val clock: FakeClock) : VisitScheduler {
        class Entry(val due: Long, val action: () -> Unit) {
            var cancelled = false
            var fired = false
        }
        val entries = mutableListOf<Entry>()
        override fun schedule(delayMs: Long, action: () -> Unit): VisitCancellation {
            assertTrue(delayMs > 0)
            val entry = Entry(clock.nowMs() + delayMs, action)
            entries.add(entry)
            return VisitCancellation { entry.cancelled = true }
        }
        val pending get() = entries.count { !it.cancelled && !it.fired }
        fun fire(entry: Entry, evenIfCancelled: Boolean = false) {
            if (!entry.cancelled || evenIfCancelled) {
                entry.fired = true
                entry.action()
            }
        }
        fun runDue() {
            while (true) {
                val next = entries.firstOrNull { !it.cancelled && !it.fired && it.due <= clock.nowMs() } ?: return
                fire(next)
            }
        }
    }

    private class Fixture {
        val clock = FakeClock()
        val scheduler = FakeScheduler(clock)
        val controller = VisitController(clock, scheduler)
        val allowed = VisitContext(InteractionGate(GateState.PARKED_ALLOWED), true, true)
        init { controller.updateContext(allowed) }
        val state get() = controller.state.value
        fun request(): VisitToken = (controller.request() as VisitRequestResult.Created).request.token
        fun noVisitor() = assertFalse(state.canShowVisitor(allowed.gate))
    }

    @Test fun defaultUnknownAndInactiveContextsCannotCreateRequests() {
        val clock = FakeClock()
        val scheduler = FakeScheduler(clock)
        val controller = VisitController(clock, scheduler)
        assertTrue(controller.request() is VisitRequestResult.Rejected)
        val valid = VisitContext(InteractionGate(GateState.PARKED_ALLOWED), true, true)
        for (context in listOf(valid.copy(foreground = false), valid.copy(screenActive = false),
            valid.copy(gate = InteractionGate(GateState.UNKNOWN)),
            valid.copy(gate = InteractionGate(GateState.RESTRICTED)))) {
            controller.updateContext(context)
            assertTrue(controller.request() is VisitRequestResult.Rejected)
            assertFalse(controller.restartDemo())
        }
        assertEquals(0, scheduler.pending)
        assertNull(controller.state.value.request)
    }

    @Test fun requestDoesNotGrantConsentOrShowAvatar() {
        val f = Fixture()
        val token = f.request()
        assertEquals(VisitPhase.REQUESTED, f.state.phase)
        assertEquals(15_000L, f.state.remainingResponseMs)
        assertEquals(VisitController.PURPOSE, f.state.request!!.purpose)
        assertEquals(VisitController.MESSAGE_ID, f.state.request!!.presetMessageId)
        assertEquals(1, f.scheduler.pending)
        assertEquals(token, f.state.request!!.token)
        f.noVisitor()
        assertFalse(f.controller.close(token))
    }

    @Test fun rejectionAndWithdrawalRejectLateAcceptanceAndRetainCooldown() {
        for (reject in listOf(true, false)) {
            val f = Fixture()
            val token = f.request()
            val timeout = f.scheduler.entries.last()
            assertTrue(if (reject) f.controller.reject(token) else f.controller.withdraw(token))
            assertEquals(if (reject) VisitPhase.REJECTED else VisitPhase.WITHDRAWN, f.state.phase)
            assertFalse(f.controller.accept(token))
            f.scheduler.fire(timeout, true)
            assertEquals(if (reject) VisitPhase.REJECTED else VisitPhase.WITHDRAWN, f.state.phase)
            assertEquals(0, f.scheduler.pending)
            assertEquals(30_000L, (f.controller.request() as VisitRequestResult.Rejected).retryAfterMs)
            f.noVisitor()
        }
    }

    @Test fun consentAt14999StartsOneVisitAndCancelsResponseTimer() {
        val f = Fixture()
        val token = f.request()
        f.clock.monotonic = 14_999
        assertTrue(f.controller.accept(token))
        assertEquals(VisitPhase.VISITING, f.state.phase)
        assertTrue(f.state.canShowVisitor(f.allowed.gate))
        assertNull(f.state.remainingResponseMs)
        assertFalse(f.controller.accept(token))
        assertFalse(f.controller.reject(token))
        assertEquals(0, f.scheduler.pending)
    }

    @Test fun exactDeadlineExpiresEvenWhenSchedulerHasNotRun() {
        for (event in listOf("accept", "reject", "withdraw")) {
            val f = Fixture()
            val token = f.request()
            f.clock.monotonic = 15_000
            val handled = when (event) {
                "accept" -> f.controller.accept(token)
                "reject" -> f.controller.reject(token)
                else -> f.controller.withdraw(token)
            }
            assertFalse(handled)
            assertEquals(VisitPhase.EXPIRED, f.state.phase)
            assertEquals(0, f.scheduler.pending)
            f.noVisitor()
        }
    }

    @Test fun scheduledExpirationRejectsLateConsent() {
        val f = Fixture()
        val token = f.request()
        f.clock.monotonic = 15_001
        f.scheduler.runDue()
        assertEquals(VisitPhase.EXPIRED, f.state.phase)
        assertFalse(f.controller.accept(token))
        assertEquals(0, f.scheduler.pending)
        f.noVisitor()
    }

    @Test fun cooldownRejects29999AndAccepts30000WithoutExtendingOnFailure() {
        val f = Fixture()
        val old = f.request()
        f.controller.reject(old)
        f.clock.monotonic = 29_999
        val denied = f.controller.request() as VisitRequestResult.Rejected
        assertEquals(1L, denied.retryAfterMs)
        assertEquals(old, f.state.request!!.token)
        f.clock.monotonic = 30_000
        val next = f.request()
        assertNotEquals(old.requestId, next.requestId)
        assertEquals(45_000L, f.state.request!!.expiresAtMonotonicMs)
        assertEquals(1, f.scheduler.pending)
    }

    @Test fun activeVisitBlocksNewRequestEvenAfterCooldown() {
        val f = Fixture()
        val token = f.request()
        f.controller.accept(token)
        f.clock.monotonic = 30_000
        assertTrue(f.controller.request() is VisitRequestResult.Rejected)
        assertEquals(token, f.state.request!!.token)
        assertEquals(VisitPhase.VISITING, f.state.phase)
    }

    @Test fun simultaneousRequestsCreateOnlyOneSessionAndTimer() {
        val f = Fixture()
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(2)
        try {
            val futures = (1..2).map {
                pool.submit<VisitRequestResult> {
                    assertTrue(start.await(5, TimeUnit.SECONDS))
                    f.controller.request()
                }
            }
            start.countDown()
            val results = futures.map { it.get(5, TimeUnit.SECONDS) }
            assertEquals(1, results.count { it is VisitRequestResult.Created })
            assertEquals(1, results.count { it is VisitRequestResult.Rejected })
            assertEquals(1, f.scheduler.pending)
            assertEquals(VisitPhase.REQUESTED, f.state.phase)
        } finally { pool.shutdownNow() }
    }

    @Test fun staleEventsDoNotAlterNewRequestOrItsTimer() {
        val f = Fixture()
        val old = f.request()
        val oldTimeout = f.scheduler.entries.last()
        f.controller.reject(old)
        f.clock.monotonic = 30_000
        val current = f.request()
        val before = f.state
        assertFalse(f.controller.accept(old))
        assertFalse(f.controller.reject(old))
        assertFalse(f.controller.withdraw(old))
        assertFalse(f.controller.close(old))
        f.scheduler.fire(oldTimeout, true)
        assertEquals(before, f.state)
        assertEquals(current, f.state.request!!.token)
        assertEquals(1, f.scheduler.pending)
    }

    @Test fun restrictionCancelsPendingAndAcceptedSessionsWithoutAutoResume() {
        for (gate in listOf(GateState.UNKNOWN, GateState.RESTRICTED)) for (accepted in listOf(false, true)) {
            val f = Fixture()
            val token = f.request()
            val timeout = f.scheduler.entries.last()
            if (accepted) f.controller.accept(token)
            f.controller.updateContext(f.allowed.copy(gate = InteractionGate(gate)))
            assertEquals(VisitPhase.CANCELLED, f.state.phase)
            assertFalse(f.state.interactionAllowed)
            assertEquals(if (gate == GateState.UNKNOWN) VisitCancelReason.UNKNOWN else VisitCancelReason.RESTRICTED, f.state.cancelReason)
            assertFalse(f.controller.accept(token))
            f.controller.updateContext(f.allowed)
            f.scheduler.fire(timeout, true)
            assertEquals(VisitPhase.CANCELLED, f.state.phase)
            assertTrue(f.controller.request() is VisitRequestResult.Rejected)
            assertEquals(0, f.scheduler.pending)
            f.noVisitor()
        }
    }

    @Test fun backgroundScreenLeaveAndModeSwitchCancelAndKeepCooldown() {
        for (event in listOf("background", "leave", "mode")) for (accepted in listOf(false, true)) {
            val f = Fixture()
            val token = f.request()
            if (accepted) f.controller.accept(token)
            when (event) {
                "background" -> f.controller.updateContext(f.allowed.copy(foreground = false))
                "leave" -> f.controller.leaveScreen()
                else -> f.controller.updateContext(f.allowed.copy(mode = DataSource.PLEOS))
            }
            assertEquals(VisitPhase.CANCELLED, f.state.phase)
            assertEquals(when (event) {
                "background" -> VisitCancelReason.BACKGROUND
                "leave" -> VisitCancelReason.SCREEN_LEFT
                else -> VisitCancelReason.MODE_CHANGED
            }, f.state.cancelReason)
            assertEquals(0, f.scheduler.pending)
            f.controller.updateContext(f.allowed)
            assertFalse(f.controller.accept(token))
            assertTrue(f.controller.request() is VisitRequestResult.Rejected)
            f.noVisitor()
        }
    }

    @Test fun presenterRestartInvalidatesPreviousGenerationAndClearsOnlyLocalCooldown() {
        val f = Fixture()
        val old = f.request()
        val timeout = f.scheduler.entries.last()
        assertTrue(f.controller.restartDemo())
        val current = f.request()
        assertTrue(current.generation > old.generation)
        assertNotEquals(old.requestId, current.requestId)
        assertFalse(f.controller.accept(old))
        f.scheduler.fire(timeout, true)
        assertEquals(current, f.state.request!!.token)
        assertEquals(1, f.scheduler.pending)
    }

    @Test fun resetFailureRemainsBlockedAndOnlyMatchingSuccessfulAckReopens() {
        val f = Fixture()
        val token = f.request()
        val timeout = f.scheduler.entries.last()
        val reset = f.controller.beginReset()
        assertEquals(VisitCancelReason.RESET, f.state.cancelReason)
        assertFalse(f.state.interactionAllowed)
        assertFalse(f.controller.accept(token))
        assertTrue(f.controller.request() is VisitRequestResult.Rejected)
        assertFalse(f.controller.restartDemo())
        assertFalse(f.controller.finishReset(reset, false))
        f.controller.updateContext(f.allowed)
        assertFalse(f.state.interactionAllowed)
        assertFalse(f.controller.finishReset(reset - 1, true))
        f.scheduler.fire(timeout, true)
        assertEquals(0, f.scheduler.pending)
        assertTrue(f.controller.finishReset(reset, true))
        assertTrue(f.state.interactionAllowed)
        assertEquals(VisitPhase.IDLE, f.state.phase)
        assertNotEquals(token, f.request())
    }

    @Test fun earlierResetAckCannotReopenNewResetOrRestrictedGate() {
        val f = Fixture()
        val old = f.controller.beginReset()
        val current = f.controller.beginReset()
        assertFalse(f.controller.finishReset(old, true))
        f.controller.updateContext(f.allowed.copy(gate = InteractionGate(GateState.UNKNOWN)))
        assertTrue(f.controller.finishReset(current, true))
        assertFalse(f.state.interactionAllowed)
        assertTrue(f.controller.request() is VisitRequestResult.Rejected)
    }

    @Test fun successfulResetStillCompletesWhenModeChangesWhilePending() {
        val f = Fixture()
        val oldVisit = f.request()
        val oldTimeout = f.scheduler.entries.last()
        val reset = f.controller.beginReset()
        f.controller.updateContext(f.allowed.copy(mode = DataSource.PLEOS))
        assertFalse(f.state.interactionAllowed)
        assertTrue(f.controller.finishReset(reset, true))
        assertTrue(f.state.interactionAllowed)
        val current = f.request()
        assertFalse(f.controller.accept(oldVisit))
        f.scheduler.fire(oldTimeout, true)
        assertEquals(current, f.state.request!!.token)
        assertEquals(1, f.scheduler.pending)
    }

    @Test fun modeChangesDoNotAllowFailedOrSupersededResetToUnlockInput() {
        val f = Fixture()
        f.request()
        val oldReset = f.controller.beginReset()
        f.controller.updateContext(f.allowed.copy(mode = DataSource.PLEOS))
        assertFalse(f.controller.finishReset(oldReset, false))
        assertFalse(f.state.interactionAllowed)
        assertTrue(f.controller.request() is VisitRequestResult.Rejected)
        val currentReset = f.controller.beginReset()
        f.controller.updateContext(f.allowed)
        assertFalse(f.controller.finishReset(oldReset, true))
        assertFalse(f.controller.finishReset(currentReset, false))
        assertFalse(f.controller.restartDemo())
        assertTrue(f.controller.request() is VisitRequestResult.Rejected)
        assertTrue(f.controller.finishReset(currentReset, true))
        assertTrue(f.state.interactionAllowed)
        assertTrue(f.controller.request() is VisitRequestResult.Created)
        assertFalse(f.controller.finishReset(currentReset, true))
    }

    @Test fun resetSuccessAfterModeChangeCannotOverrideRestrictedGate() {
        for (gate in listOf(GateState.UNKNOWN, GateState.RESTRICTED)) {
            val f = Fixture()
            val oldVisit = f.request()
            val reset = f.controller.beginReset()
            f.controller.updateContext(f.allowed.copy(mode = DataSource.PLEOS, gate = InteractionGate(gate)))
            assertTrue(f.controller.finishReset(reset, true))
            assertFalse(f.state.interactionAllowed)
            assertTrue(f.controller.request() is VisitRequestResult.Rejected)
            f.controller.updateContext(f.allowed.copy(mode = DataSource.PLEOS))
            assertTrue(f.state.interactionAllowed)
            assertFalse(f.controller.accept(oldVisit))
            assertEquals(VisitPhase.IDLE, f.state.phase)
        }
    }

    @Test fun completedConversationCannotBeReopenedByDuplicateEvents() {
        val f = Fixture()
        val token = f.request()
        val timeout = f.scheduler.entries.last()
        f.controller.accept(token)
        assertTrue(f.controller.close(token))
        assertFalse(f.controller.close(token))
        assertFalse(f.controller.accept(token))
        assertFalse(f.controller.withdraw(token))
        f.clock.monotonic = 15_000
        f.scheduler.fire(timeout, true)
        assertEquals(VisitPhase.COMPLETED, f.state.phase)
        assertEquals(0, f.scheduler.pending)
        f.noVisitor()
    }

    @Test fun epochChangesDoNotAffectDeadlineOrCooldown() {
        val f = Fixture()
        val token = f.request()
        val deadline = f.state.request!!.expiresAtMonotonicMs
        f.clock.epoch += 500_000_000
        f.scheduler.fire(f.scheduler.entries.last())
        assertEquals(VisitPhase.REQUESTED, f.state.phase)
        assertEquals(deadline, f.state.request!!.expiresAtMonotonicMs)
        f.clock.epoch = 0
        f.clock.monotonic = 14_999
        assertTrue(f.controller.accept(token))
        f.controller.close(token)
        assertEquals(15_001L, (f.controller.request() as VisitRequestResult.Rejected).retryAfterMs)
    }

    @Test fun earlyWakeUsesMonotonicRemainingTimeAndNeverAddsMultipleTimers() {
        val f = Fixture()
        f.request()
        f.clock.monotonic = 14_000
        f.scheduler.fire(f.scheduler.entries.last())
        assertEquals(VisitPhase.REQUESTED, f.state.phase)
        assertEquals(1_000L, f.state.remainingResponseMs)
        assertEquals(1, f.scheduler.pending)
        // A spurious early callback must not consume a second or expire the request.
        f.scheduler.fire(f.scheduler.entries.last())
        assertEquals(1_000L, f.state.remainingResponseMs)
        assertEquals(1, f.scheduler.pending)
        f.clock.monotonic = 15_000
        f.scheduler.runDue()
        assertEquals(VisitPhase.EXPIRED, f.state.phase)
        assertEquals(0, f.scheduler.pending)
    }

    @Test fun repeatedContextDoesNotEmitCountdownOrRescheduleOnRecomposition() {
        val f = Fixture()
        f.request()
        val before = f.state
        f.clock.monotonic = 10
        f.controller.updateContext(f.allowed)
        assertSame(before, f.state)
        assertEquals(1, f.scheduler.entries.size)
        f.clock.monotonic = 1_000
        f.scheduler.runDue()
        assertEquals(14_000L, f.state.remainingResponseMs)
        assertEquals(1, f.scheduler.pending)
    }

    @Test fun oldRequestTimeoutDoesNotEndAcceptedVisitButWithdrawalDoes() {
        val f = Fixture()
        val token = f.request()
        val timeout = f.scheduler.entries.last()
        f.controller.accept(token)
        f.clock.monotonic = 15_000
        f.scheduler.fire(timeout, true)
        assertEquals(VisitPhase.VISITING, f.state.phase)
        assertTrue(f.controller.withdraw(token))
        assertEquals(VisitPhase.WITHDRAWN, f.state.phase)
        assertFalse(f.controller.accept(token))
        assertEquals(0, f.scheduler.pending)
        f.noVisitor()
    }

    @Test fun avatarPolicyRequiresConsentCurrentGenerationAndGateEvenIfAvatarProvided() {
        val f = Fixture()
        val token = f.request()
        val state = f.state.copy(visitorAvatar = AvatarPreview("tori", "daejeon_hat"))
        for (phase in VisitPhase.entries) {
            assertEquals(phase == VisitPhase.VISITING, state.copy(phase = phase).canShowVisitor(f.allowed.gate))
        }
        val visiting = state.copy(phase = VisitPhase.VISITING)
        assertFalse(visiting.canShowVisitor(f.allowed.gate, hidden = true))
        assertFalse(visiting.copy(interactionAllowed = false).canShowVisitor(f.allowed.gate))
        assertFalse(visiting.copy(generation = token.generation + 1).canShowVisitor(f.allowed.gate))
        assertFalse(visiting.copy(request = null).canShowVisitor(f.allowed.gate))
        for (gate in listOf(GateState.UNKNOWN, GateState.RESTRICTED)) {
            assertFalse(visiting.canShowVisitor(InteractionGate(gate)))
        }
    }
}
