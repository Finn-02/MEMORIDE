package io.kailand.core.platform

import io.kailand.core.contract.GateState
import io.kailand.core.contract.InteractionGate

/** Only platform observations enter here; DEMO journey state is never an input. */
data class VehicleGateInputs(
    val connected: Boolean = false,
    val foreground: Boolean = false,
    val parked: Boolean? = null,
    val requiresDistractionOptimization: Boolean? = null,
    val activeRestrictions: Int? = null,
)

fun vehicleGate(inputs: VehicleGateInputs): InteractionGate = with(inputs) {
    when {
        !foreground -> InteractionGate(GateState.UNKNOWN, "앱이 활성 상태가 아니에요.")
        !connected -> InteractionGate(GateState.UNKNOWN, "차량 상태 연결을 확인하고 있어요.")
        requiresDistractionOptimization == true || (activeRestrictions != null && activeRestrictions != 0) ->
            InteractionGate(GateState.RESTRICTED, "차량 플랫폼의 이용 제한이 적용됐어요.")
        parked == false -> InteractionGate(GateState.RESTRICTED, "주차 기어 P에서 이용할 수 있어요.")
        parked == null || requiresDistractionOptimization == null || activeRestrictions == null ->
            InteractionGate(GateState.UNKNOWN, "주차·이용 제한 상태를 확인하지 못했어요.")
        else -> InteractionGate(GateState.PARKED_ALLOWED, "플랫폼 주차 P·이용 허용 확인")
    }
}

/** Invalidate all old observations when leaving foreground or reconnecting. Main-thread confined. */
class VehicleGateSession(private val onGate: (InteractionGate) -> Unit) {
    private var generation = 0L
    private var inputs = VehicleGateInputs()

    fun start(): Long {
        generation++
        inputs = VehicleGateInputs(foreground = true)
        onGate(vehicleGate(inputs))
        return generation
    }

    fun update(token: Long, transform: (VehicleGateInputs) -> VehicleGateInputs) {
        if (token != generation || !inputs.foreground) return
        inputs = transform(inputs)
        onGate(vehicleGate(inputs))
    }

    fun stop() {
        generation++
        inputs = VehicleGateInputs()
        onGate(vehicleGate(inputs))
    }
}
