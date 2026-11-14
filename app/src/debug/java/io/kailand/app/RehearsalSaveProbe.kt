package io.kailand.app

import io.kailand.core.contract.GameSnapshot
import io.kailand.feature.island.GameSave
import io.kailand.feature.island.GameSaveStore
import java.io.IOException
import kotlinx.coroutines.delay

/** Debug-only interception of B's transaction; B still computes and writes every game value. */
internal class RehearsalSaveProbe(private val delegate: GameSaveStore) : GameSaveStore {
    var committed = GameSave()
        private set
    var failNextArrival = false
    var failNextAcknowledgement = false
    var nextWriteDelayMs = 0L
    var writing = false
        private set
    var confirmedFailure: GameSnapshot? = null
        private set

    override suspend fun read(): GameSave? = delegate.read().also { committed = it ?: GameSave() }

    override suspend fun update(transform: (GameSave) -> GameSave): GameSave {
        writing = true
        try {
            val waitMs = nextWriteDelayMs
            nextWriteDelayMs = 0L
            println("MemorideStorageQa: " + "write started delayMs=$waitMs")
            if (waitMs > 0) delay(waitMs)
            return delegate.update { before ->
                val next = transform(before)
                if (failNextAcknowledgement && before.pendingReceiptId != null &&
                    next.pendingReceiptId == null && next.processedReceipts == before.processedReceipts) {
                    failNextAcknowledgement = false
                    println("MemorideStorageQa: " + "ack rejected before commit")
                    throw IOException("리허설용 확인 저장 실패 1회")
                }
                if (failNextArrival && (next.processedReceipts.keys - before.processedReceipts.keys).isNotEmpty()) {
                    failNextArrival = false
                    confirmedFailure = before.toSnapshot()
                    // Inside the Preferences transaction, before commit/publication: no ambiguous timeout.
                    throw IOException("리허설용 저장 확정 전 실패 1회")
                }
                next
            }.also {
                committed = it
                println("MemorideStorageQa: " + "write committed receipts=${it.processedReceipts.size} pending=${it.pendingReceiptId != null}")
            }
        } finally { writing = false }
    }
}
