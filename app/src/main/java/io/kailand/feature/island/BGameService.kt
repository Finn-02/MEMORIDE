package io.kailand.feature.island

import android.content.Context
import io.kailand.core.contract.ArrivalCommand
import io.kailand.core.contract.ArrivalResult
import io.kailand.core.contract.GameAction
import io.kailand.core.contract.GameService
import io.kailand.core.contract.GameSnapshot
import io.kailand.core.contract.MutationResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** B-only readiness signal. The shared GameService/GameSnapshot DTOs remain unchanged. */
enum class GameReadiness { LOADING, READY, READ_ERROR }

/** A creates a single instance in AppRoot's app-owned container when wiring B01. */
fun createBGameService(context: Context, scope: CoroutineScope): BGameService =
    BGameService(PreferencesGameSaveStore(context.applicationContext), scope)

class BGameService internal constructor(
    private val store: GameSaveStore,
    scope: CoroutineScope,
) : GameService {
    private val mutationMutex = Mutex()
    // While loading, this is only a placeholder: UI must examine readiness separately.
    private val _state = MutableStateFlow(GameSave().toSnapshot())
    override val state: StateFlow<GameSnapshot> = _state.asStateFlow()

    private val _readiness = MutableStateFlow(GameReadiness.LOADING)
    val readiness: StateFlow<GameReadiness> = _readiness.asStateFlow()

    init {
        scope.launch {
            try {
                val saved = store.read() ?: GameSave()
                val snapshot = saved.toSnapshot()
                mutationMutex.withLock {
                    _state.value = snapshot
                    _readiness.value = GameReadiness.READY
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutationMutex.withLock { _readiness.value = GameReadiness.READ_ERROR }
            }
        }
    }

    /** B02 implements arrivals. Never report reward success from a placeholder. */
    override suspend fun completeArrival(command: ArrivalCommand): ArrivalResult {
        if (_readiness.value != GameReadiness.READY) {
            return ArrivalResult.StorageFailure("Game save is not ready")
        }
        return ArrivalResult.Rejected("B02 arrival handling is not implemented")
    }

    /** B03 implements item/character mutations. No speculative writes in B01. */
    override suspend fun apply(action: GameAction): MutationResult {
        if (_readiness.value != GameReadiness.READY) {
            return MutationResult.StorageFailure("Game save is not ready")
        }
        return MutationResult.Rejected("B03 action handling is not implemented")
    }

    /** B02 implements receipt acknowledgement, including its ID-matching rule. */
    override suspend fun acknowledgeReceipt(journeyId: String): MutationResult {
        if (_readiness.value != GameReadiness.READY) {
            return MutationResult.StorageFailure("Game save is not ready")
        }
        return MutationResult.Rejected("B02 receipt acknowledgement is not implemented")
    }

    /** Actual reset only when the prior save was successfully loaded. */
    override suspend fun resetDemoProgress(): MutationResult {
        if (_readiness.value != GameReadiness.READY) {
            return MutationResult.StorageFailure("Game save is not ready")
        }
        return mutationMutex.withLock {
            if (_readiness.value != GameReadiness.READY) {
                return@withLock MutationResult.StorageFailure("Game save is not ready")
            }
            try {
                val committed = store.update { GameSave() }
                _state.value = committed.toSnapshot()
                MutationResult.Success
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                MutationResult.StorageFailure("Game reset could not be saved")
            }
        }
    }
}
