package io.kailand.feature.briefing

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kailand.core.contract.ArrivalReceipt
import io.kailand.core.contract.AvatarPreview
import io.kailand.core.contract.Catalog
import io.kailand.core.contract.InteractionGate
import io.kailand.core.contract.MutationResult
import io.kailand.core.ui.MemorideBrushes
import io.kailand.core.ui.MemorideColors
import io.kailand.core.ui.MemorideDimens
import io.kailand.core.ui.MemoridePanel
import io.kailand.core.ui.MemoridePrimaryButton
import io.kailand.core.ui.MemorideScreen
import io.kailand.core.ui.MemorideSecondaryButton
import io.kailand.core.ui.MemorideTheme
import io.kailand.core.ui.NewBriefingSlot
import kotlinx.coroutines.launch

/** UX-P01 S05 entry point matching [NewBriefingSlot]. A's callbacks include acknowledgement and
 * routing; this screen only waits for their result and keeps the result visible on failure.
 * [scene] lets A pass B's island scene for a revisit; without it a neutral placeholder is drawn.
 */
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
) {
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
        RegionBriefingContent(briefing, gate, state, dispatch, modifier, applyInsets, scene)
    }
}

/** Adapter for A's AppRoot wiring. */
val RegionBriefingSlot: NewBriefingSlot = { receipt, catalog, avatar, gate, island, select, modifier ->
    RegionBriefingScreen(receipt, catalog, avatar, gate, island, select, modifier)
}

/** Stateless S05 layout. A null briefing means the receipt belongs to A's legacy-compatible path. */
@Composable
internal fun RegionBriefingContent(
    briefing: RegionBriefing?,
    gate: InteractionGate,
    state: BriefingActionState,
    onAction: (BriefingAction) -> Unit,
    modifier: Modifier = Modifier,
    applyInsets: Boolean = true,
    scene: (@Composable (firstVisit: Boolean) -> Unit)? = null,
) {
    MemorideScreen(modifier, applyInsets) {
        if (!gate.canInteract) HeldMessage() else BoxWithConstraints(Modifier.fillMaxSize()) {
            val fontScale = LocalDensity.current.fontScale
            val width = maxWidth
            val wide = maxWidth >= MemorideDimens.WideBreakpoint && maxWidth > maxHeight && fontScale <= 1.2f
            val enabled = state.pending == null && !state.completed
            val picture: @Composable (Modifier) -> Unit = { m ->
                Box(m) {
                    if (briefing != null && scene != null) scene(briefing.firstVisit)
                    else ScenePlaceholder(briefing, Modifier.fillMaxSize())
                }
            }
            if (wide) {
                // Panel is at most 520dp (Figma) and never more than 55% of the content row;
                // the illustration takes the rest. Column counts use this same panel width.
                val panelWidth = minOf(520.dp, (width - 96.dp) * 0.55f)
                Row(
                    Modifier.fillMaxSize().padding(32.dp),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                ) {
                    picture(Modifier.weight(1f).fillMaxHeight())
                    BoxWithConstraints(Modifier.width(panelWidth).fillMaxHeight()) {
                        val viewport = maxHeight
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            // Tall window: actions sit at the panel bottom. Short window: everything scrolls.
                            Column(
                                Modifier.fillMaxWidth().heightIn(min = viewport),
                                verticalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    ResultBody(briefing, panelWidth, fontScale)
                                }
                                Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    ResultActions(briefing, state, enabled, onAction)
                                }
                            }
                        }
                    }
                }
            } else {
                val portrait = maxHeight > maxWidth
                val padding = if (portrait) 32.dp else 24.dp
                val sceneHeight = if (fontScale > 1.3f) 160.dp
                    else (maxHeight * if (portrait) 0.375f else 0.24f).coerceIn(160.dp, 480.dp)
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding),
                    verticalArrangement = Arrangement.spacedBy(if (portrait) 24.dp else 12.dp),
                ) {
                    picture(Modifier.fillMaxWidth().height(sceneHeight))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ResultBody(briefing, width - padding * 2, fontScale)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ResultActions(briefing, state, enabled, onAction)
                    }
                }
            }
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
private fun ResultBody(briefing: RegionBriefing?, panelWidth: Dp, fontScale: Float) {
    if (briefing == null) {
        Text("이 기록은 새 결과 화면으로 표시할 수 없어요", style = MaterialTheme.typography.titleMedium.koreanPhrases(),
            modifier = Modifier.semantics { heading() })
        Text("섬으로 돌아가면 이전 기록 확인을 마칠 수 있어요.", color = MemorideColors.TextSecondary)
    } else {
        ResultChip(if (briefing.firstVisit) "새 지역" else "재방문")
        Text(briefing.title, style = MaterialTheme.typography.headlineLarge.koreanPhrases(),
            modifier = Modifier.semantics { heading() })
        Text(briefing.summary, style = MaterialTheme.typography.bodyLarge.koreanPhrases(), color = MemorideColors.TextSecondary)
        Text(briefing.source, style = MaterialTheme.typography.labelMedium.koreanPhrases(), color = MemorideColors.TextSecondary)
        Text(briefing.growth, style = MaterialTheme.typography.bodyMedium, color = MemorideColors.TextPrimary)
        if (briefing.souvenirs.isNotEmpty()) {
            val perRow = if (panelWidth >= 168.dp * 3 * fontScale) 3 else 1
            Grid(briefing.souvenirs, perRow) { SouvenirRow(it, Modifier.fillMaxWidth()) }
        }
        Text("이번 여정 기록", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(top = 6.dp).semantics { heading() })
        val perRow = when {
            panelWidth >= 112.dp * 4 * fontScale -> 4
            panelWidth >= 112.dp * 2 * fontScale -> 2
            else -> 1
        }
        Grid(briefing.tripStats, perRow) { TripStat(it, Modifier.fillMaxWidth()) }
    }
}

