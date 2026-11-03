package io.kailand.feature.world

import io.kailand.core.contract.*
import java.io.File
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class WorldPresentationTest {
    private val root = listOf(File("."), File("..")).first { File(it, "content/catalog.json").isFile }
    private fun JsonObject.text(key: String) = getValue(key).jsonPrimitive.content
    private fun JsonElement.strings() = jsonArray.map { it.jsonPrimitive.content }
    private val json = Json.parseToJsonElement(File(root, "content/catalog.json").readText(Charsets.UTF_8)).jsonObject
    // Read the repository source of truth; do not create a second hard-coded content catalog.
    private val catalog = Catalog(
        json.text("contentVersion"), json.text("description"), emptyList(), emptyMap(),
        json.getValue("assets").jsonObject.mapValues { (_, v) -> v.jsonObject.let { AssetDefinition(it.text("path"), it.text("kind")) } },
        json.getValue("regions").jsonArray.map { v -> v.jsonObject.let {
            RegionDefinition(it.text("id"), it.text("name"), it.text("theme"),
                it.getValue("latitude").jsonPrimitive.double, it.getValue("longitude").jsonPrimitive.double,
                it.getValue("itemIds").strings())
        } },
        json.getValue("items").jsonArray.map { v -> v.jsonObject.let {
            ItemDefinition(it.text("id"), it.text("regionId"), ItemType.valueOf(it.text("type")), it.text("name"), it.text("assetKey"))
        } },
        GameRules(3, 1, "Asia/Seoul", emptyList()),
    )
    private fun sample(id: String): GameSnapshot {
        val samples = Json.parseToJsonElement(File(root, "fixtures/ui_samples.json").readText(Charsets.UTF_8))
            .jsonObject.getValue("samples").jsonArray
        val game = samples.single { it.jsonObject.text("id") == id }.jsonObject.getValue("game").jsonObject
        val avatar = game.getValue("avatar").jsonObject
        return GameSnapshot(AvatarPreview(avatar.text("characterId"), avatar["outfitItemId"]?.jsonPrimitive?.contentOrNull),
            game.getValue("dailyGrowth").jsonPrimitive.int, game.getValue("ownedItemIds").strings().toSet(),
            game.getValue("discoveredRegionIds").strings().toSet(), emptyMap(), null)
    }
    private val allowed = InteractionGate(GateState.PARKED_ALLOWED)

    @Test fun freshCatalogRegionsAreLockedButEligibleAsFirstDestinations() {
        val view = worldPresentation(sample("fresh"), catalog)
        assertEquals(catalog.regions.map { it.name }, view.regions.map { it.name })
        assertEquals(0, view.ownedCount)
        assertEquals(catalog.items.size, view.totalCount)
        assertTrue(view.regions.all { !it.discovered && it.canPrepare })
        assertTrue(view.issues.isEmpty())
    }

    @Test fun regionalFixtureKeepsDiscoverySeparateFromOwnership() {
        val snapshot = sample("regional")
        val view = worldPresentation(snapshot, catalog)
        assertEquals(3, view.ownedCount)
        assertEquals(9, view.totalCount)
        val found = view.regions.single { it.discovered }
        assertEquals(snapshot.discoveredRegionIds.single(), found.id)
        assertTrue(found.items.all { it.owned })
        assertTrue(view.regions.filter { !it.discovered }.flatMap { it.items }.none { it.owned })
        val first = found.items.first().id
        val partial = worldPresentation(snapshot.copy(ownedItemIds = setOf(first)), catalog)
        assertEquals(1, partial.ownedCount)
        assertEquals(1, partial.regions.single { it.discovered }.items.count { it.owned })
        assertEquals(snapshot, sample("regional"))
    }

    @Test fun unknownSaveIdsAreNotCountedOrDeleted() {
        val snapshot = sample("regional").copy(ownedItemIds = sample("regional").ownedItemIds + "future_item",
            discoveredRegionIds = sample("regional").discoveredRegionIds + "future_region")
        val view = worldPresentation(snapshot, catalog)
        assertEquals(3, view.ownedCount)
        assertEquals(catalog.regions.size, view.regions.size)
        assertTrue("future_item" in snapshot.ownedItemIds)
        assertTrue("future_region" in snapshot.discoveredRegionIds)
    }

    @Test fun resetInputImmediatelyRemovesPreviousDiscoveryAndCollection() {
        assertEquals(3, worldPresentation(sample("regional"), catalog).ownedCount)
        val reset = worldPresentation(sample("fresh"), catalog)
        assertEquals(0, reset.ownedCount)
        assertTrue(reset.regions.none { it.discovered })
        assertTrue(reset.regions.flatMap { it.items }.none { it.owned })
    }

    @Test fun missingItemReferenceIsVisibleAndBlocksOnlyAffectedCandidate() {
        val region = catalog.regions.first()
        val broken = catalog.copy(regions = listOf(region.copy(itemIds = region.itemIds + "missing")) + catalog.regions.drop(1))
        val view = worldPresentation(sample("regional"), broken)
        val missing = view.regions.first().items.last()
        assertNull(missing.name)
        assertNull(missing.imagePath)
        assertTrue(missing.issues.isNotEmpty())
        assertFalse(view.regions.first().canPrepare)
        assertTrue(view.regions.drop(1).all { it.canPrepare })
        assertEquals(catalog.items.size, view.totalCount)
    }

    @Test fun wrongRegionDuplicateReferencesAndMissingAssetsAreReported() {
        val region = catalog.regions.first()
        val item = catalog.items.first { it.id in region.itemIds }
        val duplicate = catalog.copy(regions = listOf(region.copy(itemIds = region.itemIds + region.itemIds.first())))
        assertFalse(worldPresentation(sample("fresh"), duplicate).regions.single().canPrepare)
        val wrong = catalog.copy(items = catalog.items.map { if (it.id == item.id) it.copy(regionId = "wrong") else it })
        assertTrue(worldPresentation(sample("fresh"), wrong).regions.first().items.first().issues.isNotEmpty())
        val missing = catalog.copy(assets = catalog.assets - item.assetKey)
        val displayed = worldPresentation(sample("regional"), missing).regions.first().items.first()
        assertNull(displayed.imagePath)
        assertTrue(displayed.issues.isNotEmpty())
    }

    @Test fun duplicateCatalogIdsNeverChooseAnArbitraryDestinationOrItem() {
        for (bad in listOf(catalog.copy(regions = catalog.regions + catalog.regions.first()),
            catalog.copy(items = catalog.items + catalog.items.first()))) {
            val view = worldPresentation(sample("fresh"), bad)
            assertTrue(view.issues.isNotEmpty())
            assertTrue(view.regions.none { it.canPrepare })
            assertEquals(9, view.totalCount)
            val actions = WorldActions({ WorldInputs(sample("fresh"), bad, allowed, true) }, { fail("ambiguous callback") }, {})
            assertFalse(actions.prepare(catalog.regions.first().id))
        }
    }

    @Test fun invalidCoordinatesAndEmptyRegionsNeverFabricateCandidates() {
        val region = catalog.regions.first()
        for (invalid in listOf(region.copy(latitude = Double.NaN), region.copy(longitude = 181.0),
            region.copy(latitude = -91.0), region.copy(name = ""), region.copy(id = ""))) {
            val broken = catalog.copy(regions = listOf(invalid))
            assertFalse(worldPresentation(sample("fresh"), broken).regions.single().canPrepare)
        }
        val empty = catalog.copy(regions = emptyList())
        assertTrue(worldPresentation(sample("fresh"), empty).regions.isEmpty())
        assertFalse(WorldActions({ WorldInputs(sample("fresh"), empty, allowed, true) }, { fail() }, {}).prepare(region.id))
    }

    @Test fun imagesUseCatalogAssetPathsAndRejectTraversalAbsoluteAndUnexpectedPaths() {
        val item = catalog.items.first()
        val expected = catalog.assets.getValue(item.assetKey).path
        assertTrue(safeWorldImagePath(expected))
        for (bad in listOf("../outside.png", "C:/secret.png", "/tmp/file.png", "assets/starter/../x.png",
            "assets/starter/sub/x.png", "assets/starter/a\\b.png", "https://example.org/x.png", "assets/starter/x.jpg")) {
            assertFalse(bad, safeWorldImagePath(bad))
        }
        val customPath = "assets/starter/catalog_named_image.png"
        val altered = catalog.copy(assets = catalog.assets + (item.assetKey to AssetDefinition(customPath, "test")))
        val projection = worldPresentation(sample("regional"), altered)
        assertEquals(customPath, projection.regions.flatMap { it.items }.single { it.id == item.id }.imagePath)
    }

    @Test fun candidateAndInventoryActionsUseOnlyCallbacksAndLatestInputs() {
        var inputs = WorldInputs(sample("fresh"), catalog, allowed, true)
        val selected = mutableListOf<String>()
        var opened = 0
        val actions = WorldActions({ inputs }, { selected.add(it) }, { opened++ })
        val region = catalog.regions.first()
        assertTrue(actions.prepare(region.id))
        assertEquals(listOf(region.id), selected)
        assertTrue(actions.openItems())
        assertEquals(1, opened)
        for (gate in listOf(GateState.UNKNOWN, GateState.RESTRICTED)) {
            inputs = inputs.copy(gate = InteractionGate(gate))
            assertFalse(actions.prepare(region.id))
            assertFalse(actions.openItems())
        }
        inputs = inputs.copy(gate = allowed, foreground = false)
        assertFalse(actions.prepare(region.id))
        assertFalse(actions.openItems())
        inputs = inputs.copy(foreground = true, catalog = catalog.copy(regions = emptyList()))
        assertFalse(actions.prepare(region.id))
        assertEquals(listOf(region.id), selected)
        assertEquals(1, opened)
        assertEquals(sample("fresh"), inputs.snapshot)
    }
}
