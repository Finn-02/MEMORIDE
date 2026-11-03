package io.kailand.feature.visit

import io.kailand.core.contract.AvatarPreview
import io.kailand.core.contract.DataSource
import io.kailand.core.contract.GateState
import io.kailand.core.contract.InteractionGate
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

fun interface VisitClock { fun nowMs(): Long }
fun interface VisitCancellation { fun cancel() }

/** Enqueue without inline execution; cancellation must not block or join a job. */
fun interface VisitScheduler {
    fun schedule(delayMs: Long, action: () -> Unit): VisitCancellation
}

enum class VisitPhase { IDLE, REQUESTED, ACCEPTED, VISITING, REJECTED, EXPIRED, WITHDRAWN, CANCELLED, COMPLETED }
enum class VisitCancelReason { RESTRICTED, UNKNOWN, BACKGROUND, SCREEN_LEFT, MODE_CHANGED, RESET }

data class VisitToken(val requestId: String, val generation: Long)
data class VisitRequest(
    val token: VisitToken,
    val senderId: String,
    val receiverId: String,
    val purpose: String,
    val presetMessageId: String,
    val createdAtMonotonicMs: Long,
    val expiresAtMonotonicMs: Long,
)

data class VisitContext(
    val gate: InteractionGate = InteractionGate(GateState.UNKNOWN),
    val foreground: Boolean = false,
    val screenActive: Boolean = false,
    val mode: DataSource = DataSource.DEMO,
)

data class VisitState(
    val phase: VisitPhase = VisitPhase.IDLE,
    val request: VisitRequest? = null,
    val generation: Long = 0,
    val interactionAllowed: Boolean = false,
    val cancelReason: VisitCancelReason? = null,
    val mode: DataSource = DataSource.DEMO,
    val remainingResponseMs: Long? = null,
    // A/C binding attaches B's current immutable preview; the controller never writes game state.
    val visitorAvatar: AvatarPreview? = null,
) {
    val active: Boolean get() = phase in setOf(VisitPhase.REQUESTED, VisitPhase.ACCEPTED, VisitPhase.VISITING)
}

sealed interface VisitRequestResult {
    data class Created(val request: VisitRequest) : VisitRequestResult
    data class Rejected(val message: String, val retryAfterMs: Long = 0) : VisitRequestResult
}

