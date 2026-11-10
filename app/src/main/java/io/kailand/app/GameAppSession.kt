package io.kailand.app

import android.content.Context
import android.os.SystemClock
import io.kailand.core.contract.*
import io.kailand.app.journey.GuardedDemoJourney
import io.kailand.app.journey.ManualJourneyRuntime
import io.kailand.app.journey.NavigationDraft
import io.kailand.app.journey.NavigationInbox
import io.kailand.app.journey.NavigationCapture
import io.kailand.feature.island.BGameService
import io.kailand.app.journey.createNavigationDraftStore
import io.kailand.app.journey.createNavigationCapture
import io.kailand.feature.demo.DemoScenario
import io.kailand.feature.demo.DemoScenarios
import io.kailand.feature.visit.VisitClock
import io.kailand.feature.island.GameReadiness
import io.kailand.feature.island.createBGameService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface CatalogLoadState {
    data object Loading : CatalogLoadState
    data class Ready(val catalog: Catalog, val demoCompletion: DemoInput.Complete,
        val scenarios: List<DemoScenario>) : CatalogLoadState
    data object Failed : CatalogLoadState
}

/** Process-owned service. Activity recreation never creates another GameService or write mutex. */
class GameAppSession internal constructor(
    context: Context,
    suppliedGame: BGameService? = null,
    suppliedInbox: NavigationInbox? = null,
    suppliedCapture: NavigationCapture? = null,
    private val onManualAttempt: (ArrivalCommand) -> Unit = {},
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) {
    private val app = context.applicationContext
    val navigationInbox = suppliedInbox ?: NavigationInbox(createNavigationDraftStore(app, scope), scope)
    val navigationCapture by lazy { suppliedCapture ?: createNavigationCapture(app, scope, navigationInbox) }
    val game = suppliedGame ?: createBGameService(app, scope)
    private var activeOwner: Any? = null
    private var inputs = GameCommandContext()
    private var runtimeOwner: Any? = null
    private var runtime: GameSimulationRuntime? = null
    private var manualOwner: Any? = null
    private var manualGeneration = -1L
    private var manualRuntime: ManualJourneyRuntime? = null
    private var modeOwner: Any? = null
    private val mutableMode = MutableStateFlow(DataSource.DEMO)
    val mode = mutableMode.asStateFlow()
    val commands = GameCommandCoordinator(game, { game.readiness.value == GameReadiness.READY }) {
        inputs.copy(journey = when (inputs.mode) {
            DataSource.DEMO -> runtime?.snapshot.takeIf { activeOwner === runtimeOwner }
            DataSource.USER_CONFIRMED -> manualRuntime?.snapshot.takeIf { activeOwner === manualOwner }
            DataSource.PLEOS -> null
        })
    }
    private val mutableCatalog = MutableStateFlow<CatalogLoadState>(CatalogLoadState.Loading)
    val catalog = mutableCatalog.asStateFlow()
    private var catalogJob: Job? = null

    init { reloadCatalog() }

    fun reloadCatalog() {
        if (catalogJob?.isActive == true) return
        catalogJob = scope.launch {
            mutableCatalog.value = CatalogLoadState.Loading
            try {
                mutableCatalog.value = withContext(Dispatchers.IO) {
                    val assets = CatalogAssets(app.assets)
                    val scenariosJson = assets.readFixtureJson("scenarios")
                    CatalogLoadState.Ready(decodeCatalog(assets.readCatalogJson()),
                        decodeDemoCompletion(scenariosJson), DemoScenarios.fromJson(scenariosJson))
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableCatalog.value = CatalogLoadState.Failed
            }
        }
    }

    /** Main-thread lifecycle input. A background entry cannot revoke a newer foreground owner. */
    fun updateHost(owner: Any, gate: InteractionGate, foreground: Boolean, mode: DataSource) {
        if (foreground) {
            if (manualOwner != null && manualOwner !== owner) manualRuntime?.invalidate()
            if (modeOwner !== owner) {
                modeOwner = owner
                if (commands.session.value.phase == GameCommandPhase.ACTIVE) mutableMode.value = mode
            }
            activeOwner = owner
            inputs = GameCommandContext(gate, true, mutableMode.value)
            runtime?.updateHost(if (runtimeOwner === owner) inputs else inputs.copy(foreground = false))
        } else if (activeOwner === owner) {
            inputs = inputs.copy(gate = gate, foreground = false)
            activeOwner = null
            runtime?.updateHost(inputs)
        }
        manualRuntime?.updateHost()
    }

    fun canHost(owner: Any): Boolean = activeOwner === owner && inputs.foreground && inputs.gate.canInteract &&
        game.readiness.value == GameReadiness.READY

    fun canUse(owner: Any): Boolean = canHost(owner) && commands.session.value.phase == GameCommandPhase.ACTIVE

    fun simulation(owner: Any, catalog: Catalog, scenarios: List<DemoScenario>): GameSimulationRuntime? {
        if (!canHost(owner)) return null
        runtime?.let { if (runtimeOwner === owner) return it }
        val old = runtime
        runtimeOwner = owner
        return GameSimulationRuntime(scope, game, commands, catalog, scenarios,
            { canHost(owner) }, { inputs.mode }, VisitClock(SystemClock::elapsedRealtime), AppVisitScheduler(scope),
            beforeReset = { navigationCapture.reset() })
            .also {
                runtime = it
                it.updateHost(inputs)
                if (old != null) scope.launch { old.dispose() }
            }
    }

    fun screenDemoControls(runtime: GameSimulationRuntime, callerIsCurrent: () -> Boolean) =
        GuardedDemoJourney(runtime.journey, runtime.demo, scope, callerIsCurrent)

    fun manualJourney(owner: Any, catalog: Catalog): ManualJourneyRuntime? {
        if (!canUse(owner) || inputs.mode != DataSource.USER_CONFIRMED) return null
        val generation = commands.session.value.generation
        manualRuntime?.let { if (manualOwner === owner && manualGeneration == generation) return it }
        manualRuntime?.invalidate()
        manualOwner = owner
        manualGeneration = generation
        return ManualJourneyRuntime(scope, commands, generation, catalog.regions.map { it.id }.toSet(), {
            canUse(owner) && inputs.mode == DataSource.USER_CONFIRMED && manualOwner === owner &&
                game.state.value.pendingReceipt == null
        }, onAttempt = onManualAttempt).also { manualRuntime = it; it.updateHost() }
    }

    fun finishManualReceipt(journeyId: String) { manualRuntime?.finishAcknowledged(journeyId) }

    fun adoptNavigation(owner: Any, catalog: Catalog, runtime: ManualJourneyRuntime,
        draft: NavigationDraft, caller: () -> Boolean): RequestResult {
        if (manualRuntime !== runtime || manualOwner !== owner || !navigationInbox.current(draft))
            return RequestResult.Failed("내비 목적지가 바뀌었어요. 다시 확인해 주세요.")
        return runtime.adoptNavigation(draft, draft.destination(catalog), { navigationInbox.current(draft) },
            saved = { navigationInbox.consume(draft); Unit },
            dismissed = { navigationInbox.dismiss(draft); Unit }, caller = caller)
    }

    suspend fun changeMode(owner: Any, target: DataSource): MutationResult {
        if (!canHost(owner)) return MutationResult.Rejected("현재 이용 상태를 확인해 주세요.")
        if (target == inputs.mode && commands.session.value.phase == GameCommandPhase.ACTIVE) return MutationResult.Success
        val captured = runtime.takeIf { runtimeOwner === owner }
        return commands.changeMode(commands.session.value.generation, { canHost(owner) },
            stopOldWork = { manualRuntime?.invalidate(); captured?.stopForModeChange(target) }, commitMode = {
                inputs = inputs.copy(mode = target)
                mutableMode.value = target
                captured?.updateHost(inputs)
            })
    }
}
