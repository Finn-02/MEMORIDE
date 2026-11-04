package io.kailand.app

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.data.DestinationArrivedInfo
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import io.kailand.core.contract.DestinationSpec
import io.kailand.core.contract.GateState
import io.kailand.core.contract.InteractionGate
import io.kailand.core.platform.VehicleGateMonitor
import io.kailand.core.contract.JourneyKind
import io.kailand.core.contract.RequestResult
import io.kailand.core.ui.MemorideTheme
import io.kailand.app.journey.ManualArrivalController
import io.kailand.app.journey.ManualJourneyConfirmationPanel
import io.kailand.app.journey.ManualReviewReason
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.math.abs

/** Explicit debug probe. SDK events are observations, never game arrivals or rewards. */
class PleosProbeActivity : ComponentActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var output: TextView
    private lateinit var status: TextView
    private lateinit var commands: LinearLayout
    private val buttons = mutableListOf<Button>()
    private val events = ArrayDeque<String>()
    private var helper: NaviHelper? = null
    private var listener: NaviHelperEventListener? = null
    private var monitor: VehicleGateMonitor? = null
    private var gate = InteractionGate(GateState.UNKNOWN)
    private var foreground = false
    private var connected = false
    private var destroyed = false
    private var generation = 0L
    private var operation = 0L
    private var expected: DestinationSpec? = null
    private var destinations = emptyList<DestinationSpec>()
    private var manual: ManualArrivalController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 24, 24, 24) }
        status = TextView(this).apply { textSize = 17f }
        commands = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        output = TextView(this).apply { textSize = 15f }
        column.addView(status)
        column.addView(commands)
        val manualView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }
        column.addView(manualView)
        column.addView(ScrollView(this).apply { addView(output) }, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(column)
        // Explicitly finish the probe so Back releases its SDK, including when this is a root task.
        onBackPressedDispatcher.addCallback(this) { finish() }
        record("A03 NaviHelper 진단 · 게임 도착/저장/보상 연결 없음")
        if (Build.VERSION.SDK_INT < 29 || !packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) {
            record("BLOCKED: Automotive API29+ required")
            return
        }
        monitor = VehicleGateMonitor(this) { gate = it; updateControls() }
        lifecycleScope.launch {
            destinations = withContext(Dispatchers.IO) {
                val regions = JSONObject(CatalogAssets(assets).readCatalogJson()).getJSONArray("regions")
                (0 until regions.length()).map { i ->
                    val r = regions.getJSONObject(i)
                    DestinationSpec(r.getString("id"), r.getString("name"), r.getDouble("latitude"), r.getDouble("longitude"))
                }
            }
            val controller = ViewModelProvider(this@PleosProbeActivity)[ManualJourneyProbeModel::class.java]
                .controller(destinations.mapNotNull { it.regionId }.toSet())
            manual = controller
            manualView.setContent { MemorideTheme { ManualJourneyConfirmationPanel(controller, destinations) } }
            updateControls()
            destinations.forEach { target ->
                button("${target.label} 목적지 요청") {
                    runCommand("requestRoute:${target.regionId}") { sdk ->
                        val selected = controller.begin(target, JourneyKind.REGION)
                        if (selected is RequestResult.Failed) {
                            record("REJECTED: ${selected.message}")
                            return@runCommand
                        }
                        expected = target
                        sdk.requestRoute(RouteInfo(
                            latitude = target.latitude, longitude = target.longitude,
                            poiName = target.label, poiId = "", address = "", poiSubId = "0",
                        ))
                    }
                }
            }
            button("현재 목적지 조회") { runCommand("getDestinationInfo") { it.getDestinationInfo() } }
            button("내비 경로 취소") { runCommand("cancelRoute") { it.cancelRoute() } }
            initializeSdk()
        }
    }

    private fun button(label: String, action: () -> Unit) {
        val button = Button(this).apply { text = label; setOnClickListener { action() } }
        buttons += button
        commands.addView(button, LinearLayout.LayoutParams(-1, (56 * resources.displayMetrics.density).toInt()))
        updateControls()
    }

    private fun initializeSdk() {
        val token = ++generation
        val sdk = NaviHelper(applicationContext)
        helper = sdk
        fun post(action: () -> Unit) {
            handler.post { if (!destroyed && generation == token && helper === sdk) action() }
        }
        val eventsListener = object : NaviHelperEventListener {
            override fun onDestinationInfo(destinationInfo: DestinationInfo) = post {
                val matches = destinations.filter { abs(it.latitude - destinationInfo.latitude) < 0.00001 && abs(it.longitude - destinationInfo.longitude) < 0.00001 }
                val selectedMatch = expected?.let { target ->
                    abs(target.latitude - destinationInfo.latitude) < 0.00001 && abs(target.longitude - destinationInfo.longitude) < 0.00001
                }
                record("CALLBACK onDestinationInfo: catalog=${matches.map { it.regionId }} selectedMatch=$selectedMatch (payload omitted)")
            }
            override fun onRouteStarted(routeStartInfo: RouteStartInfo) = post { record("CALLBACK onRouteStarted: route search only (payload omitted)") }
            override fun onRoutingCompleted() = post { record("CALLBACK onRoutingCompleted: not arrival") }
            override fun onDestinationChanged() = post {
                manual?.navigationDestinationChanged()
                record("CALLBACK onDestinationChanged: not arrival")
            }
            override fun onRouteCancelled() = post {
                manual?.requestReview(ManualReviewReason.NAVIGATION_ENDED)
                record("CALLBACK onRouteCancelled: user review hint only")
            }
            override fun onRouteEnded() = post {
                manual?.requestReview(ManualReviewReason.NAVIGATION_ENDED)
                record("CALLBACK onRouteEnded: user review hint only")
            }
            override fun onDestinationArrived(destinationArrivedInfo: DestinationArrivedInfo) = post {
                manual?.requestReview(ManualReviewReason.NAVIGATION_ARRIVAL)
                record("CALLBACK onDestinationArrived: UNVERIFIED identity; no game command (payload omitted)")
            }
            override fun onError(errorCode: NaviErrorCode) = post { record("SDK_ERROR: $errorCode") }
        }
        listener = eventsListener
        record("INITIALIZING: NaviHelper3.0.5")
        try {
            sdk.initialize(onConnected = { post {
                try {
                    sdk.addListener(eventsListener)
                    connected = true
                    record("CONNECTED: NaviHelper service")
                    updateControls()
                } catch (error: Exception) { record("LISTENER_FAILED: ${error.javaClass.simpleName}") }
            } }, onFailed = { error -> post { record("CONNECT_FAILED: ${error.javaClass.simpleName}") } })
        } catch (error: Exception) { record("INIT_FAILED: ${error.javaClass.simpleName}") }
    }

    private fun runCommand(name: String, command: (NaviHelper) -> Unit) {
        val sdk = helper
        if (!foreground || !gate.canInteract || !connected || sdk == null) {
            record("REJECTED: foreground/platform gate/SDK not ready")
            return
        }
        val sequence = ++operation
        record("REQUEST #$sequence: $name · callbacks have no request ID")
        try {
            command(sdk)
            record("RETURN #$sequence: invocation returned; outcome not confirmed")
            val token = generation
            handler.postDelayed({
                if (!destroyed && token == generation && sequence == operation) {
                    record("OBSERVATION #$sequence: 15s window ended; judge only observed callbacks")
                }
            }, 15_000)
        } catch (error: Exception) { record("CALL_FAILED #$sequence: ${error.javaClass.simpleName}") }
    }

    override fun onResume() {
        super.onResume()
        foreground = true
        monitor?.start()
        updateControls()
    }

    override fun onPause() {
        foreground = false
        monitor?.stop()
        updateControls()
        super.onPause()
    }

    override fun onDestroy() {
        destroyed = true
        generation++
        connected = false
        handler.removeCallbacksAndMessages(null)
        monitor?.stop()
        val sdk = helper
        helper = null
        val removed = runCatching { listener?.let { sdk?.removeListener(it) } }.isSuccess
        val released = runCatching { sdk?.release() }.isSuccess
        listener = null
        Log.i("MemoridePleosProbe", "CLEANUP: listenerRemoved=$removed sdkReleased=$released")
        super.onDestroy()
    }

    private fun updateControls() {
        val enabled = foreground && connected && gate.canInteract
        manual?.setInteractionAllowed(foreground && gate.canInteract)
        buttons.forEach { it.isEnabled = enabled }
        status.text = "A03 SDK 진단 · gate=${gate.state} · SDK=$connected · foreground=$foreground"
    }

    private fun record(event: String) {
        events.addLast(event)
        while (events.size > 30) events.removeFirst()
        output.text = events.joinToString("\n\n")
        Log.i("MemoridePleosProbe", event)
    }
}

/** Keeps only the user-selected in-memory journey across configuration changes. */
class ManualJourneyProbeModel : ViewModel() {
    private var current: ManualArrivalController? = null
    fun controller(regionIds: Set<String>): ManualArrivalController =
        current ?: ManualArrivalController(regionIds).also { current = it }
}
