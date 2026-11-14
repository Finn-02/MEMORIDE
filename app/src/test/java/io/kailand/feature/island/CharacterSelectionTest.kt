package io.kailand.feature.island

import androidx.compose.ui.unit.dp
import io.kailand.core.contract.AvatarPreview
import io.kailand.core.contract.GateState
import io.kailand.core.contract.InteractionGate
import io.kailand.core.contract.MutationResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterSelectionTest {
    private val parked = InteractionGate(GateState.PARKED_ALLOWED)
    private val snapshot = GameSave().toSnapshot()

    @Test fun catalogCardsUseSavedIdAndApprovedIdleArt() {
        val choices = characterSelectionChoices(snapshot, islandTestCatalog)
        assertEquals(listOf("tori" to "토리", "momo" to "모모", "lulu" to "루루"), choices.map { it.id to it.name })
        assertEquals(listOf("tori"), choices.filter { it.selected }.map { it.id })
        for (choice in choices) {
            val avatar = AvatarPreview(choice.id, null)
            val paths = AvatarAssetResolver.resolve(avatar, islandTestCatalog, "idle")
            assertEquals(
                listOf("avatars/approved/${choice.id}/idle__none.png"),
                AvatarAssetResolver.approvedFrames(avatar, "idle", paths),
            )
        }
    }

    @Test fun everyCardForwardsItsCatalogIdAndSavedBadgeFollowsSnapshot() = runBlocking {
        val submission = CharacterSelectionSubmission()
        val calls = mutableListOf<String>()
        for (choice in characterSelectionChoices(snapshot, islandTestCatalog)) {
            submission.select(choice.id, parked) { calls += it; MutationResult.Success }
        }
        assertEquals(listOf("tori", "momo", "lulu"), calls)
        assertEquals("tori", characterSelectionChoices(snapshot, islandTestCatalog).single { it.selected }.id)
        val updated = snapshot.copy(avatar = AvatarPreview("lulu", null))
        assertEquals("lulu", characterSelectionChoices(updated, islandTestCatalog).single { it.selected }.id)
        assertNull(submission.pendingId)
        assertNull(submission.failure)
    }

    @Test fun pendingRequestBlocksBothSameAndOtherCardUntilResult() = runBlocking {
        val submission = CharacterSelectionSubmission()
        val saved = CompletableDeferred<MutationResult>()
        val request = launch(start = CoroutineStart.UNDISPATCHED) {
            submission.select("momo", parked) { saved.await() }
        }
        assertEquals("momo", submission.pendingId)
        var duplicateCalls = 0
        submission.select("momo", parked) { duplicateCalls++; MutationResult.Success }
        submission.select("lulu", parked) { duplicateCalls++; MutationResult.Success }
        assertEquals(0, duplicateCalls)
        assertEquals("tori", characterSelectionChoices(snapshot, islandTestCatalog).single { it.selected }.id)
        saved.complete(MutationResult.Success)
        request.join()
        assertNull(submission.pendingId)
        assertNull(submission.failure)
    }

    @Test fun restrictedAndUnknownGateNeverCallHost() = runBlocking {
        val submission = CharacterSelectionSubmission()
        for (gate in GateState.entries.filter { it != GateState.PARKED_ALLOWED }) {
            submission.select("momo", InteractionGate(gate)) { error("gate must block this request") }
        }
        assertNull(submission.pendingId)
        assertNull(submission.failure)
    }

    @Test fun rejectionAndStorageFailureStayRetryableAndDistinctFromSuccess() = runBlocking {
        val submission = CharacterSelectionSubmission()
        submission.select("lulu", parked) { MutationResult.Rejected("gate changed") }
        val rejected = checkNotNull(submission.failure)
        assertEquals("lulu", rejected.characterId)
        assertNull(submission.pendingId)
        submission.select("lulu", parked) { MutationResult.StorageFailure("write failed") }
        val failed = checkNotNull(submission.failure)
        assertNotEquals(rejected.message, failed.message)
        assertNull(submission.pendingId)
        assertTrue(characterSelectionChoices(snapshot, islandTestCatalog).single { it.selected }.id == "tori")
        submission.select("momo", parked) { MutationResult.Success }
        assertNull(submission.failure)
    }

    @Test fun cancellationClearsPendingWithoutReportingStorageFailure() = runBlocking {
        val submission = CharacterSelectionSubmission()
        val blocked = CompletableDeferred<MutationResult>()
        val request = launch(start = CoroutineStart.UNDISPATCHED) {
            submission.select("lulu", parked) { blocked.await() }
        }
        request.cancelAndJoin()
        assertNull(submission.pendingId)
        assertNull(submission.failure)
    }

    @Test fun wideSplitPortraitAndLargeTextChooseSafeLayouts() {
        assertTrue(useWideCharacterCards(1000.dp, 600.dp, 1f))
        assertTrue(!useWideCharacterCards(600.dp, 480.dp, 1f))
        assertTrue(!useWideCharacterCards(900.dp, 1100.dp, 1f))
        assertTrue(!useWideCharacterCards(1000.dp, 600.dp, 2f))
    }
}
