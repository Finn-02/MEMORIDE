package io.kailand.feature.briefing

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kailand.R
import io.kailand.core.contract.DataSource
import io.kailand.core.ui.MemorideBrandLogo
import io.kailand.core.ui.MemoridePleosFonts
import io.kailand.core.ui.MemoridePleosTextStyle

private val ResultPrimary = Color(0xFF1F4E79)
private val ResultAccent = Color(0xFF2A7F9E)
private val ResultGreen = Color(0xFF3E9A63)
private val ResultText = Color(0xFF1E2B3A)
private val ResultSub = Color(0xFF5B6B7B)
private val ResultSurface = Color(0xFFF4F8FB)

@Composable
internal fun RegionResultPleosLayout(
    result: RegionResultPresentation?, state: BriefingActionState, onIsland: () -> Unit,
    scene: (@Composable (Boolean) -> Unit)?, playConfetti: Boolean, onConfettiStarted: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFFF3F6F9)).padding(24.dp)) {
        val wide = maxWidth + 48.dp >= 840.dp && maxWidth > maxHeight && LocalDensity.current.fontScale <= 1.3f
        if (wide) Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            ResultStage(result, scene, playConfetti, onConfettiStarted, Modifier.weight(3f).fillMaxHeight())
            ResultPanel(result, state, onIsland, Modifier.weight(2f).fillMaxHeight())
        } else Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ResultStage(result, scene, playConfetti, onConfettiStarted, Modifier.weight(1f).fillMaxWidth(), compact = true)
            ResultPanel(result, state, onIsland, Modifier.weight(1f).fillMaxWidth(), compact = true)
        }
    }
}

@Composable
private fun ResultStage(
    result: RegionResultPresentation?, scene: (@Composable (Boolean) -> Unit)?, playConfetti: Boolean,
    onConfettiStarted: () -> Unit, modifier: Modifier, compact: Boolean = false,
) {
    Column(modifier.clip(RoundedCornerShape(28.dp)).background(Brush.verticalGradient(
        listOf(Color(0xFFCDEBFA), Color(0xFFE4F5F6), Color(0xFFBFE3EA))))
        .padding(if (compact) 16.dp else 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MemorideBrandLogo(Modifier.fillMaxWidth(), markSize = 48.dp, wordmarkSize = 30)
                result?.let { ResultArrivalChip(it, Modifier.fillMaxWidth()) }
            }
        } else Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MemorideBrandLogo(Modifier.width(276.dp), markSize = 60.dp, wordmarkSize = 38)
            result?.let { ResultArrivalChip(it, Modifier.weight(1f)) }
        }
        Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(22.dp)), contentAlignment = Alignment.Center) {
            if (result != null && scene != null) scene(result.firstVisit)
            else ResultLabel("나의 섬", 30, family = MemoridePleosFonts.CharacterName)
            if (result?.firstVisit == true) ResultConfetti(playConfetti, onConfettiStarted)
        }
    }
}

@Composable
private fun ResultArrivalChip(result: RegionResultPresentation, modifier: Modifier) {
    Surface(modifier, shape = CircleShape, color = Color.White.copy(alpha = .94f), shadowElevation = 4.dp) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ResultIcon(if (result.firstVisit) R.drawable.inventory_ic_sparkles else R.drawable.world_ic_repeat,
                24.dp, if (result.firstVisit) ResultGreen else ResultAccent)
            val tail = if (result.firstVisit) "새 지역 발견!" else result.visitCount?.let { "${it}번째 방문" } ?: "재방문"
            ResultLabel("${result.regionName} 도착 · $tail", 24, Modifier.weight(1f), weight = FontWeight.Bold, maxLines = 2)
        }
    }
}

