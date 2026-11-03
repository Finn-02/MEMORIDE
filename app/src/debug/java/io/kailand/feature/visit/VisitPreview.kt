package io.kailand.feature.visit

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.kailand.core.contract.AvatarPreview
import io.kailand.core.contract.GateState
import io.kailand.core.contract.InteractionGate
import io.kailand.core.ui.MemorideTheme

// Static debug-only scenes: no controller, scheduler, live gate override, or saved game data.
private val previewRequest = VisitRequest(
    VisitToken("visit-preview-only", 0), VisitController.SENDER_ID, VisitController.TARGET_ID,
    VisitController.PURPOSE, VisitController.MESSAGE_ID, 0, 15_000,
)

@Preview(name = "C02 응답 대기", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun VisitRequestedPreview() = VisitExample(VisitPhase.REQUESTED)

@Preview(name = "C02 수락 후 방문", widthDp = 800, heightDp = 480, showBackground = true)
@Preview(name = "C02 큰 글자", widthDp = 320, heightDp = 800, fontScale = 1.3f, showBackground = true)
@Composable
fun VisitAcceptedPreview() = VisitExample(VisitPhase.VISITING)

@Preview(name = "C02 UNKNOWN", widthDp = 360, heightDp = 640, showBackground = true)
@Composable
fun VisitBlockedPreview() = VisitExample(VisitPhase.VISITING, GateState.UNKNOWN)

@Composable
private fun VisitExample(phase: VisitPhase, gateState: GateState = GateState.PARKED_ALLOWED) {
    MemorideTheme {
        VisitScreen(
            state = VisitState(
                phase = phase, request = previewRequest, interactionAllowed = true,
                remainingResponseMs = if (phase == VisitPhase.REQUESTED) 12_000 else null,
                visitorAvatar = AvatarPreview("tori", "daejeon_hat"),
            ),
            gate = InteractionGate(gateState),
            avatarSlot = { _, _, _ -> Text("임시 외형") },
            onRequest = {}, onAccept = {}, onReject = {}, onWithdraw = {}, onClose = {}, onBack = {},
        )
    }
}
