package io.kailand.feature.demo

import io.kailand.core.contract.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class DemoStep { CHOOSE, RESET, PREPARE, WAIT_READY, START, COMPLETE, RETRY_ARRIVAL, REPEAT, DONE }
data class DemoContext(
    val gate: InteractionGate = InteractionGate(GateState.UNKNOWN),
    val foreground: Boolean = false, val screenActive: Boolean = false,
    val mode: DataSource = DataSource.DEMO,
)
data class DemoRunnerState(
    val selected: DemoScenario? = null, val step: DemoStep = DemoStep.CHOOSE,
    val busy: Boolean = false, val resetBlocked: Boolean = false,
    val interactionAllowed: Boolean = false, val message: String? = null,
    val journeyId: String? = null,
)

/** Manual only. A owns ports/reset/storage coordination; C holds no GameService or saved state. */
class DemoScenarioRunner(
    val scenarios: List<DemoScenario>,
    private val catalog: Catalog,
    private val journey: JourneyPort,
    private val demo: DemoControlPort,
    private val readGame: () -> GameSnapshot?,
    private val dailyDestination: () -> DestinationSpec?,
    private val resetGame: suspend () -> RequestResult,
) {
    private val lock = Any()
    private val mutableState = MutableStateFlow(DemoRunnerState())
    val state: StateFlow<DemoRunnerState> = mutableState.asStateFlow()
    private var context = DemoContext()
    private var generation = 0L
    private var resetSequence = 0L
    private var operation: Job? = null
    private var resetBusy = false
    private var resetBlocked = false
    private val completed = mutableMapOf<String, ArrivalReceipt>()
    private var lastCompleted: String? = null
    private var beforeGame: GameSnapshot? = null
    private var expectedJourneyId: String? = null
    private var previousJourneyId: String? = null
    private var requestedDestination: DestinationSpec? = null

    init { require(scenarios.map { it.id }.distinct().size == scenarios.size) }
    private fun baseAllowed() = context.gate.canInteract && context.foreground && context.screenActive &&
        context.mode == DataSource.DEMO && journey.state.value.source == DataSource.DEMO
    private fun allowed() = baseAllowed() && !resetBlocked
    private fun publish(value: DemoRunnerState) {
        mutableState.value = value.copy(interactionAllowed = allowed(), resetBlocked = resetBlocked)
    }
    private fun invalidate(message: String): Job? {
        generation++
        expectedJourneyId = null
        previousJourneyId = null
        requestedDestination = null
        beforeGame = null
        completed.clear()
        lastCompleted = null
        val old = operation
        operation = null
        publish(DemoRunnerState(message = message, busy = resetBusy))
        return old
    }

    fun updateContext(value: DemoContext) {
        val cancel = synchronized(lock) {
            if (value == context) return
            val modeChanged = value.mode != context.mode
            context = value
            if (!baseAllowed() || modeChanged) invalidate("시연을 중단했어요. 이전 단계는 자동 재개하지 않아요.")
            else { publish(state.value); null }
        }
        cancel?.cancel() // No join, no shared lock held; reset requests retain their own acknowledgement.
    }
    fun leaveScreen() { updateContext(synchronized(lock) { context.copy(screenActive = false) }) }

    fun selectScenario(id: String): Boolean = synchronized(lock) {
        if (!allowed() || state.value.busy || state.value.step !in setOf(DemoStep.CHOOSE, DemoStep.DONE)) return false
        val plan = scenarios.singleOrNull { it.id == id } ?: return false
        if (plan.dependsOn != null && plan.dependsOn !in completed) {
            publish(state.value.copy(message = "먼저 ${plan.dependsOn} 시나리오를 마쳐 주세요.")); return false
        }
        if (plan.replayOf != null && (lastCompleted != plan.replayOf ||
                journey.state.value.journeyId != completed[plan.replayOf]?.journeyId)) {
            publish(state.value.copy(message = "반복할 원본 도착이 현재 마지막 도착이 아니에요.")); return false
        }
        generation++
        expectedJourneyId = null
        beforeGame = null
        publish(DemoRunnerState(selected = plan, step = when {
            plan.resetBefore -> DemoStep.RESET
            plan.replayOf != null -> DemoStep.REPEAT
            else -> DemoStep.PREPARE
        }))
        true
    }

    /** External whole-game reset hook. A must not join the job currently requesting its reset. */
    fun beginReset(): Long {
        val token: Long
        val cancel: Job?
        synchronized(lock) {
            resetSequence++
            token = resetSequence
            resetBlocked = true
            resetBusy = true
            cancel = invalidate("전체 초기화 중이에요.")
        }
        cancel?.cancel()
        return token
    }
    fun finishReset(token: Long, success: Boolean): Boolean = synchronized(lock) {
        if (!resetBlocked || token != resetSequence) return false
        resetBusy = false
        if (!success) { publish(state.value.copy(busy = false, message = "초기화에 실패했어요. 입력 차단을 유지해요.")); return false }
        resetBlocked = false
        publish(state.value.copy(busy = false, step = if (state.value.selected != null) DemoStep.PREPARE else DemoStep.CHOOSE,
            message = "초기화를 마쳤어요. 다음 단계는 직접 눌러 주세요."))
        true
    }

    /** User-confirmed reset is separate from scenario jobs, so A can stop the runner without self-join. */
    suspend fun resetConfirmed(): Boolean {
        val plan = synchronized(lock) {
            if (!baseAllowed() || resetBusy) return false
            resetBusy = true
            state.value.selected.takeIf { state.value.step == DemoStep.RESET }
        }
        val token = beginReset()
        synchronized(lock) { publish(state.value.copy(selected = plan, step = DemoStep.RESET, busy = true)) }
        return try {
            val result = resetGame()
            finishReset(token, result is RequestResult.Accepted)
        } catch (cancelled: CancellationException) {
            finishReset(token, false)
            throw cancelled
        } catch (_: Exception) { finishReset(token, false); false }
    }

    private fun destination(plan: DemoScenario): DestinationSpec? {
        if (plan.kind == JourneyKind.DAILY) return dailyDestination()?.takeIf {
            it.regionId == null && it.label.isNotBlank() && it.latitude.isFinite() && it.longitude.isFinite() &&
                it.latitude in -90.0..90.0 && it.longitude in -180.0..180.0
        }
        return catalog.regions.singleOrNull { it.id == plan.regionId }?.takeIf {
            it.name.isNotBlank() && it.latitude.isFinite() && it.longitude.isFinite() &&
                it.latitude in -90.0..90.0 && it.longitude in -180.0..180.0
        }?.let { DestinationSpec(it.id, it.name, it.latitude, it.longitude) }
    }

    suspend fun cancelJourney(): Boolean = try {
        coroutineScope {
            val job = currentCoroutineContext().job
            val captured = synchronized(lock) {
                val j = journey.state.value
                if (!allowed() || state.value.busy || j.source != DataSource.DEMO || j.journeyId.isNullOrBlank() ||
                    j.phase !in setOf(JourneyPhase.PREPARING, JourneyPhase.READY, JourneyPhase.TRAVELLING, JourneyPhase.ERROR)) return@coroutineScope false
                invalidate("여정 취소 결과를 기다리고 있어요.")
                operation = job
                publish(state.value.copy(busy = true))
                generation to j.journeyId
            }
            try {
                val result = demo.dispatch(DemoInput.Cancel)
                currentCoroutineContext().ensureActive()
                synchronized(lock) {
                    if (captured.first != generation || !allowed()) return@synchronized false
                    val j = journey.state.value
                    val accepted = result is RequestResult.Accepted && j.source == DataSource.DEMO &&
                        j.journeyId == captured.second && j.phase == JourneyPhase.CANCELLED
                    publish(state.value.copy(busy = false, message = if (accepted) "여정 취소를 확인했어요."
                        else (result as? RequestResult.Failed)?.message ?: "취소된 여정 상태를 확인하지 못했어요."))
                    accepted
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { synchronized(lock) { if (captured.first == generation) publish(state.value.copy(message = "여정 취소에 실패했어요.")) }; false }
            finally { synchronized(lock) { if (operation === job) { operation = null; publish(state.value.copy(busy = false)) } } }
        }
    } catch (cancelled: CancellationException) { currentCoroutineContext().ensureActive(); false }

    /** Suspends for A's acknowledgement. No subsequent scenario step is started automatically. */
    suspend fun advance(): Boolean = try {
        coroutineScope {
            val job = currentCoroutineContext().job
            val start = synchronized(lock) {
                if (!allowed() || state.value.busy || journey.state.value.source != DataSource.DEMO) return@coroutineScope false
                val plan = state.value.selected ?: return@coroutineScope false
                if (state.value.step !in setOf(DemoStep.PREPARE, DemoStep.WAIT_READY, DemoStep.START,
                        DemoStep.COMPLETE, DemoStep.RETRY_ARRIVAL, DemoStep.REPEAT)) return@coroutineScope false
                operation = job
                publish(state.value.copy(busy = true, message = null))
                Triple(generation, plan, state.value.step)
            }
            val (epoch, plan, step) = start
            try {
                val result = when (step) {
                    DemoStep.PREPARE -> destination(plan)?.let {
                        synchronized(lock) { if (generation == epoch) { previousJourneyId = journey.state.value.journeyId; requestedDestination = it } }
                        journey.requestDestination(it, plan.kind)
                    }
                        ?: RequestResult.Failed("지역 정보 또는 일상 목적지 입력을 확인해 주세요.")
                    DemoStep.WAIT_READY -> RequestResult.Accepted
                    DemoStep.START -> if (validJourney(JourneyPhase.READY)) demo.dispatch(DemoInput.StartTravel)
                        else RequestResult.Failed("같은 DEMO 여정의 READY를 먼저 확인해 주세요.")
                    DemoStep.COMPLETE -> if (validJourney(JourneyPhase.TRAVELLING)) {
                        synchronized(lock) { if (generation == epoch) beforeGame = readGame() }
                        if (synchronized(lock) { beforeGame } == null) RequestResult.Failed("게임 상태 로드를 기다려 주세요.")
                        else demo.dispatch(DemoInput.Complete(plan.completedAtEpochMs, plan.elapsedSeconds, plan.distanceMeters))
                    } else RequestResult.Failed("같은 DEMO 여정의 출발을 먼저 확인해 주세요.")
                    DemoStep.RETRY_ARRIVAL -> if (journey.state.value.journeyId == synchronized(lock) { expectedJourneyId }) journey.retryArrival()
                        else RequestResult.Failed("재시도할 원본 여정이 바뀌었어요.")
                    DemoStep.REPEAT -> {
                        val original = synchronized(lock) { completed[plan.replayOf] }
                        if (original == null || journey.state.value.journeyId != original.journeyId ||
                            journey.state.value.phase != JourneyPhase.ARRIVED) RequestResult.Failed("마지막 원본 도착을 확인해 주세요.")
                        else {
                            synchronized(lock) { if (generation == epoch) { if (beforeGame == null) beforeGame = readGame(); expectedJourneyId = original.journeyId } }
                            if (synchronized(lock) { beforeGame } == null) RequestResult.Failed("게임 상태 로드를 기다려 주세요.")
                            else demo.dispatch(DemoInput.RepeatLastArrival)
                        }
                    }
                    else -> error("Not an executable step")
                }
                currentCoroutineContext().ensureActive()
                synchronized(lock) {
                    if (epoch != generation || !allowed()) return@synchronized false
                    if (result is RequestResult.Failed) {
                        publish(state.value.copy(busy = false, message = result.message,
                            step = if (step == DemoStep.COMPLETE && beforeGame != null) DemoStep.RETRY_ARRIVAL else step))
                        return@synchronized false
                    }
                    when (step) {
                        DemoStep.PREPARE -> { expectedJourneyId = null; publish(state.value.copy(busy = false, step = DemoStep.WAIT_READY,
                            message = "목적지 요청이 접수됐어요. READY 확인은 별도 단계예요.")); true }
                        DemoStep.WAIT_READY -> {
                            val j = journey.state.value
                            val target = requestedDestination
                            if (j.phase != JourneyPhase.READY || j.journeyId.isNullOrBlank() || j.journeyId == previousJourneyId || j.source != DataSource.DEMO ||
                                j.kind != plan.kind || target == null || j.destination != target ||
                                j.journeyId in completed.values.map { it.journeyId }) {
                                publish(state.value.copy(busy = false, message = "새 요청과 일치하는 READY를 기다리고 있어요.")); false
                            } else { expectedJourneyId = j.journeyId; publish(state.value.copy(busy = false, step = DemoStep.START, journeyId = j.journeyId)); true }
                        }
                        DemoStep.START -> if (validJourney(JourneyPhase.TRAVELLING)) {
                            publish(state.value.copy(busy = false, step = DemoStep.COMPLETE)); true
                        } else { publish(state.value.copy(busy = false, message = "출발 단계 확인이 필요해요.")); false }
                        else -> verifySaved(plan, step)
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                synchronized(lock) { if (epoch == generation) publish(state.value.copy(busy = false, message = "단계 처리 중 오류가 발생했어요.",
                    step = if (step == DemoStep.COMPLETE && beforeGame != null) DemoStep.RETRY_ARRIVAL else step)) }
                false
            } finally {
                synchronized(lock) {
                    if (operation === job) { operation = null; publish(state.value.copy(busy = false)) }
                }
            }
        }
    } catch (cancelled: CancellationException) { currentCoroutineContext().ensureActive(); false }

    private fun validJourney(phase: JourneyPhase): Boolean = synchronized(lock) {
        val j = journey.state.value
        j.source == DataSource.DEMO && j.journeyId == expectedJourneyId && !j.journeyId.isNullOrBlank() && j.phase == phase
    }
    private fun verifySaved(plan: DemoScenario, step: DemoStep): Boolean {
        val before = beforeGame
        val after = readGame()
        val receipt = after?.pendingReceipt
        val repeated = plan.replayOf?.let { completed[it] }
        val valid = validJourney(JourneyPhase.ARRIVED) && before != null && after != null && receipt != null &&
            receipt.journeyId == expectedJourneyId && receipt.source == DataSource.DEMO && receipt.kind == plan.kind &&
            receipt.regionId == plan.regionId && receipt.completedAtEpochMs == plan.completedAtEpochMs &&
            receipt.elapsedSeconds == plan.elapsedSeconds && receipt.distanceMeters == plan.distanceMeters &&
            after.dailyGrowth - before.dailyGrowth == plan.expectedGrowth &&
            after.ownedItemIds - before.ownedItemIds == plan.expectedItems &&
            after.ownedItemIds.containsAll(before.ownedItemIds) && after.discoveredRegionIds.containsAll(before.discoveredRegionIds) &&
            after.discoveredRegionIds - before.discoveredRegionIds == setOfNotNull(plan.expectedDiscovery) &&
            (repeated == null || (receipt == repeated && after.avatar == before.avatar && after.decorationSlots == before.decorationSlots &&
                after.ownedItemIds == before.ownedItemIds && after.discoveredRegionIds == before.discoveredRegionIds))
        if (!valid) {
            publish(state.value.copy(busy = false, step = if (step == DemoStep.REPEAT) DemoStep.REPEAT else DemoStep.RETRY_ARRIVAL,
                message = "접수만으로 성공 처리하지 않아요. 저장 상태와 원본 영수증을 확인해 주세요."))
            return false
        }
        completed[plan.id] = receipt!!
        lastCompleted = plan.id
        publish(state.value.copy(busy = false, step = DemoStep.DONE,
            message = if (repeated != null) "같은 원본 영수증과 새 지급 0을 확인했어요." else "저장된 결과와 시나리오 기대값을 확인했어요."))
        return true
    }
}
