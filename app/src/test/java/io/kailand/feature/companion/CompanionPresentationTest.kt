package io.kailand.feature.companion

import io.kailand.core.contract.DataSource
import io.kailand.core.contract.GateState
import io.kailand.core.contract.InteractionGate
import io.kailand.core.contract.JourneyKind
import io.kailand.core.contract.JourneyPhase
import io.kailand.core.contract.JourneySnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionPresentationTest {
    private val journey = JourneySnapshot(
        journeyId = "unit-test-only", source = DataSource.DEMO, kind = JourneyKind.REGION,
        phase = JourneyPhase.TRAVELLING, routeRevision = 0, destination = null,
    )
    private val allowed = InteractionGate(GateState.PARKED_ALLOWED)

    @Test fun actualGateBlocksEveryPhaseAndSource() {
        for (source in DataSource.entries) for (phase in JourneyPhase.entries) {
            for (state in listOf(GateState.UNKNOWN, GateState.RESTRICTED)) {
                val view = companionPresentation(journey.copy(source = source, phase = phase), InteractionGate(state), true, false)
                assertFalse("$state $source $phase", view.showAvatar)
                assertFalse(view.canToggleVisibility)
                assertNotNull(view.blockedReason)
            }
        }
    }

    @Test fun backgroundUnmountsAvatarEvenWithAllowedGate() {
        val view = companionPresentation(journey, allowed, false, false)
        assertFalse(view.showAvatar)
        assertFalse(view.canToggleVisibility)
        assertNotNull(view.blockedReason)
    }

    @Test fun hideChoiceSurvivesRestrictionAndReturnWithoutChangingInput() {
        assertTrue(companionPresentation(journey, allowed, true, false).showAvatar)
        assertFalse(companionPresentation(journey, allowed, true, true).showAvatar)
        assertFalse(companionPresentation(journey, InteractionGate(GateState.RESTRICTED), true, true).showAvatar)
        val returned = companionPresentation(journey, allowed, true, true)
        assertFalse(returned.showAvatar)
        assertTrue(returned.canToggleVisibility)
        assertEquals(JourneyPhase.TRAVELLING, journey.phase)
    }

    @Test fun absentMeasurementsAndDestinationAreNotFabricated() {
        val view = companionPresentation(journey, allowed, true, false)
        assertNull(view.distance)
        assertNull(view.elapsed)
        assertEquals("목적지 미선택", view.destination)
    }

    @Test fun explicitZeroIsDisplayedButDoesNotOpenUnknownGate() {
        val zero = journey.copy(distanceMeters = 0.0, elapsedSeconds = 0)
        val view = companionPresentation(zero, InteractionGate(GateState.UNKNOWN), true, false)
        assertEquals("DEMO 누적 거리 · 0 m", view.distance)
        assertEquals("DEMO 경과 시간 · 0분 0초", view.elapsed)
        assertFalse(view.showAvatar)
    }

    @Test fun invalidMeasurementsAreOmittedInsteadOfScoredOrClamped() {
        for (distance in listOf(-1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            val view = companionPresentation(journey.copy(distanceMeters = distance, elapsedSeconds = -1), allowed, true, false)
            assertNull(view.distance)
            assertNull(view.elapsed)
        }
    }

    @Test fun sourceIsPreservedAndTotalsAreNotPresentedAsRemaining() {
        for (source in DataSource.entries) {
            val view = companionPresentation(journey.copy(source = source, distanceMeters = 5000.0, elapsedSeconds = 1200), allowed, true, false)
            assertEquals(source.name, view.source)
            assertEquals("${source.name} 누적 거리 · 5.0 km", view.distance)
            assertEquals("${source.name} 경과 시간 · 20분 0초", view.elapsed)
        }
    }

    @Test fun arrivalIsNotRewardSuccessAndErrorDoesNotBecomeArrival() {
        val arrived = companionPresentation(journey.copy(phase = JourneyPhase.ARRIVED), allowed, true, false)
        assertEquals("도착 입력을 받았어요", arrived.phase)
        assertTrue(arrived.notice.contains("저장 결과"))
        val error = companionPresentation(journey.copy(phase = JourneyPhase.ERROR, errorMessage = "연결 실패"), allowed, true, false)
        assertEquals("연결 실패", error.notice)
        assertEquals("여정 정보를 확인해 주세요", error.phase)
    }
}
