package io.kailand.feature.visit

import android.os.Handler
import android.os.Looper
import android.os.SystemClock

/** Create once per retained local visit session, never once per recomposition. */
fun createVisitController(): VisitController {
    val handler = Handler(Looper.getMainLooper())
    return VisitController(
        clock = VisitClock { SystemClock.elapsedRealtime() },
        scheduler = VisitScheduler { delayMs, action ->
            val runnable = Runnable { action() }
            handler.postDelayed(runnable, delayMs)
            VisitCancellation { handler.removeCallbacks(runnable) }
        },
    )
}
