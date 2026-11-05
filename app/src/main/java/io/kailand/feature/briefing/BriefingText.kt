package io.kailand.feature.briefing

import io.kailand.core.contract.ArrivalReceipt
import io.kailand.core.contract.AvatarPreview
import io.kailand.core.contract.Catalog
import io.kailand.core.contract.DataSource
import io.kailand.core.contract.JourneyKind
import java.math.BigDecimal
import java.math.RoundingMode

/** Display only: no reward policy, storage, or SDK calls. Null measurements stay absent. */
data class BriefingText(
    val source: String,
    val title: String,
    val greeting: String,
    val character: String,
    val outfit: String?,
    val elapsed: String?,
    val distance: String?,
    val growth: String,
    val discovery: String?,
    val souvenirs: List<String>,
    val notices: List<String>,
)

fun formatElapsedSeconds(seconds: Long?): String? {
    if (seconds == null || seconds < 0) return null
    val hours = seconds / 3600
    val minutes = seconds % 3600 / 60
    val remainder = seconds % 60
    return listOfNotNull(
        hours.takeIf { it > 0 }?.let { "${it}시간" },
        minutes.takeIf { it > 0 }?.let { "${it}분" },
        remainder.takeIf { it > 0 || seconds == 0L }?.let { "${it}초" },
    ).joinToString(" ")
}

fun formatDistanceMeters(meters: Double?): String? {
    if (meters == null || !meters.isFinite() || meters < 0) return null
    if (meters == 0.0) return "0m"
    if (meters < 1) return "1m 미만"
    val value = BigDecimal.valueOf(meters)
    if (meters < 1000) return "${value.setScale(0, RoundingMode.DOWN).toPlainString()}m"
    val km = value.movePointLeft(3).setScale(1, RoundingMode.DOWN).stripTrailingZeros()
    return "${km.toPlainString()}km"
}

fun formatBriefing(receipt: ArrivalReceipt, catalog: Catalog, avatar: AvatarPreview): BriefingText {
    val notices = mutableListOf<String>()
    val character = catalog.characters.firstOrNull { it.id == avatar.characterId }?.name
        ?: "친구".also { notices += "캐릭터 정보를 찾지 못했어요." }
    val outfit = avatar.outfitItemId?.let { id ->
        catalog.items.firstOrNull { it.id == id }?.name
            ?: "알 수 없는 의상".also { notices += "의상 정보가 누락됐어요." }
    }
    fun regionName(id: String): String = catalog.regions.firstOrNull { it.id == id }?.name
        ?: "이름을 알 수 없는 지역".also { notices += "지역 정보가 누락됐어요." }
    val region = receipt.regionId?.let(::regionName)
    val discovery = receipt.discoveredRegionId?.let { "발견 기록 · ${regionName(it)}" }
    val souvenirs = receipt.grantedItemIds.map { id ->
        catalog.items.firstOrNull { it.id == id }?.name
            ?: "이름을 알 수 없는 기념품".also { notices += "기념품 정보가 누락됐어요." }
    }
    val elapsed = formatElapsedSeconds(receipt.elapsedSeconds)
    val distance = formatDistanceMeters(receipt.distanceMeters)
    if (receipt.elapsedSeconds != null && elapsed == null) notices += "이동 시간 정보를 표시할 수 없어요."
    if (receipt.distanceMeters != null && distance == null) notices += "이동 거리 정보를 표시할 수 없어요."
    val growth = when {
        receipt.growthBefore < 0 || receipt.growthAfter < receipt.growthBefore ->
            "성장 기록을 확인할 수 없어요.".also { notices += "성장 정보가 올바르지 않아요." }
        receipt.growthBefore == receipt.growthAfter -> "추가 성장 없음 · ${receipt.growthAfter}단계"
        else -> "성장 기록 · ${receipt.growthBefore} → ${receipt.growthAfter}단계"
    }
    return BriefingText(
        source = when (receipt.source) {
            DataSource.DEMO -> "DEMO · 모의 여정"
            DataSource.PLEOS -> "PLEOS · 여정 기록"
            DataSource.USER_CONFIRMED -> "사용자 확인 완료 · 실제 방문 인증 아님"
        },
        title = when (receipt.kind) {
            JourneyKind.DAILY -> "일상의 이동이 추억으로"
            JourneyKind.REGION -> region?.let { "$it 여행의 추억" } ?: "여행의 추억"
        },
        greeting = "$character, 함께한 이동을 기억해요.",
        character = character,
        outfit = outfit,
        elapsed = elapsed,
        distance = distance,
        growth = growth,
        discovery = discovery,
        souvenirs = souvenirs,
        notices = notices.distinct(),
    )
}
