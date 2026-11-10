package io.kailand.feature.world

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalDensity
import io.kailand.R
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.kailand.core.contract.*
import io.kailand.core.ui.*

/** NewWorldSlot consumer. A owns routing/insets; this screen never requests travel or writes saves. */
@Composable
fun WorldMapScreen(
    snapshot: GameSnapshot,
    catalog: Catalog,
    history: RegionVisitHistory,
    gate: InteractionGate,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var foreground by remember(lifecycle) {
        mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ ->
            foreground = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    WorldMapContent(snapshot, catalog, history, gate, foreground, onClose, modifier)
}

/** Reject ambiguous/unknown discovery IDs; ownership alone never reveals a region. */
internal fun discoveredWorldRegions(snapshot: GameSnapshot, catalog: Catalog): List<WorldRegion> {
    val counts = catalog.regions.groupingBy { it.id }.eachCount()
    return worldPresentation(snapshot, catalog).regions.filter {
        it.discovered && it.id.isNotBlank() && it.name.isNotBlank() && counts[it.id] == 1
    }
}

/** A's measured 841x1210 split window is portrait, despite crossing the width breakpoint. */
internal fun worldMapUsesColumns(width: Float, height: Float, fontScale: Float): Boolean =
    width >= 840f && width > height && fontScale <= 1.3f

@Composable
internal fun WorldMapContent(
    snapshot: GameSnapshot, catalog: Catalog, history: RegionVisitHistory,
    gate: InteractionGate, foreground: Boolean, onClose: () -> Unit,
    modifier: Modifier = Modifier, initialSelectedId: String? = null,
) {
    var selectedId by rememberSaveable { mutableStateOf(initialSelectedId) }
    val regions = discoveredWorldRegions(snapshot, catalog)
    val selected = regions.singleOrNull { it.id == selectedId }
    LaunchedEffect(regions.map { it.id }) {
        if (selected == null) selectedId = null
    }
    val allowed = gate.canInteract && foreground
    val latestAllowed by rememberUpdatedState(allowed)
    val latestRegions by rememberUpdatedState(regions)
    val latestClose by rememberUpdatedState(onClose)
    if (LocalOnBackPressedDispatcherOwner.current != null) {
        BackHandler(enabled = allowed) {
            if (latestAllowed) {
                if (selected != null) selectedId = null else latestClose()
            }
        }
    }
    val fontScale = LocalDensity.current.fontScale
    MemorideScreen(modifier, applyInsets = false) {
        if (!allowed) {
            Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Text("월드맵 이용 중단", style = MaterialTheme.typography.headlineSmall)
                Text(gate.reason ?: "이용 가능한 활성 화면에서 다시 확인해 주세요.")
            }
        } else BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = worldMapUsesColumns(maxWidth.value, maxHeight.value, fontScale)
            val inlineHeader = maxWidth >= 640.dp && fontScale <= 1.3f
            val boardHeight = if (wide) (maxHeight - 108.dp).coerceAtLeast(260.dp)
                else (maxWidth * .8f).coerceIn(360.dp, 560.dp)
            val total = catalog.regions.map { it.id }.filter { it.isNotBlank() }.distinct().size
            val hint = "발견한 지역 ${regions.size} / $total" + when {
                selected != null -> ""
                regions.isEmpty() -> " · 다녀온 지역이 지도에 아이콘으로 생겨요"
                else -> " · 아이콘을 누르면 자세히 볼 수 있어요"
            }
            val close: @Composable () -> Unit = {
                MemorideIconButton("월드맵 닫기", { if (latestAllowed) latestClose() }) {
                    Image(painterResource(R.drawable.memoride_close), null, Modifier.size(24.dp))
                }
            }
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("월드맵·도감", style = MaterialTheme.typography.headlineSmall,
                        modifier = if (inlineHeader) Modifier else Modifier.weight(1f))
                    if (inlineHeader) Text(hint, Modifier.weight(1f), color = MemorideColors.TextSecondary)
                    close()
                }
                if (!inlineHeader) Text(hint, color = MemorideColors.TextSecondary)
                val board: @Composable (Modifier) -> Unit = { boardModifier ->
                    WorldBoard(regions, catalog, history, selectedId, { id ->
                        if (latestAllowed && latestRegions.any { it.id == id }) {
                            selectedId = if (selectedId == id) null else id
                        }
                    }, boardModifier.height(boardHeight))
                }
                if (wide && selected != null) Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    board(Modifier.weight(2f))
                    WorldMapDetail(selected, history, { if (latestAllowed) selectedId = null },
                        Modifier.weight(1f).height(boardHeight).verticalScroll(rememberScrollState()))
                } else {
                    board(Modifier.fillMaxWidth())
                    selected?.let {
                        WorldMapDetail(it, history, { if (latestAllowed) selectedId = null }, Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun WorldMapDetail(region: WorldRegion, history: RegionVisitHistory, onClose: () -> Unit,
    modifier: Modifier = Modifier) {
    val visits = worldVisitDisplay(region.id, history)
    val ownedItems = region.items.filter { it.owned }
    MemoridePanel(modifier) {
        Text("선택한 지역", color = MemorideColors.TextSecondary, style = MaterialTheme.typography.labelLarge)
        Text(region.name, style = MaterialTheme.typography.headlineSmall)
        Text(region.theme, color = MemorideColors.TextSecondary)
        BoxWithConstraints {
            val compact = maxWidth < 320.dp || LocalDensity.current.fontScale > 1.3f
            val count: @Composable () -> Unit = {
                Column(Modifier.background(MemorideColors.SurfaceSubtle,
                    androidx.compose.foundation.shape.RoundedCornerShape(14.dp)).padding(12.dp)) {
                    Text("방문 횟수", style = MaterialTheme.typography.labelMedium)
                    Text(visits.count, style = MaterialTheme.typography.titleLarge)
                }
            }
            val dates: @Composable () -> Unit = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("첫 방문  ${visits.firstDate}")
                    Text("최근 방문  ${visits.lastDate}")
                }
            }
            if (compact) Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { count(); dates() }
            else Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                count(); Column(Modifier.weight(1f)) { dates() }
            }
        }
        visits.notice?.let { Text(it, color = MemorideColors.TextSecondary) }
        HorizontalDivider(color = MemorideColors.BorderDefault)
        Text("모은 기념품 ${ownedItems.size}개", style = MaterialTheme.typography.titleMedium)
        if (ownedItems.isEmpty()) Text("모은 기념품이 없어요.")
        ownedItems.forEach { item ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                WorldItemImage(item.imagePath, item.name ?: "기념품 정보 미확인")
                Column(Modifier.weight(1f)) {
                    Text(item.name?.takeIf { it.isNotBlank() } ?: "기념품 정보 미확인",
                        style = MaterialTheme.typography.titleMedium)
                    Text(when (item.type) {
                        ItemType.FOOD -> "음식"
                        ItemType.OUTFIT -> "의상"
                        ItemType.DECOR -> "장식"
                        null -> "종류 미확인"
                    }, color = MemorideColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }
            item.issues.forEach { Text("기념품 정보 오류: $it") }
        }
        MemorideTextButton("지역 상세 닫기", onClose)
    }
}
