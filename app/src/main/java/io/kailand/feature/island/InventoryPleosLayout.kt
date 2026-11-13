package io.kailand.feature.island

import android.graphics.BitmapFactory
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kailand.R
import io.kailand.core.contract.Catalog
import io.kailand.core.contract.GameSnapshot
import io.kailand.core.contract.InteractionGate
import io.kailand.core.contract.ItemType
import io.kailand.core.ui.MemorideBrandLogo
import io.kailand.core.ui.MemoridePleosTextStyle
import io.kailand.core.ui.MemoridePleosFonts
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private object InventoryColors {
    val Primary = Color(0xFF1F4E79)
    val Accent = Color(0xFF2A7F9E)
    val Text = Color(0xFF1E2B3A)
    val Sub = Color(0xFF5B6B7B)
    val Muted = Color(0xFF8A98A6)
    val Border = Color(0xFFE3EAF1)
    val Surface2 = Color(0xFFEEF2F6)
    val PrimaryBrush = Brush.horizontalGradient(listOf(Primary, Color(0xFF2A8FAE)))
}

@Composable
private fun InventoryText(
    text: String, size: Int = 22, modifier: Modifier = Modifier,
    color: Color = InventoryColors.Text, weight: FontWeight = FontWeight.Medium,
    fontFamily: FontFamily = MemoridePleosFonts.Body,
) {
    Text(text, modifier, fontFamily = fontFamily, fontSize = size.coerceAtLeast(18).sp,
        lineHeight = (size.coerceAtLeast(18) * 1.3f).sp, fontWeight = weight, color = color,
        style = MemoridePleosTextStyle)
}

@Composable
internal fun InventoryPleosLayout(
    snapshot: GameSnapshot, catalog: Catalog, view: IslandPresentation, type: ItemType,
    cards: List<InventoryCardModel>, gate: InteractionGate, actions: IslandUiActions, enabled: Boolean, eating: Boolean,
    hasCatalogIssues: Boolean, allLocked: Boolean, picking: InventoryCardModel?,
    onTab: (ItemType) -> Unit, onUse: (InventoryCardModel) -> Unit, onClose: () -> Unit,
    onCancelDecoration: () -> Unit, onPlace: (InventoryCardModel, IslandSlot) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedSlotId by remember(picking?.item?.id) { mutableStateOf<String?>(null) }
    val selectedSlot = view.slots.singleOrNull { it.id == selectedSlotId && picking?.placeIn(it) != null }
    val chooseSlot: (IslandSlot) -> Unit = { slot ->
        if (enabled && picking?.placeIn(slot) != null) selectedSlotId = slot.id
    }
    Box(modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFEAF4FB), Color(0xFFFFF6EC))))
        .safeDrawingPadding()) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 24.dp)) {
            val wide = useWideInventoryLayout(maxWidth + 48.dp, maxHeight + 44.dp, LocalDensity.current.fontScale)
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                InventoryHeader(view, onClose)
                val stage: @Composable (Modifier) -> Unit = { stageModifier ->
                    InventoryCharacterStage(snapshot, catalog, view, type, eating, picking, selectedSlot,
                        enabled, chooseSlot, stageModifier)
                }
                val panel: @Composable (Modifier) -> Unit = { panelModifier ->
                    InventoryPleosPanel(type, cards, gate, actions, enabled, hasCatalogIssues, allLocked,
                        view.characterName, picking, view.slots, onTab, onUse,
                        onCancelDecoration, onPlace, selectedSlot, chooseSlot, panelModifier)
                }
                if (wide) {
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        // Equal weights keep the character stage and inventory panel at 1:1.
                        stage(Modifier.weight(1f).fillMaxHeight())
                        panel(Modifier.weight(1f).fillMaxHeight())
                    }
                } else {
                    // No root scroll: compact windows give the internal item viewport the remaining space.
                    Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        stage(Modifier.weight(1f).fillMaxWidth())
                        panel(Modifier.weight(1f).fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryHeader(view: IslandPresentation, onClose: () -> Unit) {
    val stage = when (view.growth) {
        IslandGrowth.GARDEN -> "작은 정원"
        IslandGrowth.PATH -> "새싹 단계"
        IslandGrowth.FLOWERS -> "숲 단계"
        IslandGrowth.REST_AREA -> "마을 단계"
        null -> "성장 정보 확인 필요"
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val chip: @Composable () -> Unit = {
            Row(Modifier.clip(CircleShape).background(Color.White)
                .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InventoryIcon(R.drawable.island_ic_sprout, 30.dp, Color(0xFF3E9A63))
                InventoryText("${view.characterName}의 섬 · $stage", 24, color = Color(0xFF2B4A63), weight = FontWeight.SemiBold)
            }
        }
        if (maxWidth >= 960.dp) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                MemorideBrandLogo(Modifier.weight(1f), markSize = 72.dp, wordmarkSize = 44)
                chip()
                InventoryCloseButton(onClose)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MemorideBrandLogo(Modifier.fillMaxWidth(), markSize = 72.dp, wordmarkSize = 44)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Box(Modifier.weight(1f)) { chip() }
                    InventoryCloseButton(onClose)
                }
            }
        }
    }
}

