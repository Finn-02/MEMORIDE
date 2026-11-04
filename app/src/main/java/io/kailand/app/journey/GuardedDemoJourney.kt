package io.kailand.app.journey

import io.kailand.core.contract.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async

/** A screen can stop awaiting, but an accepted persistence operation must finish in the app scope.
 * A's actual command boundary checks owner/gate/generation again before the write.
 */
class GuardedDemoJourney(
    private val journey: JourneyPort,
    private val demo: DemoControlPort,
    private val appScope: CoroutineScope,
    private val callerIsCurrent: () -> Boolean,
) : JourneyPort, DemoControlPort {
    override val state = journey.state
    override suspend fun requestDestination(destination: DestinationSpec, kind: JourneyKind) =
        run { journey.requestDestination(destination, kind) }
    override suspend fun cancel() = run { journey.cancel() }
    override suspend fun retryArrival() = run { journey.retryArrival() }
    override suspend fun dispatch(input: DemoInput) = run { demo.dispatch(input) }

    private suspend fun run(block: suspend () -> RequestResult): RequestResult {
        if (!callerIsCurrent()) return rejected()
        return appScope.async { if (callerIsCurrent()) block() else rejected() }.await()
    }
    private fun rejected() = RequestResult.Failed("현재 여정 화면과 이용 상태를 다시 확인해 주세요.")
}