@Composable
private fun ResultTripRecords(result: RegionResultPresentation, compact: Boolean) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = .96f), shadowElevation = 4.dp) {
        Column(Modifier.padding(if (compact) 12.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ResultLabel("이번 여정 기록", 22, Modifier.weight(1f), weight = FontWeight.Bold)
                ResultIcon(if (result.directConfirmation) R.drawable.result_ic_shield else R.drawable.world_ic_info,
                    22.dp, ResultAccent)
                ResultLabel(when (result.source) {
                    DataSource.USER_CONFIRMED -> "직접 확인"
                    DataSource.DEMO -> "DEMO"
                    DataSource.PLEOS -> "PLEOS"
                    null -> "기록 정보"
                }, 18, color = ResultSub)
            }
            val icons = listOf(R.drawable.result_ic_route, R.drawable.result_ic_clock, R.drawable.result_ic_fuel, R.drawable.result_ic_gauge)
            result.stats.chunked(2).forEachIndexed { rowIndex, row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEachIndexed { columnIndex, stat ->
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(ResultSurface)
                            .padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ResultIcon(icons[rowIndex * 2 + columnIndex], 22.dp, ResultAccent)
                                ResultLabel(stat.label, 18, color = ResultSub)
                            }
                            ResultLabel(stat.value ?: "확인 안 됨", 28, weight = FontWeight.Bold)
                        }
                    }
                }
            }
            if (result.hasMock) ResultLabel("시연용 화면이라 기록은 임의로 넣은 값이에요.", 18,
                Modifier.fillMaxWidth(), color = ResultSub, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ResultPanel(
    result: RegionResultPresentation?, state: BriefingActionState, onIsland: () -> Unit,
    modifier: Modifier, compact: Boolean = false,
) {
    Surface(modifier, shape = RoundedCornerShape(28.dp), color = Color.White, shadowElevation = 8.dp) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
        val recordHeight = maxHeight * .32f
        val shortWindow = maxHeight < 600.dp * LocalDensity.current.fontScale
        val panelModifier = if (shortWindow) Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            else Modifier.fillMaxSize()
        Column(panelModifier.padding(if (compact) 16.dp else 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            val contentModifier = if (shortWindow) Modifier.fillMaxWidth()
                else Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
            Column(contentModifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (result == null) {
                    ResultLabel("이 기록은 새 결과 화면으로 표시할 수 없어요", 30, weight = FontWeight.Bold)
                    ResultLabel("섬으로 돌아가면 이전 기록 확인을 마칠 수 있어요.", 22, color = ResultSub)
                } else {
                    ResultVisitHeader(result)
                    Column(Modifier.padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ResultLabel(result.title, if (result.firstVisit) 40 else 42,
                            Modifier.semantics { heading() }, family = MemoridePleosFonts.CharacterName)
                        ResultLabel(result.summary, if (result.firstVisit) 22 else 23, color = ResultSub)
                    }
                    ResultSouvenirs(result, Modifier.fillMaxWidth())
                }
                if (state.pending != null || state.error != null) ResultLabel(state.error ?: "확인 내용을 처리하고 있어요…", 20,
                    Modifier.semantics { liveRegion = LiveRegionMode.Polite }, color = ResultSub)
            }
            // Anchor both visit variants to the same footer, outside the souvenir scroller.
            if (result != null) {
                val recordsModifier = if (shortWindow) Modifier.fillMaxWidth()
                    else Modifier.fillMaxWidth().heightIn(max = recordHeight).verticalScroll(rememberScrollState())
                Column(recordsModifier) {
                    ResultTripRecords(result, compact)
                }
                ResultGrowth(result)
            }
            ResultIslandButton(result, state, onIsland)
        }
        }
    }
}

