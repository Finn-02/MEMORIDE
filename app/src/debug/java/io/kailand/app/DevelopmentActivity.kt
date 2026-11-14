package io.kailand.app

/** Generic debug launcher follows the product graph; Automotive still blocks this entry. */
class DevelopmentActivity : MainActivity() {
    override val developmentEntry: Boolean = true
}
