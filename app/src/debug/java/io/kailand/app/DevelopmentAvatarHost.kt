package io.kailand.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.kailand.core.contract.InteractionGate
import io.kailand.feature.island.AvatarRuntimeGalleryScreen

/** A owns debug permission and routing; B owns the read-only rendering gallery. */
@Composable
internal fun DevelopmentAvatarHost(
    gate: InteractionGate,
    onBack: () -> Unit,
) {
    Surface(Modifier.fillMaxSize()) {
        if (!gate.canInteract) {
            Column(Modifier.padding(24.dp)) {
                Text("캐릭터 개발 화면을 사용할 수 없습니다.")
                Text(gate.reason ?: "이용 상태 미확인")
            }
        } else {
            // Leaving this branch disposes the gallery and its temporary animation/loading state.
            AvatarRuntimeGalleryScreen(onBack = onBack, modifier = Modifier.fillMaxSize())
        }
    }
}
