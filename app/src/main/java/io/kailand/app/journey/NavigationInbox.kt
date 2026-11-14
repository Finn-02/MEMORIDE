package io.kailand.app.journey

import io.kailand.core.contract.Catalog
import io.kailand.core.contract.DestinationSpec
import java.util.UUID
import kotlin.math.abs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable

@Serializable
enum class NavigationDraftStatus { ACTIVE, CHANGING, NEEDS_REVIEW, CANCELLED, CONSUMED }

/** One navigation hint, never progress, a receipt, or proof of arrival. */
@Serializable
data class NavigationDraft(
    val id: String,
    val revision: Int,
    val label: String,
    val latitude: Double,
    val longitude: Double,
    val observedAtEpochMs: Long,
    val status: NavigationDraftStatus,
) {
    fun usable(now: Long): Boolean = status in setOf(NavigationDraftStatus.ACTIVE, NavigationDraftStatus.NEEDS_REVIEW) &&
        now >= observedAtEpochMs && now - observedAtEpochMs <= 24 * 60 * 60 * 1000L

    fun destination(catalog: Catalog): DestinationSpec {
        // A city named in the navigation destination is a locality hint, not proof of a visit.
        val namedRegions = catalog.regions.filter { region ->
            label.trim() == region.name || when (region.id) {
                "wonju" -> label.contains("원주시")
                "sokcho" -> label.contains("속초시")
                "daejeon" -> label.contains("대전광역시") || label.contains("대전시")
                else -> false
            }
        }
        // Multiple named cities are ambiguous. Do not choose one or infer a radius from a reference point.
        val region = if (namedRegions.isNotEmpty()) namedRegions.singleOrNull() else catalog.regions.singleOrNull {
            abs(it.latitude - latitude) < 0.00001 && abs(it.longitude - longitude) < 0.00001
        }
        return DestinationSpec(region?.id, label, latitude, longitude)
    }
}

data class NavigationInboxState(
    val ready: Boolean = false,
    val draft: NavigationDraft? = null,
    val saving: Boolean = false,
    val error: String? = null,
)

interface NavigationDraftStore {
    suspend fun read(): NavigationDraft?
    suspend fun write(draft: NavigationDraft?)
}

/** Serialized latest-only metadata storage. B remains the only writer of game progress. */
class NavigationInbox(
    private val store: NavigationDraftStore,
    scope: CoroutineScope,
    private val now: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) {
    private val mutex = Mutex()
    private var pendingObservations = 0
    private val mutable = MutableStateFlow(NavigationInboxState())
    val state = mutable.asStateFlow()
    private val loading = scope.launch {
        mutex.withLock {
            try {
                val draft = store.read()
                require(draft == null || valid(draft))
                mutable.value = NavigationInboxState(ready = true, draft = draft)
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (_: Exception) {
                mutable.value = NavigationInboxState(error = "내비 목적지 기록을 읽지 못했어요. 기존 기록을 유지합니다.")
            }
        }
    }

    fun current(draft: NavigationDraft): Boolean = state.value.let {
        pendingObservations == 0 && it.ready && !it.saving && it.error == null && it.draft == draft && draft.usable(now())
    }

    /** Main-thread fence: a queued platform event revokes UI admission before disk I/O starts. */
    fun beginObservation() { pendingObservations++; mutable.value = mutable.value.copy(saving = true) }
    fun endObservation() {
        check(pendingObservations > 0)
        pendingObservations--
        mutable.value = mutable.value.copy(saving = pendingObservations > 0)
    }

    /** Call before querying a changed route so an old confirmation cannot complete it. */
    suspend fun changing() = update { old -> old?.copy(
        id = if (old.status in setOf(NavigationDraftStatus.CANCELLED, NavigationDraftStatus.CONSUMED)) newId() else old.id,
        revision = next(old.revision), status = NavigationDraftStatus.CHANGING,
    ) }

    suspend fun observe(label: String, latitude: Double, longitude: Double) = update { old ->
        if (!coordinates(latitude, longitude) || label.isBlank()) {
            // The emulator emits an empty 0,0 destination after guidance ends. Keep the last hint.
            if (old?.status == NavigationDraftStatus.ACTIVE) old.copy(status = NavigationDraftStatus.NEEDS_REVIEW,
                revision = next(old.revision)) else old
        } else if (old?.status in setOf(NavigationDraftStatus.CANCELLED, NavigationDraftStatus.CONSUMED)) {
            old // A delayed info callback cannot resurrect a cancelled/consumed route.
        } else {
            val same = old != null && abs(old.latitude - latitude) < 0.000001 &&
                abs(old.longitude - longitude) < 0.000001 && old.label == label.take(120)
            if (same && old!!.status == NavigationDraftStatus.ACTIVE) old
            else NavigationDraft(old?.id ?: newId(), old?.let { next(it.revision) } ?: 0,
                label.take(120), latitude, longitude, now(), NavigationDraftStatus.ACTIVE)
        }
    }

    suspend fun cancelled() = update { it?.copy(status = NavigationDraftStatus.CANCELLED, revision = next(it.revision)) }
    suspend fun arrivalHint() = update {
        if (it?.status == NavigationDraftStatus.ACTIVE) it.copy(status = NavigationDraftStatus.NEEDS_REVIEW,
            revision = next(it.revision)) else it
    }
    suspend fun consume(draft: NavigationDraft) = update {
        if (it == draft) it.copy(status = NavigationDraftStatus.CONSUMED, revision = next(it.revision)) else it
    }
    suspend fun dismiss(draft: NavigationDraft) = update {
        if (it == draft) it.copy(status = NavigationDraftStatus.CANCELLED, revision = next(it.revision)) else it
    }
    /** A transport failure preserves the last hint but blocks confirmation until a valid observation. */
    suspend fun unavailable(message: String) {
        loading.join()
        mutex.withLock { mutable.value = mutable.value.copy(error = message) }
    }

    /** Awaited before game reset; failure prevents resetting while an old draft could be restored. */
    suspend fun clear() { check(update { null }) { "내비 목적지 기록을 정리하지 못했어요." } }

    private suspend fun update(transform: (NavigationDraft?) -> NavigationDraft?): Boolean {
        loading.join()
        return mutex.withLock {
            val before = mutable.value
            if (!before.ready) return@withLock false
            try {
                val after = transform(before.draft)
                require(after == null || valid(after))
                if (after == before.draft && before.error == null) return@withLock true
                mutable.value = before.copy(saving = true)
                store.write(after)
                mutable.value = NavigationInboxState(ready = true, draft = after)
                true
            } catch (cancelled: CancellationException) {
                mutable.value = before.copy(saving = false, error = "내비 목적지 저장을 다시 확인해 주세요.")
                throw cancelled
            } catch (_: Exception) {
                mutable.value = before.copy(saving = false, error = "내비 목적지를 저장하지 못했어요. 다시 조회해 주세요.")
                false
            }
        }
    }

    private fun valid(value: NavigationDraft) = runCatching {
        UUID.fromString(value.id).toString() == value.id && value.revision >= 0 &&
            value.observedAtEpochMs >= 0 && value.label.isNotBlank() && value.label.length <= 120 &&
            coordinates(value.latitude, value.longitude)
    }.getOrDefault(false)
    private fun next(revision: Int): Int { check(revision < Int.MAX_VALUE); return revision + 1 }
    private fun coordinates(latitude: Double, longitude: Double) = latitude.isFinite() && longitude.isFinite() &&
        latitude in -90.0..90.0 && longitude in -180.0..180.0 && !(latitude == 0.0 && longitude == 0.0)
}
