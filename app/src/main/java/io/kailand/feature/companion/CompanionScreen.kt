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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
            val mapHeight = if (hidden) (maxHeight * 0.5f).coerceIn(240.dp, 480.dp) else 200.dp
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
                        // Unmount the whole avatar panel while hidden; no empty slot reserves space.
                        JourneyMap(view, Modifier.fillMaxWidth(), mapHeight)
                        if (view.showAvatar) AvatarPanel(view, avatar, avatarSlot, Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun JourneyMap(view: CompanionPresentation, modifier: Modifier, mapHeight: Dp = 200.dp) {
    Card(modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("모의 경로 예시", style = MaterialTheme.typography.titleMedium)
            Canvas(
                Modifier.fillMaxWidth().height(mapHeight).clip(RoundedCornerShape(20.dp)).semantics {
                    contentDescription = "실제 위치와 연결되지 않은 정적 도형 경로"
                },
            ) {
                // C-owned static map artwork only. B's suggested art colors do not replace the theme.
                val water = Color(0xFF8CC6D7)
                val meadow = Color(0xFF9AC9A1)
                val cream = Color(0xFFF4E7D1)
                val sand = Color(0xFFD8BA91)
                val ink = Color(0xFF344B52)
                val leaf = Color(0xFF5A8264)
                val coral = Color(0xFFEE967B)
                val w = size.width
                val h = size.height
                drawRect(water.copy(alpha = 0.35f))
                // A quiet shoreline and large soft shapes; no geographic or current-position claim.
                val shore = Path().apply {
                    moveTo(-w * .05f, h * .08f)
                    cubicTo(w * .25f, -h * .12f, w * .50f, h * .16f, w * .73f, h * .06f)
                    cubicTo(w * 1.08f, -h * .04f, w * 1.05f, h * .58f, w * .88f, h * .76f)
                    cubicTo(w * .75f, h * 1.10f, w * .38f, h * .88f, -w * .05f, h * 1.08f)
                    close()
                }
                drawPath(shore, cream, style = Stroke(width = 13.dp.toPx(), cap = StrokeCap.Round))
                drawPath(shore, meadow)
                drawOval(leaf.copy(alpha = .10f), Offset(w * .04f, h * .17f), Size(w * .38f, h * .50f))
                drawOval(leaf.copy(alpha = .10f), Offset(w * .61f, h * .42f), Size(w * .28f, h * .37f))
                val start = Offset(w * .15f, h * .76f)
                val end = Offset(w * .79f, h * .23f)
                val route = Path().apply {
                    moveTo(start.x, start.y)
                    cubicTo(w * .58f, h * .80f, w * .27f, h * .30f, w * .53f, h * .32f)
                    cubicTo(w * .64f, h * .34f, w * .66f, h * .20f, end.x, end.y)
                }
                drawPath(route, sand, style = Stroke(width = 22.dp.toPx(), cap = StrokeCap.Round))
                drawPath(route, cream, style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round))
                // Fixed landscape marks, not collectible items, growth state or interactive map points.
                for (point in listOf(Offset(w * .20f, h * .29f), Offset(w * .70f, h * .66f), Offset(w * .81f, h * .53f))) {
                    drawRoundRect(sand, point + Offset(-2.dp.toPx(), 0f), Size(4.dp.toPx(), 20.dp.toPx()), CornerRadius(2.dp.toPx()))
                    drawCircle(leaf, 12.dp.toPx(), point)
                    drawCircle(meadow, 7.dp.toPx(), point + Offset(-4.dp.toPx(), -4.dp.toPx()))
                }
                drawCircle(ink, 11.dp.toPx(), start)
                drawCircle(cream, 6.dp.toPx(), start)
                drawLine(ink, end + Offset(0f, 9.dp.toPx()), end + Offset(0f, -21.dp.toPx()), 3.dp.toPx(), StrokeCap.Round)
                val flag = Path().apply {
                    moveTo(end.x, end.y - 21.dp.toPx())
                    lineTo(end.x + 18.dp.toPx(), end.y - 15.dp.toPx())
                    lineTo(end.x, end.y - 8.dp.toPx())
                    close()
                }
                drawPath(flag, coral)
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