@Composable
private fun InventoryCloseButton(onClose: () -> Unit) {
    Surface(onClick = onClose, modifier = Modifier.widthIn(min = 132.dp).heightIn(min = 72.dp)
        .semantics { contentDescription = "인벤토리 닫기" },
        shape = RoundedCornerShape(18.dp), color = Color.White) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically) {
            InventoryIcon(R.drawable.inventory_ic_close, 32.dp, InventoryColors.Text)
            InventoryText("닫기", 24, weight = FontWeight.Bold)
        }
    }
}

@Composable
private fun InventoryCharacterStage(
    snapshot: GameSnapshot, catalog: Catalog, view: IslandPresentation, type: ItemType,
    eating: Boolean, picking: InventoryCardModel?, selectedSlot: IslandSlot?,
    enabled: Boolean, onSelectSlot: (IslandSlot) -> Unit, modifier: Modifier,
) {
    BoxWithConstraints(modifier.clip(RoundedCornerShape(28.dp)).background(Brush.verticalGradient(
        0f to Color(0xFFD6EEFB), .6f to Color(0xFFEEF7F6), 1f to Color(0xFFFCEFDF))),
        contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            drawRect(Brush.radialGradient(listOf(Color.White.copy(alpha = .85f), Color.Transparent),
                center = Offset(size.width * .88f, size.height * .10f), radius = size.width * .75f))
            if (type != ItemType.DECOR) drawOval(Color(0xFFF3D9A8).copy(alpha = .8f),
                Offset(size.width * .22f, size.height * .71f), Size(size.width * .56f, size.height * .08f))
        }
        val side = if (type == ItemType.DECOR) minOf(maxWidth * 1.15f, maxHeight * 1.15f)
            else minOf(maxWidth * .82f, maxHeight * .74f)
        Box(if (type == ItemType.DECOR) Modifier.requiredSize(side) else Modifier.size(side),
            contentAlignment = Alignment.Center) {
            // Preserve the renderer choice and the existing eating/idle expression path.
            if (type == ItemType.DECOR) {
                // Only the scene receives this transient preview; the save and placement callbacks are untouched.
                val previewSlots = if (picking != null && selectedSlot != null) view.slots.map { slot ->
                    if (slot.id == selectedSlot.id) slot.copy(savedItemId = picking.item.id, itemName = picking.item.name)
                    else slot
                } else view.slots
                IslandScene(view.growth, snapshot.avatar, catalog, view.characterName, false, previewSlots,
                    avatarScale = 1.7f)
                if (picking != null) InventoryDecorationMarkers(view.slots, picking, selectedSlot?.id,
                    enabled, onSelectSlot, Modifier.fillMaxSize())
            } else {
                AvatarView(snapshot.avatar, catalog, if (eating) "eat" else "idle",
                    Modifier.fillMaxSize().padding(8.dp), highResolution = true)
                InventoryMealEffect(eating && type == ItemType.FOOD, Modifier.matchParentSize())
            }
        }
        Row(Modifier.align(Alignment.TopStart).padding(24.dp)
            .shadow(4.dp, CircleShape).clip(CircleShape).background(Color.White)
            .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InventoryIcon(R.drawable.inventory_ic_heart, 28.dp, Color(0xFFE9798A))
            Text(view.characterName,
                fontFamily = MemoridePleosFonts.CharacterName, fontSize = 30.sp, lineHeight = 39.sp,
                color = Color(0xFF1E3A5F), style = MemoridePleosTextStyle)
        }
    }
}

