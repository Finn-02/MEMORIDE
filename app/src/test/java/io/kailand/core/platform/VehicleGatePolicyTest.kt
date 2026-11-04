package io.kailand.core.platform

import io.kailand.core.contract.GateState
import io.kailand.core.contract.InteractionGate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleGatePolicyTest {
    private val allowed = VehicleGateInputs(true, true, true, false, 0)

    @Test fun bothParkingAndUnrestrictedPlatformAreRequired() {
        assertTrue(vehicleGate(allowed).canInteract)
        assertEquals(GateState.UNKNOWN, vehicleGate(allowed.copy(parked = null)).state)
        assertEquals(GateState.RESTRICTED, vehicleGate(allowed.copy(parked = false)).state)
    }

    @Test fun everyMissingObservationBlocks() {
        listOf(
            allowed.copy(connected = false), allowed.copy(foreground = false),
            allowed.copy(parked = null), allowed.copy(requiresDistractionOptimization = null),
            allowed.copy(activeRestrictions = null),
        ).forEach { assertFalse(vehicleGate(it).canInteract) }
    }

    @Test fun restrictionsOverrideParkingAndMissingParking() {
        for (parked in listOf(true, false, null)) {
            assertEquals(GateState.RESTRICTED, vehicleGate(allowed.copy(parked = parked, requiresDistractionOptimization = true)).state)
            assertEquals(GateState.RESTRICTED, vehicleGate(allowed.copy(parked = parked, activeRestrictions = 0x50)).state)
        }
    }

    @Test fun anyRestrictionBitIncludingFutureBitsBlocks() {
        for (bit in 0..31) {
            assertFalse(vehicleGate(allowed.copy(activeRestrictions = 1 shl bit)).canInteract)
        }
    }

    @Test fun stopImmediatelyBlocksAndRejectsLateAllow() {
        var gate = InteractionGate(GateState.UNKNOWN)
        val session = VehicleGateSession { gate = it }
        val token = session.start()
        session.update(token) { allowed }
        assertTrue(gate.canInteract)
        session.stop()
        session.update(token) { allowed }
        assertEquals(GateState.UNKNOWN, gate.state)
    }

    @Test fun reconnectDiscardsOldInputsAndCallbacks() {
        var gate = InteractionGate(GateState.UNKNOWN)
        val session = VehicleGateSession { gate = it }
        val old = session.start()
        session.update(old) { allowed }
        val fresh = session.start()
        session.update(old) { allowed }
        assertFalse(gate.canInteract)
        session.update(fresh) { it.copy(connected = true, parked = true) }
        assertFalse(gate.canInteract)
        session.update(fresh) { it.copy(requiresDistractionOptimization = false, activeRestrictions = 0) }
        assertTrue(gate.canInteract)
    }

    @Test fun disconnectionAndPropertyFailureCloseAnOpenGate() {
        var gate = InteractionGate(GateState.UNKNOWN)
        val session = VehicleGateSession { gate = it }
        val token = session.start()
        session.update(token) { allowed }
        session.update(token) { it.copy(parked = null) }
        assertFalse(gate.canInteract)
        session.update(token) { allowed }
        session.update(token) { VehicleGateInputs(foreground = true) }
        assertFalse(gate.canInteract)
    }
}
