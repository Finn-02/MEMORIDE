package io.kailand.feature.world

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kailand.R
import io.kailand.core.contract.Catalog
import io.kailand.core.contract.ItemType
import io.kailand.core.contract.RegionVisitHistory
import io.kailand.core.ui.MemorideBrandLogo
import io.kailand.core.ui.MemoridePleosTextStyle
import io.kailand.core.ui.MemoridePleosFonts

internal object WorldColors {
    val Primary = Color(0xFF1F4E79)
    val Accent = Color(0xFF2A7F9E)
    val Text = Color(0xFF1E2B3A)
    val Sub = Color(0xFF5B6B7B)
    val Muted = Color(0xFF8A98A6)
    val Border = Color(0xFFE3EAF1)
    val Surface = Color(0xFFF4F8FB)
    val Gradient = Brush.horizontalGradient(listOf(Primary, Color(0xFF2A8FAE)))
}

@Composable
internal fun WorldText(
    text: String, size: Int = 18, modifier: Modifier = Modifier,
    color: Color = WorldColors.Text, weight: FontWeight = FontWeight.Medium,
    textAlign: TextAlign? = null,
    fontFamily: FontFamily = MemoridePleosFonts.Body,
) {
    Text(text, modifier, fontFamily = fontFamily, fontSize = size.coerceAtLeast(18).sp,
        lineHeight = (size.coerceAtLeast(18) * 1.3f).sp, fontWeight = weight, color = color,
        textAlign = textAlign, style = MemoridePleosTextStyle)
}

@Composable
internal fun WorldIcon(resource: Int, size: Dp, tint: Color = WorldColors.Primary) {
    Icon(painterResource(resource), null, Modifier.size(size), tint = tint)
}

@Composable
internal fun WorldPleosLayout(
    allRegions: List<WorldRegion>, discovered: List<WorldRegion>, total: Int,
    catalog: Catalog, history: RegionVisitHistory, selected: WorldRegion?,
    onSelect: (String) -> Unit, onList: () -> Unit, onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFEAF4FB), Color(0xFFFFF6EC))))) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 24.dp)) {
            val panelWidth = (maxWidth * .43f).coerceAtMost(520.dp)
            val wide = worldMapUsesColumns(maxWidth.value + 48f, maxHeight.value + 44f, LocalDensity.current.fontScale)
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                WorldHeader(onClose)
                // The board and panel never branch on selection: A/B keep identical map constraints.
                if (wide) {
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        WorldBoard(allRegions, catalog, history, selected?.id, onSelect,
                            Modifier.weight(1f).fillMaxHeight())
                        WorldRightPanel(allRegions, discovered, total, history, selected, onSelect, onList,
                            Modifier.width(panelWidth).fillMaxHeight())
                    }
                } else {
                    Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        WorldBoard(allRegions, catalog, history, selected?.id, onSelect,
                            Modifier.weight(1f).fillMaxWidth())
                        WorldRightPanel(allRegions, discovered, total, history, selected, onSelect, onList,
                            Modifier.weight(1f).fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun WorldHeader(onClose: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val close: @Composable () -> Unit = {
            Surface(onClick = onClose, modifier = Modifier.widthIn(min = 132.dp).heightIn(min = 72.dp).semantics { contentDescription = "월드맵 닫기" },
                shape = RoundedCornerShape(18.dp), color = Color.White) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                    WorldIcon(R.drawable.inventory_ic_close, 32.dp, WorldColors.Text)
                    WorldText("닫기", 24, weight = FontWeight.Bold)
                }
            }
        }
        val title: @Composable () -> Unit = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(60.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFE7F0F8)),
                    contentAlignment = Alignment.Center) { WorldIcon(R.drawable.island_ic_map, 36.dp) }
                WorldText("월드맵·도감", 39, Modifier.semantics { heading() },
                    weight = FontWeight.Normal, fontFamily = MemoridePleosFonts.CharacterName)
            }
        }
        if (maxWidth >= 1040.dp) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                MemorideBrandLogo(Modifier.width(320.dp), markSize = 72.dp, wordmarkSize = 44)
                Box(Modifier.width(2.dp).height(32.dp).background(Color(0xFFC9D6E2)))
                title()
                Spacer(Modifier.weight(1f))
                close()
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MemorideBrandLogo(Modifier.weight(1f), markSize = 72.dp, wordmarkSize = 44)
                    close()
                }
                title()
            }
        }
    }
}