@Composable
private fun InventoryPleosPanel(
    type: ItemType, cards: List<InventoryCardModel>, gate: InteractionGate, actions: IslandUiActions,
    enabled: Boolean, hasCatalogIssues: Boolean, allLocked: Boolean, characterName: String,
    picking: InventoryCardModel?, slots: List<IslandSlot>, onTab: (ItemType) -> Unit,
    onUse: (InventoryCardModel) -> Unit, onCancelDecoration: () -> Unit,
    onPlace: (InventoryCardModel, IslandSlot) -> Unit, selectedSlot: IslandSlot?,
    onSelectSlot: (IslandSlot) -> Unit, modifier: Modifier,
) {
    Surface(modifier.shadow(12.dp, RoundedCornerShape(28.dp),
        ambientColor = Color(0x121A334D), spotColor = Color(0x121A334D)),
        shape = RoundedCornerShape(28.dp), color = Color.White) {
        Column(Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 36.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(60.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFE7F0F8)),
                    contentAlignment = Alignment.Center) { InventoryIcon(R.drawable.inventory_ic_backpack, 36.dp) }
                InventoryText("인벤토리", 40, Modifier.weight(1f).semantics { heading() },
                    weight = FontWeight.Normal, fontFamily = MemoridePleosFonts.CharacterName)

            }
            InventoryTabs(type, enabled, onTab, Modifier.padding(top = 6.dp))
            if (!actions.enabled || actions.busy || actions.error != null || hasCatalogIssues) {
                Column(Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }) {
                    IslandFeedback(actions, gate)
                    if (hasCatalogIssues) InventoryText("일부 기념품 정보를 확인할 수 없어요.", color = MaterialTheme.colorScheme.error)
                }
            }
            InventoryGrid(cards, enabled, picking, slots, onUse, onCancelDecoration, onPlace,
                selectedSlot, onSelectSlot, Modifier.weight(1f).fillMaxWidth().padding(top = 6.dp))
            val hint = if (allLocked) {
                "아직 모은 기념품이 없어요. 원주·대전·속초에 다녀오면 음식·의상·장식을 받아요."
            } else when (type) {
                ItemType.FOOD -> "음식은 먹어도 없어지지 않아요. 먹으면 ${characterName}가 잠깐 반응해요."
                ItemType.OUTFIT -> "의상은 하나만 입을 수 있어요. 저장되면 새 모습으로 바뀌어요."
                ItemType.DECOR -> "놓기를 누르면 섬에서 자리를 고를 수 있어요. 미리보기를 확인한 뒤 놓아 주세요."
            }
            val displayHint = if (type == ItemType.DECOR) hint.replace(". ", ".\n") else hint
            Row(Modifier.fillMaxWidth().heightIn(min = if (type == ItemType.DECOR) 96.dp else 56.dp)
                .clip(RoundedCornerShape(20.dp)).background(Color(0xFFF4F8FB))
                .padding(horizontal = 22.dp, vertical = if (type == ItemType.DECOR) 14.dp else 6.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                InventoryIcon(R.drawable.inventory_ic_info, 32.dp, InventoryColors.Accent)
                InventoryText(displayHint, 24, modifier = Modifier.weight(1f), color = Color(0xFF4A5B6C))
            }
        }
    }
}

private fun inventoryTabIcon(type: ItemType): Int = when (type) {
    ItemType.FOOD -> R.drawable.inventory_ic_utensils
    ItemType.OUTFIT -> R.drawable.inventory_ic_shirt
    ItemType.DECOR -> R.drawable.inventory_ic_sparkles
}

