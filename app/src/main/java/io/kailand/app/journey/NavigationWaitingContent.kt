package io.kailand.app.journey

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kailand.R
import io.kailand.core.contract.Catalog
import io.kailand.core.contract.GameSnapshot
import io.kailand.core.ui.MemorideJourneyLayout
import io.kailand.core.ui.MemoridePleosFonts
import io.kailand.core.ui.MemoridePleosTextStyle

private val WaitingPrimary = Color(0xFF1F4E79)
private val WaitingSub = Color(0xFF5B6B7B)

/** Display-only preview, never a grant or an arrival acknowledgement. */
private fun waitingReward(draft: NavigationDraft?, snapshot: GameSnapshot?, catalog: Catalog?): String? {
    if (draft == null || snapshot == null || catalog == null) return null
    val id = draft.destination(catalog).regionId ?: return null
    val region = catalog.regions.singleOrNull { it.id == id } ?: return null
    if (region.itemIds.isEmpty() || region.itemIds.distinct().size != region.itemIds.size) return null
    val items = region.itemIds.map { itemId ->
        catalog.items.singleOrNull { it.id == itemId && it.regionId == region.id } ?: return null
    }
    // Preview the existing first-discovery rule; revisits never promise another grant.
    if (id in snapshot.discoveredRegionIds) return "이미 기념품을 모두 모았어요"
    val first = items.firstOrNull()?.name?.takeIf { it.isNotBlank() } ?: return null
    return if (items.size == 1) first else "$first + ${items.size - 1}개"
}

/** Display-only locality; matching and destination state remain in NavigationDraft. */
private fun waitingRegionSubtitle(draft: NavigationDraft?, catalog: Catalog?): String? {
    if (draft == null || catalog == null) return null
    val id = draft.destination(catalog).regionId ?: return null
    val region = catalog.regions.singleOrNull { it.id == id } ?: return null
    return when (region.id) {
        "wonju" -> "강원도 원주시" // User-requested locality wording, not a street address.
        else -> region.name.takeIf { it.isNotBlank() && it != draft.label }
    }
}

@Composable
internal fun NavigationWaitingContent(
    label: String, state: NavigationInboxState, snapshot: GameSnapshot?, catalog: Catalog?,
    onRefresh: () -> Unit, onFinish: () -> Unit, refreshEnabled: Boolean, finishEnabled: Boolean,
) {
    val reward = remember(state.draft, snapshot, catalog) { waitingReward(state.draft, snapshot, catalog) }
    val locality = remember(state.draft, catalog) { waitingRegionSubtitle(state.draft, catalog) }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        // Enlarged card and prominent notice share the bounded scroll area; Finish remains fixed.
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)) {
            WaitingDestinationCard(label, locality, reward, state.saving, refreshEnabled, onRefresh)
            val notice = state.error ?: when {
                state.saving -> "목적지를 확인하고 있어요…"
                else -> "내비 목적지를 받아왔어요.\n도착 여부는 직접 확인해 주세요."
            }
            Surface(Modifier.fillMaxWidth().heightIn(min = 124.dp), shape = RoundedCornerShape(20.dp),
                color = if (state.error != null) Color(0xFFFFF0EB) else Color(0xFFEAF4FB)) {
                Row(Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    WaitingIcon(R.drawable.inventory_ic_info, 30.dp, WaitingPrimary)
                    WaitingText(notice, 22, Modifier.weight(1f), color = WaitingPrimary, weight = FontWeight.SemiBold)
                }
            }
        }
        WaitingFinishButton(finishEnabled, onFinish)
    }
}

