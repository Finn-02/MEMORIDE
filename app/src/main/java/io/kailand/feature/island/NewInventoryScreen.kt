package io.kailand.feature.island

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.kailand.R
import io.kailand.core.contract.Catalog
import io.kailand.core.contract.GameAction
import io.kailand.core.contract.GameSnapshot
import io.kailand.core.contract.InteractionGate
import io.kailand.core.contract.ItemType
import io.kailand.core.contract.MutationResult
import io.kailand.core.ui.MemorideBrushes
import io.kailand.core.ui.MemorideColors
import io.kailand.core.ui.MemorideDimens
import io.kailand.core.ui.MemorideIconButton
import io.kailand.core.ui.MemoridePanel
import io.kailand.core.ui.MemoridePrimaryButton
import io.kailand.core.ui.MemorideScreen
import io.kailand.core.ui.MemorideSecondaryButton
import io.kailand.core.ui.MemorideTheme
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

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

    val panel: @Composable (Modifier) -> Unit = { panelModifier ->
        InventoryPanel(
            type = type, cards = cards, gate = gate, actions = actions, enabled = enabled,
            hasCatalogIssues = view.hasCatalogIssues || cards.size != 3,
            allLocked = cardsByType.values.all { list -> list.size == 3 && list.none { it.item.owned } },
            onTab = { tab ->
                if (actions.canEditInventory(closed) && selection.type != tab) {
                    selection = selection.selectTab(tab)
                    actions.clearFeedback()
                }
            },
            onUse = useCard, onClose = close, modifier = panelModifier,
        )
    }
    val stage: @Composable (Dp, Modifier) -> Unit = { size, stageModifier ->
        Column(stageModifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            InventoryStage(snapshot, catalog, view, type, actions.eating && !closed, size)
            if (picking != null) {
                DecorationSlotPicker(picking, view.slots, enabled, {
                    if (actions.canEditInventory(closed)) selection = selection.cancelDecoration()
                }, place)
            }
        }
    }

    MemorideTheme {
        MemorideScreen(modifier) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val wide = useWideInventoryLayout(maxWidth, maxHeight, LocalDensity.current.fontScale)
                val padding = if (wide) 32.dp else 12.dp
                val sceneSize = if (wide) (maxHeight - padding * 2).coerceAtLeast(220.dp)
                    else (maxHeight * if (type == ItemType.DECOR) 0.42f else 0.30f).coerceAtLeast(140.dp)
                val body = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding)
                if (wide) {
                    Row(body, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        stage(sceneSize, Modifier.weight(1f))
                        panel(Modifier.weight(1f))
                    }
                } else {
                    Column(body, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        stage(sceneSize, Modifier.fillMaxWidth())
                        panel(Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryStage(
    snapshot: GameSnapshot,
    catalog: Catalog,
    view: IslandPresentation,
    type: ItemType,
    eating: Boolean,
    size: Dp,
) {
    val island = type == ItemType.DECOR
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(MemorideDimens.PanelRadius))
            .background(if (island) MemorideBrushes.Sea else MemorideBrushes.CharacterStage),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.widthIn(max = size).fillMaxWidth().aspectRatio(1f),
            contentAlignment = Alignment.Center,
        ) {
            if (island) {
                IslandScene(view.growth, snapshot.avatar, catalog, view.characterName, false, view.slots)
            } else {
                Canvas(Modifier.matchParentSize()) {
                    drawOval(
                        MemorideColors.Sand,
                        Offset(this.size.width * .19f, this.size.height * .78f),
                        Size(this.size.width * .62f, this.size.height * .12f),
                    )
                }
                AvatarView(
                    snapshot.avatar, catalog, if (eating) "eat" else "idle",
                    Modifier.fillMaxSize().padding(8.dp),
                )
                Text(
                    if (eating) "냠냠 · 식사 중" else view.characterName,
                    Modifier.align(Alignment.TopCenter).padding(8.dp)
                        .background(MemorideColors.Panel, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun InventoryPanel(
    type: ItemType,
    cards: List<InventoryCardModel>,
    gate: InteractionGate,
    actions: IslandUiActions,
    enabled: Boolean,
    hasCatalogIssues: Boolean,
    allLocked: Boolean,
    onTab: (ItemType) -> Unit,
    onUse: (InventoryCardModel) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier,
) {
    MemoridePanel(modifier) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("인벤토리", Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.headlineMedium)
            MemorideIconButton("인벤토리 닫기", onClose) {
                Image(painterResource(R.drawable.memoride_close), null, Modifier.size(28.dp))
            }
        }
        PrimaryTabRow(selectedTabIndex = type.ordinal) {
            ItemType.entries.forEach { tab ->
                Tab(
                    selected = type == tab, onClick = { onTab(tab) }, enabled = enabled,
                    modifier = Modifier.heightIn(min = MemorideDimens.MinimumTouchTarget),
                    text = { Text(tab.inventoryLabel()) },
                )
            }
        }
        val feedbackRequester = remember { BringIntoViewRequester() }
        LaunchedEffect(actions.error) {
            if (actions.error != null) feedbackRequester.bringIntoView()
        }
        Column(Modifier.bringIntoViewRequester(feedbackRequester).semantics { liveRegion = LiveRegionMode.Polite }) {
            IslandFeedback(actions, gate)
            if (hasCatalogIssues) Text("일부 기념품 정보를 확인할 수 없어요.", color = MaterialTheme.colorScheme.error)
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (useInventoryCardRow(maxWidth, LocalDensity.current.fontScale)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    cards.forEach { card ->
                        InventoryItemCard(card, enabled, { onUse(card) }, Modifier.weight(1f))
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    cards.forEach { card ->
                        InventoryItemCard(card, enabled, { onUse(card) }, Modifier.fillMaxWidth())
                    }
                }
            }
        }
        val hint = if (allLocked) {
            "아직 모은 기념품이 없어요. 원주·대전·속초에 다녀오면 음식·의상·장식을 받아요."
        } else when (type) {
            ItemType.FOOD -> "음식은 먹어도 없어지지 않아요. 먹으면 캐릭터가 잠깐 반응해요."
            ItemType.OUTFIT -> "의상은 하나만 입을 수 있어요. 저장되면 새 모습으로 바뀌어요."
            ItemType.DECOR -> "놓기를 누른 뒤 슬롯을 고르세요. 장식을 옮기려면 먼저 기존 슬롯에서 빼 주세요."
        }
        Text(hint, color = MemorideColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun InventoryItemCard(
    card: InventoryCardModel,
    enabled: Boolean,
    onUse: () -> Unit,
    modifier: Modifier,
) {
    val locked = !card.item.owned
    val inUse = card.inUse
    val label = when {
        locked -> "잠김"
        card.item.type == ItemType.FOOD -> "먹기"
        card.item.type == ItemType.OUTFIT -> if (inUse == null) "입기" else "벗기"
        card.slot != null -> "빼기"
        else -> "놓기"
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(MemorideDimens.PanelRadius),
        color = if (locked) MemorideColors.SurfaceLocked else MemorideColors.Surface,
        border = BorderStroke(
            if (inUse == null) 1.dp else 3.dp,
            if (inUse == null) MemorideColors.BorderCard else MemorideColors.OceanAccessible,
        ),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            InventoryThumbnail(card.imagePath, locked)
            Text(card.item.name, style = MaterialTheme.typography.titleMedium)
            Text(
                if (locked) "${card.regionName}에 가면 얻어요" else card.regionName,
                style = MaterialTheme.typography.bodyMedium,
                color = MemorideColors.TextSecondary,
            )
            Text(
                inUse ?: if (locked) "미보유" else "보유",
                modifier = Modifier
                    .background(
                        if (inUse == null) MemorideColors.SurfaceSubtle else MemorideColors.Navy,
                        RoundedCornerShape(percent = 50),
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = if (inUse == null) MemorideColors.TextSecondary else MemorideColors.OnPrimary,
            )
            val actionable = enabled && (card.action() != null || card.canChooseSlot)
            if (locked || inUse != null) {
                MemorideSecondaryButton(label, onUse, Modifier.fillMaxWidth(), actionable)
            } else {
                MemoridePrimaryButton(label, onUse, Modifier.fillMaxWidth(), actionable)
            }
        }
    }
}

@Composable
private fun InventoryThumbnail(path: String?, locked: Boolean) {
    Box(
        Modifier.fillMaxWidth().height(96.dp).clip(RoundedCornerShape(12.dp))
            .background(MemorideColors.SurfaceSubtle),
        contentAlignment = Alignment.Center,
    ) {
        if (locked) {
            val color = MemorideColors.TextSecondary
            Canvas(Modifier.size(32.dp)) {
                drawArc(
                    color, 180f, 180f, false,
                    Offset(size.width * .29f, size.height * .13f),
                    Size(size.width * .42f, size.height * .55f), style = Stroke(2.dp.toPx()),
                )
                drawRoundRect(
                    color, Offset(size.width * .18f, size.height * .40f),
                    Size(size.width * .64f, size.height * .46f), CornerRadius(3.dp.toPx()),
                )
            }
        } else if (path != null) {
            val assets = LocalContext.current.assets
            key(assets, path) {
                val image by produceState<ImageBitmap?>(null) {
                    value = withContext(Dispatchers.IO) {
                        try {
                            assets.open(path).use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
                        } catch (_: IOException) {
                            null
                        }
                    }
                }
                image?.let {
                    Image(it, null, Modifier.fillMaxSize().padding(8.dp), contentScale = ContentScale.Fit)
                }
            }
        }
    }
}

@Composable
private fun DecorationSlotPicker(
    card: InventoryCardModel,
    slots: List<IslandSlot>,
    enabled: Boolean,
    onCancel: () -> Unit,
    onPlace: (InventoryCardModel, IslandSlot) -> Unit,
) {
    val requester = remember { BringIntoViewRequester() }
    LaunchedEffect(card.item.id) { requester.bringIntoView() }

    MemoridePanel(Modifier.fillMaxWidth()) {
        Text(
            "${card.item.name}을 놓을 슬롯을 고르세요",
            Modifier.bringIntoViewRequester(requester).semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
        )
        slots.forEachIndexed { index, slot ->
            val label = when {
                !slot.available -> "슬롯 정보 확인 필요"
                slot.savedItemId == null -> "여기에 놓기"
                else -> "${slot.itemName ?: "현재 장식"} 교체"
            }
            MemorideSecondaryButton(
                "슬롯 ${index + 1} · ${slot.label} · $label",
                onClick = { onPlace(card, slot) },
                enabled = enabled && card.placeIn(slot) != null,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        MemorideSecondaryButton("슬롯 선택 취소", onCancel, Modifier.fillMaxWidth(), enabled)
    }
}
