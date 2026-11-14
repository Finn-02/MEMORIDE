package io.kailand.feature.island

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import io.kailand.R
import io.kailand.core.ui.MemoridePleosFonts
import io.kailand.core.ui.MemoridePleosTextStyle

private val PlacementPrimary = Color(0xFF1F4E79)
private val PlacementText = Color(0xFF1E2B3A)
private val PlacementSub = Color(0xFF5B6B7B)
private val PlacementSoft = Color(0xFFEAF4FB)

/** User-facing position names; geometry comes from the existing slot-specific artwork. */
private data class PlacementRegion(val number: Int, val label: String)

private fun placementRegion(slot: IslandSlot): PlacementRegion? = when (slot.id) {
    "slot_left" -> PlacementRegion(1, "왼쪽")
    "slot_right" -> PlacementRegion(3, "오른쪽")
    "slot_back" -> PlacementRegion(2, "뒤쪽")
    else -> null
}

/** Full position labels become small numbered stickers during previews, below the decoration. */
@Composable
internal fun InventoryDecorationMarkers(
    slots: List<IslandSlot>, card: InventoryCardModel, selectedId: String?, enabled: Boolean,
    onSelect: (IslandSlot) -> Unit, modifier: Modifier = Modifier,
) {
    val assets = LocalContext.current.applicationContext.assets
    val assetKey = card.imagePath?.substringAfterLast('/')?.removeSuffix(".png")
    val areas by key(assets, assetKey) {
        produceState<Map<String, DecorationPlacementArea>>(emptyMap()) {
            value = try {
                decorationPlacementAreas(assets, assetKey)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                emptyMap()
            }
        }
    }
    BoxWithConstraints(modifier) {
        val compact = maxWidth < 360.dp
        val stickers = selectedId != null
        val markerWidth = if (compact) 64.dp else 136.dp
        // The enlarged square canvas extends beyond the stage; keep controls within its visible middle.
        val inset = maxWidth * .08f
        slots.sortedBy { placementRegion(it)?.number ?: Int.MAX_VALUE }.forEach { slot ->
            val region = placementRegion(slot) ?: return@forEach
            val area = areas[slot.id] ?: return@forEach
            val selected = slot.id == selectedId
            val available = enabled && card.placeIn(slot) != null
            val markerX = (maxWidth * area.centerX - markerWidth / 2f)
                .coerceIn(inset, (maxWidth - markerWidth - inset).coerceAtLeast(inset))
            val markerY = (maxHeight * area.bottom + 18.dp)
                .coerceIn(inset, (maxHeight - 72.dp - inset).coerceAtLeast(inset))
            if (!stickers) Canvas(Modifier.matchParentSize()) {
                val anchor = Offset(size.width * area.centerX, size.height * area.bottom)
                val label = Offset((markerX + markerWidth / 2f).toPx(), markerY.toPx())
                drawLine(PlacementPrimary.copy(alpha = .7f), anchor, label, strokeWidth = 2.dp.toPx())
                drawCircle(Color.White, radius = 7.dp.toPx(), center = anchor)
                drawCircle(PlacementPrimary, radius = 4.dp.toPx(), center = anchor)
            }
            if (stickers) {
                // Preserve the original label's center and keep the artwork unobstructed.
                Surface(onClick = { onSelect(slot) }, enabled = available,
                    modifier = Modifier.offset(x = markerX + (markerWidth - 48.dp) / 2f, y = markerY + 12.dp)
                        .size(48.dp).semantics {
                            contentDescription = "${region.number}번 ${region.label} 장식 위치 선택"
                            this.selected = selected
                        },
                    shape = CircleShape, color = if (selected) PlacementPrimary else Color.White,
                    border = BorderStroke(2.dp, if (selected) Color.White else Color(0xFFC9D6E2)),
                    shadowElevation = 3.dp) {
                    Box(contentAlignment = Alignment.Center) {
                        PlacementLabel(region.number.toString(), 22,
                            color = if (selected) Color.White else PlacementPrimary, weight = FontWeight.Bold)
                    }
                }
            } else {
                Surface(onClick = { onSelect(slot) }, enabled = available,
                    modifier = Modifier.offset(x = markerX, y = markerY)
                        .width(markerWidth).heightIn(min = 72.dp).semantics {
                            contentDescription = "${region.label} 장식 위치 선택"
                            this.selected = selected
                        },
                    shape = RoundedCornerShape(22.dp),
                    color = if (selected) PlacementPrimary else Color.White.copy(alpha = .96f),
                    border = BorderStroke(2.dp, if (selected) Color.White else Color(0xFFC9D6E2)),
                    shadowElevation = 6.dp) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                        PositionBadge(region.number, selected)
                        if (!compact) PlacementLabel(region.label, 20,
                            color = if (selected) Color.White else if (available) PlacementPrimary else PlacementSub,
                            weight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
internal fun InventoryDecorationPicker(
    card: InventoryCardModel, slots: List<IslandSlot>, selectedSlot: IslandSlot?, enabled: Boolean,
    onSelect: (IslandSlot) -> Unit, onCancel: () -> Unit,
    onPlace: (InventoryCardModel, IslandSlot) -> Unit,
) {
    Surface(shape = RoundedCornerShape(26.dp), color = Color.White,
        border = BorderStroke(2.dp, Color(0xFFBFD9EA))) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(PlacementSoft),
                    contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.world_ic_pin), null, Modifier.size(30.dp), tint = PlacementPrimary)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PlacementLabel("어디에 놓을까요?", 28, Modifier.semantics { heading() }, weight = FontWeight.Bold)
                    PlacementLabel(card.item.name, 24, color = PlacementPrimary, weight = FontWeight.Bold)
                }
            }
            if (selectedSlot == null) PlacementLabel("섬에 표시된 번호나 아래 위치를 눌러 보세요.",
                20, color = PlacementSub)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                slots.sortedBy { placementRegion(it)?.number ?: Int.MAX_VALUE }.forEach { slot ->
                    val region = placementRegion(slot) ?: return@forEach
                    val selected = selectedSlot?.id == slot.id
                    val canChoose = enabled && card.placeIn(slot) != null
                    Surface(onClick = { onSelect(slot) }, enabled = canChoose,
                        modifier = Modifier.weight(1f).semantics { this.selected = selected },
                        shape = RoundedCornerShape(18.dp), color = if (selected) PlacementSoft else Color(0xFFF7F9FB),
                        border = BorderStroke(if (selected) 2.dp else 1.dp,
                            if (selected) PlacementPrimary else Color(0xFFDCE4EC))) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            PositionBadge(region.number, selected)
                            PlacementLabel(region.label, 24, color = PlacementPrimary, weight = FontWeight.Bold)
                            PlacementLabel(when {
                                !slot.available -> "위치 확인 필요"
                                slot.savedItemId == null -> "빈 자리"
                                else -> slot.itemName ?: "장식이 있는 자리"
                            }, 18, color = PlacementSub)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(PlacementSoft)
                .padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(painterResource(R.drawable.inventory_ic_info), null, Modifier.size(26.dp), tint = PlacementPrimary)
                PlacementLabel(if (selectedSlot?.savedItemId != null)
                    "여기에 놓으면 기존 장식은 인벤토리로 돌아가요."
                    else "미리보기를 확인하고 ‘여기에 놓기’를 눌러 주세요.",
                    20, Modifier.weight(1f), color = PlacementSub)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PlacementButton("취소", false, enabled, onCancel, Modifier.weight(1f))
                PlacementButton("여기에 놓기", true,
                    enabled && selectedSlot != null && card.placeIn(selectedSlot) != null,
                    { selectedSlot?.let { onPlace(card, it) } }, Modifier.weight(2f))
            }
        }
    }
}

