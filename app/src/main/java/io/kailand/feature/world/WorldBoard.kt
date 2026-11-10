package io.kailand.feature.world

import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.kailand.R
import io.kailand.core.contract.Catalog
import io.kailand.core.contract.RegionVisitHistory
import io.kailand.core.ui.*
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
    BoxWithConstraints(modifier.clip(RoundedCornerShape(20.dp)).background(MemorideBrushes.MapSea)) {
        val side = min(maxWidth.value, maxHeight.value).dp
        val left = (maxWidth - side) / 2
        val top = (maxHeight - side) / 2
        val fontScale = LocalDensity.current.fontScale
        val markerWidth = if (fontScale > 1.3f) 120.dp else 88.dp
        val markerHeight = (60 + 36 * fontScale).dp
        val anchors = regions.mapNotNull { region ->
            val definition = catalog.regions.singleOrNull { it.id == region.id }
            val point = definition?.let { worldMapPoint(it.latitude, it.longitude) }
            point?.let { WorldMapAnchor(region.id, (left + side * it.first).value, (top + side * it.second).value) }
        }
        val markers = layoutWorldMapMarkers(anchors, maxWidth.value, maxHeight.value,
            markerWidth.value, markerHeight.value, if (fontScale > 1.3f) 88f else 40f)
        Image(painterResource(R.drawable.memoride_korea_map), "대한민국 정적 지도",
            Modifier.size(side).align(Alignment.Center))
        Canvas(Modifier.fillMaxSize()) {
            markers.forEach { marker ->
                val anchor = Offset(marker.anchor.x.dp.toPx(), marker.anchor.y.dp.toPx())
                val label = Offset((marker.left.dp + markerWidth / 2).toPx(), (marker.top + 28).dp.toPx())
                drawLine(MemorideColors.Navy, anchor, label, 1.5.dp.toPx())
                drawCircle(MemorideColors.Navy, 3.dp.toPx(), anchor)
            }
        }
        markers.forEach { marker ->
            val region = regions.single { it.id == marker.anchor.id }
                val chosen = selectedId == region.id
                Column(
                    Modifier.offset(marker.left.dp, marker.top.dp)
                        .width(markerWidth).semantics(mergeDescendants = true) { selected = chosen }
                        .clickable(role = Role.Button, onClickLabel = "${region.name} 지역 상세 열기") { onSelect(region.id) }
                        .padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Box(Modifier.size(48.dp).clip(CircleShape)
                        .background(if (chosen) MemorideColors.Coral else MemorideColors.Surface)
                        .border(if (chosen) 3.dp else 1.5.dp, MemorideColors.Navy, CircleShape),
                        contentAlignment = Alignment.Center) {
                        Text("●", color = MemorideColors.Navy, style = MaterialTheme.typography.titleLarge)
                    }
                    Text(region.name, color = MemorideColors.TextPrimary, style = MaterialTheme.typography.titleMedium)
                    Text("방문 ${worldVisitDisplay(region.id, history).count}", color = MemorideColors.TextPrimary,
                        style = MaterialTheme.typography.labelMedium)
                }
        }
        Text("게임의 발견 기록 · Made with Natural Earth", color = MemorideColors.TextPrimary,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
                .background(MemorideColors.Panel, RoundedCornerShape(6.dp)).padding(4.dp))
    }
}
