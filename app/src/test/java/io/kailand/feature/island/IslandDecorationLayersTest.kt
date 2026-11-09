package io.kailand.feature.island

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IslandDecorationLayersTest {
    private val catalog = islandTestCatalog

    private fun slots(placements: Map<String, String>): List<IslandSlot> =
        islandPresentation(
            islandTestSnapshot.copy(
                ownedItemIds = islandTestSnapshot.ownedItemIds + placements.values,
                decorationSlots = placements,
            ),
            catalog,
        ).slots

    @Test fun placingDecorationInvalidatesThePreviousFrameImmediately() {
        val empty = decorationPaths(slots(emptyMap()), catalog)
        val placed = decorationPaths(slots(mapOf("slot_left" to "daejeon_sign")), catalog)
        assertTrue(empty.isEmpty())
        assertEquals(listOf("decor_bakery_sign_slot_left_v3.png"), placed)
        assertTrue(DecorationFrameBatch(empty, emptyList<String>()).forRequest(placed).isEmpty())
    }

    @Test fun movingDecorationNeverDisplaysTheOldSlotImage() {
        val left = decorationPaths(slots(mapOf("slot_left" to "daejeon_sign")), catalog)
        val right = decorationPaths(slots(mapOf("slot_right" to "daejeon_sign")), catalog)
        val previous = DecorationFrameBatch(left, listOf("old-left-bitmap"))
        assertEquals(listOf("decor_bakery_sign_slot_right_v3.png"), right)
        assertTrue(previous.forRequest(right).isEmpty())
        assertEquals(listOf("new-right-bitmap"),
            DecorationFrameBatch(right, listOf("new-right-bitmap")).forRequest(right))
    }

    @Test fun removingDecorationClearsTheOldOverlayOnTheFirstComposition() {
        val left = decorationPaths(slots(mapOf("slot_left" to "daejeon_sign")), catalog)
        val removed = decorationPaths(slots(emptyMap()), catalog)
        val previous = DecorationFrameBatch(left, listOf("old-left-bitmap"))
        assertTrue(removed.isEmpty())
        assertTrue(previous.forRequest(removed).isEmpty())
    }

    @Test fun replacingOneSlotPreservesOnlyTheNewRequestedBatch() {
        val before = decorationPaths(slots(mapOf("slot_left" to "daejeon_sign")), catalog)
        val after = decorationPaths(slots(mapOf("slot_left" to "wonju_tree")), catalog)
        val previous = DecorationFrameBatch(before, listOf("old-bakery"))
        assertEquals(listOf("decor_ginkgo_slot_left_v3.png"), after)
        assertTrue(previous.forRequest(after).isEmpty())
    }
}
