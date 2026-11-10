package io.kailand.app.journey

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.File
import java.io.IOException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class NavigationInboxTest {
    @get:Rule val temp = TemporaryFolder()
    private val id = "11111111-1111-4111-8111-111111111111"
    private class MemoryStore : NavigationDraftStore {
        var value: NavigationDraft? = null
        var failRead = false
        var failWrite = false
        var writes = 0
        override suspend fun read(): NavigationDraft? { if (failRead) throw IOException(); return value }
        override suspend fun write(draft: NavigationDraft?) {
            if (failWrite) throw IOException()
            value = draft; writes++
        }
    }

    @Test fun changedDestinationInvalidatesOldLeaseAndPreservesJourneyIdentity() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val inbox = NavigationInbox(MemoryStore(), scope, { 1000 }, { id })
            inbox.observe("대전", 36.35, 127.38)
            val first = inbox.state.value.draft!!
            assertTrue(inbox.current(first))
            inbox.beginObservation()
            assertFalse(inbox.current(first))
            inbox.endObservation()
            assertTrue(inbox.current(first))
            inbox.changing()
            assertFalse(inbox.current(first))
            inbox.observe("원주", 37.34, 127.92)
            val second = inbox.state.value.draft!!
            assertEquals(first.id, second.id)
            assertTrue(second.revision > first.revision)
            assertTrue(inbox.current(second)); assertFalse(inbox.current(first))
        } finally { scope.cancel() }
    }

    @Test fun zeroDestinationPreservesLastHintButCancellationAndLateInfoCannotCompleteIt() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val inbox = NavigationInbox(MemoryStore(), scope, { 1000 }, { id })
            inbox.observe("대전", 36.35, 127.38)
            inbox.observe("", 0.0, 0.0)
            val retained = inbox.state.value.draft!!
            assertEquals("대전", retained.label)
            assertEquals(NavigationDraftStatus.NEEDS_REVIEW, retained.status)
            inbox.cancelled()
            inbox.observe("대전", 36.35, 127.38)
            inbox.observe("", 0.0, 0.0)
            assertEquals(NavigationDraftStatus.CANCELLED, inbox.state.value.draft!!.status)
            assertFalse(inbox.current(inbox.state.value.draft!!))
        } finally { scope.cancel() }
    }

    @Test fun invalidCoordinatesNeverCreateADraftAndFutureOrExpiredHintsAreBlocked() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            var now = 1000L
            val inbox = NavigationInbox(MemoryStore(), scope, { now }, { id })
            inbox.observe("빈 목적지", 0.0, 0.0)
            inbox.observe("범위 밖", 91.0, 127.0)
            inbox.observe("NaN", Double.NaN, 127.0)
            assertNull(inbox.state.value.draft)
            inbox.observe("대전", 36.35, 127.38)
            val saved = inbox.state.value.draft!!
            now = 999; assertFalse(inbox.current(saved))
            now = 1000 + 24 * 60 * 60 * 1000L + 1; assertFalse(inbox.current(saved))
        } finally { scope.cancel() }
    }

    @Test fun failedWriteKeepsOriginalAndBlocksItsLeaseUntilSuccessfulRetry() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val store = MemoryStore()
            val inbox = NavigationInbox(store, scope, { 1000 }, { id })
            inbox.observe("대전", 36.35, 127.38)
            val original = store.value!!
            store.failWrite = true
            assertFalse(inbox.observe("원주", 37.34, 127.92))
            assertEquals(original, store.value); assertEquals(original, inbox.state.value.draft)
            assertFalse(inbox.current(original)); assertNotNull(inbox.state.value.error)
            store.failWrite = false
            assertTrue(inbox.observe("원주", 37.34, 127.92))
            assertTrue(inbox.current(inbox.state.value.draft!!))
        } finally { scope.cancel() }
    }

    @Test fun readFailureNeverOverwritesAndFailedClearPreventsReset() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val broken = MemoryStore().apply { failRead = true }
            val inbox = NavigationInbox(broken, scope, { 1000 }, { id })
            assertFalse(inbox.observe("大田", 36.35, 127.38))
            assertEquals(0, broken.writes); assertFalse(inbox.state.value.ready)
            try { inbox.clear(); fail("clear must propagate failure") } catch (_: IllegalStateException) { }
        } finally { scope.cancel() }
    }

    @Test fun consumedDraftCannotBeRevivedByRefreshOrConsumeANewerRevision() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val inbox = NavigationInbox(MemoryStore(), scope, { 1000 }, { id })
            inbox.observe("대전", 36.35, 127.38)
            val old = inbox.state.value.draft!!
            inbox.changing(); inbox.observe("원주", 37.34, 127.92)
            val current = inbox.state.value.draft!!
            inbox.consume(old)
            assertTrue(inbox.current(current))
            inbox.consume(current); inbox.observe("원주", 37.34, 127.92)
            assertEquals(NavigationDraftStatus.CONSUMED, inbox.state.value.draft!!.status)
            inbox.clear(); assertNull(inbox.state.value.draft)
        } finally { scope.cancel() }
    }

    @Test fun dataStoreRecreationRestoresSameIdRevisionAndCancelledStatus() = runBlocking {
        val file = File(temp.newFolder(), "navigation.preferences_pb")
        var saved: NavigationDraft? = null
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val store = PreferencesNavigationDraftStore(PreferenceDataStoreFactory.create(scope = scope) { file })
            val inbox = NavigationInbox(store, scope, { 1000 }, { id })
            inbox.state.first { it.ready }
            inbox.observe("대전", 36.35, 127.38); inbox.cancelled()
            saved = inbox.state.value.draft
        } finally { scope.coroutineContext[Job]!!.cancelAndJoin() }
        val restoredScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val store = PreferencesNavigationDraftStore(PreferenceDataStoreFactory.create(scope = restoredScope) { file })
            val inbox = NavigationInbox(store, restoredScope, { 1000 }, { id })
            assertEquals(saved, inbox.state.first { it.ready }.draft)
            assertFalse(inbox.current(saved!!))
        } finally { restoredScope.coroutineContext[Job]!!.cancelAndJoin() }
    }

    @Test fun unknownSchemaIsBlockedAndFileIsNotOverwritten() = runBlocking {
        val file = File(temp.newFolder(), "unknown.preferences_pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val backend = PreferenceDataStoreFactory.create(scope = scope) { file }
            val key = stringPreferencesKey("latest_navigation_draft")
            val raw = "{\"schemaVersion\":99,\"draft\":null}"
            backend.edit { it[key] = raw }
            val inbox = NavigationInbox(PreferencesNavigationDraftStore(backend), scope)
            inbox.state.first { it.error != null }
            assertFalse(inbox.observe("대전", 36.35, 127.38))
            assertEquals(raw, backend.data.first()[key])
        } finally { scope.coroutineContext[Job]!!.cancelAndJoin() }
    }
}
