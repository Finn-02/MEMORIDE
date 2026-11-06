package io.kailand.app.journey

import io.kailand.app.GameCommandCoordinator
import io.kailand.app.GameCommandSession
import io.kailand.core.contract.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** App-owned manual journey. Only this binding translates a current user decision into storage input.
 * The app main scope owns admitted work; a departing screen cannot split B's commit/publication.
 */
class ManualJourneyRuntime(
    private val scope: CoroutineScope,
    private val commands: GameCommandCoordinator,
    private val generation: Long,
    regionIds: Set<String>,
    private val hostAllowed: () -> Boolean,
    now: () -> Long = System::currentTimeMillis,
) {
    private val controller = ManualArrivalController(regionIds, now = now)
    val state = controller.state
    private val mutableBusy = MutableStateFlow(false)
    val busy = mutableBusy.asStateFlow()
    private val mutableResult = MutableStateFlow<ArrivalResult?>(null)
    val result = mutableResult.asStateFlow()
    private var valid = true
    private var original: ArrivalCommand? = null

    val snapshot: JourneySnapshot get() = state.value.let {
        JourneySnapshot(it.journeyId, DataSource.USER_CONFIRMED, it.kind, when (it.phase) {
            ManualJourneyPhase.IDLE -> JourneyPhase.IDLE
            ManualJourneyPhase.ACTIVE -> JourneyPhase.TRAVELLING
            ManualJourneyPhase.CHANGE_REQUIRED -> JourneyPhase.PREPARING
            ManualJourneyPhase.CONFIRMED -> JourneyPhase.ARRIVED
            ManualJourneyPhase.CANCELLED -> JourneyPhase.CANCELLED
        }, it.revision, it.destination)
    }

    private fun allowed() = valid && hostAllowed() && commands.session.value == GameCommandSession(generation)

    fun updateHost() { controller.setInteractionAllowed(allowed()) }

    fun invalidate() {
        valid = false
        controller.invalidate()
        original = null
    }

    fun begin(destination: DestinationSpec, kind: JourneyKind, caller: () -> Boolean): RequestResult {
        updateHost()
        if (!allowed() || !caller() || busy.value) return failed()
        return controller.begin(destination, kind).also {
            if (it == RequestResult.Accepted) { original = null; mutableResult.value = null }
        }
    }

    fun requestReview(caller: () -> Boolean): Boolean {
        updateHost()
        return allowed() && caller() && !busy.value && controller.requestReview(ManualReviewReason.USER_BUTTON)
    }

    suspend fun choose(token: ManualConfirmationToken, choice: ManualJourneyChoice, caller: () -> Boolean): RequestResult {
        updateHost()
        if (!allowed() || !caller() || busy.value) return failed()
        val chosen = controller.choose(token, choice)
        if (chosen != RequestResult.Accepted || choice != ManualJourneyChoice.ARRIVED) return chosen
        val draft = checkNotNull(state.value.confirmedDraft)
        original = ArrivalCommand(draft.token.journeyId, DataSource.USER_CONFIRMED, draft.kind,
            draft.declaredDestination.regionId, draft.token.revision, ArrivalEvidence.USER_CONFIRMED_DESTINATION,
            draft.confirmedAtEpochMs, null, null)
        return save(draft, caller)
    }

    suspend fun retry(caller: () -> Boolean): RequestResult {
        updateHost()
        val draft = state.value.confirmedDraft ?: return failed()
        if (result.value !is ArrivalResult.StorageFailure && result.value !is ArrivalResult.Rejected) return failed()
        return save(draft, caller)
    }

    private suspend fun save(draft: UserConfirmedArrivalDraft, caller: () -> Boolean): RequestResult {
        val command = original ?: return failed()
        fun current() = allowed() && caller() && state.value.interactionAllowed &&
            state.value.phase == ManualJourneyPhase.CONFIRMED && state.value.confirmedDraft === draft &&
            state.value.journeyId == draft.token.journeyId && state.value.revision == draft.token.revision
        if (busy.value || !current()) return failed()
        mutableBusy.value = true
        // Runs even if the UI coroutine is cancelled while waiting; admission still checks its lease.
        return scope.async {
            try {
                val answer = commands.completeUserConfirmed(generation, command, ::current)
                mutableResult.value = answer
                when (answer) {
                    is ArrivalResult.Applied, is ArrivalResult.AlreadyApplied -> RequestResult.Accepted
                    is ArrivalResult.Rejected -> RequestResult.Failed(answer.reason)
                    is ArrivalResult.StorageFailure -> RequestResult.Failed(answer.message)
                }
            } finally { mutableBusy.value = false }
        }.await()
    }

    /** Finish this in-memory journey after its stored receipt has been acknowledged. */
    fun finishAcknowledged(journeyId: String) {
        if (state.value.journeyId == journeyId && !busy.value &&
            (result.value is ArrivalResult.Applied || result.value is ArrivalResult.AlreadyApplied)) {
            controller.invalidate(); original = null; mutableResult.value = null
            updateHost()
        }
    }

    fun leaveScreen() { controller.setInteractionAllowed(false) }
    private fun failed() = RequestResult.Failed("현재 여정과 이용 상태를 다시 확인해 주세요.")
}
