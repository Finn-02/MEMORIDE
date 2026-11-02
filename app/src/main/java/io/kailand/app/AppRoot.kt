package io.kailand.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.kailand.core.contract.AvatarPreview
import io.kailand.core.contract.InteractionGate
import io.kailand.core.ui.AvatarSlot

private enum class BootstrapRoute(val label: String, val owner: String) {
    HOME("메인 섬", "B03"), JOURNEY("여정 준비", "A02"),
    COMPANION("동행 모의 화면", "C01"), WORLD("월드맵", "C03"),
    BRIEFING("도착 브리핑", "P01"), DEMO("시연 제어", "C04"),
}

/** A-owned placeholders. Each owner replaces its screen at this composition boundary. */
@Composable
fun AppRoot(gate: InteractionGate, developmentEntry: Boolean) {
    var route by rememberSaveable { mutableStateOf(BootstrapRoute.HOME.name) }
    val selected = BootstrapRoute.valueOf(route)
    BackHandler(enabled = route != BootstrapRoute.HOME.name) { route = BootstrapRoute.HOME.name }
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("MEMORIDE", style = MaterialTheme.typography.headlineLarge)
            Text("Ride Becomes a Memory")
            if (developmentEntry) Text("DEMO · A01 개발 샘플 · 게임 저장/보상 없음")
            if (!gate.canInteract) {
                Text("이용 상태 미확인", style = MaterialTheme.typography.titleLarge)
                Text(gate.reason ?: "이용 가능 상태를 확인하고 있습니다.")
                Text("A03에서 실제 플랫폼 입력을 연결합니다.")
            } else {
                Text(selected.label, style = MaterialTheme.typography.titleLarge)
                val avatarSlot: AvatarSlot = { avatar, _, modifier ->
                    Box(
                        modifier.background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) { Text("임시 외형\n${avatar.characterId}") }
                }
                avatarSlot(AvatarPreview("tori", null), "idle", Modifier.size(128.dp))
                Text("${selected.owner} 담당 화면의 연결 자리입니다. 실제 기능은 아직 없습니다.")
                BootstrapRoute.entries.filter { it != selected }.forEach { destination ->
                    Button(
                        onClick = {
                            if (gate.canInteract) route = destination.name
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    ) { Text(destination.label) }
                }
            }
        }
    }
}
