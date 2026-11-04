package io.kailand.app.journey

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.kailand.core.contract.DestinationSpec
import io.kailand.core.contract.JourneyKind
import io.kailand.core.contract.RequestResult

/** Reusable A-owned confirmation UI; progress storage belongs to the app coordinator and B. */
@Composable
fun ManualJourneyConfirmationPanel(controller: ManualArrivalController, destinations: List<DestinationSpec> = emptyList()) {
    val state by controller.state.collectAsState()
    var selectedId by rememberSaveable { mutableStateOf(destinations.firstOrNull()?.regionId) }
    var selectedKind by rememberSaveable { mutableStateOf(JourneyKind.REGION.name) }
    var error by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("사용자 확인 여정 · 개발 검증", style = MaterialTheme.typography.titleMedium)
        Text("실제 저장·보상 없음 · 선택 목적지: ${state.destination?.label ?: "없음"}")
        if (destinations.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                destinations.forEach { destination ->
                    FilterChip(selected = selectedId == destination.regionId,
                        onClick = { selectedId = destination.regionId },
                        enabled = state.interactionAllowed && state.phase != ManualJourneyPhase.CONFIRMED,
                        label = { Text(destination.label) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(JourneyKind.DAILY to "일상 이동", JourneyKind.REGION to "지역 여행").forEach { (kind, label) ->
                    FilterChip(selected = selectedKind == kind.name, onClick = { selectedKind = kind.name },
                        enabled = state.interactionAllowed && state.phase != ManualJourneyPhase.CONFIRMED,
                        label = { Text(label) })
                }
            }
            Button(onClick = {
                val destination = destinations.firstOrNull { it.regionId == selectedId }
                val result = destination?.let { controller.begin(it, JourneyKind.valueOf(selectedKind)) }
                error = (result as? RequestResult.Failed)?.message
            }, enabled = state.interactionAllowed && state.phase != ManualJourneyPhase.CONFIRMED) {
                Text(if (state.phase in setOf(ManualJourneyPhase.ACTIVE, ManualJourneyPhase.CHANGE_REQUIRED)) "선택 목적지로 변경" else "수동 여정 시작")
            }
            Text("위 선택은 내비 요청 없이 사용자 여정만 설정합니다.", style = MaterialTheme.typography.bodySmall)
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.journeyId?.let { Text("여정: $it / 목적지 버전 ${state.revision}", style = MaterialTheme.typography.bodySmall) }
        when (state.phase) {
            ManualJourneyPhase.CHANGE_REQUIRED -> Text("새 목적지를 선택해 주세요. 이전 목적지로는 완료할 수 없어요.")
            ManualJourneyPhase.CANCELLED -> Text("여정을 취소했어요. 도착·보상 초안은 만들지 않았어요.")
            ManualJourneyPhase.CONFIRMED -> Text("${state.destination?.label} 도착을 사용자가 확인했어요. 메모리 초안만 생성됐으며 저장·보상은 미연결입니다.")
            else -> Unit
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { controller.requestReview(ManualReviewReason.USER_BUTTON) },
                enabled = state.interactionAllowed && state.phase == ManualJourneyPhase.ACTIVE) { Text("여정 마무리") }
            TextButton(onClick = { controller.resetPreview() },
                enabled = state.interactionAllowed && state.phase != ManualJourneyPhase.IDLE) { Text("확인 흐름 초기화") }
        }
    }
    state.confirmation?.let { question ->
        AlertDialog(
            onDismissRequest = { controller.choose(question.token, ManualJourneyChoice.CONTINUE) },
            title = { Text("여정을 어떻게 마무리할까요?") },
            text = { Text("선택했던 목적지는 ${question.destination.label}입니다.\n현재 여정을 확인해 주세요. 사용자 확인은 실제 방문 인증이 아닙니다.") },
            confirmButton = {
                Column {
                    listOf(
                        "${question.destination.label}에 도착했어요" to ManualJourneyChoice.ARRIVED,
                        "목적지를 바꿀게요" to ManualJourneyChoice.CHANGE_DESTINATION,
                        "여정을 취소할게요" to ManualJourneyChoice.CANCEL,
                        "계속 이동 중이에요" to ManualJourneyChoice.CONTINUE,
                    ).forEach { (label, choice) ->
                        TextButton(onClick = { controller.choose(question.token, choice) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(label) }
                    }
                }
            },
        )
    }
}
