package io.kailand.core.platform

import io.kailand.core.contract.GateState
import io.kailand.core.contract.InteractionGate

/** A01 only: a sample host must never substitute for a vehicle/platform gate. */
fun developmentGate(
    debugBuild: Boolean,
    developmentEntry: Boolean,
    automotiveDevice: Boolean,
    carUiMode: Boolean,
    foreground: Boolean,
): InteractionGate = if (
    debugBuild && developmentEntry && !automotiveDevice && !carUiMode && foreground
) {
    InteractionGate(GateState.PARKED_ALLOWED, "일반 Android 개발 샘플 · 실제 주차 확인 아님")
} else {
    InteractionGate(GateState.UNKNOWN, "실제 플랫폼 주차·이용 제한 입력 미연결")
}
