package io.kailand.app

import io.kailand.core.contract.MutationResult
import io.kailand.core.contract.RequestResult
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex

/** Only the new product flow has routes here. Legacy route names cannot be restored into it. */
enum class ProductRoute { CHARACTER, ISLAND, INVENTORY, WORLD, CONFIRMATION, SAVE, BRIEFING, NAVIGATION_FAILURE }
data class ProductScreen(val route: ProductRoute = ProductRoute.CHARACTER, val revision: Long = 0)

/** ViewModel-owned UI navigation; only an admitted external-navigation return may restore the island. */
class ProductNavigation(
    resumeNavigationReturn: Boolean = false,
    private val onNavigationReturnChanged: (Boolean) -> Unit = {},
) {
    private var awaitingNavigationReturn = resumeNavigationReturn
    private val mutable = MutableStateFlow(ProductScreen(
        if (resumeNavigationReturn) ProductRoute.ISLAND else ProductRoute.CHARACTER))
    val state = mutable.asStateFlow()
    private val selection = Mutex()
    private val selecting = MutableStateFlow(false)
    val busy = selecting.asStateFlow()
    private var receiptId: String? = null
    private var destinationKey: String? = null

    /** A usable destination wins on an admitted return, while ordinary cold-start selection remains required. */
    fun destinationObserved(key: String) {
        if (key == destinationKey || busy.value || receiptId != null ||
            state.value.route in setOf(ProductRoute.CHARACTER, ProductRoute.CONFIRMATION, ProductRoute.SAVE)) return
        destinationKey = key
        setNavigationReturn(false)
        if (state.value.route != ProductRoute.ISLAND) move(ProductRoute.ISLAND)
    }

    fun current(screen: ProductScreen) = state.value == screen

    fun open(screen: ProductScreen, target: ProductRoute): Boolean {
        if (!current(screen) || busy.value || receiptId != null) return false
        val targets = when (screen.route) {
            ProductRoute.CHARACTER -> emptySet()
            ProductRoute.ISLAND -> setOf(ProductRoute.CHARACTER, ProductRoute.INVENTORY, ProductRoute.WORLD,
                ProductRoute.CONFIRMATION, ProductRoute.SAVE, ProductRoute.NAVIGATION_FAILURE)
            ProductRoute.CONFIRMATION -> setOf(ProductRoute.ISLAND, ProductRoute.SAVE)
            ProductRoute.SAVE -> setOf(ProductRoute.ISLAND)
            ProductRoute.NAVIGATION_FAILURE -> setOf(ProductRoute.ISLAND)
            ProductRoute.INVENTORY, ProductRoute.WORLD -> setOf(ProductRoute.ISLAND)
            ProductRoute.BRIEFING -> setOf(ProductRoute.ISLAND) // acknowledgement persisted while UI was inactive
        }
        if (target !in targets) return false
        if (target == ProductRoute.CHARACTER) setNavigationReturn(false)
        move(target)
        return true
    }

    fun presentReceipt(id: String) {
        setNavigationReturn(false)
        if (receiptId == id) return
        receiptId = id
        move(ProductRoute.BRIEFING)
    }

    /** Only the caller's validated async receipt acknowledgement may use this transition. */
    fun acknowledged(screen: ProductScreen, id: String, target: ProductRoute): Boolean {
        if (!current(screen) || receiptId != id || target !in setOf(ProductRoute.ISLAND, ProductRoute.CHARACTER)) return false
        receiptId = null
        move(target)
        return true
    }

    /** A committed ack may outlive its screen. Clear the stale display fence without navigating. */
    fun receiptAbsent() { receiptId = null }

    /** A03 supplies the platform request. Accepted means only launch/request acceptance. */
    suspend fun depart(
        screen: ProductScreen, allowed: () -> Boolean, request: suspend () -> RequestResult,
    ): RequestResult {
        if (!selection.tryLock()) return RequestResult.Failed("이전 요청을 처리하고 있어요.")
        selecting.value = true
        try {
            if (!current(screen) || receiptId != null || !allowed() ||
                screen.route !in setOf(ProductRoute.ISLAND, ProductRoute.NAVIGATION_FAILURE)) {
                return RequestResult.Failed("현재 화면과 이용 상태를 다시 확인해 주세요.")
            }
            // Set before starting the external Activity: onSaveInstanceState may follow immediately.
            val previousReturn = awaitingNavigationReturn
            setNavigationReturn(true)
            val result = try {
                request()
            } catch (error: Exception) {
                setNavigationReturn(previousReturn)
                throw error
            }
            if (result is RequestResult.Failed) setNavigationReturn(previousReturn)
            currentCoroutineContext().ensureActive()
            if (current(screen) && receiptId == null && allowed()) {
                if (result is RequestResult.Failed) move(ProductRoute.NAVIGATION_FAILURE)
                else if (screen.route == ProductRoute.NAVIGATION_FAILURE) move(ProductRoute.ISLAND)
            }
            return result
        } finally {
            selecting.value = false
            selection.unlock()
        }
    }

    suspend fun selectCharacter(
        screen: ProductScreen, allowed: () -> Boolean, save: suspend () -> MutationResult,
    ): MutationResult {
        if (!selection.tryLock()) return MutationResult.Rejected("캐릭터를 저장하고 있어요.")
        selecting.value = true
        try {
            if (screen.route != ProductRoute.CHARACTER || !current(screen) || receiptId != null || !allowed()) return rejected()
            val result = save()
            currentCoroutineContext().ensureActive()
            if (result != MutationResult.Success) return result
            if (!current(screen) || receiptId != null || !allowed()) return rejected()
            move(ProductRoute.ISLAND)
            return MutationResult.Success
        } finally {
            selecting.value = false
            selection.unlock()
        }
    }

    /** Invoked only after the confirmed, persisted demo reset succeeds. */
    fun resetAfterDemo() {
        if (busy.value) return
        receiptId = null
        destinationKey = null
        setNavigationReturn(false)
        move(ProductRoute.CHARACTER)
    }

    private fun setNavigationReturn(awaiting: Boolean) {
        if (awaitingNavigationReturn == awaiting) return
        awaitingNavigationReturn = awaiting
        onNavigationReturnChanged(awaiting)
    }

    private fun move(target: ProductRoute) { mutable.value = ProductScreen(target, state.value.revision + 1) }
    private fun rejected() = MutationResult.Rejected("현재 화면과 이용 상태를 다시 확인해 주세요.")
}
