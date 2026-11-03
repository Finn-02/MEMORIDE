package io.kailand.app

/** Explicit debug-only launcher; unavailable in release and blocked on Automotive. */
class DevelopmentActivity : MainActivity() {
    override val developmentEntry: Boolean = true
}
