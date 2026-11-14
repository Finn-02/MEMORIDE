package io.kailand.app

import io.kailand.app.journey.DemoJourneyAdapter
import io.kailand.core.contract.*
import io.kailand.feature.demo.*
import io.kailand.feature.visit.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow

/** A-owned lifetime binding. C receives stable ports; each operation pins its actual generation.
 * All entry points run on the app's main scope. No C/B internals or stored snapshots are rewritten.
 */
class GameSimulationRuntime(
    private val scope: CoroutineScope,
    private val game: GameService,
    private val commands: GameCommandCoordinator,
    private val catalog: Catalog,
    scenarios: List<DemoScenario>,
    private val hostAllowed: () -> Boolean,
    private val mode: () -> DataSource,
    clock: VisitClock,
    scheduler: VisitScheduler,
    private val beforeReset: suspend () -> Unit = {},
) {
    private data class Binding(val generation: Long, val adapter: DemoJourneyAdapter)
    private var binding = makeBinding(commands.session.value.generation)
    private var demoVisible = false
    private var visitVisible = false
    private var context = GameCommandContext()
    val visit = VisitController(clock, scheduler)
    val snapshot: JourneySnapshot get() = binding.adapter.state.value

    private val ports = object : JourneyPort, DemoControlPort {
        // Consumers rebind collection on generation changes; synchronous reads always use the current adapter.
        override val state: StateFlow<JourneySnapshot> get() = binding.adapter.state
        override suspend fun requestDestination(destination: DestinationSpec, kind: JourneyKind) =
            perform { it.requestDestination(destination, kind) }
        override suspend fun cancel() = perform { it.cancel() }
        override suspend fun retryArrival() = perform { it.retryArrival() }
        override suspend fun dispatch(input: DemoInput) = perform { it.dispatch(input) }
    }
    val journey: JourneyPort = ports
    val demo: DemoControlPort = ports
    val runner = DemoScenarioRunner(scenarios, catalog, journey, demo, { game.state.value },
        dailyDestination = { catalog.regions.firstOrNull()?.let {
            DestinationSpec(null, "DEMO 일상 목적지", it.latitude, it.longitude)
        } }, resetGame = {
            when (val result = resetFromRunner()) {
                MutationResult.Success -> RequestResult.Accepted
                is MutationResult.Rejected -> RequestResult.Failed(result.reason)
                is MutationResult.StorageFailure -> RequestResult.Failed(result.message)
            }
        })

    init {
        if (commands.session.value.phase == GameCommandPhase.RESET_FAILED) {
            runner.finishReset(runner.beginReset(), false)
            visit.beginReset()
        }
    }

    private fun permitted(value: Binding) = hostAllowed() && mode() == DataSource.DEMO &&
        commands.session.value == GameCommandSession(value.generation) && binding === value

    private fun makeBinding(generation: Long): Binding {
        lateinit var adapter: DemoJourneyAdapter
        fun allowed() = hostAllowed() && mode() == DataSource.DEMO &&
            commands.session.value == GameCommandSession(generation) && binding.adapter === adapter
        adapter = DemoJourneyAdapter(scope, catalog.regions.map { it.id }.toSet(), { allowed() },
            { commands.completeArrival(generation, it) { allowed() } })
        return Binding(generation, adapter)
    }

    private suspend fun perform(action: suspend (DemoJourneyAdapter) -> RequestResult): RequestResult {
        val captured = binding
        if (!permitted(captured)) return RequestResult.Failed("현재 DEMO 세션을 다시 확인해 주세요.")
        return scope.async {
            if (permitted(captured)) action(captured.adapter)
            else RequestResult.Failed("이전 세대의 시연 입력을 중단했어요.")
        }.await()
    }

    fun updateHost(value: GameCommandContext) {
        context = value
        refresh()
    }

    fun setScreens(demo: Boolean, visit: Boolean) {
        demoVisible = demo
        visitVisible = visit
        refresh()
    }

    private fun refresh() {
        runner.updateContext(DemoContext(context.gate, context.foreground, demoVisible, context.mode))
        visit.updateContext(VisitContext(context.gate, context.foreground, visitVisible, context.mode))
    }

    /** Runner.resetConfirmed already calls its own begin/finishReset. Never reset that token twice. */
    private suspend fun resetFromRunner(): MutationResult {
        var visitToken: Long? = null
        return try {
            commands.resetDemo(commands.session.value.generation, hostAllowed) {
                beforeReset()
                visitToken = visit.beginReset()
                binding.adapter.invalidate() // Outside the command mutex, never joins the reset requester.
                binding = makeBinding(commands.session.value.generation)
            }.also { result -> visitToken?.let { visit.finishReset(it, result == MutationResult.Success) } }
        } catch (cancelled: CancellationException) {
            visitToken?.let { visit.finishReset(it, false) }
            throw cancelled
        }
    }

    suspend fun stopForModeChange(target: DataSource) {
        runner.leaveScreen()
        visit.updateContext(VisitContext(context.gate, context.foreground, false, target))
        binding.adapter.invalidate()
        binding = makeBinding(commands.session.value.generation)
    }

    suspend fun dispose() {
        runner.leaveScreen()
        visit.leaveScreen()
        binding.adapter.invalidate()
    }
}
