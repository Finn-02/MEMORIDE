package io.kailand.app.journey

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.kailand.core.contract.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** A-owned S03. Persistence and platform checks are provided by the app composition boundary. */
@Composable
fun JourneyPreparationScreen(
    journey: JourneyPort,
    demo: DemoControlPort,
    destinations: List<DestinationSpec>,
    gate: InteractionGate,
    completion: DemoInput.Complete,
    onBack: () -> Unit,
    onOpenCompanion: () -> Unit,
    modifier: Modifier = Modifier,
    developmentControls: (@Composable () -> Unit)? = null,
    initialRegionId: String? = null,
) {
    val snapshot by journey.state.collectAsState()
    var selectedRegion by rememberSaveable(initialRegionId) {
        mutableStateOf(initialRegionId ?: snapshot.destination?.regionId)
    }
    var kind by rememberSaveable { mutableStateOf(JourneyKind.REGION) }
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val latestGate by rememberUpdatedState(gate)
    val selected = destinations.firstOrNull { it.regionId == selectedRegion } ?: destinations.firstOrNull()
    val run: (suspend () -> RequestResult) -> Unit = { action ->
        if (!busy && latestGate.canInteract) {
            busy = true
            result = null
            scope.launch {
                try {
                    if (!latestGate.canInteract) return@launch
                    result = when (val answer = action()) {
                        RequestResult.Accepted -> "요청 처리 완료 · 여정 상태와 저장 결과를 함께 확인하세요."
                        is RequestResult.Failed -> answer.message
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    result = "요청을 처리하지 못했어요. 다시 시도해 주세요."
                } finally { busy = false }
            }
        }
    }
    BackHandler { onBack() }
    Surface(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("메인 섬") }
            Text("여정 준비", style = MaterialTheme.typography.headlineMedium)
            Text("DEMO · 모의 여정")
            if (!gate.canInteract) {
                Text(gate.reason ?: "이용 상태를 확인할 수 없어 여정 조작을 중단했어요.")
            } else {
                Text("목적지 후보", style = MaterialTheme.typography.titleMedium)
                destinations.forEach { destination ->
                    OutlinedButton(onClick = { if (!busy) selectedRegion = destination.regionId }, enabled = !busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                        Text(destination.label + if (selected == destination) " · 선택됨" else "")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    JourneyKind.entries.forEach { option ->
                        FilterChip(selected = kind == option, onClick = { if (!busy) kind = option }, enabled = !busy,
                            label = { Text(if (option == JourneyKind.DAILY) "일상 이동" else "지역 여행") })
                    }
                }
                Text("현재 상태 · ${snapshot.phase.label()}", style = MaterialTheme.typography.titleLarge)
                Text("요청한 목적지 · ${snapshot.destination?.label ?: "없음"}")
                Text("여정 종류 · ${if (snapshot.kind == JourneyKind.DAILY) "일상 이동" else "지역 여행"}")
                if (snapshot.journeyId != null) Text("여정 ID · ${snapshot.journeyId}", style = MaterialTheme.typography.bodySmall)
                Text("목적지 버전 · ${snapshot.routeRevision}", style = MaterialTheme.typography.bodySmall)
                snapshot.elapsedSeconds?.let { Text("모의 경과 시간 · ${it}초") }
                snapshot.distanceMeters?.let { Text("모의 거리 · ${it}m") }
                snapshot.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                result?.let { Text(it) }
                if (busy) Text("처리 중…")
                val enabled = !busy && gate.canInteract
                JourneyButton(if (snapshot.phase in activePhases) "목적지 변경 요청" else "목적지 요청", enabled && selected != null) {
                    selected?.let { target -> run { journey.requestDestination(target, kind) } }
                }
                JourneyButton("모의 출발", enabled && snapshot.phase == JourneyPhase.READY) { run { demo.dispatch(DemoInput.StartTravel) } }
                JourneyButton("모의 도착", enabled && snapshot.phase in setOf(JourneyPhase.READY, JourneyPhase.TRAVELLING)) { run { demo.dispatch(completion) } }
                JourneyButton("여정 취소", enabled && snapshot.phase in activePhases) { run { journey.cancel() } }
                JourneyButton("도착 저장 재시도", enabled && snapshot.phase == JourneyPhase.ARRIVED && snapshot.errorMessage != null) { run { journey.retryArrival() } }
                JourneyButton("같은 도착 반복", enabled && snapshot.phase == JourneyPhase.ARRIVED && snapshot.errorMessage == null) { run { demo.dispatch(DemoInput.RepeatLastArrival) } }
                JourneyButton("동행 모의 화면", enabled && snapshot.journeyId != null) { if (latestGate.canInteract && !busy) onOpenCompanion() }
                developmentControls?.invoke()
            }
        }
    }
}

@Composable
private fun JourneyButton(label: String, enabled: Boolean, action: () -> Unit) {
    Button(onClick = action, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(label) }
}

private val activePhases = setOf(JourneyPhase.PREPARING, JourneyPhase.READY, JourneyPhase.TRAVELLING)
private fun JourneyPhase.label() = when (this) {
    JourneyPhase.IDLE -> "시작 전"
    JourneyPhase.PREPARING -> "준비 중"
    JourneyPhase.READY -> "준비 완료"
    JourneyPhase.TRAVELLING -> "모의 이동 중"
    JourneyPhase.ARRIVED -> "도착 확인 · 저장 결과 별도"
    JourneyPhase.CANCELLED -> "취소됨"
    JourneyPhase.ERROR -> "오류"
}
