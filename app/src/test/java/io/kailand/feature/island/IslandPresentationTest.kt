package io.kailand.feature.island

import io.kailand.core.contract.AvatarPreview
import io.kailand.core.contract.AssetDefinition
import io.kailand.core.contract.Catalog
import io.kailand.core.contract.CharacterDefinition
import io.kailand.core.contract.GameAction
import io.kailand.core.contract.GameRules
import io.kailand.core.contract.GameSnapshot
import io.kailand.core.contract.ItemDefinition
import io.kailand.core.contract.ItemType
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IslandPresentationTest {
    private val catalog = islandTestCatalog

    @Test fun growthUsesOnlyTheSavedStage() {
        val labels = listOf("작은 빈 정원", "길", "꽃밭", "휴식 시설")
        for (stage in labels.indices) {
            val growth = islandPresentation(islandTestSnapshot.copy(dailyGrowth = stage), catalog).growth
            assertEquals(stage, growth?.ordinal)
            assertEquals(labels[stage], growth?.label)
        }
        assertNull(islandPresentation(islandTestSnapshot.copy(dailyGrowth = 4), catalog).growth)
    }

    @Test fun threeCharactersAndCurrentChoiceComeFromCatalogAndSnapshot() {
        val view = islandPresentation(islandTestSnapshot, catalog)
        assertEquals(listOf("tori", "momo", "lulu"), view.characters.map { it.id })
        assertEquals(listOf("momo"), view.characters.filter { it.selected }.map { it.id })
        assertEquals(catalog.characters.single { it.id == "momo" }.name, view.characterName)
    }

    @Test fun allNineRegisteredItemsAreGroupedAndUnownedItemsAreLocked() {
        val view = islandPresentation(islandTestSnapshot, catalog)
        assertEquals(9, view.items.size)
        for (type in ItemType.entries) {
            val group = view.itemsFor(type)
            assertEquals(3, group.size)
            assertTrue(group.all { it.type == type })
            assertEquals(1, group.count { it.owned })
        }
        assertFalse(view.accepts(GameAction.Eat("wonju_potato")))
        assertFalse(view.accepts(GameAction.EquipOutfit("wonju_crown")))
        assertFalse(view.accepts(GameAction.Eat("daejeon_hat")))
        assertTrue(view.accepts(GameAction.Eat("daejeon_bread")))
    }

    @Test fun onlyOwnedOutfitsCanShowAsEquipped() {
        val view = islandPresentation(islandTestSnapshot, catalog)
        assertEquals("daejeon_hat", view.items.single { it.equipped }.id)
        assertEquals(catalog.items.single { it.id == "daejeon_hat" }.name, view.outfitStatus)
        val wrongType = islandTestSnapshot.copy(avatar = AvatarPreview("momo", "daejeon_bread"))
        assertTrue(islandPresentation(wrongType, catalog).items.none { it.equipped })
    }

    @Test fun slotsAreFixedAndAPlacedDecorationCannotMoveToAnotherSlot() {
        val view = islandPresentation(islandTestSnapshot, catalog)
        assertEquals(listOf("slot_left", "slot_right", "slot_back"), view.slots.map { it.id })
        assertEquals("daejeon_sign", view.slots.first().savedItemId)
        assertEquals(catalog.items.single { it.id == "daejeon_sign" }.name, view.slots.first().itemName)
        assertEquals("비어 있음", view.slots[1].status)
        assertTrue(view.accepts(GameAction.PlaceDecoration("slot_left", null)))
        assertFalse(view.accepts(GameAction.PlaceDecoration("slot_right", "daejeon_sign")))
        assertFalse(view.accepts(GameAction.PlaceDecoration("custom_slot", null)))
    }

    @Test fun ambiguousCatalogEntriesAndLegacyReferencesAreNotValidChoices() {
        val invalidCatalog = catalog.copy(
            characters = catalog.characters + catalog.characters.single { it.id == "momo" },
            items = catalog.items + catalog.items.first { it.id == "daejeon_bread" }.let {
                listOf(it.copy(type = ItemType.OUTFIT), it.copy(id = "bad id"))
            },
            rules = catalog.rules.copy(decorationSlotIds = listOf("slot_left", "slot_left", "slot_back", "other")),
        )
        val legacy = islandTestSnapshot.copy(
            avatar = AvatarPreview("momo", "unknown_outfit"),
            ownedItemIds = islandTestSnapshot.ownedItemIds + setOf("bad id", "unknown_decor"),
            decorationSlots = mapOf("slot_back" to "unknown_decor"),
        )
        val view = islandPresentation(legacy, invalidCatalog)
        assertTrue(view.hasCatalogIssues)
        assertEquals("캐릭터 정보 확인 필요", view.characterName)
        assertEquals("저장된 의상 정보 확인 필요", view.outfitStatus)
        assertEquals("저장된 장식 정보 확인 필요", view.slots.last().status)
        assertFalse(view.accepts(GameAction.SelectCharacter("momo")))
        assertFalse(view.accepts(GameAction.Eat("daejeon_bread")))
        assertFalse(view.accepts(GameAction.Eat("bad id")))
        assertFalse(view.accepts(GameAction.PlaceDecoration("slot_left", null)))
        assertFalse(view.accepts(GameAction.PlaceDecoration("other", null)))
    }

    @Test fun displayingLegacySaveDoesNotRepairOrInventItems() {
        val owned = mutableSetOf("daejeon_sign", "removed_food")
        val slots = mutableMapOf("slot_left" to "daejeon_sign", "legacy_slot" to "daejeon_sign")
        val avatar = AvatarPreview("removed_character", "removed_outfit")
        val snapshot = islandTestSnapshot.copy(avatar = avatar, ownedItemIds = owned, decorationSlots = slots)
        val view = islandPresentation(snapshot, catalog)
        assertEquals(9, view.items.size)
        assertFalse(view.items.any { it.id == "removed_food" })
        assertNull(view.slots.first().itemName)
        assertFalse(view.accepts(GameAction.PlaceDecoration("slot_left", "daejeon_sign")))
        assertEquals(setOf("daejeon_sign", "removed_food"), owned)
        assertEquals(mapOf("slot_left" to "daejeon_sign", "legacy_slot" to "daejeon_sign"), slots)
        assertEquals(avatar, snapshot.avatar)
    }
}

