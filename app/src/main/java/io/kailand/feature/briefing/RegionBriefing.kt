package io.kailand.feature.briefing

import io.kailand.core.contract.ArrivalReceipt
import io.kailand.core.contract.AvatarPreview
import io.kailand.core.contract.Catalog
import io.kailand.core.contract.DataSource
import io.kailand.core.contract.ItemType
import io.kailand.core.contract.JourneyKind

internal data class RegionBriefing(
    val firstVisit: Boolean,
    val regionName: String,
    val title: String,
    val summary: String,
    val source: String,
    val growth: String,
    val souvenirs: List<RegionBriefingSouvenir>,
    val tripStats: List<RegionBriefingStat>,
)

internal data class RegionBriefingSouvenir(
    val name: String,
    val type: ItemType?,
    val imagePath: String?,
)

internal data class RegionBriefingStat(val label: String, val value: String)

/** Formats a persisted REGION receipt for S05. A owns the route and B owns the grants. */
internal fun formatRegionBriefing(
    receipt: ArrivalReceipt,
    catalog: Catalog,
    avatar: AvatarPreview,
): RegionBriefing? {
    if (receipt.kind != JourneyKind.REGION) return null
    val region = catalog.regions.singleOrNull { it.id == receipt.regionId } ?: return null
    if (receipt.discoveredRegionId != null && receipt.discoveredRegionId != region.id) return null

    val firstVisit = receipt.discoveredRegionId == region.id
    if (!firstVisit && receipt.grantedItemIds.isNotEmpty()) return null

    val original = formatBriefing(receipt, catalog, avatar)
    val grantedIds = receipt.grantedItemIds.distinct()
    val itemIds = if (firstVisit) grantedIds else region.itemIds.distinct()
    val souvenirs = itemIds.map { id ->
        val item = catalog.items.singleOrNull { it.id == id && it.regionId == region.id }
        RegionBriefingSouvenir(
            name = item?.name ?: "이름을 알 수 없는 기념품",
            type = item?.type,
            imagePath = item?.let { catalog.assets[it.assetKey]?.path },
        )
    }

    val userConfirmed = receipt.source == DataSource.USER_CONFIRMED
    return RegionBriefing(
        firstVisit = firstVisit,
        regionName = region.name,
        title = if (firstVisit) "${withObjectParticle(region.name)} 처음 발견했어요" else "${region.name}에 다시 왔어요",
        summary = when {
            !firstVisit -> "이번 방문에서 받은 새 선물은 없어요."
            grantedIds.isEmpty() -> "이번 방문에서 받은 새 기념품은 없어요."
            else -> "${region.name} 기념품 ${grantedIds.size}개를 받았어요."
        },
        source = original.source,
        growth = original.growth,
        souvenirs = souvenirs,
        tripStats = listOf(
            RegionBriefingStat("이번 주행", if (userConfirmed) "—" else original.distance ?: "—"),
            RegionBriefingStat("주행 시간", if (userConfirmed) "—" else original.elapsed ?: "—"),
            RegionBriefingStat("연비", "—"),
            RegionBriefingStat("누적 주행", "—"),
        ),
    )
}

internal fun itemTypeLabel(type: ItemType?): String = when (type) {
    ItemType.FOOD -> "음식"
    ItemType.OUTFIT -> "의상"
    ItemType.DECOR -> "장식"
    null -> "종류 확인 안 됨"
}

/** Korean object particle: 원주를, 대전을. Non-Hangul endings keep both forms. */
internal fun withObjectParticle(word: String): String {
    val last = word.lastOrNull() ?: return word
    if (last !in '\uAC00'..'\uD7A3') return "${word}을(를)"
    return word + if ((last - '\uAC00') % 28 != 0) "을" else "를"
}
