package io.kailand.feature.island

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.kailand.core.contract.Catalog
import io.kailand.core.contract.GameAction
import io.kailand.core.contract.ItemType
import io.kailand.core.ui.MemorideDimens

private val inventoryRegionOrder = listOf("wonju", "daejeon", "sokcho")
private val inventoryAssetPath = Regex("assets/starter/[a-z0-9_]+\\.png")

internal data class InventoryCardModel(
    val item: IslandItem,
    val regionName: String,
    val imagePath: String?,
    val slot: IslandSlot?,
    val slotNumber: Int?,
) {
    val inUse: String? get() = when {
        item.equipped -> "착용 중"
        slotNumber != null -> "슬롯 ${slotNumber}에 놓음"
        else -> null
    }

    val canChooseSlot: Boolean get() = item.owned && item.type == ItemType.DECOR && item.placedSlots.isEmpty()

    fun action(): GameAction? {
        if (!item.owned) return null
        return when (item.type) {
            ItemType.FOOD -> GameAction.Eat(item.id)
            ItemType.OUTFIT -> GameAction.EquipOutfit(item.id.takeUnless { item.equipped })
            ItemType.DECOR -> slot?.let { GameAction.PlaceDecoration(it.id, null) }
        }
    }

    fun placeIn(target: IslandSlot): GameAction.PlaceDecoration? =
        if (canChooseSlot && target.available) GameAction.PlaceDecoration(target.id, item.id) else null
}

internal fun inventoryCards(type: ItemType, view: IslandPresentation, catalog: Catalog): List<InventoryCardModel> =
    inventoryRegionOrder.mapNotNull { regionId ->
        val region = catalog.regions.singleOrNull { it.id == regionId && it.name.isNotBlank() }
            ?: return@mapNotNull null
        val definition = catalog.items.singleOrNull { it.regionId == regionId && it.type == type }
            ?.takeIf { it.id in region.itemIds } ?: return@mapNotNull null
        val item = view.itemsFor(type).singleOrNull { it.id == definition.id } ?: return@mapNotNull null
        val path = catalog.assets[definition.assetKey]
            ?.takeIf { it.kind == type.name.lowercase() && inventoryAssetPath.matches(it.path) }?.path
        val slotIndex = view.slots.indexOfFirst { it.savedItemId == item.id && it.itemName != null }
        InventoryCardModel(item, region.name, path, view.slots.getOrNull(slotIndex), (slotIndex + 1).takeIf { it > 0 })
    }

internal fun useWideInventoryLayout(width: Dp, height: Dp, fontScale: Float): Boolean =
    width >= MemorideDimens.WideBreakpoint && width > height && fontScale <= 1.3f

internal fun useInventoryCardRow(width: Dp, fontScale: Float): Boolean =
    width >= 464.dp && fontScale <= 1.3f

internal fun ItemType.inventoryLabel(): String = when (this) {
    ItemType.FOOD -> "음식"
    ItemType.OUTFIT -> "의상"
    ItemType.DECOR -> "장식"
}
