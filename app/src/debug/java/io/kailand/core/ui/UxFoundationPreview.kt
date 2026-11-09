package io.kailand.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.kailand.R
import io.kailand.core.contract.InteractionGate
import io.kailand.core.contract.GateState

/** Local-only component rehearsal: no SDK, game service, progress or storage calls. */
@Composable
fun UxFoundationGallery(gate: InteractionGate, onClose: () -> Unit = {}) {
    var message by remember { mutableStateOf("버튼을 눌러 반응을 확인하세요.") }
    val enabled = gate.canInteract
    MemorideScreen {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth >= MemorideDimens.WideBreakpoint && LocalDensity.current.fontScale <= 1.2f
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(MemorideDimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("UX-A01 · 공통 UI", style = MaterialTheme.typography.headlineSmall)
                        Text("DEMO · 화면 예제 · 게임 저장 없음", style = MaterialTheme.typography.bodyMedium)
                    }
                    MemorideIconButton("닫기", onClose, enabled = enabled) {
                        Image(painterResource(R.drawable.memoride_close), null, Modifier.size(28.dp))
                    }
                }
                val buttons: @Composable (Modifier) -> Unit = { modifier ->
                    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        MemoridePrimaryButton("여정 출발", { message = "주요 버튼 클릭 확인 · 내비는 실행하지 않았어요." },
                            Modifier.fillMaxWidth(), enabled, "내비가 자동으로 켜져요") {
                            Image(painterResource(R.drawable.memoride_departure), null, Modifier.size(32.dp))
                        }
                        MemorideSecondaryButton("인벤토리", { message = "보조 버튼 클릭 확인" },
                            Modifier.fillMaxWidth(), enabled) {
                            Image(painterResource(R.drawable.memoride_inventory), null, Modifier.size(28.dp))
                        }
                        MemoridePrimaryButton("이동 마무리", {}, Modifier.fillMaxWidth(), enabled = false,
                            supportingText = "비활성 상태 예제")
                        MemorideTextButton("닫기", { message = "글자 버튼 클릭 확인" },
                            Modifier.fillMaxWidth(), enabled)
                    }
                }
                val panel: @Composable (Modifier) -> Unit = { modifier ->
                    MemoridePanel(modifier) {
                        Text("팀별 화면 연결", style = MaterialTheme.typography.titleMedium)
                        Text("B · 캐릭터 선택 / 나의 섬 / 인벤토리")
                        Text("C · 월드맵 / 지역 기록")
                        Text("PM · 결과 / 저장 안내")
                        Text(if (enabled) message else "이용 상태 미확인 · 조작 차단",
                            color = MemorideColors.TextSecondary)
                    }
                }
                if (wide) Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    buttons(Modifier.weight(1f)); panel(Modifier.weight(1f))
                } else {
                    buttons(Modifier.fillMaxWidth()); panel(Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Preview(name = "UX-A01 wide", widthDp = 1280, heightDp = 720)
@Preview(name = "UX-A01 narrow", widthDp = 640, heightDp = 720)
@Preview(name = "UX-A01 portrait", widthDp = 720, heightDp = 1280)
@Preview(name = "UX-A01 phone large text", widthDp = 411, heightDp = 891, fontScale = 2f)
@Composable
private fun FoundationPreview() {
    MemorideTheme { UxFoundationGallery(InteractionGate(GateState.PARKED_ALLOWED)) }
}
