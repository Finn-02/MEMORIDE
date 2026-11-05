package io.kailand.app

import android.content.Context
import android.os.SystemClock
import io.kailand.core.contract.*
import io.kailand.app.journey.GuardedDemoJourney
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
class GameAppSession internal constructor(context: Context) {
    private val app = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val game = createBGameService(app, scope)
    private var activeOwner: Any? = null
    private var inputs = GameCommandContext()
    private var runtimeOwner: Any? = null
    private var runtime: GameSimulationRuntime? = null
    private var modeOwner: Any? = null
    private val mutableMode = MutableStateFlow(DataSource.DEMO)
    val mode = mutableMode.asStateFlow()
    val commands = GameCommandCoordinator(game, { game.readiness.value == GameReadiness.READY }) {
        inputs.copy(journey = runtime?.snapshot.takeIf {
            activeOwner === runtimeOwner && inputs.mode == DataSource.DEMO
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
            { canHost(owner) }, { inputs.mode }, VisitClock(SystemClock::elapsedRealtime), AppVisitScheduler(scope))
            .also {
                runtime = it
                it.updateHost(inputs)
                if (old != null) scope.launch { old.dispose() }
            }
    }

    fun screenDemoControls(runtime: GameSimulationRuntime, callerIsCurrent: () -> Boolean) =
        GuardedDemoJourney(runtime.journey, runtime.demo, scope, callerIsCurrent)

    suspend fun changeMode(owner: Any, target: DataSource): MutationResult {
        if (target !in listOf(DataSource.DEMO, DataSource.PLEOS)) return MutationResult.Rejected("지원하지 않는 입력 모드입니다.")
        if (!canHost(owner)) return MutationResult.Rejected("현재 이용 상태를 확인해 주세요.")
        if (target == inputs.mode && commands.session.value.phase == GameCommandPhase.ACTIVE) return MutationResult.Success
        val captured = runtime.takeIf { runtimeOwner === owner }
        return commands.changeMode(commands.session.value.generation, { canHost(owner) },
            stopOldWork = { captured?.stopForModeChange(target) }, commitMode = {
                inputs = inputs.copy(mode = target)
                mutableMode.value = target
                captured?.updateHost(inputs)
            })
    }
}