@Composable
private fun WorldRightPanel(
    allRegions: List<WorldRegion>, discovered: List<WorldRegion>, total: Int, history: RegionVisitHistory,
    selected: WorldRegion?, onSelect: (String) -> Unit, onList: () -> Unit, modifier: Modifier,
) {
    Surface(modifier.shadow(12.dp, RoundedCornerShape(28.dp),
        ambientColor = Color(0x121A334D), spotColor = Color(0x121A334D)),
        shape = RoundedCornerShape(28.dp), color = Color.White) {
        AnimatedContent(targetState = selected?.id, modifier = Modifier.fillMaxSize().padding(24.dp).clipToBounds(),
            transitionSpec = {
                val direction = if (targetState == null) -1 else 1
                ((fadeIn(tween(200)) + slideInHorizontally(tween(200)) { direction * it / 12 }) togetherWith
                    (fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { -direction * it / 12 })).using(null)
            }, label = "world-panel") { regionId ->
            val region = discovered.singleOrNull { it.id == regionId }
            if (region == null) WorldCollectionList(allRegions, discovered.size, total, history, onSelect)
            else WorldRegionDetail(region, history, onList)
        }
    }
}

private fun worldObjectParticle(text: String): String {
    val syllable = text.lastOrNull()?.code?.minus(0xAC00)
    return if (syllable != null && syllable in 0..11171 && syllable % 28 == 0) "를" else "을"
}

@Composable
private fun WorldCollectionList(
    regions: List<WorldRegion>, discoveredCount: Int, total: Int, history: RegionVisitHistory, onSelect: (String) -> Unit,
) {
    val shownRegions = remember(regions) { regions.sortedByDescending { it.discovered } }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            WorldIcon(R.drawable.world_ic_book, 36.dp)
            WorldText("발견한 지역", 36, weight = FontWeight.Normal, fontFamily = MemoridePleosFonts.CharacterName)
            Spacer(Modifier.weight(1f))
            Text(buildAnnotatedString {
                withStyle(SpanStyle(color = WorldColors.Accent, fontSize = 36.sp)) { append("$discoveredCount") }
                withStyle(SpanStyle(color = Color(0xFF9AA8B6), fontSize = 28.sp)) { append(" / $total") }
            }, fontFamily = MemoridePleosFonts.Body, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 47.sp)
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(shownRegions, key = { it.id }) { region ->
                val owned = region.items.count { it.owned }
                val representative = region.items.firstOrNull { it.type == ItemType.FOOD && !it.name.isNullOrBlank() }
                    ?.name ?: region.items.firstOrNull { !it.name.isNullOrBlank() }?.name ?: "기념품"
                val subtitle = if (region.discovered) "방문 ${worldVisitDisplay(region.id, history).count} · 기념품 ${owned}개"
                    else "가면 ${representative}${worldObjectParticle(representative)} 얻어요"
                Surface(onClick = { onSelect(region.id) }, enabled = region.discovered,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 108.dp),
                    shape = RoundedCornerShape(20.dp), color = if (region.discovered) Color(0xFFEEF6FC) else Color(0xFFF7F9FB),
                    border = BorderStroke(if (region.discovered) 2.dp else 1.5.dp,
                        if (region.discovered) Color(0xFFBFD9EA) else WorldColors.Border)) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                        Box(Modifier.size(60.dp).clip(RoundedCornerShape(16.dp))
                            .background(if (region.discovered) WorldColors.Primary else Color(0xFFE9EEF3)),
                            contentAlignment = Alignment.Center) {
                            WorldIcon(if (region.discovered) R.drawable.world_ic_pin else R.drawable.world_ic_lock,
                                32.dp, if (region.discovered) Color.White else WorldColors.Muted)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            WorldText(region.name, 32, color = if (region.discovered) WorldColors.Text else WorldColors.Sub, weight = FontWeight.Bold)
                            WorldText(subtitle, 24, color = if (region.discovered) WorldColors.Accent else Color(0xFF7A8A99))
                        }
                        if (region.discovered) WorldIcon(R.drawable.world_ic_chevron_right, 32.dp)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(RoundedCornerShape(20.dp))
            .background(WorldColors.Surface).padding(horizontal = 22.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            WorldIcon(R.drawable.inventory_ic_info, 32.dp, WorldColors.Accent)
            WorldText("지역이나 핀을 누르면 기록과 기념품을\n자세히 볼 수 있어요.", 23,
                modifier = Modifier.weight(1f), color = Color(0xFF4A5B6C))
        }
    }
}

