package io.kailand.app

import android.app.Application

/** All activity entries share the same service and command lock for this process. */
class MemorideApplication : Application() {
    val gameSession: GameAppSession by lazy { GameAppSession(this) }
}
