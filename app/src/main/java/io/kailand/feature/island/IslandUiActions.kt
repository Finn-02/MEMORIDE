package io.kailand.feature.island

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.kailand.core.contract.GameAction
import io.kailand.core.contract.GameSnapshot
import io.kailand.core.contract.InteractionGate
import io.kailand.core.contract.MutationResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

@Composable
internal fun rememberIslandActions(
    snapshot: GameSnapshot,
    view: IslandPresentation,
    gate: InteractionGate,
    onAction: suspend (GameAction) -> MutationResult,
): IslandUiActions {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope()
    val latestGate by rememberUpdatedState(gate)
    val latestSnapshot by rememberUpdatedState(snapshot)
    val latestView by rememberUpdatedState(view)
    val latestCallback by rememberUpdatedState(onAction)
    val actions = remember(lifecycle, scope) {
        IslandUiActions(
            scope, { latestGate.canInteract }, { lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) },
            { latestSnapshot }, { latestView }, { latestCallback(it) },
        )
    }
    DisposableEffect(lifecycle, actions) {
        val observer = LifecycleEventObserver { _, _ -> actions.refreshForeground() }
        lifecycle.addObserver(observer)
        actions.refreshForeground()
        onDispose {
            lifecycle.removeObserver(observer)
            actions.clearFeedback()
        }
    }
    LaunchedEffect(actions, gate.canInteract, snapshot) {
        if (!gate.canInteract) actions.clearFeedback()
        else actions.discardOldMeal(snapshot)
    }
    return actions
}

internal class IslandUiActions(
    private val scope: CoroutineScope,
    private val permittedNow: () -> Boolean,
    private val foregroundNow: () -> Boolean,
    private val snapshot: () -> GameSnapshot,
    private val view: () -> IslandPresentation,
    private val onAction: suspend (GameAction) -> MutationResult,
) {
    // The lifecycle observer invalidates composition. Clicks also read the live lifecycle directly.
    private var foregroundAllowed by mutableStateOf(foregroundNow())
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    private var mealSnapshot by mutableStateOf<GameSnapshot?>(null)
    private var mealJob: Job? = null
    private var feedbackVersion = 0

    val enabled: Boolean get() = foregroundAllowed && canInteractNow()
    val eating: Boolean get() = enabled && mealSnapshot == snapshot()

    fun refreshForeground() {
        foregroundAllowed = foregroundNow()
        if (!foregroundAllowed) clearFeedback()
    }

    fun submit(action: GameAction) {
        if (busy || !canInteractNow() || !view().accepts(action)) return
        val version = feedbackVersion
        busy = true
        error = null
        stopMeal()
        scope.launch {
            try {
                if (version != feedbackVersion || !canInteractNow() || !view().accepts(action)) return@launch
                val startSnapshot = snapshot()
                val result = onAction(action)
                ensureActive()
                if (version != feedbackVersion || !canInteractNow()) return@launch
                when (result) {
                    MutationResult.Success -> if (action is GameAction.Eat && startSnapshot == snapshot()) {
                        showMeal(startSnapshot)
                    }
                    is MutationResult.Rejected -> error = "요청을 적용할 수 없어요."
                    is MutationResult.StorageFailure -> error = "저장 상태를 확인할 수 없어요. 다시 시도해 주세요."
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (version == feedbackVersion && canInteractNow()) error = "요청 처리 중 오류가 발생했어요."
            } finally {
                busy = false
            }
        }
    }

    private fun canInteractNow(): Boolean = permittedNow() && foregroundNow()

    fun clearFeedback() {
        feedbackVersion += 1
        error = null
        stopMeal()
    }

    fun discardOldMeal(current: GameSnapshot) {
        if (mealSnapshot != current) stopMeal()
    }

    private fun showMeal(current: GameSnapshot) {
        stopMeal()
        mealSnapshot = current
        mealJob = scope.launch {
            delay(1_500)
            mealSnapshot = null
        }
    }

    private fun stopMeal() {
        mealJob?.cancel()
        mealJob = null
        mealSnapshot = null
    }
}
