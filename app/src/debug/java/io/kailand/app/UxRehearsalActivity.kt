package io.kailand.app

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import io.kailand.app.journey.*
import io.kailand.core.contract.*
import io.kailand.core.ui.MemorideSecondaryButton
import io.kailand.core.ui.MemorideScreen
import io.kailand.feature.demo.*
import io.kailand.feature.island.*
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/** Real MainActivity vehicle gate, with isolated debug stores and no platform navigation observer. */
class UxRehearsalActivity : MainActivity() {
    private val rehearsal by lazy { ViewModelProvider(this)[UxRehearsalModel::class.java] }
    override val observePlatformNavigation = false
    override val gameSession: GameAppSession get() = rehearsal.run.app

    @Composable
    override fun AppContent(gate: InteractionGate) {
        val run = rehearsal.run
        val catalog by run.app.catalog.collectAsState()
        val holder = rememberSaveableStateHolder()
        val ready = catalog as? CatalogLoadState.Ready
        fun allowed() = run === rehearsal.run && run.app.canUse(gameOwner) &&
            lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        Column(Modifier.fillMaxSize()) {
            Text("DEMO 리허설 · 별도 시험 저장 · 목적지는 합성 표본 / 도착 입력은 사용자 확인",
                Modifier.padding(12.dp))
            if (rehearsal.showApp) {
                MemorideSecondaryButton("시연 제어로 돌아가기", {
                    if (allowed() && !run.probe.writing && !run.navigation.busy.value && !run.arrivalBusy())
                        rehearsal.showApp = false
                }, enabled = gate.canInteract && !run.probe.writing)
                Box(Modifier.weight(1f)) {
                    GameAppRoot(gate, run.app, gameOwner, false, run.navigation,
                        launchNavigation = { run.launch(::allowed) })
                }
            } else if (ready != null) {
                MemorideScreen(Modifier.weight(1f)) {
                    holder.SaveableStateProvider("c03-panel") {
                        UxDemoPanel(ready.catalog, gate, developmentEnvironment = true,
                            isolatedEnvironment = run.isolated,
                            onPrepare = { request -> rehearsal.prepare(request, gameOwner, gate, ::allowed) },
                            onOpenApp = { request, session ->
                                val current = rehearsal.run
                                if (!current.app.canUse(gameOwner) ||
                                    !lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) ||
                                    !current.matches(request, session) ||
                                    checkUxDemoPreparation(request, current.evidence(), ready.catalog).verdict != UxDemoVerdict.PASS ||
                                    current.evidence()?.finished == true) RequestResult.Failed("현재 시험 환경과 준비 결과를 다시 확인해 주세요.")
                                else { rehearsal.showApp = true; RequestResult.Accepted }
                            }, readEvidence = { rehearsal.run.evidence() }, onClose = { if (allowed()) finish() })
                    }
                }
            } else Text("별도 리허설 콘텐츠를 준비하고 있어요.")
        }
    }
}

class UxRehearsalModel(application: Application) : AndroidViewModel(application) {
    private val runs = mutableListOf<RehearsalRun>()
    internal var run by mutableStateOf(newRun())
        private set
    var showApp by mutableStateOf(false)
    private var preparing = false
    private fun newRun() = RehearsalRun(getApplication()).also(runs::add)

    internal suspend fun prepare(
        request: UxDemoRequest, owner: Any, gate: InteractionGate, caller: () -> Boolean,
    ): RequestResult {
        if (showApp || preparing || !caller() || request.requestToken.isBlank() || run.probe.writing || run.arrivalBusy())
            return RequestResult.Failed("현재 이용 상태와 진행 중인 저장을 다시 확인해 주세요.")
        preparing = true
        val old = run
        val oldGeneration = old.app.commands.session.value.generation
        val next = newRun()
        try {
            val readiness = next.app.game.readiness.first { it != GameReadiness.LOADING }
            val catalog = next.app.catalog.first { it !is CatalogLoadState.Loading } as? CatalogLoadState.Ready
            if (!caller() || old !== run || old.app.commands.session.value.generation != oldGeneration ||
                readiness != GameReadiness.READY || catalog == null)
                return RequestResult.Failed("시험 준비를 완료하지 못했어요. 다시 확인해 주세요.")
            if (request.preset == UxDemoPreset.REVISIT) {
                // Prepare only through B's real REGION boundary. Never add a DAILY grant to fake the new rule.
                val seed = ArrivalCommand(UUID.randomUUID().toString(), DataSource.DEMO, JourneyKind.REGION,
                    "wonju", 0, ArrivalEvidence.DEMO_FINAL_DESTINATION, System.currentTimeMillis())
                val result = next.app.game.completeArrival(seed)
                if (result !is ArrivalResult.Applied || next.app.game.acknowledgeReceipt(seed.journeyId) != MutationResult.Success)
                    return RequestResult.Failed("B 저장 경계에서 재방문 준비를 완료하지 못했어요.")
            }
            if (!caller() || old !== run || old.app.commands.session.value.generation != oldGeneration)
                return RequestResult.Failed("화면이 바뀌어 준비를 중단했어요.")
            old.app.updateHost(owner, InteractionGate(GateState.UNKNOWN), false, DataSource.USER_CONFIRMED)
            next.app.updateHost(owner, gate, true, DataSource.USER_CONFIRMED)
            next.initialize(request)
            run = next
            return RequestResult.Accepted
        } finally { preparing = false }
    }

    override fun onCleared() { runs.forEach { it.scope.cancel() } }
}

