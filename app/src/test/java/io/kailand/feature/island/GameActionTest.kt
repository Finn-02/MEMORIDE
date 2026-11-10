package io.kailand.feature.island

import io.kailand.core.contract.ArrivalResult
import io.kailand.core.contract.GameAction
import io.kailand.core.contract.MutationResult
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class GameActionTest {
    private val owned = setOf("daejeon_hat", "wonju_crown", "daejeon_bread", "daejeon_sign", "wonju_tree", "sokcho_shell")
    private val items = GameSave(ownedItemIds = owned)

    @Test fun registeredCharactersPreserveOutfitDecorationsAndProgress() = withService(items.copy(
        equippedOutfitId = "daejeon_hat",
        decorationSlots = mapOf("slot_left" to "daejeon_sign"),
    )) { service, store ->
        assertTrue(service.completeArrival(IslandTestData.arrival("D01")) is ArrivalResult.Applied)
        assertTrue(service.completeArrival(IslandTestData.arrival("D03")) is ArrivalResult.Applied)
        val original = store.saved
        val writes = store.writes
        for (id in listOf("momo", "lulu", "tori", "tori")) {
            assertEquals(MutationResult.Success, service.apply(GameAction.SelectCharacter(id)))
            assertEquals(original.copy(characterId = id), store.saved)
            assertEquals(id, service.state.value.avatar.characterId)
        }
        assertEquals(writes + 3, store.writes)
        assertRejectedUnchanged(service, store, GameAction.SelectCharacter("unknown"), GameAction.SelectCharacter(""))
    }

    @Test fun outfitCanBeEquippedReplacedAndCleared() = withService(items) { service, store ->
        for (id in listOf("daejeon_hat", "daejeon_hat", "wonju_crown", null, null)) {
            assertEquals(MutationResult.Success, service.apply(GameAction.EquipOutfit(id)))
            assertEquals(items.copy(equippedOutfitId = id), store.saved)
            assertEquals(id, service.state.value.avatar.outfitItemId)
        }
        assertEquals(3, store.writes)
        assertRejectedUnchanged(service, store, GameAction.EquipOutfit("sokcho_scarf"),
            GameAction.EquipOutfit("daejeon_bread"), GameAction.EquipOutfit("unknown"))
    }

    @Test fun ownedFoodCanBeUsedRepeatedlyWithoutConsumingOrWriting() = withService(items) { service, store ->
        val before = service.state.value
        repeat(2) {
            assertEquals(MutationResult.Success, service.apply(GameAction.Eat("daejeon_bread")))
            assertEquals(items, store.saved)
            assertSame(before, service.state.value)
        }
        assertEquals(0, store.writes)
        assertEquals(2, store.updateAttempts)
        assertRejectedUnchanged(service, store, GameAction.Eat("wonju_potato"),
            GameAction.Eat("daejeon_hat"), GameAction.Eat("unknown"))
    }

    @Test fun decorationSlotsSupportPlacementReplacementAndRemoval() = withService(items) { service, store ->
        val slots = mapOf("slot_left" to "daejeon_sign", "slot_right" to "wonju_tree", "slot_back" to "sokcho_shell")
        for ((slot, item) in slots) {
            assertEquals(MutationResult.Success, service.apply(GameAction.PlaceDecoration(slot, item)))
        }
        assertEquals(slots, service.state.value.decorationSlots)
        assertEquals(MutationResult.Success, service.apply(GameAction.PlaceDecoration("slot_left", "daejeon_sign")))
        assertEquals(3, store.writes)
        assertRejectedUnchanged(service, store,
            GameAction.PlaceDecoration("slot_right", "daejeon_sign"),
            GameAction.PlaceDecoration("slot_unknown", null),
            GameAction.PlaceDecoration("slot_left", "daejeon_hat"),
            GameAction.PlaceDecoration("slot_left", "unknown"))
        assertEquals(MutationResult.Success, service.apply(GameAction.PlaceDecoration("slot_right", null)))
        assertEquals(MutationResult.Success, service.apply(GameAction.PlaceDecoration("slot_right", null)))
        assertEquals(MutationResult.Success, service.apply(GameAction.PlaceDecoration("slot_left", "wonju_tree")))
        assertEquals(items.copy(decorationSlots = mapOf("slot_left" to "wonju_tree", "slot_back" to "sokcho_shell")), store.saved)
        assertEquals(store.saved.decorationSlots, service.state.value.decorationSlots)
        assertEquals(5, store.writes)
    }

    @Test fun separateDecorationMoveLeavesTheSourceEmptyWhenPlacementFails() = withService(
        items.copy(decorationSlots = mapOf("slot_left" to "daejeon_sign")),
    ) { service, store ->
        assertEquals(MutationResult.Success, service.apply(GameAction.PlaceDecoration("slot_left", null)))
        val afterRemoval = store.saved
        store.beforeCommit = { throw IOException("Destination placement failed before commit") }
        assertTrue(service.apply(GameAction.PlaceDecoration("slot_right", "daejeon_sign")) is MutationResult.StorageFailure)
        assertEquals(afterRemoval, store.saved)
        assertTrue(store.saved.decorationSlots.isEmpty())
        assertEquals(afterRemoval.toSnapshot(), service.state.value)
        assertTrue("daejeon_sign" in service.state.value.ownedItemIds)
        assertEquals(1, store.writes)
    }

    @Test fun unownedAndLegacyReferencesAreValidatedWithoutRepairingSave() = withService(
        GameSave(equippedOutfitId = "legacy_outfit", ownedItemIds = setOf("legacy_outfit", "legacy_food"),
            decorationSlots = mapOf("slot_back" to "legacy_decor", "legacy_slot" to "daejeon_sign")),
    ) { service, store ->
        val original = store.saved
        assertRejectedUnchanged(service, store, GameAction.EquipOutfit("legacy_outfit"),
            GameAction.Eat("legacy_food"), GameAction.PlaceDecoration("slot_left", "daejeon_sign"),
            GameAction.PlaceDecoration("slot_back", "legacy_decor"),
            GameAction.PlaceDecoration("legacy_slot", null))
        assertEquals(MutationResult.Success, service.apply(GameAction.SelectCharacter("momo")))
        assertEquals(original.copy(characterId = "momo"), store.saved)
    }

    @Test fun allActionsAreBlockedWhileLoadingAndOnReadError() = runBlocking {
        val pending = CompletableDeferred<GameSave>()
        val store = RecordingStore(items)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val loading = BGameService(object : GameSaveStore by store {
                override suspend fun read(): GameSave = pending.await()
            }, scope) { IslandTestData.rewards }
            val broken = BGameService(object : GameSaveStore by store {
                override suspend fun read(): GameSave = throw IOException("Unreadable save")
            }, scope) { IslandTestData.rewards }
            assertEquals(GameReadiness.LOADING, loading.readiness.value)
            assertEquals(GameReadiness.READ_ERROR, broken.readiness.value)
            for (service in listOf(loading, broken)) {
                for (action in listOf(GameAction.SelectCharacter("tori"), GameAction.EquipOutfit(null),
                    GameAction.Eat("daejeon_bread"), GameAction.PlaceDecoration("slot_left", null))) {
                    assertTrue(service.apply(action) is MutationResult.StorageFailure)
                }
            }
            assertEquals(items, store.saved)
            assertEquals(0, store.updateAttempts)
        } finally { scope.cancel() }
    }

    @Test fun failedCommitLeavesMemoryAndStoreUntouched() = withService(items) { service, store ->
        val before = service.state.value
        store.beforeCommit = { throw IOException("Commit failed after validation") }
        assertTrue(service.apply(GameAction.EquipOutfit("daejeon_hat")) is MutationResult.StorageFailure)
        assertSame(before, service.state.value)
        assertEquals(items, store.saved)
        assertEquals(0, store.writes)
        store.beforeCommit = {}
        assertEquals(MutationResult.Success, service.apply(GameAction.EquipOutfit("daejeon_hat")))
        assertEquals(1, store.writes)
    }

    @Test fun validNoOpStillReportsStorageReadFailure() = withService(items) { service, store ->
        store.updateReadFailure = IOException("Cannot read latest save")
        assertTrue(service.apply(GameAction.Eat("daejeon_bread")) is MutationResult.StorageFailure)
        assertTrue(service.apply(GameAction.SelectCharacter("tori")) is MutationResult.StorageFailure)
        assertEquals(items.toSnapshot(), service.state.value)
        assertEquals(items, store.saved)
        assertEquals(0, store.writes)
    }

    @Test fun cancellationPropagatesAndReleasesMutex() = withService(items) { service, store ->
        val cancellation = CancellationException("Cancelled before commit")
        store.beforeCommit = { throw cancellation }
        try {
            service.apply(GameAction.SelectCharacter("momo"))
            fail("Cancellation should propagate")
        } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
        }
        assertEquals(items, store.saved)
        assertEquals(items.toSnapshot(), service.state.value)
        store.beforeCommit = {}
        assertEquals(MutationResult.Success, service.apply(GameAction.SelectCharacter("momo")))
    }

    @Test fun equipWaitsForArrivalAndValidatesNewlyOwnedOutfit() = withService { service, store ->
        withTimeout(5_000) {
            val release = CompletableDeferred<Unit>()
            store.beforeCommit = { release.await() }
            val arrival = async(start = CoroutineStart.UNDISPATCHED) {
                service.completeArrival(IslandTestData.arrival("D03"))
            }
            val equip = async(start = CoroutineStart.UNDISPATCHED) {
                service.apply(GameAction.EquipOutfit("daejeon_hat"))
            }
            assertFalse(equip.isCompleted)
            assertEquals(1, store.updateAttempts)
            assertTrue(service.state.value.ownedItemIds.isEmpty())
            release.complete(Unit)
            val receipt = (arrival.await() as ArrivalResult.Applied).receipt
            assertEquals(MutationResult.Success, equip.await())
            assertEquals("daejeon_hat", store.saved.equippedOutfitId)
            assertEquals(receipt, service.state.value.pendingReceipt)
            assertEquals(receipt, store.saved.processedReceipts.getValue(receipt.journeyId).toContract())
            assertEquals(2, store.writes)
        }
    }

    @Test fun actionAndAcknowledgementDoNotOverwriteEachOther() = withService { service, store ->
        val receipt = (service.completeArrival(IslandTestData.arrival("D03")) as ArrivalResult.Applied).receipt
        withTimeout(5_000) {
            val release = CompletableDeferred<Unit>()
            store.beforeCommit = { release.await() }
            val ack = async(start = CoroutineStart.UNDISPATCHED) { service.acknowledgeReceipt(receipt.journeyId) }
            val action = async(start = CoroutineStart.UNDISPATCHED) {
                service.apply(GameAction.PlaceDecoration("slot_back", "daejeon_sign"))
            }
            assertFalse(action.isCompleted)
            assertEquals(2, store.updateAttempts)
            assertEquals(receipt, service.state.value.pendingReceipt)
            release.complete(Unit)
            assertEquals(MutationResult.Success, ack.await())
            assertEquals(MutationResult.Success, action.await())
            assertNull(service.state.value.pendingReceipt)
            assertEquals(mapOf("slot_back" to "daejeon_sign"), service.state.value.decorationSlots)
            assertEquals(receipt, store.saved.processedReceipts.getValue(receipt.journeyId).toContract())
            assertEquals(service.state.value, store.saved.toSnapshot())
        }
    }

    private suspend fun assertRejectedUnchanged(service: BGameService, store: RecordingStore, vararg actions: GameAction) {
        val original = store.saved
        val snapshot = service.state.value
        val writes = store.writes
        for (action in actions) {
            assertTrue("Must reject $action", service.apply(action) is MutationResult.Rejected)
            assertEquals(original, store.saved)
            assertSame(snapshot, service.state.value)
        }
        assertEquals(writes, store.writes)
    }

    private fun withService(
        initial: GameSave = GameSave(),
        block: suspend (BGameService, RecordingStore) -> Unit,
    ): Unit = runBlocking {
        val store = RecordingStore(initial)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            block(BGameService(store, scope) { IslandTestData.rewards }, store)
        } finally { scope.cancel() }
    }

    private class RecordingStore(var saved: GameSave) : GameSaveStore {
        var writes = 0
        var updateAttempts = 0
        var updateReadFailure: IOException? = null
        var beforeCommit: suspend () -> Unit = {}

        override suspend fun read(): GameSave = saved
        override suspend fun update(transform: (GameSave) -> GameSave): GameSave {
            updateAttempts += 1
            updateReadFailure?.let { throw it }
            val next = transform(saved)
            next.toSnapshot()
            beforeCommit()
            if (next != saved) {
                saved = next
                writes += 1
            }
            return next
        }
    }
}
