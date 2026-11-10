package io.kailand.feature.world

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.compose.runtime.Composable
import io.kailand.app.MainActivity
import io.kailand.core.contract.InteractionGate

/** Screenshot-only fixture entry. MainActivity blocks developmentEntry on Automotive.
 * No GameService, live saves, travel commands, or real parking emulation.
 */
class WorldMapSampleActivity : MainActivity() {
    override val developmentEntry: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra("wide", false)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    @Composable
    override fun AppContent(gate: InteractionGate) {
        val sample = intent.getStringExtra("sample")?.takeIf { it in setOf("regional", "wonju") } ?: "fresh"
        WorldExample(sample, gate.state, newUi = true,
            historySample = intent.getStringExtra("history") ?: "available",
            showDetail = intent.getBooleanExtra("selected", true), onClose = { finish() })
    }
}
