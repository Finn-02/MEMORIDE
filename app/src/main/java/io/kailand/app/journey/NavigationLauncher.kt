package io.kailand.app.journey

import io.kailand.core.contract.RequestResult
import kotlinx.coroutines.CancellationException

/** Resolve then recheck admission immediately before the external side effect. No game writes. */
class NavigationLauncher(private val resolve: () -> (() -> Unit)?) {
    fun launch(allowed: () -> Boolean): RequestResult {
        if (!allowed()) return RequestResult.Failed("주차 상태와 현재 화면을 확인해 주세요.")
        return try {
            val open = resolve() ?: return RequestResult.Failed("내비 앱을 찾지 못했어요. 설치 상태를 확인한 뒤 다시 시도해 주세요.")
            if (!allowed()) return RequestResult.Failed("이용 상태가 바뀌어 내비를 열지 않았어요.")
            open()
            RequestResult.Accepted // OS launch admission only, never arrival or reward success.
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (_: RuntimeException) {
            RequestResult.Failed("내비 앱을 열지 못했어요. 다시 시도하거나 섬으로 돌아가 주세요.")
        }
    }
}
