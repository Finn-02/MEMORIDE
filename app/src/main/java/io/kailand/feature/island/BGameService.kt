package io.kailand.feature.island

import android.content.Context
import io.kailand.core.contract.ArrivalCommand
import io.kailand.core.contract.ArrivalEvidence
import io.kailand.core.contract.ArrivalReceipt
import io.kailand.core.contract.ArrivalResult
import io.kailand.core.contract.DataSource
import io.kailand.core.contract.GameAction
import io.kailand.core.contract.GameService
import io.kailand.core.contract.GameSnapshot
import io.kailand.core.contract.JourneyKind
import io.kailand.core.contract.MutationResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** B-only readiness signal. The shared GameService/GameSnapshot DTOs remain unchanged. */
enum class GameReadiness { LOADING, READY, READ_ERROR }

/** A creates a single instance in AppRoot's app-owned container when wiring B01. */
fun createBGameService(context: Context, scope: CoroutineScope): BGameService =
    BGameService(PreferencesGameSaveStore(context.applicationContext), scope) {
        withContext(Dispatchers.IO) {
            context.applicationContext.assets.open("content/catalog.json")
                .bufferedReader(Charsets.UTF_8).use { RegionRewards.fromJson(it.readText()) }
        }
    }

class BGameService internal constructor(
    private val store: GameSaveStore,
    scope: CoroutineScope,
    loadRegionRewards: suspend () -> RegionRewards,
) : GameService {
    private val mutationMutex = Mutex()
    private lateinit var regionRewards: RegionRewards
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
                val rewards = loadRegionRewards()
                mutationMutex.withLock {
                    regionRewards = rewards
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

    /** REGION only. Duplicate lookup and every reward effect share one transaction. */
    override suspend fun completeArrival(command: ArrivalCommand): ArrivalResult {
        if (_readiness.value != GameReadiness.READY) {
            return ArrivalResult.StorageFailure("Game save is not ready")
        }
        return mutationMutex.withLock {
            if (_readiness.value != GameReadiness.READY) {
                return@withLock ArrivalResult.StorageFailure("Game save is not ready")
            }
            invalidArrivalReason(command)?.let {
                return@withLock ArrivalResult.Rejected(it)
            }
            try {
                var result: ArrivalResult? = null
                val committed = store.update { current ->
                    current.toSnapshot()
                    val original = current.processedReceipts[command.journeyId]
                    if (original != null) {
                        result = ArrivalResult.AlreadyApplied(original.toContract())
                        current // Never replace a newer pending receipt or reopen an acknowledged one.
                    } else {
                        val discovered = command.regionId?.takeIf { it !in current.discoveredRegionIds }
                        val gifts = discovered?.let(regionRewards::itemsFor) ?: emptyList()
                        val receipt = ArrivalReceipt(
                            journeyId = command.journeyId,
                            source = command.source,
                            kind = command.kind,
                            regionId = command.regionId,
                            completedAtEpochMs = command.completedAtEpochMs,
                            elapsedSeconds = command.elapsedSeconds,
                            distanceMeters = command.distanceMeters,
                            growthBefore = current.dailyGrowth,
                            growthAfter = current.dailyGrowth,
                            discoveredRegionId = discovered,
                            grantedItemIds = gifts,
                        )
                        result = ArrivalResult.Applied(receipt)
                        current.copy(
                            ownedItemIds = current.ownedItemIds + gifts,
                            discoveredRegionIds = current.discoveredRegionIds + listOfNotNull(discovered),
                            processedReceipts = current.processedReceipts +
                                (command.journeyId to SavedReceipt.fromContract(receipt)),
                            pendingReceiptId = command.journeyId,
                        )
                    }
                }
                _state.value = committed.toSnapshot()
                checkNotNull(result)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                ArrivalResult.StorageFailure("Arrival could not be saved")
            }
        }
    }

    private fun invalidArrivalReason(command: ArrivalCommand): String? = when {
        command.kind != JourneyKind.REGION -> "DAILY arrival handling is not implemented"
        !JOURNEY_ID.matches(command.journeyId) -> "Invalid journey ID"
        command.routeRevision < 0 -> "Invalid route revision"
        (command.source == DataSource.DEMO) !=
            (command.evidence == ArrivalEvidence.DEMO_FINAL_DESTINATION) -> "Source and evidence do not match"
        command.completedAtEpochMs < 0 -> "Invalid completion time"
        command.elapsedSeconds != null && command.elapsedSeconds < 0 -> "Invalid elapsed time"
        command.distanceMeters != null &&
            (!command.distanceMeters.isFinite() || command.distanceMeters < 0) -> "Invalid distance"
        command.regionId != null && !regionRewards.contains(command.regionId) -> "Unknown region"
        else -> null
    }

    /** B03 implements item/character mutations. No speculative writes in B01. */
    override suspend fun apply(action: GameAction): MutationResult {
        if (_readiness.value != GameReadiness.READY) {
            return MutationResult.StorageFailure("Game save is not ready")
        }
        return MutationResult.Rejected("B03 action handling is not implemented")
    }

    /** Clear only the matching pending pointer, keeping all original receipts. */
    override suspend fun acknowledgeReceipt(journeyId: String): MutationResult {
        if (_readiness.value != GameReadiness.READY) {
            return MutationResult.StorageFailure("Game save is not ready")
        }
        return mutationMutex.withLock {
            if (_readiness.value != GameReadiness.READY) {
                return@withLock MutationResult.StorageFailure("Game save is not ready")
            }
            try {
                val committed = store.update { current ->
                    current.toSnapshot()
                    when {
                        journeyId !in current.processedReceipts -> throw ReceiptMismatch()
                        current.pendingReceiptId == journeyId -> current.copy(pendingReceiptId = null)
                        current.pendingReceiptId == null -> current // Already acknowledged.
                        else -> throw ReceiptMismatch()
                    }
                }
                _state.value = committed.toSnapshot()
                MutationResult.Success
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: ReceiptMismatch) {
                MutationResult.Rejected("Receipt ID does not match the pending receipt")
            } catch (_: Exception) {
                MutationResult.StorageFailure("Receipt acknowledgement could not be saved")
            }
        }
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

private class ReceiptMismatch : Exception()

private val JOURNEY_ID = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