/** One process-local consent session. No persistence, reward, network, or SDK dependency. */
class VisitController(
    private val clock: VisitClock,
    private val scheduler: VisitScheduler,
) {
    companion object {
        const val REQUEST_TIMEOUT_MS = 15_000L
        const val COOLDOWN_MS = 30_000L
        const val SENDER_ID = "demo_my_car"
        const val TARGET_ID = "demo_front_car"
        const val PURPOSE = "주행 관련 메시지"
        const val MESSAGE_ID = "driving_lane_check"
        const val MESSAGE = "혹시 추월 중이 아니라면, 안전할 때 주행차로를 확인해 주실 수 있을까요?"
    }

    private val mutableState = MutableStateFlow(VisitState())
    val state: StateFlow<VisitState> = mutableState.asStateFlow()
    private var context = VisitContext()
    private var generation = 0L
    // Whole-game reset identity is independent of mode changes invalidating visit requests.
    private var resetSequence = 0L
    private var resetBlocked = false
    private var timeout: VisitCancellation? = null
    private val cooldownUntil = mutableMapOf<String, Long>()

    private fun allowed() = !resetBlocked && context.gate.canInteract && context.foreground && context.screenActive

    private fun publish(value: VisitState) {
        mutableState.value = value.copy(
            generation = generation,
            interactionAllowed = allowed(),
            mode = context.mode,
            remainingResponseMs = if (value.phase == VisitPhase.REQUESTED) {
                value.request?.let { (it.expiresAtMonotonicMs - clock.nowMs()).coerceAtLeast(0L) }
            } else null,
        )
    }

    private fun end(phase: VisitPhase, reason: VisitCancelReason? = null) {
        timeout?.cancel()
        timeout = null
        publish(state.value.copy(phase = phase, cancelReason = reason))
    }

    @Synchronized
    fun updateContext(value: VisitContext) {
        // Recomposition can repeat identical inputs; only scheduler/events refresh the countdown.
        if (value == context) return
        val modeChanged = value.mode != context.mode
        context = value
        if (modeChanged) {
            generation++
            if (state.value.active) end(VisitPhase.CANCELLED, VisitCancelReason.MODE_CHANGED)
        }
        if (!allowed() && state.value.active) {
            val reason = when {
                resetBlocked -> VisitCancelReason.RESET
                value.gate.state == GateState.UNKNOWN -> VisitCancelReason.UNKNOWN
                value.gate.state == GateState.RESTRICTED -> VisitCancelReason.RESTRICTED
                !value.screenActive -> VisitCancelReason.SCREEN_LEFT
                else -> VisitCancelReason.BACKGROUND
            }
            end(VisitPhase.CANCELLED, reason)
        }
        publish(state.value)
    }

    @Synchronized
    fun leaveScreen() = updateContext(context.copy(screenActive = false))

    @Synchronized
    fun request(): VisitRequestResult {
        if (!allowed()) return VisitRequestResult.Rejected("이용 가능한 화면에서만 요청할 수 있어요.")
        expireCurrentIfDue()
        if (state.value.active) return VisitRequestResult.Rejected("진행 중인 가상 방문을 먼저 마쳐 주세요.")
        val now = clock.nowMs()
        val remaining = (cooldownUntil[TARGET_ID] ?: 0L) - now
        if (remaining > 0) return VisitRequestResult.Rejected("같은 대상에게는 30초 간격으로 요청할 수 있어요.", remaining)
        val request = VisitRequest(
            VisitToken(UUID.randomUUID().toString(), generation), SENDER_ID, TARGET_ID,
            PURPOSE, MESSAGE_ID, now, now + REQUEST_TIMEOUT_MS,
        )
        cooldownUntil[TARGET_ID] = now + COOLDOWN_MS
        publish(VisitState(phase = VisitPhase.REQUESTED, request = request))
        scheduleExpiry(request)
        return VisitRequestResult.Created(request)
    }

    private fun matches(token: VisitToken) =
        token.generation == generation && state.value.request?.token == token

    private fun expireCurrentIfDue() {
        val current = state.value
        if (current.phase == VisitPhase.REQUESTED && clock.nowMs() >= current.request!!.expiresAtMonotonicMs) {
            end(VisitPhase.EXPIRED)
        }
    }

    private fun canAnswer(token: VisitToken): Boolean {
        if (!matches(token) || !allowed()) return false
        expireCurrentIfDue()
        return state.value.phase == VisitPhase.REQUESTED
    }

    @Synchronized
    fun accept(token: VisitToken): Boolean {
        if (!canAnswer(token)) return false
        timeout?.cancel()
        timeout = null
        // Both transitions are serialized; no delayed animation can reopen an ended request.
        publish(state.value.copy(phase = VisitPhase.ACCEPTED))
        publish(state.value.copy(phase = VisitPhase.VISITING))
        return true
    }

    @Synchronized
    fun reject(token: VisitToken): Boolean {
        if (!canAnswer(token)) return false
        end(VisitPhase.REJECTED)
        return true
    }

    @Synchronized
    fun withdraw(token: VisitToken): Boolean {
        if (!matches(token) || !allowed()) return false
        expireCurrentIfDue()
        if (!state.value.active) return false
        end(VisitPhase.WITHDRAWN)
        return true
    }

    @Synchronized
    fun close(token: VisitToken): Boolean {
        if (!matches(token) || !allowed() || state.value.phase != VisitPhase.VISITING) return false
        end(VisitPhase.COMPLETED)
        return true
    }

    private fun scheduleExpiry(request: VisitRequest) {
        timeout?.cancel()
        val remaining = request.expiresAtMonotonicMs - clock.nowMs()
        if (remaining <= 0) {
            end(VisitPhase.EXPIRED)
        } else {
            publish(state.value)
            // One outstanding wake-up drives both the display and the absolute deadline.
            timeout = scheduler.schedule(remaining.coerceAtMost(1_000L)) { expire(request.token) }
        }
    }

    @Synchronized
    private fun expire(token: VisitToken) {
        if (!matches(token) || state.value.phase != VisitPhase.REQUESTED) return
        // A late/early scheduler wake-up never substitutes for the monotonic deadline.
        scheduleExpiry(state.value.request!!)
    }

    /** Explicit presenter-only visit restart. It cannot open a restricted gate or reset B's save. */
    @Synchronized
    fun restartDemo(): Boolean {
        if (!allowed()) return false
        generation++
        timeout?.cancel()
        timeout = null
        cooldownUntil.clear()
        publish(VisitState())
        return true
    }

    /** A calls this before whole-game reset; no lock is held while waiting for any other owner. */
    @Synchronized
    fun beginReset(): Long {
        resetBlocked = true
        resetSequence++
        generation++
        end(VisitPhase.CANCELLED, VisitCancelReason.RESET)
        return resetSequence
    }

    @Synchronized
    fun finishReset(resetGeneration: Long, success: Boolean): Boolean {
        if (!resetBlocked || resetGeneration != resetSequence || !success) return false
        resetBlocked = false
        cooldownUntil.clear()
        publish(VisitState())
        return true
    }
}
