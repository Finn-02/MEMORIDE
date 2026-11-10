package io.kailand.feature.demo

import io.kailand.core.contract.*

/** C-local integration proposal. No storage or SDK implementation lives in this package. */
enum class UxDemoPreset(val title: String, val guide: List<String>) {
    NEW_REGION("1. 원주 새 지역", listOf(
        "캐릭터를 고르고 섬으로 이동", "빈 지도·잠긴 기념품 확인", "여정 출발 후 내비 사용",
        "주차 및 이용 허용 상태에서 직접 앱 열기", "원주 목적지 확인 후 이동 마무리·도착 확인",
        "실제 저장 성공 뒤 결과·지도·기념품 확인")),
    REVISIT("2. 원주 재방문", listOf(
        "원주 발견·기념품·방문 기록 확인", "같은 KST 날짜에 원주 여정 진행",
        "직접 앱 복귀 후 도착 확인", "저장 뒤 성장0·추가 선물0·방문 횟수 증가 확인")),
    SAVE_RETRY("3. 저장 실패 후 재시도", listOf(
        "원주 새 지역과 같은 흐름 진행", "A가 주입한 확정 저장 실패 확인",
        "같은 원본 도착으로 다시 시도", "저장 성공 뒤 선물·성장이 한 번만 반영됐는지 확인")),
    NAVIGATION_FAILURE("4. 내비 실행 실패", listOf(
        "캐릭터 선택 후 여정 출발", "A가 주입한 내비 실행 실패 확인",
        "다시 시도 또는 섬으로 돌아가기", "실패 시점에는 출발·도착 저장·보상이 없었는지 확인")),
}

/** requestToken correlates this control request only; never use it as a journey ID. */
data class UxDemoRequest(val preset: UxDemoPreset, val requestToken: String)

data class UxDemoSaveFailure(
    val originalCommand: ArrivalCommand,
    val retriedCommand: ArrivalCommand?,
    val snapshotAtConfirmedFailure: GameSnapshot,
)

data class UxDemoNavigationFailure(
    val snapshotAtFailure: GameSnapshot,
    val travelStarted: Boolean,
    val arrivalSubmitted: Boolean,
    val receiptAtFailure: ArrivalReceipt?,
)

enum class UxDemoNavigationRecovery { NONE, RETURNED_TO_ISLAND, RETRIED_SUCCESSFULLY }

/** A supplies immutable observations from its admission/B boundaries. Never infer these from a timer.
 * preparedGame: isolated seed state; beforeAttempt: immediately before navigation/arrival attempt,
 * afterAttempt: committed state after the observed branch. Character selection may precede beforeAttempt.
 */
data class UxDemoEvidence(
    val request: UxDemoRequest,
    val sessionId: String,
    val isolatedEnvironment: Boolean,
    val source: DataSource,
    val preparedGame: GameSnapshot,
    val visitsBefore: Int?,
    val revisitGrowthAlreadyUsedToday: Boolean,
    val beforeAttempt: GameSnapshot? = null,
    val afterAttempt: GameSnapshot? = null,
    val visitsAfter: Int? = null,
    val receipt: ArrivalReceipt? = null,
    val saveFailure: UxDemoSaveFailure? = null,
    val navigationFailure: UxDemoNavigationFailure? = null,
    val navigationRecovery: UxDemoNavigationRecovery = UxDemoNavigationRecovery.NONE,
    val finished: Boolean = false,
)

enum class UxDemoVerdict { PASS, FAIL, NOT_RUN }
data class UxDemoCheck(val verdict: UxDemoVerdict, val message: String)

/** Pure read-only checking, not reward computation or a storage command. */
fun checkUxDemoPreparation(request: UxDemoRequest, evidence: UxDemoEvidence?, catalog: Catalog): UxDemoCheck {
    fun failed(message: String) = UxDemoCheck(UxDemoVerdict.FAIL, message)
    if (evidence == null) return UxDemoCheck(UxDemoVerdict.NOT_RUN, "준비 결과를 기다리고 있어요.")
    if (request.requestToken.isBlank() || request != evidence.request || evidence.sessionId.isBlank())
        return failed("현재 준비 요청과 시험 환경이 일치하지 않아요.")
    if (!evidence.isolatedEnvironment) return failed("별도 시험 환경이 아니므로 진행할 수 없어요.")
    val region = catalog.regions.singleOrNull { it.id == "wonju" }
        ?: return failed("등록된 원주 지역을 확인할 수 없어요.")
    if (region.itemIds.isEmpty() || region.itemIds.distinct().size != region.itemIds.size ||
        region.itemIds.any { id -> catalog.items.count { it.id == id && it.regionId == region.id } != 1 })
        return failed("원주 기념품 Catalog를 확인할 수 없어요.")
    val game = evidence.preparedGame
    if (game.pendingReceipt != null) return failed("먼저 기존 결과를 확인해 주세요.")
    if (request.preset == UxDemoPreset.REVISIT) {
        if (game.discoveredRegionIds != setOf(region.id) || game.ownedItemIds != region.itemIds.toSet() ||
            evidence.visitsBefore == null || evidence.visitsBefore < 1 ||
            game.dailyGrowth !in 0..3 || !evidence.revisitGrowthAlreadyUsedToday)
            return failed("원주만 발견한 같은 날 재방문 준비를 확인할 수 없어요.")
    } else if (game.discoveredRegionIds.isNotEmpty() || game.ownedItemIds.isNotEmpty() ||
        game.dailyGrowth != 0 || evidence.visitsBefore != 0) {
        return failed("빈 지도·잠긴 기념품의 독립 초기 상태가 아니에요.")
    }
    return UxDemoCheck(UxDemoVerdict.PASS, "시연 준비 확인 · 실행 결과는 아직 미확인")
}

