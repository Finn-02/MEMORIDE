package io.kailand.feature.island

import io.kailand.core.contract.Catalog
import io.kailand.core.contract.GameAction
import io.kailand.core.contract.GameSnapshot
import io.kailand.core.contract.ItemType

internal enum class IslandGrowth(val label: String) {
    GARDEN("작은 빈 정원"), PATH("길"), FLOWERS("꽃밭"), REST_AREA("휴식 시설");
}

internal data class IslandCharacter(val id: String, val name: String, val selected: Boolean)

internal data class IslandItem(
    val id: String,
    val name: String,
    val type: ItemType,
    val owned: Boolean,
    val equipped: Boolean,
    val placedSlots: Set<String>,
)

internal data class IslandSlot(
    val id: String,
    val label: String,
    val available: Boolean,
    val savedItemId: String?,
    val itemName: String?,
) {
    val status: String get() = when {
        !available -> "슬롯 정보 확인 필요"
        savedItemId == null -> "비어 있음"
        else -> itemName ?: "저장된 장식 정보 확인 필요"
    }
}

internal data class IslandPresentation(
    val growth: IslandGrowth?,
    val characters: List<IslandCharacter>,
    val items: List<IslandItem>,
    val slots: List<IslandSlot>,
    val outfitId: String?,
    val hasCatalogIssues: Boolean,
) {
    val characterName: String get() = characters.firstOrNull { it.selected }?.name ?: "캐릭터 정보 확인 필요"
    val outfitStatus: String get() = when (outfitId) {
        null -> "장착한 의상 없음"
        else -> items.firstOrNull { it.equipped }?.name ?: "저장된 의상 정보 확인 필요"
    }

    fun itemsFor(type: ItemType): List<IslandItem> = items.filter { it.type == type }

    fun accepts(action: GameAction): Boolean = when (action) {
        is GameAction.SelectCharacter -> characters.any { it.id == action.characterId }
        is GameAction.EquipOutfit -> action.itemId == null || ownedItem(action.itemId, ItemType.OUTFIT) != null
        is GameAction.Eat -> ownedItem(action.itemId, ItemType.FOOD) != null
        is GameAction.PlaceDecoration -> slots.any { it.id == action.slotId && it.available } &&
            (action.itemId == null ||
                ownedItem(action.itemId, ItemType.DECOR)?.placedSlots?.all { it == action.slotId } == true)
    }

    private fun ownedItem(id: String, type: ItemType) = items.firstOrNull { it.id == id && it.type == type && it.owned }
}

internal fun islandSlotLabel(id: String): String = when (id) {
    "slot_left" -> "왼쪽"
    "slot_right" -> "오른쪽"
    "slot_back" -> "뒤쪽"
    else -> "알 수 없는 슬롯"
}

private val fixedSlotIds = listOf("slot_left", "slot_right", "slot_back")
private val contentId = Regex("[a-z][a-z0-9_]*")

/** Derive display and selection state from the current snapshot without repairing saved references. */
internal fun islandPresentation(snapshot: GameSnapshot, catalog: Catalog): IslandPresentation {
    val characters = catalog.characters.groupBy { it.id }.values.mapNotNull { entries ->
        val character = entries.singleOrNull()?.takeIf { contentId.matches(it.id) && it.name.isNotBlank() }
            ?: return@mapNotNull null
        IslandCharacter(character.id, character.name, character.id == snapshot.avatar.characterId)
    }
    val occupied = snapshot.decorationSlots.entries.groupBy({ it.value }, { it.key })
    val items = catalog.items.groupBy { it.id }.values.mapNotNull { entries ->
        val item = entries.singleOrNull()?.takeIf { contentId.matches(it.id) && it.name.isNotBlank() }
            ?: return@mapNotNull null
        val owned = item.id in snapshot.ownedItemIds
        IslandItem(item.id, item.name, item.type, owned,
            equipped = owned && item.type == ItemType.OUTFIT && item.id == snapshot.avatar.outfitItemId,
            placedSlots = if (item.type == ItemType.DECOR) occupied[item.id].orEmpty().toSet() else emptySet())
    }
    val slotCounts = catalog.rules.decorationSlotIds.groupingBy { it }.eachCount()
    val slots = fixedSlotIds.map { id ->
        val saved = snapshot.decorationSlots[id]
        val available = slotCounts[id] == 1
        val item = items.firstOrNull {
            it.id == saved && it.type == ItemType.DECOR && it.owned && it.placedSlots == setOf(id)
        }
        IslandSlot(id, islandSlotLabel(id), available, saved, item?.name?.takeIf { available })
    }
    return IslandPresentation(
        IslandGrowth.entries.getOrNull(snapshot.dailyGrowth), characters, items, slots, snapshot.avatar.outfitItemId,
        hasCatalogIssues = characters.size != catalog.characters.size || items.size != catalog.items.size ||
            catalog.rules.decorationSlotIds.size != fixedSlotIds.size || slots.any { !it.available },
    )
}
