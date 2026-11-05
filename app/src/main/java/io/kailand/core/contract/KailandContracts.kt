package io.kailand.core.contract

import kotlinx.coroutines.flow.StateFlow

// A01 executable contract. These are app contracts, not Pleos SDK API signatures.
// Keep feature implementations out of this file. GameSave is B's internal storage model.
// USER_CONFIRMED is a user declaration, never SDK arrival or verified visitation.
enum class DataSource { DEMO, PLEOS, USER_CONFIRMED }
enum class JourneyKind { DAILY, REGION }
enum class JourneyPhase { IDLE, PREPARING, READY, TRAVELLING, ARRIVED, CANCELLED, ERROR }
enum class ItemType { FOOD, OUTFIT, DECOR }
enum class GateState { UNKNOWN, RESTRICTED, PARKED_ALLOWED }
enum class ArrivalEvidence { DEMO_FINAL_DESTINATION, PLEOS_FINAL_DESTINATION, USER_CONFIRMED_DESTINATION }

data class Catalog(
    val contentVersion: String,
    val description: String,
    val characters: List<CharacterDefinition>,
    val expressions: Map<String, String>,
    val assets: Map<String, AssetDefinition>,
    val regions: List<RegionDefinition>,
    val items: List<ItemDefinition>,
    val rules: GameRules,
)
data class CharacterDefinition(val id: String, val name: String, val assetKey: String)
data class AssetDefinition(val path: String, val kind: String)
data class RegionDefinition(
    val id: String,
    val name: String,
    val theme: String,
    val latitude: Double,
    val longitude: Double,
    val itemIds: List<String>,
)
data class ItemDefinition(
    val id: String,
    val regionId: String,
    val type: ItemType,
    val name: String,
    val assetKey: String,
)
data class GameRules(
    val dailyGrowthCap: Int,
    val dailyGrantLimit: Int,
    val timeZone: String,
    val decorationSlotIds: List<String>,
)

data class InteractionGate(val state: GateState, val reason: String? = null) {
    val canInteract: Boolean get() = state == GateState.PARKED_ALLOWED
}

data class DestinationSpec(
    val regionId: String?,
    val label: String,
    val latitude: Double,
    val longitude: Double,
)

data class JourneySnapshot(
    val journeyId: String?,
    val source: DataSource,
    val kind: JourneyKind,
    val phase: JourneyPhase,
    val routeRevision: Int,
    val destination: DestinationSpec?,
    val elapsedSeconds: Long? = null,
    val distanceMeters: Double? = null,
    val errorMessage: String? = null,
)

/** Source/evidence pairs must match. USER_CONFIRMED always has both measurements null.
 * B implementations must validate these invariants before storage; enum supply does not enable A's arrival input.
 */
data class ArrivalCommand(
    val journeyId: String,
    val source: DataSource,
    val kind: JourneyKind,
    val regionId: String?,
    val routeRevision: Int,
    val evidence: ArrivalEvidence,
    val completedAtEpochMs: Long,
    val elapsedSeconds: Long? = null,
    val distanceMeters: Double? = null,
)

data class AvatarPreview(val characterId: String, val outfitItemId: String?)

data class ArrivalReceipt(
    val journeyId: String,
    val source: DataSource,
    val kind: JourneyKind,
    val regionId: String?,
    val completedAtEpochMs: Long,
    val elapsedSeconds: Long?,
    val distanceMeters: Double?,
    val growthBefore: Int,
    val growthAfter: Int,
    val discoveredRegionId: String?,
    val grantedItemIds: List<String>,
)

data class GameSnapshot(
    val avatar: AvatarPreview,
    val dailyGrowth: Int,
    val ownedItemIds: Set<String>,
    val discoveredRegionIds: Set<String>,
    val decorationSlots: Map<String, String>,
    val pendingReceipt: ArrivalReceipt?,
)

sealed interface ArrivalResult {
    data class Applied(val receipt: ArrivalReceipt) : ArrivalResult
    data class AlreadyApplied(val receipt: ArrivalReceipt) : ArrivalResult
    data class Rejected(val reason: String) : ArrivalResult
    data class StorageFailure(val message: String) : ArrivalResult
}

sealed interface GameAction {
    data class SelectCharacter(val characterId: String) : GameAction
    data class EquipOutfit(val itemId: String?) : GameAction
    data class Eat(val itemId: String) : GameAction
    data class PlaceDecoration(val slotId: String, val itemId: String?) : GameAction
}

sealed interface MutationResult {
    data object Success : MutationResult
    data class Rejected(val reason: String) : MutationResult
    data class StorageFailure(val message: String) : MutationResult
}

interface GameService {
    val state: StateFlow<GameSnapshot>
    suspend fun completeArrival(command: ArrivalCommand): ArrivalResult
    suspend fun apply(action: GameAction): MutationResult
    suspend fun acknowledgeReceipt(journeyId: String): MutationResult
    suspend fun resetDemoProgress(): MutationResult
}

sealed interface RequestResult {
    data object Accepted : RequestResult // request accepted, not arrival or route success
    data class Failed(val message: String) : RequestResult
}

interface JourneyPort {
    val state: StateFlow<JourneySnapshot>
    suspend fun requestDestination(destination: DestinationSpec, kind: JourneyKind): RequestResult
    suspend fun cancel(): RequestResult
    suspend fun retryArrival(): RequestResult // retry the original validated command, not a new journey
}

sealed interface DemoInput {
    data object StartTravel : DemoInput
    data object RepeatLastArrival : DemoInput // DEMO only; preserve the original journey ID and command
    data class Complete(
        val completedAtEpochMs: Long,
        val elapsedSeconds: Long?,
        val distanceMeters: Double?,
    ) : DemoInput
    data object Cancel : DemoInput
}

interface DemoControlPort {
    // Await this step: Complete/Repeat wait for GameService; failures return Failed.
    // C checks the resulting committed state; requestDestination acceptance is separate.
    suspend fun dispatch(input: DemoInput): RequestResult
}

// UI API sketches (Compose declarations belong to each feature, not this pure DTO file):
// IslandScreen(snapshot, catalog, gate, onAction, onNavigate)
// CompanionScreen(journey, avatar, gate, avatarSlot, onHideChanged)
// WorldScreen(snapshot, catalog, gate, onOpenInventory)
// BriefingScreen(receipt, catalog, avatar, gate, onDismiss, onOpenItems)
// AppRoot injects the renderer slot and callbacks. No feature-to-feature imports.
