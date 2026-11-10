package io.kailand.feature.world

import io.kailand.core.contract.RegionVisitHistory
import io.kailand.core.contract.RegionVisitSummary
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class WorldVisitDisplayTest {
    private fun available(vararg rows: RegionVisitSummary) = RegionVisitHistory.Available(rows.toList())

    @Test fun missingAndLoadingRecordsAreNotZeroVisits() {
        assertEquals("미확인", worldVisitDisplay("region", RegionVisitHistory.Loading).count)
        assertEquals("미확인", worldVisitDisplay("region", RegionVisitHistory.Unavailable("error")).count)
        assertEquals("0번", worldVisitDisplay("region", available()).count)
        assertEquals("미확인", worldVisitDisplay("region", available()).firstDate)
    }

    @Test fun datesUseKstYearAndKoreanWeekdayAcrossUtcYearBoundary() {
        val first = Instant.parse("2025-12-31T15:00:00Z").toEpochMilli()
        val last = Instant.parse("2026-01-01T15:00:00Z").toEpochMilli()
        val view = worldVisitDisplay("region", available(RegionVisitSummary("region", 2, first, last)))
        assertEquals("2번", view.count)
        assertEquals("2026.01.01 (목)", view.firstDate)
        assertEquals("2026.01.02 (금)", view.lastDate)
    }

    @Test fun nullDatesRemainUnknownAndEpochZeroIsNotMissing() {
        val view = worldVisitDisplay("region", available(RegionVisitSummary("region", 1, null, 0)))
        assertEquals("1번", view.count)
        assertEquals("미확인", view.firstDate)
        assertEquals("1970.01.01 (목)", view.lastDate)
    }

    @Test fun ambiguousAndInconsistentRecordsAreNeverSummedOrRepaired() {
        val row = RegionVisitSummary("region", 1, 1, 2)
        for (history in listOf(available(row, row), available(row.copy(visitCount = -1)),
            available(row.copy(firstVisitedAtEpochMs = 3)), available(row.copy(visitCount = 0)))) {
            assertEquals("미확인", worldVisitDisplay("region", history).count)
            assertEquals("미확인", worldVisitDisplay("region", history).firstDate)
        }
        assertEquals("0번", worldVisitDisplay("other", available(row)).count)
        assertEquals(1, row.visitCount)
    }
}