@Composable
private fun WaitingDestinationCard(label: String, locality: String?, reward: String?, refreshing: Boolean, enabled: Boolean, onRefresh: () -> Unit) {
    Surface(Modifier.fillMaxWidth().heightIn(min = 460.dp), shape = RoundedCornerShape(26.dp), color = Color.Transparent, shadowElevation = 8.dp) {
        Box(Modifier.fillMaxWidth().height(460.dp)
            .background(Brush.linearGradient(listOf(WaitingPrimary, Color(0xFF2A8FAE))))
            .padding(30.dp)) {
            Row(Modifier.align(Alignment.TopCenter).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.clip(CircleShape).background(Color.White.copy(alpha = .16f))
                    .padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WaitingIcon(R.drawable.island_ic_navigation, 22.dp, Color.White)
                    WaitingText("내비 목적지", 22, color = Color.White, weight = FontWeight.Bold)
                }
                Spacer(Modifier.weight(1f))
                val rotation = if (refreshing) {
                    val transition = rememberInfiniteTransition(label = "destination-refresh")
                    val angle by transition.animateFloat(0f, 360f,
                        infiniteRepeatable(tween(1000, easing = LinearEasing)), label = "refresh-angle")
                    angle
                } else 0f
                Surface(onClick = onRefresh, enabled = enabled,
                    modifier = Modifier.size(56.dp).semantics { contentDescription = "목적지 다시 조회" },
                    shape = CircleShape, color = Color.White.copy(alpha = .16f)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.journey_ic_refresh), null, Modifier.size(28.dp).rotate(rotation), tint = Color.White)
                    }
                }
            }
            Column(Modifier.align(Alignment.TopStart).padding(top = 88.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    WaitingIcon(R.drawable.world_ic_pin, 40.dp, Color.White)
                    Text(label, Modifier.weight(1f), fontFamily = MemoridePleosFonts.Body, fontSize = 56.sp,
                        lineHeight = 68.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, style = MemoridePleosTextStyle)
                }
                locality?.let { WaitingText(it, 22, color = Color.White.copy(alpha = .88f)) }
            }
            // Locality is a display label only. No street address or reverse-geocoding is inferred.
            if (reward != null) Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().heightIn(min = 104.dp).clip(RoundedCornerShape(18.dp)).background(Color.White)
                .padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFE7F0F8)),
                    contentAlignment = Alignment.Center) { WaitingIcon(R.drawable.world_ic_gift, 32.dp, WaitingPrimary) }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (reward != "이미 기념품을 모두 모았어요") WaitingText("도착하면 받을 수 있어요", 22, color = WaitingSub)
                    WaitingText(reward, if (reward == "이미 기념품을 모두 모았어요") 22 else 24,
                        weight = if (reward == "이미 기념품을 모두 모았어요") FontWeight.Medium else FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun WaitingFinishButton(enabled: Boolean, onFinish: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && enabled) .98f else 1f, tween(120), label = "waiting-finish-press")
    Surface(onClick = onFinish, enabled = enabled, interactionSource = interaction,
        modifier = Modifier.fillMaxWidth().height(MemorideJourneyLayout.footerHeight(LocalDensity.current.fontScale)).scale(scale),
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent, shadowElevation = if (enabled) 4.dp else 0.dp) {
        val colors = if (enabled) listOf(Color(0xFF168F9B), Color(0xFF2C8C68))
            else listOf(Color(0xFFE9EEF3), Color(0xFFE9EEF3))
        Row(Modifier.background(Brush.horizontalGradient(colors)).padding(horizontal = 22.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            val ink = if (enabled) Color.White else Color(0xFF8A98A6)
            Box(Modifier.size(64.dp).clip(CircleShape).background(ink.copy(alpha = .18f)), contentAlignment = Alignment.Center) {
                WaitingIcon(R.drawable.journey_ic_flag, 34.dp, ink)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                WaitingText("목적지에 도착했어요", 30, Modifier.offset(x = 8.dp), color = ink, weight = FontWeight.Bold)
                WaitingText("주차한 뒤 눌러 주세요", 22, Modifier.padding(start = 8.dp), color = ink.copy(alpha = .9f))
            }
            WaitingIcon(R.drawable.character_ic_arrow_right, 44.dp, ink)
        }
    }
}

@Composable
private fun WaitingText(text: String, size: Int, modifier: Modifier = Modifier, color: Color = Color(0xFF1E2B3A),
    weight: FontWeight = FontWeight.Medium, textAlign: TextAlign? = null) {
    Text(text, modifier, fontFamily = MemoridePleosFonts.Body, fontSize = size.coerceAtLeast(18).sp,
        lineHeight = (size.coerceAtLeast(18) * 1.3f).sp, color = color, fontWeight = weight,
        textAlign = textAlign, style = MemoridePleosTextStyle)
}

@Composable
private fun WaitingIcon(resource: Int, size: Dp, tint: Color) {
    Icon(painterResource(resource), null, Modifier.size(size), tint = tint)
}
