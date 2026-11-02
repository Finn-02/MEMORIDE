package io.kailand.core.platform

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DevelopmentGateTest {
    @Test fun onlyExplicitForegroundGenericDebugSampleCanOpen() {
        assertTrue(developmentGate(true, true, false, false, true).canInteract)
    }

    @Test fun platformEntryStaysUnknownEvenInDebug() {
        assertFalse(developmentGate(true, false, false, false, true).canInteract)
    }

    @Test fun automotiveAndCarModeCannotUseSampleGate() {
        assertFalse(developmentGate(true, true, true, false, true).canInteract)
        assertFalse(developmentGate(true, true, false, true, true).canInteract)
    }

    @Test fun releaseAndBackgroundCannotUseSampleGate() {
        assertFalse(developmentGate(false, true, false, false, true).canInteract)
        assertFalse(developmentGate(true, true, false, false, false).canInteract)
    }
}
