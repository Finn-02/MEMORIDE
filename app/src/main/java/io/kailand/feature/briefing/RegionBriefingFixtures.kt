package io.kailand.feature.briefing

import io.kailand.core.contract.ArrivalReceipt
import io.kailand.core.contract.AssetDefinition
import io.kailand.core.contract.AvatarPreview
import io.kailand.core.contract.Catalog
import io.kailand.core.contract.CharacterDefinition
import io.kailand.core.contract.DataSource
import io.kailand.core.contract.GameRules
import io.kailand.core.contract.GateState
import io.kailand.core.contract.InteractionGate
import io.kailand.core.contract.ItemDefinition
import io.kailand.core.contract.ItemType
import io.kailand.core.contract.JourneyKind
import io.kailand.core.contract.RegionDefinition

/** Preview/test samples only. IDs and names mirror content/catalog.json; values are not live records. */
internal object RegionBriefingFixtures {
    val allowed = InteractionGate(GateState.PARKED_ALLOWED)
    val avatar = AvatarPreview("tori", null)

    val catalog = Catalog(
        contentVersion = "fixture",
        description = "S05 preview fixture",
        characters = listOf(CharacterDefinition("tori", "토리", "char_tori")),
        expressions = emptyMap(),
        assets = mapOf(
            "food_potato" to AssetDefinition("assets/starter/food_potato.png", "food"),
            "outfit_ginkgo_crown" to AssetDefinition("assets/starter/outfit_ginkgo_crown.png", "outfit"),
            "decor_ginkgo" to AssetDefinition("assets/starter/decor_ginkgo.png", "decor"),
        ),
        regions = listOf(RegionDefinition("wonju", "원주", "", 37.34, 127.92,
            listOf("wonju_potato", "wonju_crown", "wonju_tree"))),
        items = listOf(
            ItemDefinition("wonju_potato", "wonju", ItemType.FOOD, "감자 음식", "food_potato"),
            ItemDefinition("wonju_crown", "wonju", ItemType.OUTFIT, "은행잎 머리장식", "outfit_ginkgo_crown"),
            ItemDefinition("wonju_tree", "wonju", ItemType.DECOR, "은행나무", "decor_ginkgo"),
        ),
        rules = GameRules(3, 1, "Asia/Seoul", listOf("slot_left", "slot_right", "slot_back")),
    )

    /** User-confirmed arrival: distance/time are not measured, so all four tiles stay "—". */
    val firstVisitReceipt = ArrivalReceipt(
        journeyId = "fixture-wonju-first", source = DataSource.USER_CONFIRMED, kind = JourneyKind.REGION,
        regionId = "wonju", completedAtEpochMs = 0, elapsedSeconds = null, distanceMeters = null,
        growthBefore = 0, growthAfter = 1, discoveredRegionId = "wonju",
        grantedItemIds = listOf("wonju_potato", "wonju_crown", "wonju_tree"),
    )

    /** Same-day revisit: no new grant and no growth. */
    val revisitReceipt = firstVisitReceipt.copy(
        journeyId = "fixture-wonju-revisit", growthBefore = 1, growthAfter = 1,
        discoveredRegionId = null, grantedItemIds = emptyList(),
    )

    /** DEMO arrival with mock distance/time from the receipt; fuel economy/odometer stay "—". */
    val firstVisitDemoReceipt = firstVisitReceipt.copy(
        journeyId = "fixture-wonju-demo", source = DataSource.DEMO, elapsedSeconds = 1_200, distanceMeters = 5_000.0,
    )

    fun firstVisitUserConfirmed() = formatRegionBriefing(firstVisitReceipt, catalog, avatar)
    fun firstVisitDemo() = formatRegionBriefing(firstVisitDemoReceipt, catalog, avatar)
    fun revisitUserConfirmed() = formatRegionBriefing(revisitReceipt, catalog, avatar)
}
