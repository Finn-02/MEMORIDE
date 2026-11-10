package io.kailand.core.contract

/** Additive UX-A01 proposal; B/C confirm before live wiring. Not a new save schema.
 * B derives this from unique committed REGION receipts. Sources may include DEMO;
 * these records are game progress, never proof of a real vehicle visit.
 */
data class RegionVisitSummary(
    val regionId: String,
    val visitCount: Int,
    val firstVisitedAtEpochMs: Long?,
    val lastVisitedAtEpochMs: Long?,
)

/** Available(emptyList()) means known-empty. Loading/Unavailable must not appear as zero visits.
 * A passes this read model to C; C never imports B's GameSave or derives dates from discovery IDs.
 * Epochs are UTC; display dates in Asia/Seoul with the year.
 */
sealed interface RegionVisitHistory {
    data object Loading : RegionVisitHistory
    data class Available(val regions: List<RegionVisitSummary>) : RegionVisitHistory
    data class Unavailable(val message: String) : RegionVisitHistory
}
