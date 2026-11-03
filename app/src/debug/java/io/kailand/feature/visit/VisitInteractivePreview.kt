package io.kailand.feature.visit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.kailand.core.contract.AvatarPreview
import io.kailand.core.contract.GateState
import io.kailand.core.contract.InteractionGate
import io.kailand.core.ui.MemorideTheme

/** IDE-only interactive harness. Never connected to an Activity or the app's real gate. */
@Preview(name = "C02 직접 체험", widthDp = 420, heightDp = 1000, showBackground = true)
@Composable
fun VisitInteractivePreview() {
    val controller = remember { createVisitController() }
    val previewContext = remember {
        VisitContext(InteractionGate(GateState.PARKED_ALLOWED), foreground = true, screenActive = true)
    }
    val state by controller.state.collectAsState()
    var feedback by remember { mutableStateOf<String?>(null) }
    DisposableEffect(controller) {
        controller.updateContext(previewContext)
        onDispose { controller.leaveScreen() }
    }
    MemorideTheme {
        Column(Modifier.fillMaxSize()) {
            Text("IDE 전용 모의 체험 · 실제 주차/차량 입력 아님", Modifier.padding(8.dp))
            Button(onClick = {
                controller.updateContext(previewContext)
                controller.restartDemo()
                feedback = null
            }, modifier = Modifier.padding(horizontal = 8.dp)) { Text("가상 방문 다시 준비 (진행자)") }
            VisitScreen(
                state = state.copy(visitorAvatar = AvatarPreview("tori", "daejeon_hat")),
                gate = previewContext.gate,
                avatarSlot = { _, _, _ -> Text("임시 외형") },
                onRequest = {
                    feedback = when (val result = controller.request()) {
                        is VisitRequestResult.Created -> null
                        is VisitRequestResult.Rejected -> result.message
                    }
                },
                onAccept = { controller.accept(it) },
                onReject = { controller.reject(it) },
                onWithdraw = { controller.withdraw(it) },
                onClose = { controller.close(it) },
                onBack = { controller.leaveScreen() },
                feedback = feedback,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