internal val islandTestSnapshot = GameSnapshot(
    AvatarPreview("momo", "daejeon_hat"), 2,
    setOf("daejeon_bread", "daejeon_hat", "daejeon_sign"), setOf("daejeon"),
    mapOf("slot_left" to "daejeon_sign"), null,
)

internal val islandTestRoot: File by lazy {
    generateSequence(File(checkNotNull(System.getProperty("user.dir")))) { it.parentFile }
        .first { File(it, "content/catalog.json").isFile }
}

internal val islandTestCatalog: Catalog by lazy {
    val root = Json.parseToJsonElement(islandTestRoot.resolve("content/catalog.json").readText(Charsets.UTF_8)).jsonObject
    val rules = root.getValue("rules").jsonObject
    Catalog(
        root.getValue("contentVersion").jsonPrimitive.content, "", root.getValue("characters").jsonArray.map {
            val entry = it.jsonObject
            CharacterDefinition(entry.getValue("id").jsonPrimitive.content, entry.getValue("name").jsonPrimitive.content,
                entry.getValue("assetKey").jsonPrimitive.content)
        },
        root.getValue("expressions").jsonObject.mapValues { it.value.jsonPrimitive.content },
        root.getValue("assets").jsonObject.mapValues { (_, value) ->
            val asset = value.jsonObject
            AssetDefinition(asset.getValue("path").jsonPrimitive.content, asset.getValue("kind").jsonPrimitive.content)
        },
        emptyList(),
        root.getValue("items").jsonArray.map {
            val entry = it.jsonObject
            ItemDefinition(entry.getValue("id").jsonPrimitive.content, entry.getValue("regionId").jsonPrimitive.content,
                ItemType.valueOf(entry.getValue("type").jsonPrimitive.content), entry.getValue("name").jsonPrimitive.content,
                entry.getValue("assetKey").jsonPrimitive.content)
        },
        GameRules(rules.getValue("dailyGrowthCap").jsonPrimitive.int, rules.getValue("dailyGrantLimit").jsonPrimitive.int,
            rules.getValue("timeZone").jsonPrimitive.content,
            rules.getValue("decorationSlotIds").jsonArray.map { it.jsonPrimitive.content }),
    )
}
