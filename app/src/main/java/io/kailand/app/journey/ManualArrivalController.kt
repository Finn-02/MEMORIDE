package io.kailand.app.journey

import io.kailand.core.contract.DestinationSpec
import io.kailand.core.contract.JourneyKind
import io.kailand.core.contract.RequestResult
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ManualJourneyPhase { IDLE, ACTIVE, CHANGE_REQUIRED, CONFIRMED, CANCELLED }
enum class ManualReviewReason { USER_BUTTON, NAVIGATION_ENDED, NAVIGATION_ARRIVAL, NAVIGATION_CHANGED }
enum class ManualJourneyChoice { ARRIVED, CHANGE_DESTINATION, CANCEL, CONTINUE }

data class ManualConfirmationToken(val journeyId: String, val revision: Int, val generation: Long, val nonce: Long)
data class ManualConfirmation(val token: ManualConfirmationToken, val destination: DestinationSpec, val reason: ManualReviewReason)

/** App-owned draft, deliberately NOT an ArrivalCommand or an SDK-verified receipt. */
data class UserConfirmedArrivalDraft(
    val token: ManualConfirmationToken,
    val kind: JourneyKind,
    val declaredDestination: DestinationSpec,
    val confirmedAtEpochMs: Long,
    val elapsedSeconds: Long? = null,
    val distanceMeters: Double? = null,
)

data class ManualJourneySnapshot(
    val phase: ManualJourneyPhase = ManualJourneyPhase.IDLE,
    val journeyId: String? = null,
    val revision: Int = 0,
    val kind: JourneyKind = JourneyKind.REGION,
    val destination: DestinationSpec? = null,
    val interactionAllowed: Boolean = false,
    val pendingReason: ManualReviewReason? = null,
    val confirmation: ManualConfirmation? = null,
    val confirmedDraft: UserConfirmedArrivalDraft? = null,
)

/** Small atomic UI transitions only. Never writes progress or fabricates platform permission.
 * Navigation callbacks are hints to ask the user, not evidence that a journey arrived.
 */
