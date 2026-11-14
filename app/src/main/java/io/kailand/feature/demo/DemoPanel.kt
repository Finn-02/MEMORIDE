package io.kailand.feature.demo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.kailand.core.contract.DataSource
import io.kailand.core.contract.InteractionGate
import kotlinx.coroutines.launch

/** A retains runner/ports, supplies current mode/gate, and owns all app-level commands. */
@Composable
fun DemoPanel(
    runner: DemoScenarioRunner, gate: InteractionGate, mode: DataSource, modifier: Modifier = Modifier,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentGate by rememberUpdatedState(gate)
    val currentMode by rememberUpdatedState(mode)
    var foreground by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    val state by runner.state.collectAsState()
    val scope = rememberCoroutineScope()
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    fun refresh() {
        foreground = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        runner.updateContext(DemoContext(currentGate, foreground, true, currentMode))
    }
    DisposableEffect(runner, lifecycle) {
        val observer = LifecycleEventObserver { _, _ -> refresh() }
        lifecycle.addObserver(observer)
        refresh()
        onDispose { lifecycle.removeObserver(observer); runner.leaveScreen() }
    }
    SideEffect { refresh() }
    val visible = gate.canInteract && foreground && mode == DataSource.DEMO
    LaunchedEffect(visible) { if (!visible) confirmReset = false }
    Surface(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("DEMO · 수동 시연 제어", style = MaterialTheme.typography.headlineSmall)
            Text("모의 입력만 요청해요. 실제 주차 상태·SDK 성공을 바꾸지 않아요.")
            if (!visible) {
                Text("시연 입력 중단", style = MaterialTheme.typography.titleLarge)
                Text(gate.reason ?: if (mode != DataSource.DEMO) "PLEOS 모드에서는 DEMO 입력을 사용할 수 없어요."
                    else "이용 가능한 활성 화면에서 다시 확인해 주세요.")
            } else {
                Text("게임 성장·아이템·발견을 지우는 전체 초기화는 확인 후 A가 실행해요.")
                OutlinedButton(onClick = { confirmReset = true },
                    enabled = !(state.resetBlocked && state.busy), modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Text(if (state.resetBlocked && !state.busy) "전체 초기화 다시 확인" else "전체 게임 초기화")
                }
                state.message?.let { Text(it) }
                OutlinedButton(onClick = { refresh(); scope.launch { runner.cancelJourney() } },
                    enabled = state.interactionAllowed && !state.busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("모의 여정 취소") }
                if (state.busy) Text("처리 결과를 기다리고 있어요. 다음 단계는 자동 실행하지 않아요.")
                if (state.resetBlocked) Text("초기화가 성공하기 전에는 새 시연 입력을 받지 않아요.")
                if (state.step == DemoStep.CHOOSE || state.step == DemoStep.DONE) {
                    if (runner.scenarios.isEmpty()) Text("준비된 시나리오가 없어요. 입력 파일 로드를 확인해 주세요.")
                    runner.scenarios.forEach { scenario ->
                        OutlinedButton(onClick = { refresh(); runner.selectScenario(scenario.id) },
                            enabled = state.interactionAllowed && !state.busy,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("${scenario.id} · ${scenario.title}") }
                    }
                }
                state.selected?.let { plan ->
                    Text("${plan.id} · ${plan.title}", style = MaterialTheme.typography.titleLarge)
                    Text("${plan.kind} · ${stepLabel(state.step)}")
                    state.journeyId?.let { Text("여정 ID: $it") }
                    if (state.step == DemoStep.RESET) Text("이 시나리오는 먼저 전체 초기화 확인이 필요해요.")
                    if (state.step !in setOf(DemoStep.CHOOSE, DemoStep.RESET, DemoStep.DONE)) {
                        Button(onClick = { refresh(); scope.launch { runner.advance() } },
                            enabled = state.interactionAllowed && !state.busy,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(stepLabel(state.step)) }
                    }
                }
            }
        }
    }
    if (confirmReset && visible) AlertDialog(
        onDismissRequest = { confirmReset = false },
        title = { Text("전체 게임을 초기화할까요?") },
        text = { Text("성장·아이템·지역 발견과 진행 중 시연을 지워요. 방문만 다시 준비하는 기능과 달라요.") },
        confirmButton = { TextButton(onClick = { confirmReset = false; refresh(); scope.launch { runner.resetConfirmed() } }) { Text("초기화") } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("취소") } },
    )
}

private fun stepLabel(step: DemoStep) = when (step) {
    DemoStep.PREPARE -> "목적지 요청"
    DemoStep.WAIT_READY -> "READY 확인"
    DemoStep.START -> "모의 출발"
    DemoStep.COMPLETE -> "모의 완료 · 저장 결과 대기"
    DemoStep.RETRY_ARRIVAL -> "같은 도착 저장 재시도"
    DemoStep.REPEAT -> "마지막 원본 도착 반복"
    DemoStep.VERIFY_SAVED -> "저장 결과 다시 확인"
    DemoStep.CHOOSE -> "시나리오 선택"
    DemoStep.RESET -> "전체 초기화 확인"
    DemoStep.DONE -> "저장 결과 확인 완료"
}
