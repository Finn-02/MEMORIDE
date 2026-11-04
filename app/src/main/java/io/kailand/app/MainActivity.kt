package io.kailand.app

import android.app.UiModeManager
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.kailand.BuildConfig
import io.kailand.core.contract.GateState
import io.kailand.core.contract.InteractionGate
import io.kailand.core.platform.developmentGate
import io.kailand.core.ui.MemorideTheme

open class MainActivity : ComponentActivity() {
    protected open val developmentEntry: Boolean = false
    private var gate by mutableStateOf(InteractionGate(GateState.UNKNOWN))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MemorideTheme { AppContent(gate) }
        }
    }

    @Composable
    protected open fun AppContent(gate: InteractionGate) {
        AppRoot(gate, developmentEntry)
    }

    override fun onResume() {
        super.onResume()
        updateGate(foreground = true)
    }

    override fun onPause() {
        updateGate(foreground = false)
        super.onPause()
    }

    private fun updateGate(foreground: Boolean) {
        gate = developmentGate(
            debugBuild = BuildConfig.DEBUG,
            developmentEntry = developmentEntry,
            automotiveDevice = packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE),
            carUiMode = getSystemService(UiModeManager::class.java).currentModeType ==
                Configuration.UI_MODE_TYPE_CAR,
            foreground = foreground,
        )
    }
}
