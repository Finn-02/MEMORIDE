package io.kailand.app.journey

import kotlinx.coroutines.flow.StateFlow

/** Read-only platform observation. No navigation control, game commands, or UI launches. */
interface NavigationCapture {
    val status: StateFlow<String>
    fun start()
    fun refresh()
    suspend fun reset()
}
