package io.kailand.feature.briefing

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.kailand.core.contract.ArrivalReceipt
import io.kailand.core.contract.AvatarPreview
import io.kailand.core.contract.Catalog
import io.kailand.core.contract.GameSnapshot
import io.kailand.core.contract.InteractionGate
import io.kailand.core.contract.MutationResult
import io.kailand.core.contract.RegionVisitHistory
import io.kailand.core.ui.MemorideColors
import io.kailand.core.ui.MemoridePleosTheme
import io.kailand.core.ui.MemorideScreen
import io.kailand.core.ui.MemorideTheme
import io.kailand.core.ui.NewBriefingSlot
import kotlinx.coroutines.launch

/** S05 presentation only. Existing acknowledgement, retry and route callbacks remain authoritative. */
@Composable
fun RegionBriefingScreen(
    receipt: ArrivalReceipt,
    catalog: Catalog,
    avatar: AvatarPreview,
    gate: InteractionGate,
    onIsland: suspend () -> MutationResult,
    onSelectCharacter: suspend () -> MutationResult,
    modifier: Modifier = Modifier,
    applyInsets: Boolean = true,
    scene: (@Composable (firstVisit: Boolean) -> Unit)? = null,
    gameSnapshot: GameSnapshot? = null,
    visitHistory: RegionVisitHistory = RegionVisitHistory.Loading,
    useDemoTripRecords: Boolean = ResultDemoOptions.USE_MOCK_TRIP_RECORDS,
) {
    var confettiStarted by rememberSaveable(receipt.journeyId) { mutableStateOf(false) }
    key(receipt, gate) {
        val actions = remember { BriefingActions(gate) }
        val state by actions.state.collectAsState()
        val scope = rememberCoroutineScope()
        val island by rememberUpdatedState(onIsland)
        val select by rememberUpdatedState(onSelectCharacter)
        DisposableEffect(actions) { onDispose { actions.deactivate() } }
        val dispatch: (BriefingAction) -> Unit = { action ->
            scope.launch {
                actions.perform(action, if (action == BriefingAction.SELECT_CHARACTER) select else island)
            }
        }
        // Back is consumed and treated as "섬으로 가기" so acknowledgement cannot be bypassed.
        BackHandler { dispatch(BriefingAction.ISLAND) }
        val briefing = remember(receipt, catalog, avatar) { formatRegionBriefing(receipt, catalog, avatar) }
        RegionBriefingContent(briefing, gate, state, dispatch, modifier, applyInsets, scene,
            receipt, catalog, gameSnapshot, visitHistory, useDemoTripRecords,
            playConfetti = !confettiStarted, onConfettiStarted = { confettiStarted = true })
    }
}

/** Adapter for A's AppRoot wiring. */
val RegionBriefingSlot: NewBriefingSlot = { receipt, catalog, avatar, gate, island, select, modifier ->
    RegionBriefingScreen(receipt, catalog, avatar, gate, island, select, modifier)
}

/** Stateless layout; null briefing preserves the original legacy-compatible recovery action. */
@Composable
internal fun RegionBriefingContent(
    briefing: RegionBriefing?, gate: InteractionGate, state: BriefingActionState,
    onAction: (BriefingAction) -> Unit, modifier: Modifier = Modifier, applyInsets: Boolean = true,
    scene: (@Composable (firstVisit: Boolean) -> Unit)? = null,
    receipt: ArrivalReceipt? = null, catalog: Catalog? = null, gameSnapshot: GameSnapshot? = null,
    visitHistory: RegionVisitHistory = RegionVisitHistory.Loading,
    useDemoTripRecords: Boolean = ResultDemoOptions.USE_MOCK_TRIP_RECORDS,
    playConfetti: Boolean = false, onConfettiStarted: () -> Unit = {},
) {
    val presentation = remember(briefing, receipt, catalog, gameSnapshot, visitHistory, useDemoTripRecords) {
        regionResultPresentation(briefing, receipt, catalog, gameSnapshot, visitHistory, useDemoTripRecords)
    }
    MemoridePleosTheme {
        MemorideScreen(modifier, applyInsets) {
            if (!gate.canInteract) HeldMessage()
            else RegionResultPleosLayout(presentation, state, { onAction(BriefingAction.ISLAND) },
                scene, playConfetti, onConfettiStarted)
        }
    }
}

@Composable
private fun HeldMessage() {
    Column(Modifier.padding(32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("결과 확인이 잠시 보류됐어요", style = MaterialTheme.typography.headlineSmall.koreanPhrases())
        Text("이용 가능한 상태가 확인되면 결과를 볼 수 있어요.", color = MemorideColors.TextSecondary)
    }
}

@Composable
internal fun rememberAssetImage(path: String?): ImageBitmap? {
    val context = LocalContext.current
    return remember(path) {
        path?.let { p ->
            runCatching { context.assets.open(p).use { BitmapFactory.decodeStream(it)?.asImageBitmap() } }.getOrNull()
        }
    }
}

@Preview(name = "S05 first wide", widthDp = 1280, heightDp = 720)
@Preview(name = "S05 first split", widthDp = 841, heightDp = 1210)
@Preview(name = "S05 first narrow", widthDp = 640, heightDp = 720)
@Preview(name = "S05 first phone large text", widthDp = 411, heightDp = 891, fontScale = 2f)
@Composable
private fun RegionBriefingFirstVisitPreview() {
    MemorideTheme {
        RegionBriefingContent(RegionBriefingFixtures.firstVisitDemo(), RegionBriefingFixtures.allowed,
            BriefingActionState(), {})
    }
}

@Preview(name = "S05 revisit wide", widthDp = 1280, heightDp = 720)
@Preview(name = "S05 revisit portrait", widthDp = 720, heightDp = 1280)
@Composable
private fun RegionBriefingRevisitPreview() {
    MemorideTheme {
        RegionBriefingContent(RegionBriefingFixtures.revisitUserConfirmed(), RegionBriefingFixtures.allowed,
            BriefingActionState(), {})
    }
}

@Preview(name = "S05 ack pending narrow", widthDp = 640, heightDp = 720)
@Composable
private fun RegionBriefingPendingPreview() {
    MemorideTheme {
        RegionBriefingContent(RegionBriefingFixtures.firstVisitUserConfirmed(), RegionBriefingFixtures.allowed,
            BriefingActionState(pending = BriefingAction.ISLAND), {})
    }
}

@Preview(name = "S05 ack failed split", widthDp = 841, heightDp = 1210)
@Composable
private fun RegionBriefingFailedPreview() {
    MemorideTheme {
        RegionBriefingContent(RegionBriefingFixtures.revisitUserConfirmed(), RegionBriefingFixtures.allowed,
            BriefingActionState(failed = BriefingAction.ISLAND,
                error = "확인 내용을 저장하지 못했어요. 다시 시도해 주세요."), {})
    }
}