fun checkUxDemoResult(request: UxDemoRequest, evidence: UxDemoEvidence?, catalog: Catalog): UxDemoCheck {
    val prepared = checkUxDemoPreparation(request, evidence, catalog)
    if (prepared.verdict != UxDemoVerdict.PASS) return prepared
    val e = checkNotNull(evidence)
    if (!e.finished) return UxDemoCheck(UxDemoVerdict.NOT_RUN, "실제 시연·저장 결과를 기다리고 있어요.")
    fun failed(message: String) = UxDemoCheck(UxDemoVerdict.FAIL, message)
    val before = e.beforeAttempt ?: return failed("시도 전 상태를 확인할 수 없어요.")
    val after = e.afterAttempt ?: return failed("시도 후 확정 상태를 확인할 수 없어요.")
    if (before.copy(avatar = e.preparedGame.avatar) != e.preparedGame)
        return failed("시도 전 게임 진행이 준비 상태와 달라요.")
    if (request.preset == UxDemoPreset.NAVIGATION_FAILURE) {
        val failure = e.navigationFailure ?: return failed("내비 실행 실패 근거가 없어요.")
        if (failure.snapshotAtFailure != before || failure.travelStarted || failure.arrivalSubmitted ||
            failure.receiptAtFailure != null) return failed("내비 실패 중 진행·저장 변화가 발생했어요.")
        when (e.navigationRecovery) {
            UxDemoNavigationRecovery.NONE -> return UxDemoCheck(UxDemoVerdict.NOT_RUN, "실패 뒤 재시도 또는 섬 복귀를 확인해 주세요.")
            UxDemoNavigationRecovery.RETURNED_TO_ISLAND -> {
                if (after != before || e.receipt != null || e.visitsAfter != e.visitsBefore)
                    return failed("섬 복귀 중 기록·보상 변화가 발생했어요.")
                return UxDemoCheck(UxDemoVerdict.PASS, "내비 실패·무지급·섬 복귀 확인")
            }
            UxDemoNavigationRecovery.RETRIED_SUCCESSFULLY -> Unit
        }
    }
    val receipt = e.receipt ?: return failed("확정된 원본 영수증이 없어요.")
    val revisit = request.preset == UxDemoPreset.REVISIT
    val expectedItems = if (revisit) emptySet() else catalog.regions.single { it.id == "wonju" }.itemIds.toSet()
    val expectedGrowth = (before.dailyGrowth + if (revisit) 0 else 1).coerceAtMost(3)
    if (receipt.journeyId.isBlank() || receipt.kind != JourneyKind.REGION || receipt.regionId != "wonju" ||
        receipt.source != e.source || receipt.growthBefore != before.dailyGrowth ||
        receipt.completedAtEpochMs < 0 || (receipt.source == DataSource.USER_CONFIRMED &&
            (receipt.elapsedSeconds != null || receipt.distanceMeters != null)) ||
        receipt.growthAfter != expectedGrowth || after.dailyGrowth != receipt.growthAfter ||
        receipt.discoveredRegionId != if (revisit) null else "wonju")
        return failed("원본 영수증의 지역·출처·성장·발견 결과가 기대와 달라요.")
    if (receipt.grantedItemIds.toSet() != expectedItems || receipt.grantedItemIds.size != expectedItems.size ||
        after.ownedItemIds != before.ownedItemIds + expectedItems ||
        after.discoveredRegionIds != before.discoveredRegionIds + "wonju" ||
        e.visitsAfter?.toLong() != checkNotNull(e.visitsBefore).toLong() + 1)
        return failed("기념품·발견·방문 횟수가 기대와 달라요.")
    if (request.preset == UxDemoPreset.SAVE_RETRY) {
        val failure = e.saveFailure ?: return failed("확정 저장 실패 근거가 없어요.")
        val original = failure.originalCommand
        if (failure.snapshotAtConfirmedFailure != before || original.journeyId.isBlank() ||
            original != failure.retriedCommand || original.journeyId != receipt.journeyId ||
            original.completedAtEpochMs != receipt.completedAtEpochMs || original.source != receipt.source ||
            original.kind != receipt.kind || original.regionId != receipt.regionId ||
            original.elapsedSeconds != receipt.elapsedSeconds || original.distanceMeters != receipt.distanceMeters)
            return failed("재시도 원본 ID·시각·내용 또는 실패 전후 상태가 달라요.")
    }
    return UxDemoCheck(UxDemoVerdict.PASS, "확정 결과 확인 · 시연 표본 PASS")
}