class ManualArrivalController(
    private val regionIds: Set<String>,
    private val newId: () -> String = { UUID.randomUUID().toString() },
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val mutable = MutableStateFlow(ManualJourneySnapshot())
    val state = mutable.asStateFlow()
    private var generation = 0L
    private var nonce = 0L

    /** Restore only a navigation hint's stable identity. This is not an arrival declaration. */
    @Synchronized fun adoptNavigation(draft: NavigationDraft, destination: DestinationSpec): RequestResult {
        val before = mutable.value
        if (!before.interactionAllowed || before.phase == ManualJourneyPhase.CONFIRMED ||
            !draft.usable(now()) || destination.label.isBlank() ||
            destination.latitude != draft.latitude || destination.longitude != draft.longitude ||
            (destination.regionId != null && destination.regionId !in regionIds)) return failed("현재 내비 목적지를 다시 확인해 주세요.")
        if (runCatching { UUID.fromString(draft.id).toString() == draft.id }.getOrDefault(false).not() ||
            draft.revision < 0) return failed("목적지 기록을 확인해 주세요.")
        mutable.value = ManualJourneySnapshot(phase = ManualJourneyPhase.ACTIVE,
            journeyId = draft.id, revision = draft.revision, kind = JourneyKind.REGION,
            destination = destination, interactionAllowed = true)
        return RequestResult.Accepted
    }

    @Synchronized fun begin(destination: DestinationSpec, kind: JourneyKind): RequestResult {
        val before = mutable.value
        if (!before.interactionAllowed) return failed("주차·이용 허용 상태에서 여정을 선택해 주세요.")
        if (before.phase == ManualJourneyPhase.CONFIRMED) return failed("확인 초안이 남아 있어요.")
        if (!destination.latitude.isFinite() || destination.latitude !in -90.0..90.0 ||
            !destination.longitude.isFinite() || destination.longitude !in -180.0..180.0 ||
            destination.label.isBlank() || (kind == JourneyKind.REGION && destination.regionId !in regionIds)) {
            return failed("목적지를 확인해 주세요.")
        }
        val active = before.phase in setOf(ManualJourneyPhase.ACTIVE, ManualJourneyPhase.CHANGE_REQUIRED)
        if (active && before.kind != kind) return failed("이동 종류를 바꾸려면 현재 여정을 취소해 주세요.")
        if (active && before.revision == Int.MAX_VALUE) return failed("새 여정을 시작해 주세요.")
        val destinationSnapshot = if (kind == JourneyKind.DAILY) destination.copy(regionId = null) else destination
        mutable.value = ManualJourneySnapshot(
            phase = ManualJourneyPhase.ACTIVE,
            journeyId = if (active) before.journeyId else newId(),
            revision = if (active) before.revision + 1 else 0,
            kind = kind,
            destination = destinationSnapshot,
            interactionAllowed = true,
        )
        return RequestResult.Accepted
    }

    @Synchronized fun setInteractionAllowed(allowed: Boolean) {
        val before = mutable.value
        if (before.interactionAllowed == allowed) return
        mutable.value = withPrompt(before.copy(interactionAllowed = allowed, confirmation = null))
    }

    @Synchronized fun requestReview(reason: ManualReviewReason): Boolean {
        val before = mutable.value
        if (before.phase != ManualJourneyPhase.ACTIVE) return false
        if (reason == ManualReviewReason.USER_BUTTON && !before.interactionAllowed) return false
        // Do not replace an open question merely because an unrelated callback repeats.
        mutable.value = withPrompt(before.copy(pendingReason = before.pendingReason ?: reason))
        return true
    }

    @Synchronized fun navigationDestinationChanged() {
        val before = mutable.value
        if (before.phase != ManualJourneyPhase.ACTIVE) return
        mutable.value = withPrompt(before.copy(confirmation = null, pendingReason = ManualReviewReason.NAVIGATION_CHANGED))
    }

    @Synchronized fun choose(token: ManualConfirmationToken, choice: ManualJourneyChoice): RequestResult {
        val before = mutable.value
        if (!before.interactionAllowed || before.phase != ManualJourneyPhase.ACTIVE ||
            before.confirmation?.token != token || token.generation != generation ||
            before.journeyId != token.journeyId || before.revision != token.revision) {
            return failed("여정이나 이용 상태가 바뀌었어요. 현재 여정을 다시 확인해 주세요.")
        }
        val cleared = before.copy(confirmation = null, pendingReason = null)
        mutable.value = when (choice) {
            ManualJourneyChoice.ARRIVED -> {
                val timestamp = now()
                if (timestamp < 0) return failed("확인 시각을 확인해 주세요.")
                cleared.copy(phase = ManualJourneyPhase.CONFIRMED, confirmedDraft = UserConfirmedArrivalDraft(
                    token, before.kind, checkNotNull(before.destination), timestamp,
                ))
            }
            ManualJourneyChoice.CHANGE_DESTINATION -> cleared.copy(phase = ManualJourneyPhase.CHANGE_REQUIRED)
            ManualJourneyChoice.CANCEL -> cleared.copy(phase = ManualJourneyPhase.CANCELLED)
            ManualJourneyChoice.CONTINUE -> cleared
        }
        // Accepted is a local user decision, NOT a saved reward/receipt.
        return RequestResult.Accepted
    }

    /** Debug draft reset only. A04 must coordinate actual GameService reset separately. */
    @Synchronized fun resetPreview(): Boolean {
        if (!mutable.value.interactionAllowed) return false
        invalidate()
        mutable.value = mutable.value.copy(interactionAllowed = true)
        return true
    }

    /** Owner/mode/reset invalidation is unconditional, including while the platform blocks input. */
    @Synchronized fun invalidate() {
        generation++
        mutable.value = ManualJourneySnapshot()
    }

    private fun withPrompt(snapshot: ManualJourneySnapshot): ManualJourneySnapshot {
        if (!snapshot.interactionAllowed || snapshot.phase != ManualJourneyPhase.ACTIVE ||
            snapshot.pendingReason == null || snapshot.confirmation != null) return snapshot
        return snapshot.copy(confirmation = ManualConfirmation(
            ManualConfirmationToken(checkNotNull(snapshot.journeyId), snapshot.revision, generation, ++nonce),
            checkNotNull(snapshot.destination), snapshot.pendingReason,
        ))
    }

    private fun failed(reason: String) = RequestResult.Failed(reason)
}
