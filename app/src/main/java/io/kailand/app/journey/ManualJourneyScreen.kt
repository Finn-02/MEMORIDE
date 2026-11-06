package io.kailand.app.journey

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.kailand.core.contract.*
import kotlinx.coroutines.launch

/** The production declaration flow. The separate diagnostic panel remains draft-only. */
@Composable
fun ManualJourneyScreen(
    runtime: ManualJourneyRuntime, destinations: List<DestinationSpec>, initialRegionId: String?,
    callerIsCurrent: () -> Boolean,
) {
    val state by runtime.state.collectAsState()
    val busy by runtime.busy.collectAsState()
    val saved by runtime.result.collectAsState()
    var selected by rememberSaveable(initialRegionId) { mutableStateOf(initialRegionId ?: destinations.firstOrNull()?.regionId) }
    var kind by rememberSaveable { mutableStateOf(JourneyKind.REGION) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    DisposableEffect(runtime) { onDispose { runtime.leaveScreen() } }
    SideEffect { runtime.updateHost() }
    fun show(result: RequestResult) { error = (result as? RequestResult.Failed)?.message }
    val enabled = state.interactionAllowed && !busy && callerIsCurrent()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("사용자 확인 여정", style = MaterialTheme.typography.headlineMedium)
        Text("도착을 직접 확인해 기록합니다. 실제 방문 인증이나 SDK 자동 도착이 아닙니다.")
        Text("시간과 거리는 측정하지 않습니다. 내비게이션은 별도로 이용해 주세요.")
        if (!state.interactionAllowed) Text("주차·이용 허용 상태에서 현재 여정을 다시 확인해 주세요.")
        if (state.phase != ManualJourneyPhase.CONFIRMED) {
            destinations.forEach { destination ->
                OutlinedButton(onClick = { selected = destination.regionId }, enabled = enabled,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Text(destination.label + if (selected == destination.regionId) " · 선택됨" else "")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                JourneyKind.entries.forEach { value ->
                    FilterChip(selected = kind == value, onClick = { kind = value }, enabled = enabled,
                        label = { Text(if (value == JourneyKind.DAILY) "일상 이동" else "지역 여행") })
                }
            }
            Button(onClick = {
                destinations.firstOrNull { it.regionId == selected }?.let {
                    show(runtime.begin(it, kind, callerIsCurrent))
                }
            }, enabled = enabled && destinations.any { it.regionId == selected },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text(if (state.phase in setOf(ManualJourneyPhase.ACTIVE, ManualJourneyPhase.CHANGE_REQUIRED)) "선택 목적지로 변경" else "여정 시작")
            }
        }
        Text("현재 목적지 · ${state.destination?.label ?: "없음"}")
        when (state.phase) {
            ManualJourneyPhase.IDLE -> Text("목적지와 이동 종류를 선택해 주세요.")
            ManualJourneyPhase.ACTIVE -> Text("이동 후 주차 상태에서 여정을 마무리해 주세요.")
            ManualJourneyPhase.CHANGE_REQUIRED -> Text("새 목적지를 선택해 주세요. 이전 목적지로 완료하지 않습니다.")
            ManualJourneyPhase.CANCELLED -> Text("여정을 취소했습니다. 도착 기록을 저장하지 않았습니다.")
            ManualJourneyPhase.CONFIRMED -> Text(if (busy) "확인한 도착을 저장하고 있어요…" else when (saved) {
                is ArrivalResult.Applied, is ArrivalResult.AlreadyApplied -> "저장된 기록을 확인해 주세요."
                else -> "저장을 마치지 못했어요. 최초 확인 내용으로 다시 시도해 주세요."
            })
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = { runtime.requestReview(callerIsCurrent) },
            enabled = enabled && state.phase == ManualJourneyPhase.ACTIVE,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("여정 마무리") }
        if (saved is ArrivalResult.StorageFailure || saved is ArrivalResult.Rejected) {
            Button(onClick = { scope.launch { show(runtime.retry(callerIsCurrent)) } }, enabled = enabled,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("도착 저장 재시도") }
        }
    }
    state.confirmation?.let { question ->
        AlertDialog(onDismissRequest = {
            scope.launch { show(runtime.choose(question.token, ManualJourneyChoice.CONTINUE, callerIsCurrent)) }
        }, title = { Text("여정을 어떻게 마무리할까요?") },
            text = { Text("목적지는 ${question.destination.label}입니다. 도착 확인은 실제 방문 인증이 아닙니다.") },
            confirmButton = {
                Column {
                    listOf("${question.destination.label}에 도착했어요" to ManualJourneyChoice.ARRIVED,
                        "목적지를 바꿀게요" to ManualJourneyChoice.CHANGE_DESTINATION,
                        "여정을 취소할게요" to ManualJourneyChoice.CANCEL,
                        "계속 이동 중이에요" to ManualJourneyChoice.CONTINUE).forEach { (label, choice) ->
                        TextButton(onClick = { scope.launch { show(runtime.choose(question.token, choice, callerIsCurrent)) } },
                            enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(label) }
                    }
                }
            })
    }
}
