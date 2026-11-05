package io.kailand.app.journey

import io.kailand.core.contract.*
import org.junit.Assert.*
import org.junit.Test

class ManualArrivalControllerTest {
    private val daejeon = DestinationSpec("daejeon", "대전", 36.35, 127.38)
    private val wonju = DestinationSpec("wonju", "원주", 37.34, 127.92)
    private fun controller() = ManualArrivalController(setOf("daejeon", "wonju"), now = { 1234L })
    private fun active(c: ManualArrivalController) {
        c.setInteractionAllowed(true)
        assertEquals(RequestResult.Accepted, c.begin(daejeon, JourneyKind.REGION))
    }
    private fun question(c: ManualArrivalController): ManualConfirmationToken {
        assertTrue(c.requestReview(ManualReviewReason.USER_BUTTON))
        return checkNotNull(c.state.value.confirmation).token
    }

    @Test fun noActiveJourneyOrUnknownGateCannotFinish() {
        val c = controller()
        assertFalse(c.requestReview(ManualReviewReason.USER_BUTTON))
        assertTrue(c.begin(daejeon, JourneyKind.REGION) is RequestResult.Failed)
        active(c)
        c.setInteractionAllowed(false)
        assertFalse(c.requestReview(ManualReviewReason.USER_BUTTON))
        assertNull(c.state.value.confirmedDraft)
    }

    @Test fun nativeEndIsOnlyAHintAndWaitsForParking() {
        val c = controller(); active(c); c.setInteractionAllowed(false)
        assertTrue(c.requestReview(ManualReviewReason.NAVIGATION_ENDED))
        assertNull(c.state.value.confirmation)
        assertNull(c.state.value.confirmedDraft)
        c.setInteractionAllowed(true)
        assertNotNull(c.state.value.confirmation)
        assertNull(c.state.value.confirmedDraft)
    }

    @Test fun restrictionDropsVisibleQuestionAndRejectsItsLateClick() {
        val c = controller(); active(c); val old = question(c)
        c.setInteractionAllowed(false)
        assertNull(c.state.value.confirmation)
        assertTrue(c.choose(old, ManualJourneyChoice.ARRIVED) is RequestResult.Failed)
        c.setInteractionAllowed(true)
        assertNotEquals(old, c.state.value.confirmation?.token)
        assertTrue(c.choose(old, ManualJourneyChoice.ARRIVED) is RequestResult.Failed)
    }

    @Test fun destinationChangeKeepsJourneyIdButInvalidatesOldCard() {
        val c = controller(); active(c); val old = question(c)
        c.begin(wonju, JourneyKind.REGION)
        assertEquals(old.journeyId, c.state.value.journeyId)
        assertEquals(old.revision + 1, c.state.value.revision)
        assertNull(c.state.value.confirmation)
        assertTrue(c.choose(old, ManualJourneyChoice.ARRIVED) is RequestResult.Failed)
        val fresh = question(c)
        c.choose(fresh, ManualJourneyChoice.ARRIVED)
        assertEquals(wonju, c.state.value.confirmedDraft?.declaredDestination)
    }

    @Test fun userCanRequireReselectionWithoutConfirmingOldDestination() {
        val c = controller(); active(c)
        c.choose(question(c), ManualJourneyChoice.CHANGE_DESTINATION)
        assertEquals(ManualJourneyPhase.CHANGE_REQUIRED, c.state.value.phase)
        assertFalse(c.requestReview(ManualReviewReason.USER_BUTTON))
        assertNull(c.state.value.confirmedDraft)
        c.begin(wonju, JourneyKind.REGION)
        assertEquals(ManualJourneyPhase.ACTIVE, c.state.value.phase)
    }

    @Test fun cancelAndLateNativeHintsNeverCreateADraft() {
        val c = controller(); active(c); val token = question(c)
        c.choose(token, ManualJourneyChoice.CANCEL)
        assertFalse(c.requestReview(ManualReviewReason.NAVIGATION_ARRIVAL))
        assertTrue(c.choose(token, ManualJourneyChoice.ARRIVED) is RequestResult.Failed)
        assertNull(c.state.value.confirmedDraft)
        c.begin(wonju, JourneyKind.REGION)
        assertNotEquals(token.journeyId, c.state.value.journeyId)
    }

    @Test fun continuePreservesJourneyAndDoesNotComplete() {
        val c = controller(); active(c); val token = question(c)
        c.choose(token, ManualJourneyChoice.CONTINUE)
        assertEquals(token.journeyId, c.state.value.journeyId)
        assertEquals(ManualJourneyPhase.ACTIVE, c.state.value.phase)
        assertNull(c.state.value.confirmedDraft)
        assertNull(c.state.value.confirmation)
    }

    @Test fun duplicateArrivalClickCreatesExactlyOneImmutableDraft() {
        var clockCalls = 0
        val c = ManualArrivalController(setOf("daejeon"), now = { (++clockCalls).toLong() })
        active(c); val token = question(c)
        assertEquals(RequestResult.Accepted, c.choose(token, ManualJourneyChoice.ARRIVED))
        val draft = c.state.value.confirmedDraft
        assertNotNull(draft)
        assertTrue(c.choose(token, ManualJourneyChoice.ARRIVED) is RequestResult.Failed)
        assertEquals(1, clockCalls)
        c.requestReview(ManualReviewReason.NAVIGATION_ARRIVAL)
        c.setInteractionAllowed(false); c.setInteractionAllowed(true)
        assertSame(draft, c.state.value.confirmedDraft)
        assertEquals(token.journeyId, draft?.token?.journeyId)
        assertNull(draft?.elapsedSeconds)
        assertNull(draft?.distanceMeters)
    }

    @Test fun resetRejectsOldCardEvenWhenIdsAndSaveValuesRepeat() {
        val c = ManualArrivalController(setOf("daejeon"), newId = { "same-test-id" }, now = { 1234 })
        active(c); val old = question(c)
        c.resetPreview(); active(c); val fresh = question(c)
        assertNotEquals(old.generation, fresh.generation)
        assertTrue(c.choose(old, ManualJourneyChoice.ARRIVED) is RequestResult.Failed)
        assertNull(c.state.value.confirmedDraft)
    }

    @Test fun externalDestinationSignalInvalidatesOpenConfirmation() {
        val c = controller(); active(c); val old = question(c)
        c.navigationDestinationChanged()
        assertNotEquals(old, c.state.value.confirmation?.token)
        assertTrue(c.choose(old, ManualJourneyChoice.ARRIVED) is RequestResult.Failed)
        assertNull(c.state.value.confirmedDraft)
    }

    @Test fun unknownDestinationsAndActiveKindChangesAreRejected() {
        val c = controller(); c.setInteractionAllowed(true)
        assertTrue(c.begin(wonju.copy(regionId = "unknown"), JourneyKind.REGION) is RequestResult.Failed)
        active(c)
        assertTrue(c.begin(daejeon, JourneyKind.DAILY) is RequestResult.Failed)
    }

    @Test fun dailyDraftNormalizesRegionAndNeverInventsMeasurements() {
        val c = controller(); c.setInteractionAllowed(true)
        c.begin(daejeon, JourneyKind.DAILY)
        c.choose(question(c), ManualJourneyChoice.ARRIVED)
        val draft = checkNotNull(c.state.value.confirmedDraft)
        assertNull(draft.declaredDestination.regionId)
        assertNull(draft.elapsedSeconds)
        assertNull(draft.distanceMeters)
        assertEquals(1234L, draft.confirmedAtEpochMs)
        assertTrue(c.begin(wonju, JourneyKind.REGION) is RequestResult.Failed)
    }
}
