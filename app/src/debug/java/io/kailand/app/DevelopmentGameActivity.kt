package io.kailand.app

/** Generic debug only, with the same process-owned service as the real app entry. */
class DevelopmentGameActivity : MainActivity() {
    override val developmentEntry: Boolean = true
}
