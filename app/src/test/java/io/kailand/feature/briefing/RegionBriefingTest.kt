package io.kailand.feature.briefing

import io.kailand.core.contract.ArrivalReceipt
import io.kailand.core.contract.AssetDefinition
import io.kailand.core.contract.AvatarPreview
import io.kailand.core.contract.Catalog
import io.kailand.core.contract.DataSource
import io.kailand.core.contract.GameRules
import io.kailand.core.contract.ItemDefinition
import io.kailand.core.contract.ItemType
import io.kailand.core.contract.JourneyKind
import io.kailand.core.contract.RegionDefinition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RegionBriefingTest {
    private val avatar = AvatarPreview("tori", null)
    private val catalog = Catalog(
        contentVersion = "test",
        description = "",
        characters = emptyList(),
        expressions = emptyMap(),
        assets = mapOf("food_potato" to AssetDefinition("assets/starter/food_potato.png", "food")),
        regions = listOf(RegionDefinition("wonju", "원주", "", 37.34, 127.92,
            listOf("wonju_potato", "wonju_crown", "wonju_tree"))),
        items = listOf(
            ItemDefinition("wonju_potato", "wonju", ItemType.FOOD, "감자 음식", "food_potato"),
            ItemDefinition("wonju_crown", "wonju", ItemType.OUTFIT, "은행잎 머리장식", "outfit_ginkgo_crown"),
            ItemDefinition("wonju_tree", "wonju", ItemType.DECOR, "은행나무", "decor_ginkgo"),
        ),
        rules = GameRules(3, 1, "Asia/Seoul", emptyList()),
    )
    private val receipt = ArrivalReceipt(
        journeyId = "wonju-1", source = DataSource.USER_CONFIRMED, kind = JourneyKind.REGION,
        regionId = "wonju", completedAtEpochMs = 0, elapsedSeconds = null, distanceMeters = null,
        growthBefore = 0, growthAfter = 1, discoveredRegionId = "wonju",
        grantedItemIds = listOf("wonju_potato", "wonju_crown"),
    )

    @Test fun firstVisitDisplaysOnlyConfirmedGrantsAndFourUnavailableMetrics() {
        val result = requireNotNull(formatRegionBriefing(receipt, catalog, avatar))
        assertTrue(result.firstVisit)
        assertEquals("원주를 처음 발견했어요", result.title)
        assertEquals("원주 기념품 2개를 받았어요.", result.summary)
        assertEquals(listOf("감자 음식", "은행잎 머리장식"), result.souvenirs.map { it.name })
        assertEquals("assets/starter/food_potato.png", result.souvenirs.first().imagePath)
        assertEquals(listOf("이번 주행", "주행 시간", "연비", "누적 주행"), result.tripStats.map { it.label })
        assertEquals(List(4) { "—" }, result.tripStats.map { it.value })
        assertEquals("사용자 확인 완료 · 실제 방문 인증 아님", result.source)
    }

    @Test fun revisitCanGrowWithoutAwardingNewSouvenirs() {
        val revisit = receipt.copy(journeyId = "wonju-2", growthBefore = 1, growthAfter = 2,
            discoveredRegionId = null, grantedItemIds = emptyList())
        val result = requireNotNull(formatRegionBriefing(revisit, catalog, avatar))
        assertFalse(result.firstVisit)
        assertEquals("원주에 다시 왔어요", result.title)
        assertEquals("이번 방문에서 받은 새 선물은 없어요.", result.summary)
        assertEquals("성장 기록 · 1 → 2단계", result.growth)
        assertEquals(listOf("감자 음식", "은행잎 머리장식", "은행나무"), result.souvenirs.map { it.name })
    }

    @Test fun demoMeasurementsCanDisplayRealZerosWhileUnprovidedMetricsRemainUnavailable() {
        val demo = receipt.copy(source = DataSource.DEMO, elapsedSeconds = 0, distanceMeters = 0.0)
        val result = requireNotNull(formatRegionBriefing(demo, catalog, avatar))
        assertEquals("DEMO · 모의 여정", result.source)
        assertEquals(listOf("0m", "0초", "—", "—"), result.tripStats.map { it.value })
        val malformed = demo.copy(source = DataSource.USER_CONFIRMED)
        assertEquals(List(4) { "—" }, requireNotNull(formatRegionBriefing(malformed, catalog, avatar)).tripStats.map { it.value })
    }

    @Test fun unsupportedOrConflictingReceiptsDoNotCreateARegionalScreen() {
        assertNull(formatRegionBriefing(receipt.copy(kind = JourneyKind.DAILY), catalog, avatar))
        assertNull(formatRegionBriefing(receipt.copy(regionId = "unregistered"), catalog, avatar))
        assertNull(formatRegionBriefing(receipt.copy(discoveredRegionId = "daejeon"), catalog, avatar))
        assertNull(formatRegionBriefing(receipt.copy(discoveredRegionId = null), catalog, avatar))
    }

    @Test fun objectParticleFollowsFinalConsonant() {
        assertEquals("원주를", withObjectParticle("원주"))
        assertEquals("대전을", withObjectParticle("대전"))
        assertEquals("속초를", withObjectParticle("속초"))
        assertEquals("Seoul을(를)", withObjectParticle("Seoul"))
        assertEquals(listOf("음식", "의상", "장식"), result().souvenirs.map { itemTypeLabel(it.type) })
    }

    private fun result() = requireNotNull(formatRegionBriefing(
        receipt.copy(grantedItemIds = listOf("wonju_potato", "wonju_crown", "wonju_tree")), catalog, avatar))
}
