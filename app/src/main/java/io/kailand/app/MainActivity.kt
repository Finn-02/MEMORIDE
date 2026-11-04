package io.kailand.app

import android.app.UiModeManager
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.kailand.BuildConfig
import io.kailand.core.contract.GateState
import io.kailand.core.contract.InteractionGate
import io.kailand.core.contract.DataSource
import io.kailand.core.platform.developmentGate
import io.kailand.core.platform.VehicleGateMonitor
import io.kailand.core.ui.MemorideTheme

open class MainActivity : ComponentActivity() {
    protected open val developmentEntry: Boolean = false
    private var gate by mutableStateOf(InteractionGate(GateState.UNKNOWN))
    private var vehicleGateMonitor: VehicleGateMonitor? = null
    private var foreground = false
    protected val gameOwner = Any()
    protected val gameSession: GameAppSession
        get() = (application as MemorideApplication).gameSession

    private fun publishGameContext() {
        gameSession.updateHost(gameOwner, gate, foreground,
            if (developmentEntry) DataSource.DEMO else DataSource.PLEOS)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!developmentEntry && Build.VERSION.SDK_INT >= 29 &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) {
            vehicleGateMonitor = VehicleGateMonitor(this) {
                gate = it
                publishGameContext()
                if (BuildConfig.DEBUG) Log.i("MemorideVehicleGate", "gate=${it.state}")
            }
        }
        setContent {
            MemorideTheme { AppContent(gate) }
        }
    }

    @Composable
    protected open fun AppContent(gate: InteractionGate) {
        GameAppRoot(gate, gameSession, gameOwner, developmentEntry)
    }

    override fun onResume() {
        super.onResume()
        foreground = true
        publishGameContext()
        val monitor = vehicleGateMonitor
        if (monitor != null) monitor.start() else updateGate(foreground = true)
    }

    override fun onPause() {
        foreground = false
        vehicleGateMonitor?.stop()
        updateGate(foreground = false)
        super.onPause()
    }

    override fun onDestroy() {
        vehicleGateMonitor?.stop()
        vehicleGateMonitor = null
        super.onDestroy()
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
        publishGameContext()
    }
}
