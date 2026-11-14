package io.kailand.app.journey

import io.kailand.core.contract.Catalog
import io.kailand.core.contract.DestinationSpec
import io.kailand.core.contract.GameSnapshot

/** Read-only hint, never an arrival validation or a reward grant. null hides the whole hint. */
internal data class ArrivalRewardHint(val itemName: String?)

internal fun arrivalRewardHint(destination: DestinationSpec?, catalog: Catalog, snapshot: GameSnapshot): ArrivalRewardHint? {
    val region = catalog.regions.singleOrNull { it.id == destination?.regionId } ?: return null
    if (region.itemIds.isEmpty() || region.itemIds.distinct().size != region.itemIds.size) return null
    val items = region.itemIds.map { id ->
        catalog.items.singleOrNull { it.id == id && it.regionId == region.id } ?: return null
    }
    val next = items.firstOrNull { it.id !in snapshot.ownedItemIds }
    return ArrivalRewardHint(next?.name?.takeIf { it.isNotBlank() })
}

internal fun arrivalObjectParticle(name: String): String {
    val last = name.lastOrNull() ?: return "을"
    return if (last in '\uAC00'..'\uD7A3' && (last.code - 0xAC00) % 28 == 0) "를" else "을"
}