@Composable
private fun WorldRegionDetail(region: WorldRegion, history: RegionVisitHistory, onList: () -> Unit) {
    val visits = worldVisitDisplay(region.id, history)
    val ownedItems = region.items.filter { it.owned }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(25.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Surface(onClick = onList, modifier = Modifier.heightIn(min = 64.dp),
                shape = RoundedCornerShape(16.dp), color = Color(0xFFF2F5F8)) {
                Row(Modifier.padding(horizontal = 22.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WorldIcon(R.drawable.world_ic_chevron_left, 30.dp, WorldColors.Text)
                    WorldText("목록", 24, weight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.weight(1f))
            WorldText("발견지역", 24, modifier = Modifier.clip(CircleShape).background(Color(0xFFE3F5EA))
                .padding(horizontal = 18.dp, vertical = 8.dp), color = Color(0xFF2F8A55), weight = FontWeight.Bold)
        }
        // Add 3dp to the shared 25dp section gap: list controls to region card = 28dp.
        Column(Modifier.fillMaxWidth().padding(top = 3.dp).clip(RoundedCornerShape(22.dp)).background(WorldColors.Gradient)
            .padding(horizontal = 24.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WorldIcon(R.drawable.world_ic_pin, 36.dp, Color.White)
                WorldText(region.name, 36, color = Color.White, weight = FontWeight.Bold)
            }
            WorldText(region.theme, 22, color = Color.White.copy(alpha = .85f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            WorldStat(R.drawable.world_ic_repeat, "방문 횟수", visits.count, Modifier.weight(1f))
            WorldStat(R.drawable.world_ic_gift, "모은 기념품", "${ownedItems.size}개", Modifier.weight(1f))
        }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(WorldColors.Surface)
            .padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            WorldVisitDate("첫 방문", visits.firstDate)
            WorldVisitDate("최근 방문", visits.lastDate)
            visits.notice?.let { WorldText(it, 21, color = WorldColors.Sub) }
        }
        WorldText("모은 기념품", 26, weight = FontWeight.Bold)
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (ownedItems.isEmpty()) item { WorldText("모은 기념품이 없어요.", 24, color = WorldColors.Sub) }
            items(ownedItems, key = { it.id }) { item ->
                Surface(modifier = Modifier.fillMaxWidth().heightIn(min = 104.dp),
                    shape = RoundedCornerShape(18.dp), color = Color.White, border = BorderStroke(1.5.dp, WorldColors.Border)) {
                    Row(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Box(Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFEAF4FB)),
                            contentAlignment = Alignment.Center) {
                            WorldItemImage(item.imagePath, item.name ?: "기념품 정보 미확인", size = 64.dp)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            WorldText(item.name?.takeIf { it.isNotBlank() } ?: "기념품 정보 미확인", 27, weight = FontWeight.Bold)
                            WorldText(when (item.type) {
                                ItemType.FOOD -> "음식"
                                ItemType.OUTFIT -> "의상"
                                ItemType.DECOR -> "장식"
                                null -> "종류 미확인"
                            }, 21, modifier = Modifier.clip(CircleShape).background(Color(0xFFEEF2F6))
                                .padding(horizontal = 10.dp, vertical = 4.dp), color = WorldColors.Sub)
                        }
                    }
                }
                item.issues.forEach { WorldText("기념품 정보 오류: $it", 22, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun WorldStat(icon: Int, label: String, value: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(WorldColors.Surface)
        .padding(horizontal = 16.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            WorldIcon(icon, 24.dp, WorldColors.Accent)
            WorldText(label, 22, color = WorldColors.Sub)
        }
        WorldText(value, 31, modifier = Modifier.fillMaxWidth(),
            weight = FontWeight.Bold, textAlign = TextAlign.End)
    }
}

@Composable
private fun WorldVisitDate(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        WorldIcon(R.drawable.world_ic_calendar, 24.dp, WorldColors.Accent)
        WorldText(label, 21, color = WorldColors.Sub)
        WorldText(value, 21, modifier = Modifier.weight(1f), weight = FontWeight.Bold, textAlign = TextAlign.End)
    }
}
