package io.kailand.feature.island

import io.kailand.core.contract.GameAction
import io.kailand.core.contract.MutationResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IslandUiActionsTest {
    @Test fun onlyOneRequestCanBeInFlightAndSuccessUsesThePublishedSnapshot() = runBlocking {
        val fixture = Fixture(this)
        val pending = CompletableDeferred<MutationResult>()
        fixture.reply = { pending.await() }
        val select = GameAction.SelectCharacter("lulu")
        fixture.actions.submit(select)
        fixture.actions.submit(select)
        yield()
        assertEquals(listOf(select), fixture.calls)
        assertTrue(fixture.actions.busy)
        assertEquals("momo", fixture.snapshot.avatar.characterId)
        fixture.snapshot = fixture.snapshot.copy(avatar = fixture.snapshot.avatar.copy(characterId = "lulu"))
        pending.complete(MutationResult.Success)
        yield()
        assertFalse(fixture.actions.busy)
        assertNull(fixture.actions.error)
        assertEquals("lulu", fixture.view().characters.single { it.selected }.id)
    }

    @Test fun failureReportsErrorAndNeverStartsEating() = runBlocking {
        val fixture = Fixture(this)
        val original = fixture.snapshot
        for (result in listOf(MutationResult.Rejected("invalid"), MutationResult.StorageFailure("disk"))) {
            fixture.reply = { result }
            fixture.actions.submit(eat)
            yield()
            assertNotNull(fixture.actions.error)
            assertFalse(fixture.actions.eating)
            assertEquals(original, fixture.snapshot)
        }
    }

    @Test fun latestGateForegroundAndSelectionAreCheckedBeforeDispatch() = runBlocking {
        val fixture = Fixture(this)
        fixture.actions.submit(eat)
        fixture.gateAllowed = false
        yield()
        assertTrue(fixture.calls.isEmpty())
        fixture.gateAllowed = true
        fixture.foreground = false
        fixture.actions.submit(eat)
        fixture.foreground = true
        fixture.actions.submit(GameAction.Eat("wonju_potato"))
        yield()
        assertTrue(fixture.calls.isEmpty())
        fixture.actions.submit(eat)
        fixture.snapshot = fixture.snapshot.copy(ownedItemIds = emptySet())
        yield()
        assertTrue(fixture.calls.isEmpty())
    }

    @Test fun successfulEatStartsWithoutSnapshotChangeAndClearsOnBackgroundOrClose() = runBlocking {
        val fixture = Fixture(this)
        val original = fixture.snapshot
        try {
            fixture.actions.submit(eat)
            yield()
            assertTrue(fixture.actions.eating)
            assertEquals(original, fixture.snapshot)
            fixture.foreground = false
            fixture.actions.refreshForeground()
            fixture.foreground = true
            fixture.actions.refreshForeground()
            assertFalse(fixture.actions.eating)
            fixture.actions.submit(eat)
            yield()
            assertTrue(fixture.actions.eating)
        } finally {
            fixture.actions.clearFeedback()
        }
        assertFalse(fixture.actions.eating)
    }

    @Test fun lateSuccessCannotStartEatingAfterFeedbackWasInvalidated() = runBlocking {
        val fixture = Fixture(this)
        val pending = CompletableDeferred<MutationResult>()
        fixture.reply = { pending.await() }
        fixture.actions.submit(eat)
        yield()
        fixture.gateAllowed = false
        fixture.actions.clearFeedback()
        fixture.gateAllowed = true
        pending.complete(MutationResult.Success)
        yield()
        assertFalse(fixture.actions.eating)
        assertFalse(fixture.actions.busy)
    }

    @Test fun replacingSnapshotHidesAndDiscardsAnOldMeal() = runBlocking {
        val fixture = Fixture(this)
        val original = fixture.snapshot
        try {
            fixture.actions.submit(eat)
            yield()
            assertTrue(fixture.actions.eating)
            fixture.snapshot = original.copy(ownedItemIds = emptySet())
            assertFalse(fixture.actions.eating)
            fixture.actions.discardOldMeal(fixture.snapshot)
            fixture.snapshot = original
            assertFalse(fixture.actions.eating)
        } finally {
            fixture.actions.clearFeedback()
        }
    }

    private class Fixture(scope: CoroutineScope) {
        var snapshot = islandTestSnapshot
        var gateAllowed = true
        var foreground = true
        var reply: suspend () -> MutationResult = { MutationResult.Success }
        val calls = mutableListOf<GameAction>()
        fun view() = islandPresentation(snapshot, islandTestCatalog)
        val actions = IslandUiActions(scope, { gateAllowed }, { foreground }, { snapshot }, ::view) {
            calls += it
            reply()
        }
    }

    private val eat = GameAction.Eat("daejeon_bread")
}
