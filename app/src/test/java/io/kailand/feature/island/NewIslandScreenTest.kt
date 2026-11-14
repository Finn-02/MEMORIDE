package io.kailand.feature.island

import androidx.compose.ui.unit.dp
import io.kailand.core.contract.AvatarPreview
import io.kailand.core.contract.GateState
import io.kailand.core.contract.InteractionGate
import io.kailand.core.ui.IslandDestinationSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NewIslandScreenTest {
    private val catalog = islandTestCatalog
    private val parked = InteractionGate(GateState.PARKED_ALLOWED)

    @Test fun storedGrowthSelectsOnlyTheApprovedLayersForThatStage() {
        val files = listOf(
            "island_base_v3.png",
            "growth_path_v3.png",
            "growth_flowers_v3.png",
            "growth_rest_area_v3.png",
        )
        val expectedLabels = listOf("작은 빈 정원", "길", "꽃밭", "휴식 시설")
        for (stage in files.indices) {
            val view = islandPresentation(islandTestSnapshot.copy(dailyGrowth = stage), catalog)
            val growth = checkNotNull(view.growth)
            assertEquals(stage, growth.ordinal)
            assertEquals(expectedLabels[stage], growth.label)
            assertEquals(files.take(stage + 1), islandLayerFiles(growth))
        }
        for (invalid in listOf(-1, 4)) {
            assertNull(islandPresentation(islandTestSnapshot.copy(dailyGrowth = invalid), catalog).growth)
        }
    }

    @Test fun updatedSnapshotSuppliesAllThreeCharactersAndTheirSavedOutfit() {
        val appearances = listOf(
            Triple("tori", "토리", "daejeon_hat" to "bread_hat"),
            Triple("momo", "모모", "wonju_crown" to "ginkgo_crown"),
            Triple("lulu", "루루", "sokcho_scarf" to "wave_scarf"),
        )
        for ((id, name, outfit) in appearances) {
            val avatar = AvatarPreview(id, outfit.first)
            val next = islandTestSnapshot.copy(
                avatar = avatar,
                ownedItemIds = islandTestSnapshot.ownedItemIds + outfit.first,
            )
            val view = islandPresentation(next, catalog)
            assertEquals(name, view.characterName)
            assertFalse(hasIslandCatalogIssue(next, catalog, view))
            val layers = AvatarAssetResolver.resolve(next.avatar, catalog, "idle")
            assertEquals("avatars/approved/$id/idle__${outfit.second}.png",
                AvatarAssetResolver.approvedFrames(next.avatar, "idle", layers).first())
        }
    }

    @Test fun currentSnapshotSelectsZeroOneOrAllThreeDecorations() {
        val placements = linkedMapOf(
            "slot_left" to "daejeon_sign",
            "slot_right" to "wonju_tree",
            "slot_back" to "sokcho_shell",
        )
        val files = listOf(
            "decor_bakery_sign_slot_left_v3.png",
            "decor_ginkgo_slot_right_v3.png",
            "decor_shell_slot_back_v3.png",
        )
        for (count in listOf(0, 1, 3)) {
            val snapshot = islandTestSnapshot.copy(
                ownedItemIds = islandTestSnapshot.ownedItemIds + placements.values,
                decorationSlots = placements.entries.take(count).associate { it.key to it.value },
            )
            val view = islandPresentation(snapshot, catalog)
            assertEquals(placements.keys.toList(), view.slots.map { it.id })
            assertEquals(files.take(count), decorationPaths(view.slots, catalog))
            assertFalse(hasIslandCatalogIssue(snapshot, catalog, view))
        }
    }

    @Test fun incompleteCatalogAndUnknownSavedReferencesProduceAnIssue() {
        val snapshot = islandTestSnapshot
        for (incomplete in listOf(
            catalog.copy(characters = emptyList()),
            catalog.copy(items = emptyList()),
            catalog.copy(assets = catalog.assets - "char_momo"),
            catalog.copy(assets = catalog.assets - "outfit_bread_hat"),
            catalog.copy(assets = catalog.assets - "decor_bakery_sign"),
        )) {
            assertTrue(hasIslandCatalogIssue(snapshot, incomplete, islandPresentation(snapshot, incomplete)))
        }
        val invalid = snapshot.copy(avatar = AvatarPreview("unknown", null), decorationSlots = emptyMap())
        assertTrue(hasIslandCatalogIssue(invalid, catalog, islandPresentation(invalid, catalog)))
    }

    @Test fun onlyReplacementHidesAllFourDefaultActionsAndBlocksDispatch() {
        val replacing = IslandDestinationSlot(replacesActions = true) {}
        val notice = IslandDestinationSlot(replacesActions = false) {}
        val invoked = mutableListOf<Int>()
        val actions = List(4) { i -> { invoked.add(i); Unit } }
        for ((destination, visible) in listOf(null to true, replacing to false, notice to true, null to true)) {
            assertEquals(visible, showsIslandActions(destination))
            invoked.clear()
            actions.forEach { dispatchIslandAction(parked, destination, it) }
            assertEquals(if (visible) listOf(0, 1, 2, 3) else emptyList<Int>(), invoked)
        }
    }

    @Test fun dispatchConsultsTheCurrentGateAndDestinationAtClickTime() {
        var gate = parked
        var destination: IslandDestinationSlot? = null
        var calls = 0
        val click = { dispatchIslandAction(gate, destination) { calls++ } }
        for (state in GateState.entries.filterNot { it == GateState.PARKED_ALLOWED }) {
            gate = InteractionGate(state)
            click()
        }
        gate = parked
        destination = IslandDestinationSlot(replacesActions = true) {}
        click()
        assertEquals(0, calls)
        destination = IslandDestinationSlot(replacesActions = false) {}
        click()
        assertEquals(1, calls)
    }

    @Test fun compactLayoutHandlesNarrowPortraitAndLargeText() {
        assertTrue(useWideIslandLayout(1100.dp, 640.dp, 1f))
        assertFalse(useWideIslandLayout(640.dp, 720.dp, 1f))
        assertFalse(useWideIslandLayout(900.dp, 1280.dp, 1f))
        assertFalse(useWideIslandLayout(411.dp, 740.dp, 1f))
        assertFalse(useWideIslandLayout(1100.dp, 640.dp, 2f))
        assertFalse(useWideIslandLayout(411.dp, 740.dp, 2f))
    }
}
