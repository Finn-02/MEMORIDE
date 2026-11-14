package io.kailand.feature.briefing

import io.kailand.core.contract.AvatarPreview
import io.kailand.core.contract.DataSource
import io.kailand.core.contract.GateState
import java.io.File
import org.junit.Assert.*
import org.junit.Test

internal fun fixture(id: String): BriefingSample {
    val root = listOf(File("."), File("..")).first { File(it, "fixtures/ui_samples.json").exists() }
    return readBriefingSample(File(root, "content/catalog.json").readText(), File(root, "fixtures/ui_samples.json").readText(), id)
}

class BriefingTextTest {
    @Test fun regionalFixtureUsesCatalogAndPreservesOriginalSouvenirsOnReplay() {
        val sample = fixture("regional")
        val receipt = requireNotNull(sample.receipt)
        val text = formatBriefing(receipt, sample.catalog, sample.avatar)
        assertEquals("DEMO · 모의 여정", text.source)
        assertEquals("대전 여행의 추억", text.title)
        assertEquals("토리", text.character)
        assertEquals("빵모자", text.outfit)
        assertEquals("20분", text.elapsed)
        assertEquals("5km", text.distance)
        assertEquals(listOf("빵", "빵모자", "작은 빵 간판"), text.souvenirs)
        assertEquals("발견 기록 · 대전", text.discovery)
        assertEquals(text, formatBriefing(receipt.copy(), sample.catalog, sample.avatar))
        assertEquals(3, receipt.grantedItemIds.size)
        assertEquals(0, receipt.growthBefore)
        assertEquals(1, receipt.growthAfter)
        assertEquals("성장 기록 · 0 → 1단계", text.growth)
        assertTrue(text.notices.isEmpty())
    }

    @Test fun dailyAndRevisitDoNotInventGiftsOrDiscovery() {
        val daily = fixture("daily")
        val text = formatBriefing(requireNotNull(daily.receipt), daily.catalog, daily.avatar)
        assertEquals("일상의 이동이 추억으로", text.title)
        assertEquals("성장 기록 · 0 → 1단계", text.growth)
        assertNull(text.discovery)
        assertTrue(text.souvenirs.isEmpty())
        val current = fixture("daily_current")
        val currentText = formatBriefing(requireNotNull(current.receipt), current.catalog, current.avatar)
        assertEquals("추가 성장 없음 · 0단계", currentText.growth)
        assertTrue(currentText.souvenirs.isEmpty())
        assertNull(currentText.discovery)

        val regional = fixture("regional")
        val first = requireNotNull(regional.receipt)
        val revisit = formatBriefing(
            first.copy(growthBefore = first.growthAfter, discoveredRegionId = null, grantedItemIds = emptyList()),
            regional.catalog, regional.avatar,
        )
        assertEquals("추가 성장 없음 · 1단계", revisit.growth)
        assertTrue(revisit.souvenirs.isEmpty())
        assertNull(revisit.discovery)
    }

    @Test fun missingMetricsAreAbsentButActualZerosAreVisible() {
        val s = fixture("missing_metrics")
        val receipt = requireNotNull(s.receipt)
        val missing = formatBriefing(receipt, s.catalog, s.avatar)
        assertNull(missing.elapsed)
        assertNull(missing.distance)
        val zeros = formatBriefing(receipt.copy(elapsedSeconds = 0, distanceMeters = 0.0), s.catalog, s.avatar)
        assertEquals("0초", zeros.elapsed)
        assertEquals("0m", zeros.distance)
        assertEquals("원주 여행의 추억", missing.title)
    }

    @Test fun unknownReferencesFallBackWithoutChangingReceipt() {
        val s = fixture("regional")
        val receipt = requireNotNull(s.receipt).copy(regionId = "bad", discoveredRegionId = "bad", grantedItemIds = listOf("bad"))
        val text = formatBriefing(receipt, s.catalog, AvatarPreview("bad", "bad"))
        assertEquals("친구", text.character)
        assertEquals("알 수 없는 의상", text.outfit)
        assertEquals(listOf("이름을 알 수 없는 기념품"), text.souvenirs)
        assertEquals(4, text.notices.size)
        assertEquals(listOf("bad"), receipt.grantedItemIds)
    }

    @Test fun invalidAndExtremeMeasurementsAreSafeAndDoNotRoundUp() {
        assertNull(formatElapsedSeconds(-1))
        assertNotNull(formatElapsedSeconds(Long.MAX_VALUE))
        for (meters in listOf(-1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            assertNull(formatDistanceMeters(meters))
        }
        assertEquals("1m 미만", formatDistanceMeters(0.4))
        assertEquals("999m", formatDistanceMeters(999.9))
        assertEquals("1km", formatDistanceMeters(1099.9))
        assertEquals("1.2km", formatDistanceMeters(1299.9))
        assertEquals("1시간 1분 1초", formatElapsedSeconds(3661))
        assertNotNull(formatDistanceMeters(Double.MAX_VALUE))
    }

    @Test fun freshRemainsEmptyAndRestrictedFixtureRetainsItsGate() {
        assertNull(fixture("fresh").receipt)
        assertEquals(GateState.RESTRICTED, fixture("restricted_pending").gate.state)
        assertEquals(fixture("regional").receipt, fixture("restricted_pending").receipt)
    }

    @Test fun userConfirmedReceiptHasExplicitLabelAndKeepsMeasurementsAbsent() {
        val s = fixture("missing_metrics")
        val receipt = requireNotNull(s.receipt).copy(source = DataSource.USER_CONFIRMED)
        val text = formatBriefing(receipt, s.catalog, s.avatar)
        assertEquals("사용자 확인 완료 · 실제 방문 인증 아님", text.source)
        assertNull(text.elapsed)
        assertNull(text.distance)
        assertEquals(DataSource.USER_CONFIRMED, receipt.source)
        val demo = formatBriefing(receipt.copy(source = DataSource.DEMO), s.catalog, s.avatar)
        val pleos = formatBriefing(receipt.copy(source = DataSource.PLEOS), s.catalog, s.avatar)
        assertEquals("DEMO · 모의 여정", demo.source)
        assertEquals("PLEOS · 여정 기록", pleos.source)
    }

    @Test fun sourceAndInvalidGrowthDoNotClaimSdkVerificationOrGrantSuccess() {
        val s = fixture("daily")
        val text = formatBriefing(requireNotNull(s.receipt).copy(source = DataSource.PLEOS, growthBefore = 2, growthAfter = 1), s.catalog, s.avatar)
        assertEquals("PLEOS · 여정 기록", text.source)
        assertEquals("성장 기록을 확인할 수 없어요.", text.growth)
        assertTrue(text.notices.isNotEmpty())
    }
}
