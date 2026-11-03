package io.kailand.feature.island

import io.kailand.core.contract.ArrivalReceipt
import io.kailand.core.contract.AvatarPreview
import io.kailand.core.contract.DataSource
import io.kailand.core.contract.GameSnapshot
import io.kailand.core.contract.JourneyKind
import java.time.LocalDate
import kotlinx.serialization.Serializable

/** B-owned, local-only save representation. Not a shared contract or SDK type. */
@Serializable
internal data class GameSave(
    val schemaVersion: Int = SAVE_SCHEMA_VERSION,
    val contentVersion: String = "1.1.0",
    val profileId: String = "local-player",
    val characterId: String = "tori",
    val equippedOutfitId: String? = null,
    val dailyGrowth: Int = 0,
    val lastDailyGrantDate: String? = null,
    val ownedItemIds: Set<String> = emptySet(),
    val discoveredRegionIds: Set<String> = emptySet(),
    val decorationSlots: Map<String, String> = emptyMap(),
    val processedReceipts: Map<String, SavedReceipt> = emptyMap(),
    val pendingReceiptId: String? = null,
) {
    fun toSnapshot(): GameSnapshot {
        check(schemaVersion == SAVE_SCHEMA_VERSION) { "Unsupported game save schemaVersion" }
        check(dailyGrowth in 0..3) { "Invalid saved dailyGrowth" }
        check(characterId.isNotBlank()) { "Invalid saved characterId" }
        lastDailyGrantDate?.let { check(LocalDate.parse(it).toString() == it) { "Invalid saved DAILY date" } }
        // Validate historical receipts too: acknowledging must not hide corrupt entries.
        processedReceipts.forEach { (id, receipt) ->
            check(id == receipt.journeyId && id.isNotBlank()) { "Mismatched receipt ID" }
            check(receipt.growthBefore in 0..3 && receipt.growthAfter in 0..3)
            check(receipt.completedAtEpochMs >= 0)
            check(receipt.elapsedSeconds == null || receipt.elapsedSeconds >= 0)
            check(receipt.distanceMeters == null ||
                (receipt.distanceMeters.isFinite() && receipt.distanceMeters >= 0))
            receipt.toContract() // Includes persisted enum validation.
        }
        val pending = pendingReceiptId?.let { id ->
            val receipt = checkNotNull(processedReceipts[id]) {
                "pendingReceiptId does not reference an original receipt"
            }
            check(receipt.journeyId == id) { "Mismatched receipt ID" }
            receipt.toContract()
        }
        return GameSnapshot(
            avatar = AvatarPreview(characterId, equippedOutfitId),
            dailyGrowth = dailyGrowth,
            ownedItemIds = ownedItemIds.toSet(),
            discoveredRegionIds = discoveredRegionIds.toSet(),
            decorationSlots = decorationSlots.toMap(),
            pendingReceipt = pending,
        )
    }
}

internal const val SAVE_SCHEMA_VERSION = 1

/** Dedicated serializable representation: do not annotate or change common DTOs. */
@Serializable
internal data class SavedReceipt(
    val journeyId: String,
    val source: String,
    val kind: String,
    val regionId: String?,
    val completedAtEpochMs: Long,
    val elapsedSeconds: Long?,
    val distanceMeters: Double?,
    val growthBefore: Int,
    val growthAfter: Int,
    val discoveredRegionId: String?,
    val grantedItemIds: List<String>,
) {
    fun toContract(): ArrivalReceipt = ArrivalReceipt(
        journeyId = journeyId,
        source = DataSource.valueOf(source),
        kind = JourneyKind.valueOf(kind),
        regionId = regionId,
        completedAtEpochMs = completedAtEpochMs,
        elapsedSeconds = elapsedSeconds,
        distanceMeters = distanceMeters,
        growthBefore = growthBefore,
        growthAfter = growthAfter,
        discoveredRegionId = discoveredRegionId,
        grantedItemIds = grantedItemIds.toList(),
    )

    companion object {
        fun fromContract(receipt: ArrivalReceipt): SavedReceipt = SavedReceipt(
            journeyId = receipt.journeyId,
            source = receipt.source.name,
            kind = receipt.kind.name,
            regionId = receipt.regionId,
            completedAtEpochMs = receipt.completedAtEpochMs,
            elapsedSeconds = receipt.elapsedSeconds,
            distanceMeters = receipt.distanceMeters,
            growthBefore = receipt.growthBefore,
            growthAfter = receipt.growthAfter,
            discoveredRegionId = receipt.discoveredRegionId,
            grantedItemIds = receipt.grantedItemIds.toList(),
        )
    }
}
