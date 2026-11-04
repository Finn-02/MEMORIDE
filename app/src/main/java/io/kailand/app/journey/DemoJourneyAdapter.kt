package io.kailand.app.journey

import io.kailand.core.contract.*
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield

/** DEMO orchestration only. The injected arrival boundary owns persistence/rewards.
 * A supplies the latest gate AND foreground check; no sample parking is invented here.
 */
class DemoJourneyAdapter(
    private val scope: CoroutineScope,
    private val regionIds: Set<String>,
    private val interactionAllowed: () -> Boolean,
    private val completeArrival: suspend (ArrivalCommand) -> ArrivalResult,
    private val prepare: suspend (DestinationSpec) -> Unit = { yield() },
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : JourneyPort, DemoControlPort {
    private val mutableState = MutableStateFlow(emptyJourney())
    override val state = mutableState.asStateFlow()
    private val lock = Mutex()
    private var preparation: Job? = null
    private var generation = 0L
    private var retryCommand: ArrivalCommand? = null
    private var lastSuccessfulCommand: ArrivalCommand? = null

    override suspend fun requestDestination(destination: DestinationSpec, kind: JourneyKind): RequestResult {
        if (!lock.tryLock()) return failed("다른 여정 요청을 처리하고 있어요.")
        var job: Job? = null
        try {
            if (!interactionAllowed() || !scope.isActive) return blocked()
            if (!validDestination(destination)) return failed("목적지 정보를 확인해 주세요.")
            val previous = mutableState.value
            val active = previous.phase in activePhases
            if (active && previous.kind != kind) return failed("이동 종류를 바꾸려면 현재 여정을 취소해 주세요.")
            val revision = if (active) previous.routeRevision.toLong() + 1 else 0L
            if (revision > Int.MAX_VALUE) return failed("새 여정을 시작해 주세요.")
            val id = if (active) checkNotNull(previous.journeyId) else newId()
            if (!uuid.matches(id)) return failed("여정 ID를 만들지 못했어요.")
            preparation?.cancel()
            generation++
            val token = generation
            val normalized = if (kind == JourneyKind.DAILY) destination.copy(regionId = null) else destination
            retryCommand = null
            lastSuccessfulCommand = null
            mutableState.value = JourneySnapshot(id, DataSource.DEMO, kind, JourneyPhase.PREPARING,
                revision.toInt(), normalized)
            job = scope.launch(start = CoroutineStart.LAZY) {
                try {
                    prepare(normalized)
                    currentCoroutineContext().ensureActive()
                    lock.withLock {
                        if (generation == token && mutableState.value.phase == JourneyPhase.PREPARING) {
                            mutableState.value = mutableState.value.copy(phase = JourneyPhase.READY)
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    lock.withLock {
                        if (generation == token && mutableState.value.phase == JourneyPhase.PREPARING) {
                            mutableState.value = mutableState.value.copy(phase = JourneyPhase.ERROR,
                                errorMessage = "모의 여정을 준비하지 못했어요.")
                        }
                    }
                }
            }
            preparation = job
        } finally {
            lock.unlock()
        }
        job?.start()
        return RequestResult.Accepted // Preparation is observed through state, not this result.
    }

    override suspend fun cancel(): RequestResult = exclusive {
        val snapshot = mutableState.value
        if (snapshot.phase == JourneyPhase.CANCELLED) return@exclusive RequestResult.Accepted
        if (snapshot.phase !in activePhases) return@exclusive failed("취소할 진행 중 여정이 없어요.")
        // Cleanup remains possible when the gate closes. It grants no gameplay permission.
        generation++
        preparation?.cancel()
        preparation = null
        retryCommand = null
        lastSuccessfulCommand = null
        mutableState.value = snapshot.copy(phase = JourneyPhase.CANCELLED, errorMessage = null)
        RequestResult.Accepted
    }

    override suspend fun dispatch(input: DemoInput): RequestResult {
        if (input == DemoInput.Cancel) return cancel()
        return exclusive {
            if (!interactionAllowed()) return@exclusive blocked()
            val snapshot = mutableState.value
            when (input) {
                DemoInput.StartTravel -> {
                    if (snapshot.phase != JourneyPhase.READY) return@exclusive failed("준비된 여정에서 출발할 수 있어요.")
                    mutableState.value = snapshot.copy(phase = JourneyPhase.TRAVELLING, errorMessage = null)
                    RequestResult.Accepted
                }
                is DemoInput.Complete -> {
                    if (snapshot.phase !in setOf(JourneyPhase.READY, JourneyPhase.TRAVELLING)) {
                        return@exclusive failed("도착을 처리할 여정이 없어요.")
                    }
                    if (input.completedAtEpochMs < 0 || input.elapsedSeconds?.let { it < 0 } == true ||
                        input.distanceMeters?.let { !it.isFinite() || it < 0 } == true) {
                        return@exclusive failed("도착 측정값을 확인해 주세요.")
                    }
                    val command = ArrivalCommand(checkNotNull(snapshot.journeyId), DataSource.DEMO,
                        snapshot.kind, snapshot.destination?.regionId, snapshot.routeRevision,
                        ArrivalEvidence.DEMO_FINAL_DESTINATION, input.completedAtEpochMs,
                        input.elapsedSeconds, input.distanceMeters)
                    // Arrival is distinct from successful persistence. The caller must await save().
                    mutableState.value = snapshot.copy(phase = JourneyPhase.ARRIVED,
                        elapsedSeconds = input.elapsedSeconds, distanceMeters = input.distanceMeters,
                        errorMessage = null)
                    save(command)
                }
                DemoInput.RepeatLastArrival -> {
                    val command = lastSuccessfulCommand
                    if (snapshot.phase != JourneyPhase.ARRIVED || command == null || !isCurrent(command)) {
                        return@exclusive failed("같은 세션에서 반복할 도착 기록이 없어요.")
                    }
                    save(command) // Always redeliver; ARRIVED may not emit another StateFlow value.
                }
                DemoInput.Cancel -> error("Handled above")
            }
        }
    }

    override suspend fun retryArrival(): RequestResult = exclusive {
        if (!interactionAllowed()) return@exclusive blocked()
        val command = retryCommand
        if (command == null || !isCurrent(command)) return@exclusive failed("재시도할 도착 기록이 없어요.")
        save(command)
    }

    /** A calls this on session/mode disposal or reset. This does not reset B's save.
     * Waits for an in-flight write, then invalidates retained commands and preparation.
     * A04 still owns input blocking and Runner cancellation outside its coordination lock.
     */
    suspend fun invalidate() = lock.withLock {
        generation++
        preparation?.cancel()
        preparation = null
        retryCommand = null
        lastSuccessfulCommand = null
        mutableState.value = emptyJourney()
    }

    private suspend fun save(command: ArrivalCommand): RequestResult {
        retryCommand = command
        mutableState.value = mutableState.value.copy(errorMessage = null)
        val result = try {
            completeArrival(command).also { currentCoroutineContext().ensureActive() }
        } catch (cancelled: CancellationException) {
            mutableState.value = mutableState.value.copy(errorMessage = "저장 처리가 중단됐어요. 재시도해 주세요.")
            throw cancelled
        } catch (_: Exception) {
            return saveFailed("도착 기록을 저장하지 못했어요. 재시도해 주세요.")
        }
        return when (result) {
            is ArrivalResult.Applied -> saved(command, result.receipt)
            is ArrivalResult.AlreadyApplied -> saved(command, result.receipt)
            is ArrivalResult.StorageFailure -> saveFailed("도착 기록을 저장하지 못했어요. 재시도해 주세요.")
            is ArrivalResult.Rejected -> {
                retryCommand = null
                saveFailed("도착 기록을 처리할 수 없어요.")
            }
        }
    }

    private fun saved(command: ArrivalCommand, receipt: ArrivalReceipt): RequestResult {
        if (receipt.journeyId != command.journeyId || receipt.source != command.source ||
            receipt.kind != command.kind || receipt.regionId != command.regionId) {
            return saveFailed("도착 결과가 현재 여정과 일치하지 않아요.")
        }
        retryCommand = null
        lastSuccessfulCommand = command
        mutableState.value = mutableState.value.copy(errorMessage = null)
        return RequestResult.Accepted
    }

    private fun saveFailed(message: String): RequestResult {
        mutableState.value = mutableState.value.copy(errorMessage = message)
        return failed(message)
    }

    private fun isCurrent(command: ArrivalCommand): Boolean = mutableState.value.let {
        it.phase == JourneyPhase.ARRIVED && it.journeyId == command.journeyId &&
            it.routeRevision == command.routeRevision && it.source == command.source
    }

    private fun validDestination(destination: DestinationSpec): Boolean = destination.label.isNotBlank() &&
        destination.latitude.isFinite() && destination.latitude in -90.0..90.0 &&
        destination.longitude.isFinite() && destination.longitude in -180.0..180.0 &&
        (destination.regionId == null || destination.regionId in regionIds)

    private suspend fun exclusive(block: suspend () -> RequestResult): RequestResult {
        if (!lock.tryLock()) return failed("다른 여정 요청을 처리하고 있어요.")
        return try { block() } finally { lock.unlock() }
    }

    private fun blocked() = failed("지금은 여정을 조작할 수 없어요.")
    private fun failed(message: String) = RequestResult.Failed(message)

    private companion object {
        val activePhases = setOf(JourneyPhase.PREPARING, JourneyPhase.READY, JourneyPhase.TRAVELLING)
        val uuid = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
        fun emptyJourney() = JourneySnapshot(null, DataSource.DEMO, JourneyKind.DAILY, JourneyPhase.IDLE, 0, null)
    }
}