/** Status line plus the two acknowledgement actions; kept together so a tall panel can bottom-align them. */
@Composable
private fun ResultActions(
    briefing: RegionBriefing?,
    state: BriefingActionState,
    enabled: Boolean,
    onAction: (BriefingAction) -> Unit,
) {
    Column(
        Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (state.pending != null) Text("확인 내용을 처리하고 있어요…", color = MemorideColors.TextSecondary)
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
    MemoridePrimaryButton(
        text = if (state.failed == BriefingAction.ISLAND) "다시 시도 · 섬으로 가기" else "섬으로 가기",
        onClick = { onAction(BriefingAction.ISLAND) },
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        supportingText = "나의 섬으로",
    )
    if (briefing != null) {
        MemorideSecondaryButton(
            text = if (state.failed == BriefingAction.SELECT_CHARACTER) "다시 시도 · 캐릭터 선택" else "캐릭터 선택",
            onClick = { onAction(BriefingAction.SELECT_CHARACTER) },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
        )
    }
}

@Composable
private fun <T> Grid(items: List<T>, perRow: Int, cell: @Composable (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(perRow).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { Box(Modifier.weight(1f)) { cell(it) } }
                repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ResultChip(label: String) {
    Text(
        label,
        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(MemorideColors.ChipBackground)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        color = MemorideColors.ChipText,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
    )
}

@Composable
private fun SouvenirRow(souvenir: RegionBriefingSouvenir, modifier: Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        val image = rememberAssetImage(souvenir.imagePath)
        Box(
            Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)).background(MemorideColors.SandLight),
            contentAlignment = Alignment.Center,
        ) {
            if (image != null) Image(image, contentDescription = null, modifier = Modifier.size(52.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(souvenir.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp))
            Text(itemTypeLabel(souvenir.type), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = MemorideColors.TextSecondary)
        }
    }
}

@Composable
private fun TripStat(stat: RegionBriefingStat, modifier: Modifier) {
    Column(
        modifier.heightIn(min = 80.dp).clip(RoundedCornerShape(12.dp)).background(MemorideColors.SurfaceSubtle)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(stat.label, style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp), color = MemorideColors.TextSecondary)
        Text(stat.value, style = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp, lineHeight = 34.sp))
        if (stat.value == "—") Text("확인 안 됨", style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
            color = MemorideColors.TextSecondary)
    }
}

/** Neutral stand-in until approved regional artwork or B's island scene is connected. */
@Composable
private fun ScenePlaceholder(briefing: RegionBriefing?, modifier: Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(24.dp)).background(MemorideBrushes.Sea),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.fillMaxWidth(0.65f).fillMaxHeight(0.42f).clip(RoundedCornerShape(50))
                .background(MemorideColors.Sand),
        )
        if (briefing != null) Text(
            briefing.regionName,
            style = MaterialTheme.typography.headlineMedium,
            color = MemorideColors.Navy,
        )
    }
}

@Composable
private fun rememberAssetImage(path: String?): ImageBitmap? {
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
