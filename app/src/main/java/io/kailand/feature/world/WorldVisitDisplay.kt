package io.kailand.feature.world

import io.kailand.core.contract.RegionVisitHistory
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal data class WorldVisitDisplay(
    val count: String,
    val firstDate: String = "미확인",
    val lastDate: String = "미확인",
    val notice: String? = null,
)

private val visitDateFormat = DateTimeFormatter.ofPattern("yyyy.MM.dd (E)", Locale.KOREAN)
    .withZone(ZoneId.of("Asia/Seoul"))

/** A/B own aggregation. C only formats committed read data, never reconstructs visit counts. */
internal fun worldVisitDisplay(regionId: String, history: RegionVisitHistory): WorldVisitDisplay {
    fun unknown(message: String) = WorldVisitDisplay("미확인", notice = message)
    return when (history) {
        RegionVisitHistory.Loading -> unknown("방문 기록 불러오는 중")
        is RegionVisitHistory.Unavailable -> unknown("방문 기록을 확인할 수 없어요.")
        is RegionVisitHistory.Available -> {
            val matches = history.regions.filter { it.regionId == regionId }
            if (matches.isEmpty()) return WorldVisitDisplay("0번", notice = "저장된 방문 기록이 없어요.")
            if (matches.size != 1) return unknown("방문 기록이 중복되어 확인할 수 없어요.")
            val record = matches.single()
            val first = record.firstVisitedAtEpochMs
            val last = record.lastVisitedAtEpochMs
            if (record.visitCount < 0 || (first != null && last != null && first > last) ||
                (record.visitCount == 0 && (first != null || last != null))) {
                return unknown("방문 기록을 확인할 수 없어요.")
            }
            WorldVisitDisplay("${record.visitCount}번",
                first?.let { visitDateFormat.format(Instant.ofEpochMilli(it)) } ?: "미확인",
                last?.let { visitDateFormat.format(Instant.ofEpochMilli(it)) } ?: "미확인")
        }
    }
}
