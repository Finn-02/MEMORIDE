package io.kailand.app.journey

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.data.DestinationArrivedInfo
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

fun createNavigationCapture(context: Context, scope: CoroutineScope, inbox: NavigationInbox): NavigationCapture =
    DebugNavigationCapture(context.applicationContext, scope, inbox)

/** Kept alive across Activity pause; process death is not a guaranteed background service. */
private class DebugNavigationCapture(
    private val context: Context, private val scope: CoroutineScope, private val inbox: NavigationInbox,
) : NavigationCapture {
    private val mutable = MutableStateFlow("내비 연결 대기 · 개발 빌드")
    override val status = mutable.asStateFlow()
    private val handler = Handler(Looper.getMainLooper())
    private var helper: NaviHelper? = null
    private var listener: NaviHelperEventListener? = null
    private var generation = 0L
    private var epoch = 0L
    private var inFlight: Long? = null
    private var again = false
    private var connected = false
    private var starting = false

    override fun start() {
        if (starting || helper != null) return
        if (Build.VERSION.SDK_INT < 29 || !context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) {
            mutable.value = "Pleos 내비 연결은 차량 에뮬레이터에서 확인해 주세요."
            return
        }
        if (context.checkSelfPermission("pleos.car.permission.NAVI_ROUTE") != PackageManager.PERMISSION_GRANTED) {
            mutable.value = "내비 목적지 조회 권한을 확인해 주세요."
            return
        }
        starting = true
        val token = generation
        scope.launch {
            val loaded = inbox.state.first { it.ready || it.error != null }
            if (token != generation) return@launch
            starting = false
            if (!loaded.ready) { mutable.value = "저장된 목적지를 읽지 못해 연결을 보류합니다."; return@launch }
            try {
                val sdk = NaviHelper(context)
                helper = sdk
                fun event(action: suspend () -> Unit) { handler.post {
                    if (token == generation && helper === sdk) {
                        inbox.beginObservation()
                        scope.launch {
                            try { if (token == generation && helper === sdk) action() }
                            finally { inbox.endObservation() }
                        }
                    }
                } }
                val events = object : NaviHelperEventListener {
                    override fun onDestinationInfo(destinationInfo: DestinationInfo) = event {
                        val requested = inFlight
                        inFlight = null
                        if (requested != null && requested != epoch) {
                            again = true // Route changed while a query was in flight; discard that response.
                        } else {
                            inbox.observe(destinationInfo.name.orEmpty(), destinationInfo.latitude, destinationInfo.longitude)
                            Log.i("MemorideNavigation", "destination observed; status=${inbox.state.value.draft?.status}")
                        }
                        if (again) { again = false; query() }
                    }
                    override fun onDestinationChanged() = event { changed() }
                    override fun onRouteStarted(routeStartInfo: RouteStartInfo) = event { changed() }
                    // SDK 3.0.5: route calculation completion, NEVER arrival.
                    override fun onRouteEnded() = event { query() }
                    override fun onRoutingCompleted() = event { query() }
                    override fun onRouteCancelled() = event {
                        epoch++; inbox.cancelled()
                        Log.i("MemorideNavigation", "navigation cancelled; no game command")
                    }
                    override fun onDestinationArrived(destinationArrivedInfo: DestinationArrivedInfo) = event {
                        epoch++; inbox.arrivalHint() // Only a user review hint, never PLEOS arrival evidence.
                    }
                    override fun onError(errorCode: NaviErrorCode) = event {
                        inFlight = null; again = false
                        mutable.value = "내비 조회 오류 · 다시 조회해 주세요."
                    }
                }
                listener = events
                sdk.initialize(onConnected = { event {
                    sdk.addListener(events); connected = true
                    mutable.value = "PLEOS 목적지 연결 · 도착은 사용자 확인"
                    query()
                } }, onFailed = { event {
                    mutable.value = "내비에 연결하지 못했어요. 다시 조회해 주세요."
                    helper = null; listener = null; connected = false
                    runCatching { sdk.release() }
                } })
            } catch (_: Exception) {
                mutable.value = "내비 연결을 시작하지 못했어요."
                helper?.let { runCatching { it.release() } }
                helper = null; listener = null; connected = false
            }
        }
    }

    private suspend fun changed() { epoch++; inbox.changing(); query() }
    private fun query() {
        if (!connected) return
        if (inFlight != null) { again = true; return }
        inFlight = epoch
        try { helper?.getDestinationInfo() } catch (_: Exception) {
            inFlight = null; mutable.value = "내비 목적지를 조회하지 못했어요."
        }
    }
    override fun refresh() { if (helper == null) start() else query() }
    override suspend fun reset() {
        generation++; epoch++; starting = false; connected = false; inFlight = null; again = false
        helper?.let { sdk ->
            runCatching { listener?.let { sdk.removeListener(it) } }
            runCatching { sdk.release() }
        }
        helper = null; listener = null
        inbox.clear() // A failure propagates and prevents the game reset.
        mutable.value = "내비 목적지 초기화됨 · 다시 조회해 주세요."
    }
}
