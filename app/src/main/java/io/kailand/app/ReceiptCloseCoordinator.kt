package io.kailand.app

import io.kailand.core.contract.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex

/** Keep the displayed receipt alive while B clears pending, until the async result permits routing. */
class ReceiptCloseCoordinator(
    private val game: GameService,
    private val commands: GameCommandCoordinator,
    private val generation: Long,
    private val callerIsCurrent: () -> Boolean,
) {
    private val lock = Mutex()
    private val mutable = MutableStateFlow<ArrivalReceipt?>(null)
    val heldReceipt = mutable.asStateFlow()

    suspend fun close(
        receipt: ArrivalReceipt, screenIsCurrent: () -> Boolean = { true }, onSuccess: () -> Unit,
    ): MutationResult {
        if (!lock.tryLock()) return MutationResult.Rejected("결과 확인을 처리하고 있어요.")
        try {
            if (!callerIsCurrent() || !screenIsCurrent()) return MutationResult.Rejected("현재 화면을 다시 확인해 주세요.")
            mutable.value = receipt
            val result = commands.acknowledge(generation, receipt.journeyId) { callerIsCurrent() && screenIsCurrent() }
            if (result == MutationResult.Success) {
                currentCoroutineContext().ensureActive()
                if (!commands.canRoute(generation) { callerIsCurrent() && screenIsCurrent() } ||
                    game.state.value.pendingReceipt != null) {
                    return MutationResult.Rejected("새로운 기록과 이용 상태를 다시 확인해 주세요.")
                }
                onSuccess()
            }
            return result
        } finally {
            mutable.value = null
            lock.unlock()
        }
    }
}
