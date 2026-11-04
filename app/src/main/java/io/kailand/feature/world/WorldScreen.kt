package io.kailand.feature.world

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.kailand.core.contract.Catalog
import io.kailand.core.contract.GameSnapshot
import io.kailand.core.contract.InteractionGate
import io.kailand.core.contract.ItemType

@Composable
fun WorldScreen(
    snapshot: GameSnapshot,
    catalog: Catalog,
    gate: InteractionGate,
    onSelectRegion: (String) -> Unit,
    onOpenItems: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var foreground by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ -> foreground = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
        lifecycle.addObserver(observer)
        foreground = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        onDispose { lifecycle.removeObserver(observer) }
    }
    WorldContent(snapshot, catalog, gate, foreground, onSelectRegion, onOpenItems, modifier)
}

@Composable
internal fun WorldContent(
    snapshot: GameSnapshot, catalog: Catalog, gate: InteractionGate, foreground: Boolean,
    onSelectRegion: (String) -> Unit, onOpenItems: () -> Unit, modifier: Modifier = Modifier,
    initialSelectedId: String? = null,
) {
    var selectedId by rememberSaveable { mutableStateOf(initialSelectedId) }
    val view = worldPresentation(snapshot, catalog)
    val selected = view.regions.singleOrNull { it.id == selectedId }
    LaunchedEffect(catalog) { if (selectedId != null && selected == null) selectedId = null }
    if (LocalOnBackPressedDispatcherOwner.current != null) {
        BackHandler(enabled = selected != null) { selectedId = null }
    }
    val currentInputs by rememberUpdatedState(WorldInputs(snapshot, catalog, gate, foreground))
    val currentSelect by rememberUpdatedState(onSelectRegion)
    val currentOpen by rememberUpdatedState(onOpenItems)
    val actions = remember { WorldActions({ currentInputs }, { currentSelect(it) }, { currentOpen() }) }
    val fontScale = LocalDensity.current.fontScale
    Surface(modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth >= 640.dp && fontScale <= 1.2f
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("월드맵 · 지역 발견 보드", style = MaterialTheme.typography.headlineSmall)
                Text("게임 저장 상태 · 실제 도로 지도나 방문 인증이 아니에요.")
                if (!gate.canInteract || !foreground) {
                    Text("월드맵 이용 중단", style = MaterialTheme.typography.titleLarge)
                    Text(gate.reason ?: "이용 가능한 활성 화면에서 다시 확인해 주세요.")
                } else {
                    Text("기념품 보유 ${view.ownedCount} / 전체 ${view.totalCount}")
                    view.issues.forEach { Text("콘텐츠 오류: $it") }
                    Button(onClick = { actions.openItems() }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("보유 아이템 보기") }
                    if (view.regions.isEmpty()) Text("준비된 지역이 없어요.")
                    val cards: @Composable (Modifier) -> Unit = { cardModifier ->
                        Column(cardModifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            view.regions.forEach { region ->
                                OutlinedButton(onClick = {
                                    val latest = currentInputs
                                    if (latest.gate.canInteract && latest.foreground &&
                                        latest.catalog.regions.count { it.id == region.id } == 1) selectedId = region.id
                                }, enabled = region.id.isNotBlank() && view.regions.count { it.id == region.id } == 1,
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalAlignment = Alignment.CenterVertically) {
                                        RegionArtwork(region.discovered)
                                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(region.name.ifBlank { "지역 이름 오류" }, style = MaterialTheme.typography.titleMedium)
                                            Text(if (region.discovered) "발견함" else "미발견 · 상세 잠금")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (wide) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            cards(Modifier.weight(1f))
                            Column(Modifier.weight(1f)) { WorldDetail(selected, { selectedId = null }, actions) }
                        }
                    } else {
                        cards(Modifier.fillMaxWidth())
                        WorldDetail(selected, { selectedId = null }, actions)
                    }
                }
            }
        }
    }
}

/** Identical generic island silhouette for every Catalog region; never reveals locked content. */
@Composable
private fun RegionArtwork(discovered: Boolean) {
    Canvas(Modifier.size(width = 88.dp, height = 72.dp).clip(RoundedCornerShape(16.dp))) {
        val w = size.width
        val h = size.height
        val ink = Color(0xFF344B52)
        drawRect(Color(0xFF8CC6D7).copy(alpha = .28f))
        drawOval(Color(0xFFD8BA91), Offset(w * .12f, h * .36f), Size(w * .76f, h * .48f))
        drawOval(if (discovered) Color(0xFF9AC9A1) else Color(0xFFD6DDD4),
            Offset(w * .12f, h * .26f), Size(w * .76f, h * .48f))
        drawOval(Color(0xFFF4E7D1), Offset(w * .32f, h * .43f), Size(w * .36f, h * .17f))
        if (discovered) {
            drawCircle(ink, 9.dp.toPx(), Offset(w * .50f, h * .37f))
            drawLine(Color(0xFFF4E7D1), Offset(w * .45f, h * .37f), Offset(w * .49f, h * .42f), 2.dp.toPx())
            drawLine(Color(0xFFF4E7D1), Offset(w * .49f, h * .42f), Offset(w * .56f, h * .32f), 2.dp.toPx())
        } else {
            // Visible text also states the lock; color is never the sole status indication.
            drawArc(ink, 180f, 180f, false, Offset(w * .43f, h * .25f), Size(w * .14f, h * .24f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
            drawRoundRect(ink, Offset(w * .40f, h * .35f), Size(w * .20f, h * .20f),
                androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
        }
    }
}

@Composable
private fun WorldDetail(region: WorldRegion?, onClose: () -> Unit, actions: WorldActions) {
    if (region == null) {
        Text("지역을 선택하면 상세를 볼 수 있어요.")
        return
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(region.name, style = MaterialTheme.typography.titleLarge)
            OutlinedButton(onClick = onClose, modifier = Modifier.heightIn(min = 56.dp)) { Text("지역 상세 닫기") }
            region.issues.forEach { Text("지역 오류: $it") }
            if (region.discovered) {
                Text("발견한 지역 · ${region.theme}")
                if (region.items.isEmpty()) Text("등록된 기념품이 없어요.")
                region.items.forEach { item ->
                    Text("${item.name?.takeIf { it.isNotBlank() } ?: "기념품 정보 오류"} · ${when (item.type) {
                        ItemType.FOOD -> "음식"
                        ItemType.OUTFIT -> "의상"
                        ItemType.DECOR -> "장식"
                        null -> "종류 미확인"
                    }} · ${if (item.owned) "보유" else "미보유"}")
                    WorldItemImage(item.imagePath, item.name ?: "기념품 정보 미확인")
                    item.issues.forEach { Text("기념품 오류: $it") }
                }
            } else {
                Text("미발견 · 기념품 상세 잠금")
                Text("이 지역을 발견한 뒤 테마와 기념품을 확인할 수 있어요.")
            }
            if (!region.canPrepare) Text("콘텐츠 오류로 목적지 후보를 선택할 수 없어요.")
            Button(onClick = { actions.prepare(region.id) }, enabled = region.canPrepare,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("이 지역으로 여정 준비") }
            Text("목적지 후보만 전달해요. 여정 준비 화면에서 최종 확인해 주세요.")
        }
    }
}
