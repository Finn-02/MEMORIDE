package io.kailand.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.kailand.core.contract.*

/** Additive compile-checked proposals, not replacements for existing screen signatures.
 * Consumers confirm their slot before A wires it into AppRoot. All app-level callbacks must
 * recheck current foreground/gate/generation; an enabled button alone grants no permission.
 */
typealias CharacterSelectionSlot = @Composable (
    snapshot: GameSnapshot, catalog: Catalog, gate: InteractionGate,
    onSelect: suspend (String) -> MutationResult, modifier: Modifier,
) -> Unit

/** A's purpose-specific slot; B owns layout. Replaces actions only for a usable candidate.
 * A may supply an explanatory inactive card with replacesActions=false.
 */
data class IslandDestinationSlot(
    val replacesActions: Boolean,
    val content: @Composable () -> Unit,
)

typealias NewIslandSlot = @Composable (
    snapshot: GameSnapshot, catalog: Catalog, gate: InteractionGate,
    destination: IslandDestinationSlot?,
    onSelectCharacter: () -> Unit, onStartJourney: () -> Unit,
    onOpenInventory: () -> Unit, onOpenWorld: () -> Unit, modifier: Modifier,
) -> Unit

typealias NewWorldSlot = @Composable (
    snapshot: GameSnapshot, catalog: Catalog, history: RegionVisitHistory,
    gate: InteractionGate, onClose: () -> Unit, modifier: Modifier,
) -> Unit

/** A's callbacks await receipt acknowledgement before changing route. PM only displays results.
 * Existing receipt measurements retain null; fuel economy/odometer are unavailable in this flow.
 */
typealias NewBriefingSlot = @Composable (
    receipt: ArrivalReceipt, catalog: Catalog, avatar: AvatarPreview, gate: InteractionGate,
    onIsland: suspend () -> MutationResult,
    onSelectCharacter: suspend () -> MutationResult, modifier: Modifier,
) -> Unit

sealed interface ArrivalSaveUiState {
    data object Saving : ArrivalSaveUiState
    data class Failed(val message: String) : ArrivalSaveUiState
}

/** No command, timer or storage object is passed to this pure PM screen.
 * A serializes retry/dismiss; dismiss preserves the original retry command.
 */
typealias ArrivalSaveSlot = @Composable (
    state: ArrivalSaveUiState, gate: InteractionGate,
    onRetry: () -> Unit, onDismiss: () -> Unit, modifier: Modifier,
) -> Unit
