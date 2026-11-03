package io.kailand.feature.companion

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.kailand.core.contract.AvatarPreview
import io.kailand.core.contract.DataSource
import io.kailand.core.contract.DestinationSpec
import io.kailand.core.contract.GateState
import io.kailand.core.contract.InteractionGate
import io.kailand.core.contract.JourneyKind
import io.kailand.core.contract.JourneyPhase
import io.kailand.core.contract.JourneySnapshot
import io.kailand.core.ui.MemorideTheme

// Debug Preview only: never installs sample state, sample gate, or fixture IDs into AppRoot.
private val previewJourney = JourneySnapshot(
    journeyId = "preview-only",
    source = DataSource.DEMO,
    kind = JourneyKind.REGION,
    phase = JourneyPhase.TRAVELLING,
    routeRevision = 0,
    destination = DestinationSpec("daejeon", "대전", 36.35, 127.38),
    elapsedSeconds = 1200,
    distanceMeters = 5000.0,
)

@Preview(name = "C01 DEMO 세로", widthDp = 360, heightDp = 800, showBackground = true)
@Preview(name = "C01 DEMO 가로", widthDp = 800, heightDp = 480, showBackground = true)
@Preview(name = "C01 DEMO 글자 1.3배", widthDp = 320, heightDp = 800, fontScale = 1.3f, showBackground = true)
@Composable
fun CompanionDemoPreview() {
    PreviewContent(InteractionGate(GateState.PARKED_ALLOWED))
}

@Preview(name = "C01 UNKNOWN 차단", widthDp = 360, heightDp = 640, showBackground = true)
@Composable
fun CompanionBlockedPreview() {
    PreviewContent(InteractionGate(GateState.UNKNOWN))
}

@Composable
private fun PreviewContent(gate: InteractionGate) {
    MemorideTheme {
        CompanionContent(
            journey = previewJourney,
            avatar = AvatarPreview("tori", "daejeon_hat"),
            gate = gate,
            avatarSlot = { _, _, _ -> Text("임시 외형") },
            foreground = true,
            onBack = {},
            onHideChanged = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
