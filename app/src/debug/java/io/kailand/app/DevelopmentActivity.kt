package io.kailand.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelProvider
import io.kailand.core.contract.InteractionGate

/** Explicit debug-only launcher; unavailable in release and blocked on Automotive. */
class DevelopmentActivity : MainActivity() {
    override val developmentEntry: Boolean = true

    @Composable
    override fun AppContent(gate: InteractionGate) {
        val model = remember { ViewModelProvider(this)[DevelopmentJourneyModel::class.java] }
        DevelopmentJourneyRoot(gate, model)
    }
}
