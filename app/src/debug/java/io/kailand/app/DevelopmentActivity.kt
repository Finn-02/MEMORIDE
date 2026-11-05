package io.kailand.app

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import io.kailand.core.contract.InteractionGate

/** Explicit debug-only launcher; unavailable in release and blocked on Automotive. */
class DevelopmentActivity : MainActivity() {
    override val developmentEntry: Boolean = true

    @Composable
    override fun AppContent(gate: InteractionGate) {
        val model = remember { ViewModelProvider(this)[DevelopmentJourneyModel::class.java] }
        var avatarPreview by rememberSaveable { mutableStateOf(false) }
        if (avatarPreview) {
            BackHandler { avatarPreview = false }
            DevelopmentAvatarHost(gate, onBack = { avatarPreview = false })
        } else {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                if (gate.canInteract) {
                    Button(onClick = { if (gate.canInteract) avatarPreview = true }) {
                        Text("B04 캐릭터 이미지 확인")
                    }
                    Button(onClick = {
                        if (gate.canInteract) startActivity(Intent(this@DevelopmentActivity, DevelopmentGameActivity::class.java))
                    }) { Text("A04 실제 저장 화면 확인") }
                }
                Box(Modifier.weight(1f)) {
                    DevelopmentJourneyRoot(gate, model)
                }
            }
        }
    }
}
