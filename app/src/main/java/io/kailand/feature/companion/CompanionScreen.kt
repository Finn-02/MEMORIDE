package io.kailand.feature.companion

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.kailand.core.contract.AvatarPreview
import io.kailand.core.contract.InteractionGate
import io.kailand.core.contract.JourneySnapshot
import io.kailand.core.ui.AvatarSlot

/** AppRoot supplies immutable inputs and B's renderer. C owns only local visibility. */
@Composable
fun CompanionScreen(
    journey: JourneySnapshot,
    avatar: AvatarPreview,
    gate: InteractionGate,
    avatarSlot: AvatarSlot,
    onBack: () -> Unit,
    onHideChanged: (Boolean) -> Unit,
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
        foreground = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        onDispose { lifecycle.removeObserver(observer) }
    }
    CompanionContent(journey, avatar, gate, avatarSlot, foreground, onBack, onHideChanged, modifier)
}

@Composable
internal fun CompanionContent(
    journey: JourneySnapshot,
    avatar: AvatarPreview,
    gate: InteractionGate,
    avatarSlot: AvatarSlot,
    foreground: Boolean,
    onBack: () -> Unit,
    onHideChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var hidden by rememberSaveable { mutableStateOf(false) }
    val view = companionPresentation(journey, gate, foreground, hidden)
    val latestGate by rememberUpdatedState(gate)
    val latestForeground by rememberUpdatedState(foreground)
    val latestHideChanged by rememberUpdatedState(onHideChanged)
    val fontScale = LocalDensity.current.fontScale
    Surface(modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth >= 640.dp && fontScale <= 1.2f
            val mapHeight = if (hidden) (maxHeight * 0.5f).coerceIn(240.dp, 480.dp) else 176.dp
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                  OutlinedButton(onClick = onBack, modifier = Modifier.heightIn(min = 56.dp)) { Text("뒤로") }
                Text("${view.source} · 동행 모의 화면", style = MaterialTheme.typography.headlineSmall)
                Text("주차 상태에서 체험하는 자체 모의 지도 · 실제 길 안내가 아니에요.")
                if (view.blockedReason != null) {
                    Text("동행 표시 중단", style = MaterialTheme.typography.titleLarge)
                    Text(view.blockedReason)
                } else {
                    // Keep visibility control before the scrollable map/avatar content in small windows.
                    Button(
                        onClick = {
                            // Recheck current inputs; a previously enabled button cannot reopen a blocked slot.
                            if (latestGate.canInteract && latestForeground) {
                                hidden = !hidden
                                latestHideChanged(hidden)
                            }
                        },
                        enabled = view.canToggleVisibility,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    ) { Text(if (hidden) "캐릭터 다시 표시" else "캐릭터 숨기기") }
                    Text(view.destination, style = MaterialTheme.typography.titleLarge)
                    Text(view.phase, style = MaterialTheme.typography.titleMedium)
                    Text(view.notice)
                    if (wide && view.showAvatar) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            JourneyMap(view, Modifier.weight(1f))
                            AvatarPanel(view, avatar, avatarSlot, Modifier.weight(0.55f))
                        }
                    } else {
                        // Hidden companions do not reserve a card or a column in the layout.
                        JourneyMap(view, Modifier.fillMaxWidth(), mapHeight)
                        if (view.showAvatar) AvatarPanel(view, avatar, avatarSlot, Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun JourneyMap(view: CompanionPresentation, modifier: Modifier, mapHeight: Dp = 176.dp) {
    val road = MaterialTheme.colorScheme.primary
    val land = MaterialTheme.colorScheme.tertiaryContainer
    Card(modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("모의 경로 예시", style = MaterialTheme.typography.titleMedium)
            Canvas(
                Modifier.fillMaxWidth().height(mapHeight).semantics {
                    contentDescription = "실제 위치와 연결되지 않은 정적 도형 경로"
                },
            ) {
                drawCircle(land, size.minDimension * 0.30f, Offset(size.width * 0.3f, size.height * 0.4f))
                drawCircle(land, size.minDimension * 0.23f, Offset(size.width * 0.75f, size.height * 0.7f))
                val start = Offset(size.width * 0.12f, size.height * 0.82f)
                val end = Offset(size.width * 0.86f, size.height * 0.18f)
                val path = Path().apply {
                    moveTo(start.x, start.y)
                    lineTo(size.width * 0.4f, size.height * 0.65f)
                    lineTo(size.width * 0.4f, size.height * 0.35f)
                    lineTo(end.x, end.y)
                }
                drawPath(path, road, style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round))
                drawCircle(road, 9.dp.toPx(), start)
                drawCircle(road, 9.dp.toPx(), end)
            }
            view.distance?.let { Text(it) }
            view.elapsed?.let { Text(it) }
            if (view.distance == null && view.elapsed == null) Text("거리·시간 정보 미확인")
        }
    }
}

@Composable
private fun AvatarPanel(
    view: CompanionPresentation,
    avatar: AvatarPreview,
    avatarSlot: AvatarSlot,
    modifier: Modifier,
) {
    Card(modifier) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("내 캐릭터", style = MaterialTheme.typography.titleMedium)
            Box(
                Modifier.sizeIn(maxWidth = 160.dp).fillMaxWidth().aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (view.showAvatar) avatarSlot(avatar, "idle", Modifier.fillMaxSize())
                else Text("캐릭터 숨김")
            }
            Text("섬에서 선택한 외형을 함께 사용해요.")
        }
    }
}
