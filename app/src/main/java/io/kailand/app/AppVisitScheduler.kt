package io.kailand.app

import io.kailand.feature.visit.VisitCancellation
import io.kailand.feature.visit.VisitScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Cancellation never joins. Even delay zero is enqueued rather than executing inline. */
class AppVisitScheduler(private val scope: CoroutineScope) : VisitScheduler {
    override fun schedule(delayMs: Long, action: () -> Unit): VisitCancellation {
        val job = scope.launch { delay(delayMs.coerceAtLeast(1)); action() }
        return VisitCancellation { job.cancel() }
    }
}
