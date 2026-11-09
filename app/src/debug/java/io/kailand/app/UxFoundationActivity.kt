package io.kailand.app

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import io.kailand.core.contract.GateState
import io.kailand.core.contract.InteractionGate
import io.kailand.core.platform.VehicleGateMonitor
import io.kailand.core.ui.*

/** Debug-only, real vehicle gate. No game service, navigation observer or progress commands.
 * Unlike the generic development entry, this never supplies a fake PARKED_ALLOWED state.
 */
class UxFoundationActivity : ComponentActivity() {
    private var gate by mutableStateOf(InteractionGate(GateState.UNKNOWN))
    private var resumed by mutableStateOf(false)
    private var monitor: VehicleGateMonitor? = null
    private val measurements by lazy { UxWindowMeasurements(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 29 &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) {
            monitor = VehicleGateMonitor(this) { input ->
                gate = if (resumed) input else InteractionGate(GateState.UNKNOWN)
                Log.i("MemorideUxPreview", "gate=${gate.state}")
            }
        }
        setContent {
            MemorideTheme {
                // Gate transitions dispose the visible UI. Keep restorable state outside that
                // branch so resize/pause never requires keeping a restricted UI alive.
                val screenState = rememberSaveableStateHolder()
                if (resumed && gate.canInteract) {
                    screenState.SaveableStateProvider("foundation") {
                        UxFoundationGallery(gate,
                            onClose = { if (resumed && gate.canInteract) finish() },
                            onViewportMeasured = measurements::viewportMeasured)
                    }
                } else {
                    MemorideScreen {
                        Column(Modifier.padding(MemorideDimens.ScreenPadding)) {
                            Text("공통 UI 확인 대기")
                            Text("주차 및 플랫폼 이용 허용 상태가 확인되면 표시합니다.")
                            Text(gate.reason ?: "이용 상태 미확인")
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumed = true
        gate = InteractionGate(GateState.UNKNOWN)
        monitor?.start()
        measurements.start()
    }

    override fun onPause() {
        resumed = false
        gate = InteractionGate(GateState.UNKNOWN)
        monitor?.stop()
        measurements.stop()
        super.onPause()
    }

    override fun onDestroy() {
        monitor?.stop()
        monitor = null
        super.onDestroy()
    }
}
