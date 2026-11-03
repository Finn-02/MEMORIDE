package io.kailand.feature.briefing

import io.kailand.core.contract.InteractionGate
import io.kailand.core.contract.MutationResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex

internal enum class BriefingAction { DISMISS, OPEN_ITEMS }

internal data class BriefingActionState(
    val pending: BriefingAction? = null,
    val failed: BriefingAction? = null,
    val error: String? = null,
    val completed: Boolean = false,
)

/** One instance per immutable receipt/gate session. A owns acknowledgement and routing. */
internal class BriefingActions(private val gate: InteractionGate) {
    private val mutableState = MutableStateFlow(BriefingActionState())
    val state = mutableState.asStateFlow()
    private val lock = Mutex()
    private var active = true

    fun deactivate() { active = false }

    suspend fun perform(action: BriefingAction, callback: suspend () -> MutationResult) {
        if (!active || !gate.canInteract || !lock.tryLock()) return
        try {
            if (mutableState.value.completed) return
            mutableState.value = BriefingActionState(pending = action)
            val result = callback()
            currentCoroutineContext().ensureActive()
            if (!active) return
            mutableState.value = when (result) {
                MutationResult.Success -> BriefingActionState(completed = true)
                is MutationResult.Rejected -> failure(action, "지금은 처리할 수 없어요. 이용 상태를 확인하고 다시 시도해 주세요.")
                is MutationResult.StorageFailure -> failure(action, "확인 내용을 저장하지 못했어요. 다시 시도해 주세요.")
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            if (active) mutableState.value = failure(action, "처리를 마치지 못했어요. 다시 시도해 주세요.")
        } finally {
            if (active && mutableState.value.pending != null) mutableState.value = BriefingActionState()
            lock.unlock()
        }
    }

    private fun failure(action: BriefingAction, message: String) =
        BriefingActionState(failed = action, error = message)
}
