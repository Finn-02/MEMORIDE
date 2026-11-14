package io.kailand.feature.briefing

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveStatusInputGuardTest {
    @Test fun secondTapBeforeReleaseIsIgnoredAcrossBothButtons() {
        val guard = SaveStatusInputGuard()
        var retries = 0
        var dismisses = 0
        if (guard.tryEnter(true)) retries++
        if (guard.tryEnter(true)) retries++ // double tap on 다시 시도
        if (guard.tryEnter(true)) dismisses++ // immediate 닫기 after 다시 시도
        assertTrue(retries == 1 && dismisses == 0)
    }

    @Test fun releaseReArmsSoARepeatedIdenticalFailureIsNotLocked() {
        val guard = SaveStatusInputGuard()
        assertTrue(guard.tryEnter(true))
        guard.release() // next frame or a newly observed state
        assertTrue(guard.tryEnter(true))
    }

    @Test fun blockedGateNeverEntersOrLocks() {
        val guard = SaveStatusInputGuard()
        assertFalse(guard.tryEnter(false))
        assertFalse(guard.busy)
        assertTrue(guard.tryEnter(true))
    }

    @Test fun staleFallbackDoesNotReleaseANewerTap() {
        val guard = SaveStatusInputGuard()
        assertTrue(guard.tryEnter(true))
        val first = guard.generation
        guard.release() // A's Saving observed
        assertTrue(guard.tryEnter(true)) // next Failed, new tap
        guard.releaseIf(first) // fallback from the first tap fires late
        assertTrue(guard.busy)
        guard.releaseIf(guard.generation)
        assertFalse(guard.busy)
    }
}