@Composable
private fun ResultVisitHeader(result: RegionResultPresentation) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = if (result.firstVisit) Color(0xFFE3F5EA) else Color(0xFFE7F0F8)) {
            Row(Modifier.heightIn(min = 60.dp).padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ResultIcon(if (result.firstVisit) R.drawable.inventory_ic_sparkles else R.drawable.world_ic_repeat,
                    28.dp, if (result.firstVisit) ResultGreen else ResultPrimary)
                ResultLabel(if (result.firstVisit) "첫 방문" else "재방문", 26,
                    color = if (result.firstVisit) Color(0xFF2F8A55) else ResultPrimary, weight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ResultSouvenirs(result: RegionResultPresentation, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val title = when {
                result.newItems.isNotEmpty() -> "받은 기념품"
                result.collectedCount != null -> "${result.regionName}에서 모은 기념품"
                else -> "지역 기념품"
            }
            ResultLabel(title, if (result.firstVisit) 24 else 28, Modifier.weight(1f), weight = FontWeight.Bold)
            if (result.collectedCount != null && result.totalCount != null) {
                ResultLabel("${result.collectedCount}", 26, color = ResultAccent, weight = FontWeight.Bold)
                ResultLabel("/ ${result.totalCount}", 20, color = ResultSub)
            }
        }
        if (result.newItems.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                .background(Color(0xFFFFE8B5)).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ResultIcon(R.drawable.world_ic_gift, 32.dp, Color(0xFFB87720))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ResultLabel("첫 방문 선물이 도착했어요!", 24, color = Color(0xFF88551E),
                        family = MemoridePleosFonts.CharacterName)
                    ResultLabel("새 기념품 ${result.newItems.size}개를 선물로 받았어요", 18, color = Color(0xFF88551E))
                }
                ResultIcon(R.drawable.inventory_ic_sparkles, 28.dp, Color(0xFFB87720))
            }
        }
        // One visible row for up to three granted souvenirs; the outer panel owns scrolling.
        val souvenirs = result.newItems.ifEmpty { result.collectedItems }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            souvenirs.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { item ->
                        Surface(Modifier.weight(1f), shape = RoundedCornerShape(18.dp),
                            color = if (result.newItems.isNotEmpty()) Color(0xFFFFF8E8) else Color(0xFFF7F9FB),
                            border = BorderStroke(if (result.newItems.isNotEmpty()) 2.dp else 1.5.dp,
                                if (result.newItems.isNotEmpty()) Color(0xFFE7BB65) else Color(0xFFE3EAF1)),
                            shadowElevation = if (result.newItems.isNotEmpty()) 4.dp else 0.dp) {
                            Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (result.newItems.isNotEmpty()) ResultLabel("NEW", 18,
                                    Modifier.clip(CircleShape).background(Color(0xFFB87720))
                                        .padding(horizontal = 12.dp, vertical = 4.dp),
                                    color = Color.White, weight = FontWeight.Bold)
                                ResultThumbnail(item, 84.dp)
                                ResultLabel(item.name, 24, textAlign = TextAlign.Center,
                                    family = MemoridePleosFonts.CharacterName, maxLines = 2)
                            }
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            if (souvenirs.isEmpty()) ResultLabel("모은 기념품이 없어요.", 22, color = ResultSub)
        }
    }
}

@Composable
private fun ResultGrowth(result: RegionResultPresentation) {
    val stage = result.growthAfter
    val icon = when (stage) {
        2 -> R.drawable.island_ic_trees
        3 -> R.drawable.island_ic_house
        else -> R.drawable.island_ic_sprout_small
    }
    Row(Modifier.fillMaxWidth().heightIn(min = 104.dp).clip(RoundedCornerShape(18.dp))
        .background(if (result.grew) Color(0xFFE8F6EE) else ResultSurface).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
            .background(if (result.grew) ResultGreen else Color(0xFFE7F0F8)), contentAlignment = Alignment.Center) {
            ResultIcon(icon, 30.dp, if (result.grew) Color.White else ResultPrimary)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ResultLabel(when {
                result.grew -> "섬이 자랐어요!"
                stage != null -> "${resultStageLabel(stage)} · $stage/3단계"
                else -> result.originalGrowth
            }, 24, color = if (result.grew) Color(0xFF2F8A55) else ResultText, weight = FontWeight.Bold)
            val detail = when {
                result.grew && result.growthBefore == 0 -> "0 → $stage 단계"
                result.grew -> "${resultStageLabel(result.growthBefore!!)} → ${resultStageLabel(stage!!)}"
                stage != null -> "추가 성장 없음"
                else -> "성장 기록 확인 필요"
            }
            ResultLabel(detail, 20, color = if (result.grew) ResultGreen else ResultSub)
        }
    }
}

