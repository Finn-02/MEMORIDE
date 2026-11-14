package io.kailand.feature.briefing

import io.kailand.core.contract.ArrivalReceipt
import io.kailand.core.contract.Catalog
import io.kailand.core.contract.DataSource
import io.kailand.core.contract.GameSnapshot
import io.kailand.core.contract.RegionVisitHistory

/** Explicitly labelled presentation samples only; never persisted into receipt measurements. */
internal object ResultDemoOptions {
    const val USE_MOCK_TRIP_RECORDS = true
    data class Record(val distanceKm: Double, val durationMin: Int, val fuelEfficiency: Double, val totalDistanceKm: Double) {
        fun values() = listOf("${distanceKm}km", "${durationMin}분", "${fuelEfficiency}km/L", "${totalDistanceKm}km")
    }
    val FIRST = Record(38.2, 52, 13.6, 276.0)
    val REVISIT = Record(42.6, 48, 14.2, 318.4)
}

internal data class ResultTripStat(val label: String, val value: String?, val mock: Boolean)

internal data class RegionResultPresentation(
    val firstVisit: Boolean,
    val regionName: String,
    val title: String,
    val summary: String,
    val visitCount: Int?,
    val newItems: List<RegionBriefingSouvenir>,
    val collectedItems: List<RegionBriefingSouvenir>,
    val collectedCount: Int?,
    val totalCount: Int?,
    val growthBefore: Int?,
    val growthAfter: Int?,
    val originalGrowth: String,
    val stats: List<ResultTripStat>,
    val source: DataSource?,
    val sourceLabel: String,
) {
    val grew: Boolean get() = growthBefore != null && growthAfter != null && growthAfter > growthBefore
    val hasMock: Boolean get() = stats.any { it.mock }
    val directConfirmation: Boolean get() = source == DataSource.USER_CONFIRMED
}

/** Read-only view adapter. Existing formatRegionBriefing remains the validity/visit discriminator. */
internal fun regionResultPresentation(
    briefing: RegionBriefing?, receipt: ArrivalReceipt?, catalog: Catalog?, snapshot: GameSnapshot?,
    history: RegionVisitHistory, useDemoRecords: Boolean,
): RegionResultPresentation? {
    if (briefing == null) return null
    val region = receipt?.regionId?.let { id -> catalog?.regions?.singleOrNull { it.id == id } }
    fun souvenir(id: String): RegionBriefingSouvenir {
        val item = catalog?.items?.singleOrNull { it.id == id && it.regionId == region?.id }
        return RegionBriefingSouvenir(item?.name ?: "이름을 알 수 없는 기념품", item?.type,
            item?.let { catalog?.assets?.get(it.assetKey)?.path })
    }
    val grantedIds = receipt?.grantedItemIds?.distinct()
    val newItems = when {
        grantedIds != null && catalog != null -> grantedIds.map(::souvenir)
        briefing.firstVisit -> briefing.souvenirs
        else -> emptyList()
    }
    val knownCollection = region != null && snapshot != null
    val collected = if (region != null && snapshot != null) region.itemIds.distinct()
        .filter { it in snapshot.ownedItemIds }.map(::souvenir) else briefing.souvenirs
    val count = (history as? RegionVisitHistory.Available)?.regions
        ?.singleOrNull { it.regionId == receipt?.regionId }?.visitCount?.takeIf { it > 0 }
    val validGrowth = receipt != null && receipt.growthBefore in 0..3 && receipt.growthAfter in receipt.growthBefore..3
    val mockAllowed = useDemoRecords // User-enabled exhibition presentation, independent of receipt source.
    val fallback = (if (briefing.firstVisit) ResultDemoOptions.FIRST else ResultDemoOptions.REVISIT).values()
    val labels = listOf("이번 주행", "주행 시간", "연비", "누적 주행")
    val stats = labels.mapIndexed { index, label ->
        // Honor the original formatter, including its deliberately absent USER_CONFIRMED metrics.
        val original = if (receipt?.source == DataSource.USER_CONFIRMED) null
            else briefing.tripStats.getOrNull(index)?.value?.takeIf { it.isNotBlank() && it != "—" }
        ResultTripStat(label, original ?: if (mockAllowed) fallback[index] else null,
            mock = original == null && mockAllowed)
    }
    return RegionResultPresentation(
        firstVisit = briefing.firstVisit, regionName = briefing.regionName,
        title = if (briefing.firstVisit) "${withObjectParticle(briefing.regionName)} 처음 발견했어요!" else briefing.title,
        summary = if (newItems.isNotEmpty()) "새 기념품 ${newItems.size}개를 받았어요."
            else if (briefing.firstVisit) "이번 방문에서 받은 새 기념품은 없어요." else "이번 방문에서 받은 새 선물은 없어요.",
        visitCount = count, newItems = newItems, collectedItems = collected,
        collectedCount = if (knownCollection) collected.size else null,
        totalCount = region?.itemIds?.distinct()?.size,
        growthBefore = receipt?.growthBefore?.takeIf { validGrowth },
        growthAfter = receipt?.growthAfter?.takeIf { validGrowth },
        originalGrowth = briefing.growth, stats = stats, source = receipt?.source, sourceLabel = briefing.source,
    )
}

internal fun resultStageLabel(stage: Int): String = when (stage) {
    0, 1 -> "새싹 섬"
    2 -> "숲 섬"
    3 -> "마을 섬"
    else -> "성장 정보 확인"
}
