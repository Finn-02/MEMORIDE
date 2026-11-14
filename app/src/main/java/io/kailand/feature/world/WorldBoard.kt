package io.kailand.feature.world

import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.kailand.R
import io.kailand.core.contract.Catalog
import io.kailand.core.contract.RegionVisitHistory
import kotlin.math.min

/** Same 640-square equirectangular projection as scripts/build_world_map.py. */
internal fun worldMapPoint(latitude: Double, longitude: Double): Pair<Float, Float>? {
    if (!latitude.isFinite() || !longitude.isFinite() || latitude !in 32.8..39.0 || longitude !in 124.4..132.0) return null
    return Pair(((12.5735 + (longitude - 124.4) * 80.9016994) / 640).toFloat(),
        ((10 + (39.0 - latitude) * 100) / 640).toFloat())
}

@Composable
internal fun WorldBoard(
    regions: List<WorldRegion>, catalog: Catalog, history: RegionVisitHistory,
    selectedId: String?, onSelect: (String) -> Unit, modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.clip(RoundedCornerShape(28.dp)).background(
        Brush.verticalGradient(listOf(Color(0xFFC4E8F1), Color(0xFF9FD3E3))))) {
        val side = min(maxWidth.value, maxHeight.value).dp
        val left = (maxWidth - side) / 2
        val top = (maxHeight - side) / 2
        val fontScale = LocalDensity.current.fontScale
        val markerWidth = if (fontScale > 1.3f) 176.dp else 136.dp
        val markerHeight = (56 + 8 + 12 + 52 * fontScale).dp
        val footerHeight = if (maxWidth >= 960.dp && fontScale <= 1.3f) 112f else 168f
        // Neither projection nor marker layout depends on selection. A/B share the same plan.
        val anchors = remember(regions, catalog, side, left, top) {
            regions.mapNotNull { region ->
                val definition = catalog.regions.singleOrNull { it.id == region.id }
                val point = definition?.let { worldMapPoint(it.latitude, it.longitude) }
                point?.let { WorldMapAnchor(region.id, (left + side * it.first).value, (top + side * it.second).value) }
            }
        }
        val markers = remember(anchors, maxWidth, maxHeight, markerWidth, markerHeight, footerHeight) {
            layoutWorldMapMarkers(anchors, maxWidth.value, maxHeight.value,
                markerWidth.value, markerHeight.value, footerHeight)
        }
        WorldLandLayer(side, Modifier.align(Alignment.Center))
        Canvas(Modifier.fillMaxSize()) {
            markers.forEach { marker ->
                val dim = if (selectedId != null && selectedId != marker.anchor.id) .55f else 1f
                val anchor = Offset(marker.anchor.x.dp.toPx(), marker.anchor.y.dp.toPx())
                val label = Offset((marker.left.dp + markerWidth / 2).toPx(), (marker.top + 28).dp.toPx())
                drawLine(WorldColors.Primary.copy(alpha = .35f * dim), anchor, label, 1.5.dp.toPx())
                drawCircle(WorldColors.Primary.copy(alpha = dim), 3.dp.toPx(), anchor)
            }
        }
        markers.forEach { marker ->
            val region = regions.single { it.id == marker.anchor.id }
            val chosen = selectedId == region.id
            val dim = if (selectedId != null && !chosen) .55f else 1f
            Column(Modifier.offset(marker.left.dp, marker.top.dp).width(markerWidth).alpha(dim)
                .semantics(mergeDescendants = true) { selected = chosen }
                .clickable(enabled = region.discovered, role = Role.Button,
                    onClickLabel = "${region.name} 지역 상세 열기") { onSelect(region.id) },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                    if (chosen) Canvas(Modifier.matchParentSize()) {
                        drawCircle(Color(0xFF2A8FAE).copy(alpha = .3f), radius = 34.dp.toPx())
                    }
                    val pin = Modifier.size(52.dp)
                        .shadow(if (region.discovered) 6.dp else 0.dp, CircleShape,
                            ambientColor = WorldColors.Primary.copy(alpha = .35f), spotColor = WorldColors.Primary.copy(alpha = .35f))
                        .clip(CircleShape).background(if (region.discovered) WorldColors.Primary else Color.White.copy(alpha = .9f))
                    Box(if (region.discovered) pin.border(4.dp, Color.White, CircleShape) else pin.drawBehind {
                        drawCircle(Color(0xFFA9B6C3), radius = size.minDimension / 2f - 1.dp.toPx(),
                            style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))))
                    }, contentAlignment = Alignment.Center) {
                        WorldIcon(if (region.discovered) R.drawable.world_ic_pin else R.drawable.world_ic_lock,
                            26.dp, if (region.discovered) Color.White else Color(0xFF7A8A99))
                    }
                }
                Column(Modifier.shadow(3.dp, RoundedCornerShape(14.dp)).clip(RoundedCornerShape(14.dp))
                    .background(if (chosen) WorldColors.Primary else Color.White.copy(alpha = if (region.discovered) 1f else .85f))
                    .padding(horizontal = 14.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    WorldText(region.name, 22, color = if (chosen) Color.White else if (region.discovered) WorldColors.Text else Color(0xFF7A8A99),
                        weight = FontWeight.Bold, textAlign = TextAlign.Center)
                    WorldText(if (region.discovered) "방문 ${worldVisitDisplay(region.id, history).count}" else "미발견",
                        color = if (chosen) Color.White else if (region.discovered) WorldColors.Accent else WorldColors.Muted,
                        textAlign = TextAlign.Center)
                }
            }
        }
        WorldMapFooter(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(20.dp),
            inline = maxWidth >= 960.dp && fontScale <= 1.3f)
    }
}

/** Stable resource/painter leaf: no selection key or reload of the Natural Earth geometry. */
@Composable
private fun WorldLandLayer(side: Dp, modifier: Modifier) {
    Image(painterResource(R.drawable.memoride_korea_map), "대한민국 정적 지도", modifier.size(side))
}

@Composable
private fun WorldMapFooter(modifier: Modifier, inline: Boolean) {
    val legend: @Composable () -> Unit = {
        Row(Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = .88f))
            .padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Canvas(Modifier.size(26.dp)) { drawCircle(WorldColors.Primary) }
                WorldText("발견", 24, weight = FontWeight.SemiBold)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Canvas(Modifier.size(26.dp)) {
                    drawCircle(Color.White.copy(alpha = .9f))
                    drawCircle(Color(0xFFA9B6C3), radius = size.minDimension / 2f - 1.dp.toPx(),
                        style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()))))
                }
                WorldText("미발견", 24, weight = FontWeight.SemiBold)
            }
        }
    }
    if (inline) Row(modifier, verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        legend()
        WorldText("지도 데이터 · Made with Natural Earth", 24, modifier = Modifier.weight(1f),
            color = Color(0xFF2B4A63).copy(alpha = .7f), textAlign = TextAlign.End)
    } else Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        legend()
        WorldText("지도 데이터 · Made with Natural Earth", 24, modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF2B4A63).copy(alpha = .7f), textAlign = TextAlign.End)
    }
}
