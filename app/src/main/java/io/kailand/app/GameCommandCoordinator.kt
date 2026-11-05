package io.kailand.app

import io.kailand.core.contract.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class GameCommandContext(
    val gate: InteractionGate = InteractionGate(GateState.UNKNOWN),
    val foreground: Boolean = false,
    val mode: DataSource = DataSource.DEMO,
    val journey: JourneySnapshot? = null,
)

enum class GameCommandPhase { ACTIVE, RESETTING, RESET_FAILED, CHANGING_MODE, MODE_FAILED }
data class GameCommandSession(val generation: Long = 0, val phase: GameCommandPhase = GameCommandPhase.ACTIVE)

/** A's write boundary. One coordinator wraps the app's one GameService.
 * Callers retain a generation when creating callbacks, never refresh an old callback's token.
 * Route changes still require a latest-context check after a suspending acknowledgement.
 */
class GameCommandCoordinator(
    private val game: GameService,
    private val ready: () -> Boolean,
    private val context: () -> GameCommandContext,
) {
    private val mutex = Mutex()
    private val mutable = MutableStateFlow(GameCommandSession())
    val session = mutable.asStateFlow()

    suspend fun apply(
        generation: Long,
        action: GameAction,
        callerIsCurrent: () -> Boolean = { true },
    ): MutationResult = mutex.withLock {
        currentCoroutineContext().ensureActive()
        if (!allowed(generation) || !callerIsCurrent()) return@withLock rejected()
        // Once admitted, finish B's disk transaction AND its snapshot publication.
        // Screen cancellation cannot split those two steps; UI routing remains cancellable.
        withContext(NonCancellable) { game.apply(action) }
    }

    /** Only a caller-validated arrival may enter here; no SDK event is converted to evidence here. */
    suspend fun completeArrival(
        generation: Long, command: ArrivalCommand, callerIsCurrent: () -> Boolean = { true },
    ): ArrivalResult = mutex.withLock {
        currentCoroutineContext().ensureActive()
        if (!allowed(generation) || !callerIsCurrent() || !currentArrival(command)) {
            return@withLock ArrivalResult.Rejected("현재 여정과 이용 상태를 다시 확인해 주세요.")
        }
        withContext(NonCancellable) { game.completeArrival(command) }
    }

    suspend fun acknowledge(
        generation: Long, journeyId: String, callerIsCurrent: () -> Boolean = { true },
    ): MutationResult = mutex.withLock {
        currentCoroutineContext().ensureActive()
        if (!allowed(generation) || !callerIsCurrent() || game.state.value.pendingReceipt?.journeyId != journeyId) {
            return@withLock rejected()
        }
        val result = withContext(NonCancellable) { game.acknowledgeReceipt(journeyId) }
        // Persistence may succeed while the app leaves the foreground. Do not permit stale routing.
        if (result == MutationResult.Success && (!allowed(generation) || !callerIsCurrent())) rejected() else result
    }

    /** Stop jobs/listeners OUTSIDE the write mutex. The hook must not join its own calling job.
     * After admission, finish reset bookkeeping despite screen cancellation. Recheck permission
     * before B's write; actual reset failure keeps inputs blocked until an explicit retry.
     */
    suspend fun resetDemo(
        generation: Long, callerIsCurrent: () -> Boolean = { true }, stopOldWork: suspend () -> Unit,
    ): MutationResult {
        val resetGeneration = mutex.withLock {
            currentCoroutineContext().ensureActive()
            val current = mutable.value
            if (generation != current.generation || current.phase !in setOf(GameCommandPhase.ACTIVE, GameCommandPhase.RESET_FAILED) ||
                !callerIsCurrent() || !inputsAllowed() || context().mode != DataSource.DEMO) return rejected()
            val next = current.generation + 1
            mutable.value = GameCommandSession(next, GameCommandPhase.RESETTING)
            next
        }
        return withContext(NonCancellable) {
            try {
                stopOldWork()
                mutex.withLock {
                    if (!callerIsCurrent() || !inputsAllowed() || context().mode != DataSource.DEMO) {
                        mutable.value = GameCommandSession(resetGeneration, GameCommandPhase.RESET_FAILED)
                        return@withLock rejected()
                    }
                    val result = game.resetDemoProgress()
                    mutable.value = GameCommandSession(resetGeneration,
                        if (result == MutationResult.Success) GameCommandPhase.ACTIVE else GameCommandPhase.RESET_FAILED)
                    result
                }
            } catch (cancelled: CancellationException) {
                blockFailedReset(resetGeneration)
                throw cancelled
            } catch (_: Exception) {
                blockFailedReset(resetGeneration)
                MutationResult.StorageFailure("초기화를 마치지 못했어요. 다시 시도해 주세요.")
            }
        }
    }

    /** Called immediately before routing, after persistence and any suspended caller resumption. */
    fun canRoute(generation: Long, callerIsCurrent: () -> Boolean): Boolean =
        allowed(generation) && callerIsCurrent()

    /** Non-destructive session change. The new source is committed only after old work is stopped. */
    suspend fun changeMode(
        generation: Long, callerIsCurrent: () -> Boolean, stopOldWork: suspend () -> Unit,
        commitMode: () -> Unit,
    ): MutationResult {
        val next = mutex.withLock {
            val current = mutable.value
            if (generation != current.generation || current.phase !in setOf(GameCommandPhase.ACTIVE, GameCommandPhase.MODE_FAILED) ||
                !callerIsCurrent() || !inputsAllowed()) return rejected()
            (current.generation + 1).also { mutable.value = GameCommandSession(it, GameCommandPhase.CHANGING_MODE) }
        }
        try {
            stopOldWork()
            return mutex.withLock {
                if (!callerIsCurrent() || !inputsAllowed()) {
                    mutable.value = GameCommandSession(next, GameCommandPhase.MODE_FAILED)
                    return@withLock rejected()
                }
                commitMode()
                mutable.value = GameCommandSession(next)
                MutationResult.Success
            }
        } catch (cancelled: CancellationException) {
            blockFailedMode(next)
            throw cancelled
        } catch (_: Exception) {
            blockFailedMode(next)
            return MutationResult.StorageFailure("모드 전환을 마치지 못했어요. 다시 선택해 주세요.")
        }
    }

    private suspend fun blockFailedMode(generation: Long) = withContext(NonCancellable) {
        mutex.withLock {
            if (mutable.value == GameCommandSession(generation, GameCommandPhase.CHANGING_MODE)) {
                mutable.value = GameCommandSession(generation, GameCommandPhase.MODE_FAILED)
            }
        }
    }

    private suspend fun blockFailedReset(generation: Long) = withContext(NonCancellable) {
        mutex.withLock {
            if (mutable.value == GameCommandSession(generation, GameCommandPhase.RESETTING)) {
                mutable.value = GameCommandSession(generation, GameCommandPhase.RESET_FAILED)
            }
        }
    }

    private fun allowed(generation: Long) =
        mutable.value == GameCommandSession(generation, GameCommandPhase.ACTIVE) && inputsAllowed()

    private fun inputsAllowed(): Boolean {
        val current = context()
        return ready() && current.foreground && current.gate.canInteract
    }

    private fun currentArrival(command: ArrivalCommand): Boolean {
        val current = context()
        val journey = current.journey ?: return false
        return current.mode == command.source && journey.source == command.source &&
            journey.journeyId == command.journeyId && journey.routeRevision == command.routeRevision &&
            journey.kind == command.kind && journey.phase == JourneyPhase.ARRIVED &&
            (command.kind == JourneyKind.DAILY || journey.destination?.regionId == command.regionId)
    }

    private fun rejected() = MutationResult.Rejected("현재 이용 상태와 저장 세대를 다시 확인해 주세요.")
}