@Composable
private fun InventoryTabs(
    type: ItemType, enabled: Boolean, onTab: (ItemType) -> Unit, modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(InventoryColors.Surface2)
        .padding(8.dp).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ItemType.entries.forEach { tab ->
            val selected = type == tab
            val ink = if (selected) Color.White else InventoryColors.Sub
            Box(Modifier.weight(1f).heightIn(min = 84.dp)
                .shadow(if (selected) 4.dp else 0.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp)).background(if (selected) InventoryColors.Primary else Color.Transparent)
                .selectable(selected, enabled = enabled, role = Role.Tab, onClick = { onTab(tab) })
                .padding(horizontal = 8.dp, vertical = 20.dp), contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    InventoryIcon(inventoryTabIcon(tab), 32.dp, ink)
                    InventoryText(tab.inventoryLabel(), 30, color = ink,
                        weight = if (selected) FontWeight.Bold else FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun InventoryGrid(
    cards: List<InventoryCardModel>, enabled: Boolean, picking: InventoryCardModel?, slots: List<IslandSlot>,
    onUse: (InventoryCardModel) -> Unit, onCancelDecoration: () -> Unit,
    onPlace: (InventoryCardModel, IslandSlot) -> Unit, selectedSlot: IslandSlot?,
    onSelectSlot: (IslandSlot) -> Unit, modifier: Modifier,
) {
    BoxWithConstraints(modifier.clipToBounds()) {
        val fontScale = LocalDensity.current.fontScale
        val columns = when {
            maxWidth >= 600.dp && fontScale <= 1.3f -> 3
            maxWidth >= 390.dp && fontScale <= 1.3f -> 2
            else -> 1
        }
        // Slightly lengthen the intermediate card while keeping it within the grid viewport.
        val fullRowHeight = (maxHeight - 8.dp).coerceAtLeast(0.dp)
        val rowHeight = (fullRowHeight + fullRowHeight.coerceAtMost(408.dp)) / 2f
        val listState = rememberLazyListState()
        // A newly chosen decoration's picker is revealed within the bounded item viewport.
        LaunchedEffect(picking?.item?.id) { if (picking != null) listState.animateScrollToItem(0) }
        LazyColumn(Modifier.fillMaxSize().clipToBounds(), state = listState,
            contentPadding = PaddingValues(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            if (picking != null) item(key = "decoration-picker") {
                InventoryDecorationPicker(picking, slots, selectedSlot, enabled, onSelectSlot,
                    onCancelDecoration, onPlace)
            }
            items(cards.chunked(columns), key = { row -> row.first().item.id }) { row ->
                Row(Modifier.fillMaxWidth().height(rowHeight), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    row.forEach { card -> InventoryPleosCard(card, enabled, { onUse(card) }, Modifier.weight(1f).fillMaxHeight()) }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            if (cards.isEmpty()) item(key = "empty") {
                InventoryText("표시할 아이템이 없어요. 기념품 정보를 확인해 주세요.", color = InventoryColors.Sub)
            }
        }
    }
}

@Composable
private fun InventoryPleosCard(card: InventoryCardModel, enabled: Boolean, onUse: () -> Unit, modifier: Modifier) {
    val locked = !card.item.owned
    val inUse = if (card.item.type == ItemType.DECOR && card.slot != null)
        "${card.slot.label}에 놓여 있어요" else card.inUse
    val label = when {
        locked -> "잠김"
        card.item.type == ItemType.FOOD -> "먹기"
        card.item.type == ItemType.OUTFIT -> if (inUse == null) "입기" else "벗기"
        card.slot != null -> "빼기"
        else -> "놓기"
    }
    val actionable = enabled && (card.action() != null || card.canChooseSlot)
    Surface(modifier, shape = RoundedCornerShape(24.dp), color = if (locked) Color(0xFFF7F9FB) else Color.White,
        border = BorderStroke(if (locked) 1.5.dp else 2.dp, if (locked) InventoryColors.Border else Color(0xFFBFD9EA))) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            // The frame and action stay visible; longer details scroll only within this card.
            Column(Modifier.weight(1f).fillMaxWidth().clipToBounds().verticalScroll(rememberScrollState())) {
                InventoryPleosThumbnail(card.imagePath, locked)
                Spacer(Modifier.height(15.dp))
                InventoryText(card.item.name, 32, color = if (locked) InventoryColors.Sub else InventoryColors.Text, weight = FontWeight.Normal, fontFamily = MemoridePleosFonts.CharacterName)
                Spacer(Modifier.height(15.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val color = if (locked) Color(0xFF7A8A99) else InventoryColors.Accent
                    InventoryIcon(R.drawable.inventory_ic_map_pin, 24.dp, color)
                    InventoryText(card.regionName, 24,
                        modifier = Modifier.weight(1f), color = color)
                }
                Spacer(Modifier.height(15.dp))
                InventoryText(inUse ?: if (locked) "미보유" else "보유 중", 24,
                    modifier = Modifier.clip(CircleShape).background(if (locked) InventoryColors.Surface2 else Color(0xFFE3F5EA))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    color = if (locked) Color(0xFF7A8A99) else Color(0xFF2F8A55), weight = FontWeight.Bold)
                Spacer(Modifier.height(15.dp))
                InventoryText(inventoryItemDescription(card), 20, color = InventoryColors.Sub)
            }
            Spacer(Modifier.height(14.dp))
            InventoryActionButton(label, if (locked) R.drawable.inventory_ic_lock_small else inventoryTabIcon(card.item.type),
                actionable, onUse, Modifier.fillMaxWidth(), primary = !locked && inUse == null, compact = true)
        }
    }
}

/** Presentation copy only: no catalog/save schema or item effects are changed. */
private fun inventoryItemDescription(card: InventoryCardModel): String = when (card.item.id) {
    "wonju_potato" -> "감자의 포근한 느낌을 담은 ${inventoryKeepTogether("음식이에요.")}"
    "daejeon_bread" -> "갓 구운 빵을 떠올리게 하는 음식이에요."
    "sokcho_sashimi" -> "바다의 풍미를 담은 ${inventoryKeepTogether("회 한 접시예요.")}"
    "daejeon_hat" -> "빵 모양으로 꾸민 귀여운 모자예요."
    "wonju_crown" -> "은행잎의 색을 담은 머리장식이에요."
    "sokcho_scarf" -> "파도를 닮은 무늬의 ${inventoryKeepTogether("목도리예요.")}"
    "daejeon_sign" -> "빵집 분위기를 더하는 ${inventoryKeepTogether("작은 간판이에요.")}"
    "wonju_tree" -> "섬에 초록빛을 더하는 ${inventoryKeepTogether("은행나무예요.")}"
    "sokcho_shell" -> "섬에 바다의 분위기를 더하는 조개예요."
    else -> "${card.regionName}의 분위기를 담은 기념품이에요."
}

/** Keep the requested ending phrase on one line without changing its visible wording. */
private fun inventoryKeepTogether(text: String): String =
    text.replace(' ', '\u00A0').toList().joinToString("\u2060")

@Composable
private fun InventoryPleosThumbnail(path: String?, locked: Boolean) {
    Box(Modifier.fillMaxWidth().height(147.dp).clip(RoundedCornerShape(20.dp))
        .background(if (locked) InventoryColors.Surface2 else Color(0xFFEAF4FB)), contentAlignment = Alignment.Center) {
        if (locked) {
            Box(Modifier.size(96.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                InventoryIcon(R.drawable.inventory_ic_lock, 48.dp, InventoryColors.Muted)
            }
        } else if (path != null) {
            val assets = LocalContext.current.assets
            key(assets, path) {
                val image by produceState<ImageBitmap?>(null) {
                    value = withContext(Dispatchers.IO) {
                        try { assets.open(path).use { BitmapFactory.decodeStream(it)?.asImageBitmap() } }
                        catch (_: IOException) { null }
                    }
                }
                image?.let { Image(it, null, Modifier.fillMaxSize().padding(4.dp).scale(1.12f), contentScale = ContentScale.Fit) }
            }
        }
    }
}

@Composable
private fun InventoryActionButton(
    label: String, icon: Int?, enabled: Boolean, onClick: () -> Unit, modifier: Modifier,
    primary: Boolean = true, compact: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && enabled && primary) .98f else 1f, tween(100), label = "inventory-press")
    Surface(onClick = onClick, enabled = enabled, interactionSource = interaction,
        modifier = modifier.heightIn(min = if (compact) 72.dp else 80.dp).scale(scale), shape = RoundedCornerShape(18.dp), color = Color.Transparent) {
        val ink = if (!enabled) InventoryColors.Muted else if (primary) Color.White else InventoryColors.Primary
        val fill = if (enabled && primary) InventoryColors.PrimaryBrush else Brush.horizontalGradient(
            listOf(Color(0xFFE9EEF3), Color(0xFFE9EEF3)))
        Row(Modifier.background(fill).padding(horizontal = 12.dp, vertical = if (compact) 15.dp else 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) InventoryIcon(icon, 30.dp, ink)
            InventoryText(label, 26, color = ink, weight = FontWeight.Bold)
        }
    }
}

@Composable
private fun InventoryIcon(resource: Int, size: Dp, tint: Color = InventoryColors.Primary) {
    Icon(painterResource(resource), null, Modifier.size(size), tint = tint)
}
