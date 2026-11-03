package io.kailand.feature.companion

import io.kailand.core.contract.DataSource
import io.kailand.core.contract.GateState
import io.kailand.core.contract.InteractionGate
import io.kailand.core.contract.JourneyPhase
import io.kailand.core.contract.JourneySnapshot
import java.util.Locale

/** C-local display values only; never dispatches journey, game, or storage commands. */
internal data class CompanionPresentation(
    val source: String,
    val destination: String,
    val phase: String,
    val notice: String,
    val distance: String?,
    val elapsed: String?,
    val blockedReason: String?,
    val canToggleVisibility: Boolean,
    val showAvatar: Boolean,
)

internal fun companionPresentation(
    journey: JourneySnapshot,
    gate: InteractionGate,
    foreground: Boolean,
    hidden: Boolean,
): CompanionPresentation {
    val source = journey.source.name
    val blocked = when {
        gate.state == GateState.UNKNOWN -> gate.reason ?: "이용 상태를 확인하지 못했어요."
        gate.state == GateState.RESTRICTED -> gate.reason ?: "현재는 동행 화면을 이용할 수 없어요."
        !foreground -> "화면이 활성 상태가 아니어서 동행 표시를 멈췄어요."
        else -> null
    }
    val phase = when (journey.phase) {
        JourneyPhase.IDLE -> "시작된 여정이 없어요"
        JourneyPhase.PREPARING -> "여정을 준비하고 있어요"
        JourneyPhase.READY -> "여정이 준비됐어요"
        JourneyPhase.TRAVELLING -> if (journey.source == DataSource.DEMO) "모의 이동 중" else "이동 단계"
        JourneyPhase.ARRIVED -> "도착 입력을 받았어요"
        JourneyPhase.CANCELLED -> "여정이 취소됐어요"
        JourneyPhase.ERROR -> "여정 정보를 확인해 주세요"
    }
    val notice = when (journey.phase) {
        JourneyPhase.IDLE -> "여정 준비 화면에서 목적지를 선택해 주세요."
        JourneyPhase.PREPARING -> "목적지 요청만으로 도착이나 보상이 확정되지 않아요."
        JourneyPhase.READY -> "출발·완료 조작은 여정 또는 시연 화면에서 진행해요."
        JourneyPhase.TRAVELLING -> "여정 단계는 실제 차량의 주차·주행 상태와 구분돼요."
        JourneyPhase.ARRIVED -> "보상과 브리핑은 저장 결과를 확인한 뒤 연결돼요."
        JourneyPhase.CANCELLED -> "취소된 여정으로 도착이나 보상을 처리하지 않아요."
        JourneyPhase.ERROR -> journey.errorMessage?.takeIf { it.isNotBlank() } ?: "여정 정보를 받지 못했어요."
    }
    val distance = journey.distanceMeters?.takeIf { it.isFinite() && it >= 0.0 }?.let {
        if (it < 1000.0) String.format(Locale.KOREA, "%.0f m", it)
        else String.format(Locale.KOREA, "%.1f km", it / 1000.0)
    }
    val elapsed = journey.elapsedSeconds?.takeIf { it >= 0 }?.let { "${it / 60}분 ${it % 60}초" }
    return CompanionPresentation(
        source = source,
        destination = journey.destination?.label?.takeIf { it.isNotBlank() } ?: "목적지 미선택",
        phase = phase,
        notice = notice,
        distance = distance?.let { "$source 누적 거리 · $it" },
        elapsed = elapsed?.let { "$source 경과 시간 · $it" },
        blockedReason = blocked,
        canToggleVisibility = blocked == null,
        showAvatar = blocked == null && !hidden,
    )
}
