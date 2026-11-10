package io.kailand.app

import io.kailand.core.contract.GameSnapshot
import io.kailand.feature.island.GameSave
import io.kailand.feature.island.GameSaveStore
import java.io.IOException

/** Debug-only interception of B's transaction; B still computes and writes every game value. */
internal class RehearsalSaveProbe(private val delegate: GameSaveStore) : GameSaveStore {
    var committed = GameSave()
        private set
    var failNextArrival = false
    var writing = false
        private set
    var confirmedFailure: GameSnapshot? = null
        private set

    override suspend fun read(): GameSave? = delegate.read().also { committed = it ?: GameSave() }

    override suspend fun update(transform: (GameSave) -> GameSave): GameSave {
        writing = true
        try {
            return delegate.update { before ->
                val next = transform(before)
                if (failNextArrival && (next.processedReceipts.keys - before.processedReceipts.keys).isNotEmpty()) {
                    failNextArrival = false
                    confirmedFailure = before.toSnapshot()
                    // Inside the Preferences transaction, before commit/publication: no ambiguous timeout.
                    throw IOException("리허설용 저장 확정 전 실패 1회")
                }
                next
            }.also { committed = it }
        } finally { writing = false }
    }
}
