package io.kailand.feature.island

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import io.kailand.core.contract.Catalog
import io.kailand.core.contract.GameAction
import io.kailand.core.contract.GameSnapshot
import io.kailand.core.contract.InteractionGate
import io.kailand.core.contract.ItemType
import io.kailand.core.contract.MutationResult
import io.kailand.core.ui.MemoridePleosTheme
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal data class InventorySelection(val type: ItemType, val decorationId: String? = null) {
    fun selectTab(tab: ItemType): InventorySelection = if (type == tab) this else InventorySelection(tab)

    fun cancelDecoration(): InventorySelection = if (decorationId == null) this else copy(decorationId = null)

    fun selectedDecoration(cards: List<InventoryCardModel>): InventoryCardModel? =
        if (type == ItemType.DECOR && decorationId != null) {
            cards.singleOrNull { it.item.id == decorationId && it.canChooseSlot }
        } else null

    fun revalidate(cards: List<InventoryCardModel>): InventorySelection =
        if (selectedDecoration(cards) == null) cancelDecoration() else this

    fun placement(
        slot: IslandSlot, cards: List<InventoryCardModel>, slots: List<IslandSlot>,
    ): GameAction.PlaceDecoration? =
        selectedDecoration(cards)?.takeIf { slot in slots }?.placeIn(slot)

    fun afterAction(action: GameAction, result: MutationResult): InventorySelection =
        if (result == MutationResult.Success && action is GameAction.PlaceDecoration &&
            action.itemId != null && action.itemId == decorationId
        ) cancelDecoration() else this
}

// Only UI choices are saved, as a list of strings supported by the Android saveable registry.
internal val InventorySelectionSaver = listSaver<InventorySelection, String>(
    save = { listOfNotNull(it.type.name, it.decorationId) },
    restore = { values ->
        val type = ItemType.entries.firstOrNull { it.name == values.firstOrNull() }
        if (type == null || values.size !in 1..2) null
        else InventorySelection(type, values.getOrNull(1)?.takeIf { type == ItemType.DECOR && it.isNotBlank() })
    },
)

internal fun IslandUiActions.canEditInventory(closed: Boolean): Boolean = !closed && enabled && !busy

/** S03 standalone screen. The host owns readiness, navigation and the persisted snapshot. */
@Composable
fun NewInventoryScreen(
    snapshot: GameSnapshot,
    catalog: Catalog,
    gate: InteractionGate,
    onAction: suspend (GameAction) -> MutationResult,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InventoryScreenContent(snapshot, catalog, gate, onAction, onClose, modifier, ItemType.FOOD)
}

@Composable
internal fun InventoryScreenContent(
    snapshot: GameSnapshot,
    catalog: Catalog,
    gate: InteractionGate,
    onAction: suspend (GameAction) -> MutationResult,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    initialTab: ItemType,
) {
    val view = remember(snapshot, catalog) { islandPresentation(snapshot, catalog) }
    var selection by rememberSaveable(stateSaver = InventorySelectionSaver) {
        mutableStateOf(InventorySelection(initialTab))
    }
    var closed by remember { mutableStateOf(false) }
    val actions = rememberIslandActions(snapshot, view, gate) { action ->
        val result = onAction(action)
        currentCoroutineContext().ensureActive()
        if (!closed) selection = selection.afterAction(action, result)
        result
    }
    val shownSelection = selection
    val type = shownSelection.type
    val cardsByType = remember(view, catalog) {
        ItemType.entries.associateWith { inventoryCards(it, view, catalog) }
    }
    val cards = cardsByType.getValue(type)
    val latestCards by rememberUpdatedState(cards)
    val latestView by rememberUpdatedState(view)
    val enabled = actions.canEditInventory(closed)

    fun currentCard(id: String) = latestCards.singleOrNull { it.item.id == id }

    val picking = shownSelection.selectedDecoration(cards)
    LaunchedEffect(shownSelection, picking?.item?.id) {
        if (selection == shownSelection) selection = selection.revalidate(latestCards)
    }

    val close = {
        if (!closed) {
            closed = true
            selection = InventorySelection(initialTab)
            actions.clearFeedback()
            onClose()
        }
    }
    val useCard: (InventoryCardModel) -> Unit = useCard@{ shown ->
        if (!actions.canEditInventory(closed) || selection.type != shown.item.type) return@useCard
        val current = currentCard(shown.item.id) ?: return@useCard
        if (current.canChooseSlot && shown.canChooseSlot) {
            selection = selection.copy(decorationId = current.item.id)
        } else {
            val action = shown.action() ?: return@useCard
            actions.submit(action) {
                !closed && selection.type == shown.item.type && currentCard(shown.item.id)?.action() == action
            }
        }
    }
    val place: (InventoryCardModel, IslandSlot) -> Unit = place@{ shown, slot ->
        if (!actions.canEditInventory(closed) || selection.decorationId != shown.item.id) return@place
        val action = selection.placement(slot, latestCards, latestView.slots) ?: return@place
        actions.submit(action) {
            !closed && selection.placement(slot, latestCards, latestView.slots) == action
        }
    }

    MemoridePleosTheme {
        InventoryPleosLayout(
            snapshot = snapshot, catalog = catalog, view = view, type = type, cards = cards,
            gate = gate, actions = actions, enabled = enabled,
            eating = actions.eating && !closed,
            hasCatalogIssues = view.hasCatalogIssues || cards.size != 3,
            allLocked = cardsByType.values.all { list -> list.size == 3 && list.none { it.item.owned } },
            picking = picking,
            onTab = { tab ->
                if (actions.canEditInventory(closed) && selection.type != tab) {
                    selection = selection.selectTab(tab)
                    actions.clearFeedback()
                }
            },
            onUse = useCard, onClose = close,
            onCancelDecoration = {
                if (actions.canEditInventory(closed)) selection = selection.cancelDecoration()
            },
            onPlace = place, modifier = modifier,
        )
    }
}
