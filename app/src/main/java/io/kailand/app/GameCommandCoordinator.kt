package io.kailand.app

import io.kailand.core.contract.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
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

enum class GameCommandPhase { ACTIVE, RESETTING, RESET_FAILED }
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
        if (!allowed(generation) || !callerIsCurrent()) return@withLock rejected()
        game.apply(action)
    }

    /** Only a caller-validated arrival may enter here; no SDK event is converted to evidence here. */
    suspend fun completeArrival(
        generation: Long, command: ArrivalCommand, callerIsCurrent: () -> Boolean = { true },
    ): ArrivalResult = mutex.withLock {
        if (!allowed(generation) || !callerIsCurrent() || !currentArrival(command)) {
            return@withLock ArrivalResult.Rejected("현재 여정과 이용 상태를 다시 확인해 주세요.")
        }
        game.completeArrival(command)
    }

    suspend fun acknowledge(
        generation: Long, journeyId: String, callerIsCurrent: () -> Boolean = { true },
    ): MutationResult = mutex.withLock {
        if (!allowed(generation) || !callerIsCurrent() || game.state.value.pendingReceipt?.journeyId != journeyId) {
            return@withLock rejected()
        }
        val result = game.acknowledgeReceipt(journeyId)
        // Persistence may succeed while the app leaves the foreground. Do not permit stale routing.
        if (result == MutationResult.Success && (!allowed(generation) || !callerIsCurrent())) rejected() else result
    }

    /** Stop jobs/listeners OUTSIDE the write mutex. The hook must not join its own calling job.
     * Reset failure/cancellation keeps inputs blocked; a fresh explicit reset is the only retry.
     */
    suspend fun resetDemo(generation: Long, stopOldWork: suspend () -> Unit): MutationResult {
        val resetGeneration = mutex.withLock {
            val current = mutable.value
            if (generation != current.generation || current.phase == GameCommandPhase.RESETTING ||
                !inputsAllowed() || context().mode != DataSource.DEMO) return rejected()
            val next = current.generation + 1
            mutable.value = GameCommandSession(next, GameCommandPhase.RESETTING)
            next
        }
        try {
            stopOldWork()
            return mutex.withLock {
                if (!inputsAllowed() || context().mode != DataSource.DEMO) {
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
            return MutationResult.StorageFailure("초기화를 마치지 못했어요. 다시 시도해 주세요.")
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
