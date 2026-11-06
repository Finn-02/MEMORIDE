package io.kailand.feature.visit

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.kailand.core.contract.*
import io.kailand.core.ui.MemorideTheme

/** Isolated C05 harness: real Handler/monotonic clock and VisitRoute lifecycle, sample inputs only. */
@Preview(name = "C05 방문 수명주기 검사", widthDp = 420, heightDp = 1000, showBackground = true)
@Composable
fun VisitAcceptancePreview() {
    val controller = remember { createVisitController() }
    val state by controller.state.collectAsState()
    var visible by remember { mutableStateOf(true) }
    var tools by rememberSaveable { mutableStateOf(false) }
    var gate by remember { mutableStateOf(GateState.PARKED_ALLOWED) }
    var reset by remember { mutableStateOf<Long?>(null) }
    var ack by remember { mutableStateOf("초기화 통지 전") }
    DisposableEffect(controller) { onDispose { controller.leaveScreen() } }
    MemorideTheme {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Text("C05 SAMPLE · 실제 주차/게임 초기화 아님", Modifier.padding(horizontal = 12.dp))
            Text("${state.phase} · 응답 ${state.remainingResponseMs?.let { (it + 999) / 1000 } ?: "-"}초",
                Modifier.padding(horizontal = 12.dp))
            OutlinedButton(onClick = { tools = !tools }, modifier = Modifier.padding(horizontal = 12.dp).heightIn(min = 56.dp)) {
                Text(if (tools) "검사 도구 접기" else "검사 도구 열기")
            }
            if (tools) {
                Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { controller.restartDemo() }, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) { Text("방문만 다시 준비") }
                    OutlinedButton(onClick = { visible = !visible }, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) { Text(if (visible) "화면 이탈" else "화면 복귀") }
                }
                Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { gate = GateState.PARKED_ALLOWED }, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) { Text("샘플 허용") }
                    OutlinedButton(onClick = { gate = GateState.UNKNOWN }, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) { Text("샘플 UNKNOWN") }
                }
                Text("방문 reset 통지만 대역 처리 · B 저장 호출 없음", Modifier.padding(horizontal = 12.dp))
                Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { reset = controller.beginReset(); ack = "입력 차단 중" }, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) { Text("reset 시작") }
                    OutlinedButton(onClick = { reset?.let { ack = "실패 통지: ${controller.finishReset(it, false)}" } }, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) { Text("실패 통지") }
                    OutlinedButton(onClick = { reset?.let { ack = "성공 통지: ${controller.finishReset(it, true)}" } }, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) { Text("성공 통지") }
                }
                Text(ack, Modifier.padding(horizontal = 12.dp))
            }
            if (visible) VisitRoute(controller, InteractionGate(gate), DataSource.DEMO,
                AvatarPreview("tori", null), { _, _, modifier -> Box(modifier) { Text("임시 외형") } },
                onBack = { visible = false }, modifier = Modifier.weight(1f))
            else Text("시연 화면 이탈 중 · 복귀해도 이전 요청을 자동 재개하지 않아요.", Modifier.padding(12.dp))
        }
    }
}