internal class RehearsalRun(application: Application) {
    val id = UUID.randomUUID().toString()
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val root = File(application.noBackupFilesDir, "ux-c03").canonicalFile
    private val file = File(root, "$id/game.preferences_pb").canonicalFile
    val isolated = file.toPath().startsWith(root.toPath()) && file.name == "game.preferences_pb"
    val probe = RehearsalSaveProbe(PreferencesGameSaveStore(
        PreferenceDataStoreFactory.create(scope = scope) { file }))
    private val inbox = NavigationInbox(object : NavigationDraftStore {
        private var draft: NavigationDraft? = null
        override suspend fun read() = draft
        override suspend fun write(draft: NavigationDraft?) { this.draft = draft }
    }, scope)
    private val capture = object : NavigationCapture {
        override val status = MutableStateFlow("DEMO · 별도 시험 목적지")
        override fun start() = Unit
        override fun refresh() = Unit
        override suspend fun reset() { inbox.clear() }
    }
    private val attempts = mutableListOf<ArrivalCommand>()
    val app = GameAppSession(application,
        suppliedGame = BGameService(probe, scope) {
            withContext(Dispatchers.IO) {
                application.assets.open("content/catalog.json").bufferedReader(Charsets.UTF_8)
                    .use { RegionRewards.fromJson(it.readText()) }
            }
        }, suppliedInbox = inbox, suppliedCapture = capture,
        onManualAttempt = { attempts += it }, scope = scope)
    val navigation = ProductNavigation()
    private var request: UxDemoRequest? = null
    private var preparedGeneration = -1L
    private var prepared: GameSnapshot? = null
    private var visitsBefore = 0
    private var growthUsed = false
    private var seedIds = emptySet<String>()
    private var before: GameSnapshot? = null
    private var navFailure: UxDemoNavigationFailure? = null
    private var recovery = UxDemoNavigationRecovery.NONE
    private var navigationRetried = false

    init {
        check(isolated)
        scope.launch {
            navigation.state.collect {
                if (navFailure != null && it.route == ProductRoute.ISLAND && !navigationRetried)
                    recovery = UxDemoNavigationRecovery.RETURNED_TO_ISLAND
            }
        }
    }

    fun initialize(value: UxDemoRequest) {
        request = value
        preparedGeneration = app.commands.session.value.generation
        prepared = app.game.state.value
        visitsBefore = visits()
        seedIds = probe.committed.processedReceipts.keys.toSet()
        val today = Instant.ofEpochMilli(System.currentTimeMillis()).atZone(ZoneId.of("Asia/Seoul")).toLocalDate().toString()
        growthUsed = probe.committed.lastDailyGrantDate == today ||
            probe.committed.processedReceipts.values.any {
                it.growthAfter > it.growthBefore && Instant.ofEpochMilli(it.completedAtEpochMs)
                    .atZone(ZoneId.of("Asia/Seoul")).toLocalDate().toString() == today
            }
        probe.failNextArrival = value.preset == UxDemoPreset.SAVE_RETRY
    }

    fun matches(value: UxDemoRequest, session: String) = request == value && session == id && isolated &&
        app.commands.session.value == GameCommandSession(preparedGeneration)
    fun arrivalBusy() = attempts.isNotEmpty() && probe.writing

    suspend fun launch(allowed: () -> Boolean): RequestResult {
        val value = request ?: return RequestResult.Failed("시험 상태를 준비해 주세요.")
        if (!allowed() || !isolated) return RequestResult.Failed("현재 시험 환경과 이용 상태를 다시 확인해 주세요.")
        if (before == null) before = app.game.state.value
        if (value.preset == UxDemoPreset.NAVIGATION_FAILURE && navFailure == null) {
            navFailure = UxDemoNavigationFailure(app.game.state.value, false, false, null)
            navigationRetried = false
            return RequestResult.Failed("DEMO · 내비 실행 실패 1회")
        }
        // A synthetic candidate only. This never launches or claims success of a real external navigation app.
        val catalog = (app.catalog.value as? CatalogLoadState.Ready)?.catalog
            ?: return RequestResult.Failed("콘텐츠를 다시 확인해 주세요.")
        val wonju = catalog.regions.single { it.id == "wonju" }
        inbox.changing()
        inbox.observe("DEMO 리허설 · 원주", wonju.latitude, wonju.longitude)
        navigationRetried = navFailure != null
        return RequestResult.Accepted
    }

    private fun visits() = probe.committed.processedReceipts.values.count {
        it.kind == JourneyKind.REGION.name && it.regionId == "wonju"
    }

    fun evidence(): UxDemoEvidence? {
        val value = request ?: return null
        val initial = prepared ?: return null
        val receipt = probe.committed.processedReceipts.filterKeys { it !in seedIds }.values
            .singleOrNull()?.toContract()
        val observedRecovery = if (navFailure != null && receipt != null)
            UxDemoNavigationRecovery.RETRIED_SUCCESSFULLY else recovery
        val finished = receipt != null || observedRecovery == UxDemoNavigationRecovery.RETURNED_TO_ISLAND
        return UxDemoEvidence(value, id, isolated, DataSource.USER_CONFIRMED, initial, visitsBefore, growthUsed,
            beforeAttempt = before, afterAttempt = if (finished) app.game.state.value else null,
            visitsAfter = if (finished) visits() else null, receipt = receipt,
            saveFailure = probe.confirmedFailure?.let { failed -> attempts.firstOrNull()?.let { original ->
                UxDemoSaveFailure(original, attempts.drop(1).firstOrNull(), failed)
            } }, navigationFailure = navFailure, navigationRecovery = observedRecovery, finished = finished)
    }
}