@Composable
private fun ResultIslandButton(result: RegionResultPresentation?, state: BriefingActionState, onIsland: () -> Unit) {
    val enabled = state.pending == null && !state.completed
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && enabled) .98f else 1f, tween(120), label = "result-island-press")
    Surface(onClick = onIsland, enabled = enabled, interactionSource = interaction,
        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp).scale(scale),
        shape = RoundedCornerShape(24.dp), color = Color.Transparent, shadowElevation = if (enabled) 8.dp else 0.dp) {
        val ink = if (enabled) Color.White else Color(0xFF8A98A6)
        val colors = if (enabled) listOf(ResultPrimary, Color(0xFF2A8FAE)) else listOf(Color(0xFFE9EEF3), Color(0xFFE9EEF3))
        Row(Modifier.background(Brush.horizontalGradient(colors)).padding(horizontal = 22.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Box(Modifier.size(60.dp).clip(CircleShape).background(ink.copy(alpha = .18f)), contentAlignment = Alignment.Center) {
                ResultIcon(R.drawable.character_ic_island, 32.dp, ink)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ResultLabel(if (state.failed == BriefingAction.ISLAND) "다시 시도 · 섬으로 가기" else "섬으로 가기", 30,
                    color = ink, weight = FontWeight.Bold)
                ResultLabel(if (result?.firstVisit == true || result?.grew == true) "자란 섬 보러 가기" else "나의 섬으로 돌아가기",
                    20, color = ink.copy(alpha = .85f))
            }
            ResultIcon(R.drawable.character_ic_arrow_right, 28.dp, ink)
        }
    }
}

@Composable
private fun ResultThumbnail(item: RegionBriefingSouvenir, size: Dp) {
    val image = rememberAssetImage(item.imagePath)
    Box(Modifier.size(size).clip(RoundedCornerShape(14.dp)).background(Color(0xFFFFF1D7)), contentAlignment = Alignment.Center) {
        if (image != null) Image(image, item.name, Modifier.fillMaxSize().padding(4.dp))
        else ResultIcon(R.drawable.world_ic_gift, 30.dp, ResultAccent)
    }
}

@Composable
private fun ResultConfetti(play: Boolean, onStarted: () -> Unit) {
    val eligible = remember { play }
    val progress = remember { Animatable(if (eligible) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (eligible) {
            onStarted()
            progress.animateTo(1f, tween(1500))
        }
    }
    val fraction = progress.value
    if (fraction < 1f) Canvas(Modifier.fillMaxSize()) {
        val colors = listOf(Color(0xFFF2C94C), Color(0xFF7ED39A), Color(0xFF8CC8EA), Color(0xFFF4A9B4), Color(0xFFC9B4E6))
        repeat(32) { index ->
            val x = ((index * 37 % 97) / 97f) * size.width
            val y = (fraction * 1.3f - (index % 5) * .08f) * size.height
            val point = Offset(x, y)
            rotate(index * 17f + fraction * 90f, point) {
                drawRect(colors[index % colors.size].copy(alpha = 1f - fraction), point, Size(8.dp.toPx(), 12.dp.toPx()))
            }
        }
    }
}

@Composable
private fun ResultIcon(resource: Int, size: Dp, color: Color) {
    Icon(painterResource(resource), null, Modifier.size(size), tint = color)
}

@Composable
private fun ResultLabel(
    text: String, size: Int, modifier: Modifier = Modifier, color: Color = ResultText,
    weight: FontWeight = FontWeight.Medium, family: FontFamily = MemoridePleosFonts.Body,
    textAlign: TextAlign? = null, maxLines: Int = Int.MAX_VALUE,
) {
    Text(text, modifier, fontFamily = family, fontSize = size.coerceAtLeast(18).sp,
        lineHeight = (size.coerceAtLeast(18) * 1.3f).sp, fontWeight = weight, color = color,
        textAlign = textAlign, maxLines = maxLines, overflow = TextOverflow.Ellipsis, style = MemoridePleosTextStyle)
}