@Composable
private fun PositionBadge(number: Int, selected: Boolean) {
    Box(Modifier.size(36.dp).clip(CircleShape)
        .background(if (selected) Color.White else PlacementPrimary), contentAlignment = Alignment.Center) {
        PlacementLabel(number.toString(), 20, color = if (selected) PlacementPrimary else Color.White,
            weight = FontWeight.Bold)
    }
}

@Composable
private fun PlacementButton(label: String, primary: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Surface(onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 76.dp),
        shape = RoundedCornerShape(18.dp),
        color = if (primary) { if (enabled) PlacementPrimary else Color(0xFFE9EEF3) } else Color.White,
        border = if (primary) null else BorderStroke(1.5.dp, Color(0xFFC9D6E2))) {
        Box(Modifier.padding(14.dp), contentAlignment = Alignment.Center) {
            PlacementLabel(label, 24, color = if (primary && enabled) Color.White else PlacementText,
                weight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PlacementLabel(
    text: String, size: Int, modifier: Modifier = Modifier, color: Color = PlacementText,
    weight: FontWeight = FontWeight.Medium,
) {
    Text(text, modifier, fontFamily = MemoridePleosFonts.Body, fontSize = size.sp,
        lineHeight = (size * 1.3f).sp, fontWeight = weight, color = color, style = MemoridePleosTextStyle)
}
