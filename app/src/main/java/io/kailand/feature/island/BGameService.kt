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
import io.kailand.core.contract.ItemType
import io.kailand.core.contract.JourneyKind
import io.kailand.core.contract.MutationResult
import io.kailand.core.contract.RegionVisitHistory
import io.kailand.core.contract.RegionVisitSummary
import java.time.Instant
import java.time.LocalDate
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

    private val _regionVisitHistory = MutableStateFlow<RegionVisitHistory>(RegionVisitHistory.Loading)

    /** Committed game visits. A can pass this to C without exposing the save. */
    val regionVisitHistory: StateFlow<RegionVisitHistory> = _regionVisitHistory.asStateFlow()

    init {
        scope.launch {
            try {
                val saved = store.read() ?: GameSave()
                val snapshot = saved.toSnapshot()
                val rewards = loadRegionRewards()
                mutationMutex.withLock {
                    regionRewards = rewards
                    _state.value = snapshot
                    _regionVisitHistory.value = saved.toVisitHistory()
                    _readiness.value = GameReadiness.READY
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutationMutex.withLock {
                    _regionVisitHistory.value = RegionVisitHistory.Unavailable("Game save could not be read")
                    _readiness.value = GameReadiness.READ_ERROR
                }
            }
        }
    }

    /** Duplicate lookup, REGION growth and first-discovery gifts share one transaction. */
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
                        // DAILY never carries a region, even if the caller supplied one.
                        val regionId = command.regionId.takeIf { command.kind == JourneyKind.REGION }
                        val discovered = regionId?.takeIf { it !in current.discoveredRegionIds }
                        val gifts = discovered?.let(regionRewards::itemsFor) ?: emptyList()
                        val grantDate = regionId?.let { completionDate(command.completedAtEpochMs) }?.takeIf { date ->
                            current.dailyGrowth < regionRewards.dailyGrowthCap && !hasUsedGrowthDate(current, date)
                        }
                        val growthAfter = current.dailyGrowth + if (grantDate != null) 1 else 0
                        val receipt = ArrivalReceipt(
                            journeyId = command.journeyId,
                            source = command.source,
                            kind = command.kind,
                            regionId = regionId,
                            completedAtEpochMs = command.completedAtEpochMs,
                            elapsedSeconds = command.elapsedSeconds,
                            distanceMeters = command.distanceMeters,
                            growthBefore = current.dailyGrowth,
                            growthAfter = growthAfter,
                            discoveredRegionId = discovered,
                            grantedItemIds = gifts,
                        )
                        result = ArrivalResult.Applied(receipt)
                        current.copy(
                            dailyGrowth = growthAfter,
                            lastDailyGrantDate = if (grantDate == null) current.lastDailyGrantDate else
                                maxOf(grantDate, current.lastDailyGrantDate?.let(LocalDate::parse) ?: grantDate).toString(),
                            ownedItemIds = current.ownedItemIds + gifts,
                            discoveredRegionIds = current.discoveredRegionIds + listOfNotNull(discovered),
                            processedReceipts = current.processedReceipts +
                                (command.journeyId to SavedReceipt.fromContract(receipt)),
                            pendingReceiptId = command.journeyId,
                        )
                    }
                }
                _state.value = committed.toSnapshot()
                _regionVisitHistory.value = committed.toVisitHistory()
                checkNotNull(result)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                ArrivalResult.StorageFailure("Arrival could not be saved")
            }
        }
    }

    private fun GameSave.toVisitHistory(): RegionVisitHistory.Available {
        // The validated map has one receipt per journey ID, including acknowledged receipts.
        val regions = processedReceipts.values
            .filter { it.kind == JourneyKind.REGION.name }
            .groupBy { it.regionId }
            .mapNotNull { (regionId, receipts) ->
                if (regionId == null || !regionRewards.contains(regionId)) return@mapNotNull null
                RegionVisitSummary(
                    regionId = regionId,
                    visitCount = receipts.size,
                    firstVisitedAtEpochMs = receipts.minOf { it.completedAtEpochMs },
                    lastVisitedAtEpochMs = receipts.maxOf { it.completedAtEpochMs },
                )
            }
            .sortedBy { it.regionId }
        return RegionVisitHistory.Available(regions)
    }

    private fun completionDate(completedAtEpochMs: Long): LocalDate = Instant.ofEpochMilli(completedAtEpochMs)
        .atZone(regionRewards.timeZone).toLocalDate()

    private fun hasUsedGrowthDate(save: GameSave, date: LocalDate): Boolean =
        save.lastDailyGrantDate == date.toString() || save.processedReceipts.values.any { receipt ->
            // Old registered REGION completions count even when they awarded no growth.
            val usesDate = when (receipt.kind) {
                JourneyKind.REGION.name -> receipt.regionId?.let(regionRewards::contains) == true
                JourneyKind.DAILY.name -> receipt.growthAfter > receipt.growthBefore
                else -> false
            }
            usesDate && completionDate(receipt.completedAtEpochMs) == date
        }

    private fun invalidArrivalReason(command: ArrivalCommand): String? = when {
        !JOURNEY_ID.matches(command.journeyId) -> "Invalid journey ID"
        command.routeRevision < 0 -> "Invalid route revision"
        command.evidence != when (command.source) {
            DataSource.DEMO -> ArrivalEvidence.DEMO_FINAL_DESTINATION
            DataSource.PLEOS -> ArrivalEvidence.PLEOS_FINAL_DESTINATION
            DataSource.USER_CONFIRMED -> ArrivalEvidence.USER_CONFIRMED_DESTINATION
        } -> "Source and evidence do not match"
        command.source == DataSource.USER_CONFIRMED &&
            (command.elapsedSeconds != null || command.distanceMeters != null) ->
            "User-confirmed arrivals must not contain measurements"
        command.completedAtEpochMs < 0 -> "Invalid completion time"
        command.elapsedSeconds != null && command.elapsedSeconds < 0 -> "Invalid elapsed time"
        command.distanceMeters != null &&
            (!command.distanceMeters.isFinite() || command.distanceMeters < 0) -> "Invalid distance"
        command.kind == JourneyKind.REGION && command.regionId != null &&
            !regionRewards.contains(command.regionId) -> "Unknown region"
        else -> null
    }

    override suspend fun apply(action: GameAction): MutationResult {
        if (_readiness.value != GameReadiness.READY) {
            return MutationResult.StorageFailure("Game save is not ready")
        }
        return mutationMutex.withLock {
            if (_readiness.value != GameReadiness.READY) {
                return@withLock MutationResult.StorageFailure("Game save is not ready")
            }
            try {
                val committed = store.update { current -> applyToSave(current, action) }
                _state.value = committed.toSnapshot()
                MutationResult.Success
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (rejected: RejectedAction) {
                MutationResult.Rejected(rejected.reason)
            } catch (_: Exception) {
                MutationResult.StorageFailure("Game action could not be completed")
            }
        }
    }

    private fun applyToSave(current: GameSave, action: GameAction): GameSave = when (action) {
        is GameAction.SelectCharacter -> {
            if (action.characterId !in regionRewards.characterIds) throw RejectedAction("Unknown character")
            current.copy(characterId = action.characterId)
        }
        is GameAction.EquipOutfit -> {
            action.itemId?.let { requireOwnedItem(current, it, ItemType.OUTFIT) }
            current.copy(equippedOutfitId = action.itemId)
        }
        is GameAction.Eat -> {
            requireOwnedItem(current, action.itemId, ItemType.FOOD)
            current // The UI uses Success to play the temporary expression.
        }
        is GameAction.PlaceDecoration -> {
            if (action.slotId !in regionRewards.decorationSlotIds) throw RejectedAction("Unknown decoration slot")
            val itemId = action.itemId
            if (itemId == null) {
                current.copy(decorationSlots = current.decorationSlots - action.slotId)
            } else {
                requireOwnedItem(current, itemId, ItemType.DECOR)
                if (current.decorationSlots.any { (slot, item) -> slot != action.slotId && item == itemId }) {
                    throw RejectedAction("Decoration is already placed in another slot")
                }
                current.copy(decorationSlots = current.decorationSlots + (action.slotId to itemId))
            }
        }
    }

    private fun requireOwnedItem(save: GameSave, itemId: String, expectedType: ItemType) {
        val type = regionRewards.itemTypes[itemId] ?: throw RejectedAction("Unknown item")
        if (type != expectedType) throw RejectedAction("Wrong item type")
        if (itemId !in save.ownedItemIds) throw RejectedAction("Item is not owned")
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
                _regionVisitHistory.value = committed.toVisitHistory()
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
private class RejectedAction(val reason: String) : Exception(reason)

private val JOURNEY_ID = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
