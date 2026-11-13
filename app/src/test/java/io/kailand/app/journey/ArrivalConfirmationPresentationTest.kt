package io.kailand.app.journey

import io.kailand.core.contract.*
import org.junit.Assert.*
import org.junit.Test

class ArrivalConfirmationPresentationTest {
    private val catalog = Catalog("test", "", emptyList(), emptyMap(), emptyMap(),
        listOf(RegionDefinition("wonju", "원주", "", 0.0, 0.0, listOf("food", "outfit", "decor"))),
        listOf(ItemDefinition("food", "wonju", ItemType.FOOD, "감자 음식", "food"),
            ItemDefinition("outfit", "wonju", ItemType.OUTFIT, "머리장식", "outfit"),
            ItemDefinition("decor", "wonju", ItemType.DECOR, "나무", "decor")),
        GameRules(3, 1, "Asia/Seoul", emptyList()))
    private val destination = DestinationSpec("wonju", "원주", 0.0, 0.0)
    private fun snapshot(vararg owned: String) = GameSnapshot(AvatarPreview("tori", null), 0,
        owned.toSet(), emptySet(), emptyMap(), null)

    @Test fun previewUsesFirstUnownedItemInRegionOrder() {
        assertEquals("감자 음식", arrivalRewardHint(destination, catalog, snapshot())?.itemName)
        assertEquals("머리장식", arrivalRewardHint(destination, catalog, snapshot("food"))?.itemName)
    }

    @Test fun collectedRegionKeepsNoticeWithoutPromisingAnotherItem() {
        val hint = arrivalRewardHint(destination, catalog, snapshot("food", "outfit", "decor"))
        assertNotNull(hint)
        assertNull(hint?.itemName)
    }

    @Test fun unregisteredOrMissingDestinationHidesHint() {
        assertNull(arrivalRewardHint(destination.copy(regionId = null), catalog, snapshot()))
        assertNull(arrivalRewardHint(destination.copy(regionId = "unknown"), catalog, snapshot()))
        assertNull(arrivalRewardHint(null, catalog, snapshot()))
    }

    @Test fun inconsistentCatalogDoesNotPromiseGift() {
        assertNull(arrivalRewardHint(destination, catalog.copy(items = emptyList()), snapshot()))
    }

    @Test fun koreanObjectParticleMatchesFinalConsonant() {
        assertEquals("을", arrivalObjectParticle("감자 음식"))
        assertEquals("를", arrivalObjectParticle("나무"))
        assertEquals("을", arrivalObjectParticle("머리장식"))
    }
}
