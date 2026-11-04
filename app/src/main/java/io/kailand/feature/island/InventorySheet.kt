package io.kailand.feature.island

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.kailand.core.contract.Catalog
import io.kailand.core.contract.GameAction
import io.kailand.core.contract.GameSnapshot
import io.kailand.core.contract.InteractionGate
import io.kailand.core.contract.ItemType
import io.kailand.core.contract.MutationResult

/** The caller owns visibility and game readiness, including when opening from a different screen. */
@Composable
fun InventorySheet(
    snapshot: GameSnapshot,
    catalog: Catalog,
    gate: InteractionGate,
    onAction: suspend (GameAction) -> MutationResult,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = remember(snapshot, catalog) { islandPresentation(snapshot, catalog) }
    val actions = rememberIslandActions(snapshot, view, gate, onAction)
    IslandInventoryModal(view, gate, actions, onClose, modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun IslandInventoryModal(
    view: IslandPresentation,
    gate: InteractionGate,
    actions: IslandUiActions,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var closed by remember { mutableStateOf(false) }
    val dismiss = {
        if (!closed) {
            closed = true
            actions.clearFeedback()
            onClose()
        }
    }
    DisposableEffect(actions) {
        onDispose { actions.clearFeedback() }
    }
    ModalBottomSheet(
        onDismissRequest = dismiss,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        InventoryContent(view, gate, actions, dismiss, isOpen = { !closed })
    }
}

@Composable
private fun InventoryContent(
    view: IslandPresentation,
    gate: InteractionGate,
    actions: IslandUiActions,
    onClose: () -> Unit,
    isOpen: () -> Boolean,
) {
    var type by remember { mutableStateOf(ItemType.FOOD) }
    var selectedSlotId by remember { mutableStateOf("slot_left") }
    val slot = view.slots.firstOrNull { it.id == selectedSlotId && it.available }
        ?: view.slots.firstOrNull { it.available }
    val enabled = isOpen() && actions.enabled && !actions.busy
    val submit: (GameAction) -> Unit = { if (isOpen()) actions.submit(it) }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("인벤토리", style = MaterialTheme.typography.headlineSmall)
        OutlinedButton(onClick = onClose, modifier = Modifier.islandTouchTarget()) { Text("인벤토리 닫기") }
        IslandFeedback(actions, gate)
        if (actions.eating) Text("냠냠 · 식사 중", style = MaterialTheme.typography.titleMedium)
        if (view.hasCatalogIssues) Text("일부 콘텐츠 정보를 확인할 수 없어요.")
        PrimaryTabRow(selectedTabIndex = type.ordinal) {
            ItemType.entries.forEach { tab ->
                Tab(
                    selected = type == tab,
                    onClick = { if (isOpen() && actions.enabled && !actions.busy) type = tab },
                    enabled = enabled,
                    modifier = Modifier.heightIn(min = 56.dp),
                    text = { Text(tab.label()) },
                )
            }
        }
        if (type == ItemType.OUTFIT) {
            Text("현재 의상 · ${view.outfitStatus}")
            OutlinedButton(
                onClick = { submit(GameAction.EquipOutfit(null)) },
                enabled = enabled && view.outfitId != null,
                modifier = Modifier.islandTouchTarget(),
            ) { Text("의상 해제") }
        }
        if (type == ItemType.DECOR) {
            Text("배치할 슬롯", style = MaterialTheme.typography.titleMedium)
            view.slots.forEach { entry ->
                OutlinedButton(
                    onClick = {
                        if (isOpen() && actions.enabled && !actions.busy && entry.available) selectedSlotId = entry.id
                    },
                    enabled = enabled && entry.available,
                    modifier = Modifier.islandTouchTarget(),
                ) {
                    val selected = if (entry.id == slot?.id) "선택됨 · " else ""
                    Text("$selected${entry.label} · ${entry.status}")
                }
            }
            if (slot != null) {
                OutlinedButton(
                    onClick = { submit(GameAction.PlaceDecoration(slot.id, null)) },
                    enabled = enabled && slot.savedItemId != null,
                    modifier = Modifier.islandTouchTarget(),
                ) { Text("${slot.label} 슬롯 비우기") }
            }
        }

        val items = view.itemsFor(type)
        if (items.isEmpty()) Text("표시할 ${type.label()}이 없어요.")
        items.forEach { item ->
            val action = when (type) {
                ItemType.FOOD -> GameAction.Eat(item.id)
                ItemType.OUTFIT -> GameAction.EquipOutfit(item.id)
                ItemType.DECOR -> slot?.let { GameAction.PlaceDecoration(it.id, item.id) }
            }
            val placedHere = item.owned && slot?.itemName != null && item.placedSlots == setOf(slot.id)
            val label = when (type) {
                ItemType.FOOD -> "먹기"
                ItemType.OUTFIT -> if (item.equipped) "장착 중" else "장착"
                ItemType.DECOR -> if (placedHere) "배치 중" else "선택한 슬롯에 배치"
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                    Text(if (item.owned) "보유" else "미보유 · 잠금")
                    if (item.owned && item.placedSlots.isNotEmpty()) {
                        Text("배치 · " + item.placedSlots.joinToString { islandSlotLabel(it) })
                    }
                    Button(
                        onClick = { if (action != null) submit(action) },
                        enabled = enabled && action != null && view.accepts(action) && !item.equipped && !placedHere,
                        modifier = Modifier.islandTouchTarget(),
                    ) { Text(label) }
                }
            }
        }
    }
}

private fun ItemType.label(): String = when (this) {
    ItemType.FOOD -> "음식"
    ItemType.OUTFIT -> "의상"
    ItemType.DECOR -> "장식"
}
