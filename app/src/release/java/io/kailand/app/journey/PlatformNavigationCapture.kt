package io.kailand.app.journey

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow

/** SDK observation remains debug-only until lifecycle/arrival acceptance is complete. */
@Suppress("UNUSED_PARAMETER")
fun createNavigationCapture(context: Context, scope: CoroutineScope, inbox: NavigationInbox): NavigationCapture =
    object : NavigationCapture {
        override val status = MutableStateFlow("내비 목적지 자동 수신은 개발 빌드 검증 중입니다.")
        override fun start() = Unit
        override fun refresh() = Unit
        override suspend fun reset() { inbox.clear() }
    }
