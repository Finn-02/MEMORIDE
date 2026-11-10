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
    private val onAttempt: (ArrivalCommand) -> Unit = {},
) {
    private val controller = ManualArrivalController(regionIds, now = now)
    val state = controller.state
    private val mutableBusy = MutableStateFlow(false)
    val busy = mutableBusy.asStateFlow()
    private val mutableResult = MutableStateFlow<ArrivalResult?>(null)
    val result = mutableResult.asStateFlow()
    private var valid = true
    private var original: ArrivalCommand? = null
    private var acknowledgedWhileSaving: String? = null
    private var navigationCurrent: (() -> Boolean)? = null
    private var navigationSaved: (suspend () -> Unit)? = null
    private var navigationDismissed: (suspend () -> Unit)? = null
    val isNavigationJourney: Boolean get() = navigationCurrent != null

    val snapshot: JourneySnapshot get() = state.value.let {
        JourneySnapshot(it.journeyId, DataSource.USER_CONFIRMED, it.kind, when (it.phase) {
            ManualJourneyPhase.IDLE -> JourneyPhase.IDLE
            ManualJourneyPhase.ACTIVE -> JourneyPhase.TRAVELLING
            ManualJourneyPhase.CHANGE_REQUIRED -> JourneyPhase.PREPARING
            ManualJourneyPhase.CONFIRMED -> JourneyPhase.ARRIVED
            ManualJourneyPhase.CANCELLED -> JourneyPhase.CANCELLED
        }, it.revision, it.destination)
    }

    private fun allowed() = valid && hostAllowed() && commands.session.value == GameCommandSession(generation) &&
        (navigationCurrent?.invoke() ?: true)

    fun updateHost() { controller.setInteractionAllowed(allowed()) }

    fun invalidate() {
        valid = false
        controller.invalidate()
        original = null
        acknowledgedWhileSaving = null
        navigationCurrent = null; navigationSaved = null; navigationDismissed = null
    }

    fun begin(destination: DestinationSpec, kind: JourneyKind, caller: () -> Boolean): RequestResult {
        if (!valid || !hostAllowed() || !caller() || busy.value ||
            commands.session.value != GameCommandSession(generation)) return failed()
        // A valid fresh manual choice may replace a stale hint, but a rejected choice must retain its fence.
        controller.setInteractionAllowed(true)
        return controller.begin(destination, kind).also {
            if (it == RequestResult.Accepted) {
                navigationCurrent = null; navigationSaved = null; navigationDismissed = null
                original = null; mutableResult.value = null
            }
            updateHost()
        }
    }

    fun adoptNavigation(draft: NavigationDraft, destination: DestinationSpec, current: () -> Boolean,
        saved: suspend () -> Unit, dismissed: suspend () -> Unit = {}, caller: () -> Boolean): RequestResult {
        if (!valid || !hostAllowed() || !caller() || busy.value || !current() ||
            commands.session.value != GameCommandSession(generation)) return failed()
        controller.setInteractionAllowed(true)
        return controller.adoptNavigation(draft, destination).also {
            if (it == RequestResult.Accepted) {
                navigationCurrent = current; navigationSaved = saved
                navigationDismissed = dismissed
                original = null; mutableResult.value = null
            }
            updateHost()
        }
    }

    fun leaveNavigation(caller: () -> Boolean) {
        if (!valid || !hostAllowed() || !caller() || busy.value ||
            result.value is ArrivalResult.Applied || result.value is ArrivalResult.AlreadyApplied) return
        controller.invalidate(); navigationCurrent = null; navigationSaved = null; navigationDismissed = null
        original = null; mutableResult.value = null; updateHost()
    }

    fun requestReview(caller: () -> Boolean): Boolean {
        updateHost()
        return allowed() && caller() && !busy.value && controller.requestReview(ManualReviewReason.USER_BUTTON)
    }

    suspend fun choose(token: ManualConfirmationToken, choice: ManualJourneyChoice, caller: () -> Boolean): RequestResult {
        updateHost()
        if (!allowed() || !caller() || busy.value) return failed()
        val chosen = controller.choose(token, choice)
        if (chosen == RequestResult.Accepted && choice == ManualJourneyChoice.CANCEL) navigationDismissed?.invoke()
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
                onAttempt(command)
                val answer = commands.completeUserConfirmed(generation, command, ::current)
                if (answer is ArrivalResult.Applied || answer is ArrivalResult.AlreadyApplied) navigationSaved?.invoke()
                mutableResult.value = answer
                when (answer) {
                    is ArrivalResult.Applied, is ArrivalResult.AlreadyApplied -> RequestResult.Accepted
                    is ArrivalResult.Rejected -> RequestResult.Failed(answer.reason)
                    is ArrivalResult.StorageFailure -> RequestResult.Failed(answer.message)
                }
            } finally {
                mutableBusy.value = false
                acknowledgedWhileSaving?.let { id ->
                    acknowledgedWhileSaving = null
                    finishAcknowledged(id)
                }
            }
        }.await()
    }

    /** Finish this in-memory journey after its stored receipt has been acknowledged. */
    fun finishAcknowledged(journeyId: String) {
        if (state.value.journeyId == journeyId && busy.value) {
            acknowledgedWhileSaving = journeyId
            return
        }
        if (state.value.journeyId == journeyId && !busy.value &&
            (result.value is ArrivalResult.Applied || result.value is ArrivalResult.AlreadyApplied)) {
            controller.invalidate(); original = null; mutableResult.value = null
            navigationCurrent = null; navigationSaved = null; navigationDismissed = null
            updateHost()
        }
    }

    fun leaveScreen() { controller.setInteractionAllowed(false) }
    private fun failed() = RequestResult.Failed("현재 여정과 이용 상태를 다시 확인해 주세요.")
}
