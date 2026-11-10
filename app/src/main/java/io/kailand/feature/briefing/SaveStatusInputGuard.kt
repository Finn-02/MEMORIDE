package io.kailand.feature.briefing

/** Main-thread single-flight guard shared by S06 `다시 시도` and `닫기`.
 * The decision is made when the tap is handled (not from a value captured at composition), so a
 * second tap — on either button — is ignored until A's next state or gate is observed. If A returns
 * to an identical `Failed` that StateFlow conflates (no change observed), the screen re-arms input
 * after [INPUT_REARM_FALLBACK_MS] so the buttons are never locked permanently. AppRoot still
 * serializes in-flight requests by journey ID; this guard is a UI convenience, not the contract.
 */
internal class SaveStatusInputGuard {
    var busy: Boolean = false
        private set

    /** Increments on every successful entry so a stale fallback cannot release a newer lock. */
    var generation: Int = 0
        private set

    fun tryEnter(canInteract: Boolean): Boolean {
        if (!canInteract || busy) return false
        busy = true
        generation++
        return true
    }

    fun release() { busy = false }

    fun releaseIf(entry: Int) { if (busy && generation == entry) busy = false }
}
