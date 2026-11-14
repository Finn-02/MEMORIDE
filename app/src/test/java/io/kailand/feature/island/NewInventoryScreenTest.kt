package io.kailand.feature.island

import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.unit.dp
import io.kailand.core.contract.GameAction
import io.kailand.core.contract.GameSnapshot
import io.kailand.core.contract.ItemType
import io.kailand.core.contract.MutationResult
import io.kailand.core.contract.RegionDefinition
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NewInventoryScreenTest {
    private val regions = listOf("daejeon" to "대전", "sokcho" to "속초", "wonju" to "원주")
    private val catalog = islandTestCatalog.copy(regions = regions.map { (id, name) ->
        RegionDefinition(id, name, "", 0.0, 0.0,
            islandTestCatalog.items.filter { it.regionId == id }.map { it.id })
    })
    private val original = GameSave().toSnapshot()

    private fun cards(type: ItemType, snapshot: GameSnapshot = original) =
        inventoryCards(type, islandPresentation(snapshot, catalog), catalog)

    @Test fun selectionSaverRoundTripsEveryTabAndAnActiveDecoration() {
        val scope = SaverScope { it is String }
        val states = ItemType.entries.map { InventorySelection(it) } +
            InventorySelection(ItemType.DECOR, "wonju_tree")
        for (selection in states) {
            val saved = with(InventorySelectionSaver) { checkNotNull(scope.save(selection)) }
            assertEquals(selection, InventorySelectionSaver.restore(saved))
        }
        assertNull(InventorySelectionSaver.restore(listOf("missing-tab")))
        assertEquals(InventorySelection(ItemType.OUTFIT),
            InventorySelectionSaver.restore(listOf("OUTFIT", "wonju_tree")))
    }

    @Test fun onlyActualItemInvalidationClearsAValidDecorationSelection() {
        val selection = InventorySelection(ItemType.DECOR, "wonju_tree")
        val owned = original.copy(ownedItemIds = setOf("wonju_tree"))
        val available = cards(ItemType.DECOR, owned)
        assertEquals(selection, selection.revalidate(available))
        assertEquals("wonju_tree", selection.selectedDecoration(available)?.item?.id)
        val placed = owned.copy(decorationSlots = mapOf("slot_left" to "wonju_tree"))
        val missingCatalog = catalog.copy(items = catalog.items.filterNot { it.id == "wonju_tree" })
        val missing = inventoryCards(ItemType.DECOR, islandPresentation(owned, missingCatalog), missingCatalog)
        for (invalid in listOf(cards(ItemType.DECOR), cards(ItemType.DECOR, placed), missing)) {
            assertEquals(InventorySelection(ItemType.DECOR), selection.revalidate(invalid))
        }
        val slot = islandPresentation(owned, catalog).slots.first()
        assertNull(selection.placement(slot, available, listOf(slot.copy(available = false))))
        assertEquals(selection, selection.revalidate(available))
    }

    @Test fun tabChangeAndExplicitCancelClearOnlyTheIntendedSelection() {
        val decor = InventorySelection(ItemType.DECOR, "wonju_tree")
        assertEquals(decor, decor.selectTab(ItemType.DECOR))
        assertEquals(InventorySelection(ItemType.OUTFIT), decor.selectTab(ItemType.OUTFIT))
        assertEquals(InventorySelection(ItemType.DECOR), decor.cancelDecoration())
        assertEquals(decor, decor.afterAction(GameAction.PlaceDecoration("slot_left", "daejeon_sign"),
            MutationResult.Success))
        assertEquals(decor, decor.afterAction(GameAction.PlaceDecoration("slot_left", null),
            MutationResult.Success))
    }

    @Test fun gateForegroundAndBusyBlockInputWithoutLosingSelection() = runBlocking {
        val chosen = InventorySelection(ItemType.DECOR, "wonju_tree")
        var selection = chosen
        val snapshot = original.copy(ownedItemIds = setOf("wonju_tree"))
        val view = islandPresentation(snapshot, catalog)
        val cards = cards(ItemType.DECOR, snapshot)
        val slot = view.slots.first()
        val calls = mutableListOf<GameAction>()
        var permitted = true
        var foreground = true
        var closed = false
        var reply = CompletableDeferred<MutationResult>()
        val actions = IslandUiActions(this, { permitted }, { foreground }, { snapshot }, { view }) { action ->
            calls += action
            reply.await().also { selection = selection.afterAction(action, it) }
        }
        fun place() {
            if (!actions.canEditInventory(closed)) return
            val request = selection.placement(slot, cards, view.slots) ?: return
            actions.submit(request) { !closed && selection.placement(slot, cards, view.slots) == request }
        }
        fun changeTab() {
            if (actions.canEditInventory(closed)) selection = selection.selectTab(ItemType.FOOD)
        }

        permitted = false
        place()
        changeTab()
        assertEquals(chosen, selection)
        permitted = true
        foreground = false
        actions.refreshForeground()
        place()
        changeTab()
        assertEquals(chosen, selection)
        foreground = true
        actions.refreshForeground()
        place()
        permitted = false // A new restriction before the queued callback.
        yield()
        assertTrue(calls.isEmpty())
        assertEquals(chosen, selection)

        permitted = true
        val expected = GameAction.PlaceDecoration(slot.id, "wonju_tree")
        place()
        yield()
        assertTrue(actions.busy)
        place()
        changeTab()
        assertEquals(listOf(expected), calls)
        assertEquals(chosen, selection)
        reply.complete(MutationResult.Rejected("not applied"))
        yield()
        assertEquals(chosen, selection)

        reply = CompletableDeferred()
        place()
        yield()
        reply.complete(MutationResult.StorageFailure("not saved"))
        yield()
        assertEquals(chosen, selection)

        reply = CompletableDeferred()
        closed = true
        place()
        changeTab()
        assertEquals(chosen, selection)
        assertEquals(2, calls.size)
        closed = false
        place()
        yield()
        reply.complete(MutationResult.Success)
        yield()
        assertEquals(InventorySelection(ItemType.DECOR), selection)
        place() // Still the pre-commit snapshot, but the successful selection is consumed.
        yield()
        assertEquals(listOf(expected, expected, expected), calls)
    }

    @Test fun allTabsHaveThreeLockedCardsInDesignOrderBeforeAnyRewards() {
        val order = mapOf(
            ItemType.FOOD to listOf("wonju_potato", "daejeon_bread", "sokcho_sashimi"),
            ItemType.OUTFIT to listOf("wonju_crown", "daejeon_hat", "sokcho_scarf"),
            ItemType.DECOR to listOf("wonju_tree", "daejeon_sign", "sokcho_shell"),
        )
        assertEquals(listOf("음식", "의상", "장식"), ItemType.entries.map { it.inventoryLabel() })
        for ((type, ids) in order) {
            val list = cards(type)
            assertEquals(ids, list.map { it.item.id })
            assertEquals(listOf("원주", "대전", "속초"), list.map { it.regionName })
            assertTrue(list.all { !it.item.owned && it.action() == null && !it.canChooseSlot })
            assertTrue(list.all { it.imagePath != null && islandTestRoot.resolve(it.imagePath).isFile })
        }
    }

    @Test fun ownedFoodAndOutfitActionsUseTheCurrentSavedStatus() {
        val state = original.copy(ownedItemIds = setOf("wonju_potato", "wonju_crown", "daejeon_hat"))
        val food = cards(ItemType.FOOD, state)
        assertEquals(listOf(true, false, false), food.map { it.item.owned })
        assertEquals(GameAction.Eat("wonju_potato"), food.first().action())
        val before = cards(ItemType.OUTFIT, state)
        assertEquals(GameAction.EquipOutfit("wonju_crown"), before.first().action())
        assertNull(before.first().inUse)
        val after = cards(ItemType.OUTFIT, state.copy(avatar = state.avatar.copy(outfitItemId = "wonju_crown")))
        assertEquals("착용 중", after.first().inUse)
        assertEquals(GameAction.EquipOutfit(null), after.first().action())
        assertEquals(GameAction.EquipOutfit("daejeon_hat"), after[1].action())
        assertNull(after.last().action())
    }

    @Test fun decorationUsesOnlyCurrentOwnedItemAndFixedSlots() = runBlocking {
        var saved = GameSave(ownedItemIds = setOf("wonju_tree", "daejeon_sign"))
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val store = object : GameSaveStore {
                override suspend fun read() = saved
                override suspend fun update(transform: (GameSave) -> GameSave) = transform(saved).also { saved = it }
            }
            val service = BGameService(store, scope) { IslandTestData.rewards }
            fun view() = islandPresentation(service.state.value, catalog)
            fun tree() = inventoryCards(ItemType.DECOR, view(), catalog).first()
            val slots = view().slots
            assertEquals(listOf("slot_left", "slot_right", "slot_back"), slots.map { it.id })
            assertTrue(tree().canChooseSlot)
            assertNull(tree().placeIn(slots[0].copy(available = false)))
            assertNull(cards(ItemType.DECOR, service.state.value).last().placeIn(slots[0]))

            val left = checkNotNull(tree().placeIn(slots[0]))
            assertEquals(MutationResult.Success, service.apply(left))
            assertEquals("슬롯 1에 놓음", tree().inUse)
            assertNull(tree().placeIn(slots[1]))
            assertTrue(service.apply(GameAction.PlaceDecoration("slot_right", "wonju_tree")) is MutationResult.Rejected)
            assertTrue(service.apply(GameAction.PlaceDecoration("slot_unknown", "wonju_tree")) is MutationResult.Rejected)

            assertEquals(MutationResult.Success, service.apply(checkNotNull(tree().action())))
            assertTrue(decorationPaths(view().slots, catalog).isEmpty())
            assertEquals(MutationResult.Success, service.apply(checkNotNull(tree().placeIn(slots[1]))))
            assertEquals("슬롯 2에 놓음", tree().inUse)
            assertEquals(listOf("decor_ginkgo_slot_right_v3.png"), decorationPaths(view().slots, catalog))
            assertEquals(mapOf("slot_right" to "wonju_tree"), saved.decorationSlots)
        } finally {
            scope.cancel()
        }
    }

    @Test fun queuedRequestsRecheckTheCurrentCardAndTargetBeforeCallingTheHost() = runBlocking {
        val snapshot = original.copy(ownedItemIds = setOf("wonju_tree"))
        val view = islandPresentation(snapshot, catalog)
        var stillValid = true
        val calls = mutableListOf<GameAction>()
        val actions = IslandUiActions(this, { true }, { true }, { snapshot }, { view }) {
            calls += it
            MutationResult.Success
        }
        val target = checkNotNull(cards(ItemType.DECOR, snapshot).first().placeIn(view.slots[0]))
        actions.submit(target) { stillValid }
        assertTrue(actions.busy)
        stillValid = false
        yield()
        assertTrue(calls.isEmpty())
        assertFalse(actions.busy)

        stillValid = true
        actions.submit(target) { stillValid }
        actions.clearFeedback() // close, before queued dispatch
        yield()
        assertTrue(calls.isEmpty())

        actions.submit(target) { stillValid }
        actions.submit(target) { stillValid }
        yield()
        assertEquals(listOf(target), calls)
    }

    @Test fun ambiguousCatalogCardsAndUnsafeThumbnailPathsAreNotOfferedAsValidData() {
        val duplicate = catalog.items.first { it.id == "wonju_potato" }
        val ambiguous = catalog.copy(items = catalog.items + duplicate)
        val reduced = inventoryCards(ItemType.FOOD, islandPresentation(original, ambiguous), ambiguous)
        assertEquals(listOf("daejeon_bread", "sokcho_sashimi"), reduced.map { it.item.id })
        val unsafe = catalog.copy(assets = catalog.assets + (
            duplicate.assetKey to catalog.assets.getValue(duplicate.assetKey).copy(path = "assets/starter/../private.png")
        ))
        assertNull(inventoryCards(ItemType.FOOD, islandPresentation(original, unsafe), unsafe).first().imagePath)
    }

    @Test fun actualWindowAndFontScaleSelectScrollableFallbacks() {
        assertTrue(useWideInventoryLayout(1100.dp, 640.dp, 1f))
        assertFalse(useWideInventoryLayout(640.dp, 720.dp, 1f))
        assertFalse(useWideInventoryLayout(900.dp, 1280.dp, 1f))
        assertFalse(useWideInventoryLayout(411.dp, 740.dp, 1f))
        assertFalse(useWideInventoryLayout(1100.dp, 640.dp, 2f))
        assertFalse(useWideInventoryLayout(411.dp, 740.dp, 2f))
        assertTrue(useInventoryCardRow(480.dp, 1f))
        assertFalse(useInventoryCardRow(411.dp, 1f))
        assertFalse(useInventoryCardRow(650.dp, 2f))
    }
}
