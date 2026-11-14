package io.kailand.feature.island

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kailand.core.ui.memorideCharacterColors
import io.kailand.R
import io.kailand.core.contract.Catalog
import io.kailand.core.contract.GameSnapshot
import io.kailand.core.contract.InteractionGate
import io.kailand.core.ui.IslandDestinationSlot
import io.kailand.core.ui.MemorideJourneyLayout
import io.kailand.core.ui.MemoridePleosFonts
import io.kailand.core.ui.MemoridePleosTextStyle

/** B owns the island frame; A's existing destination slot owns destination content and actions. */
@Composable
internal fun IslandWaitingPanel(
    snapshot: GameSnapshot, catalog: Catalog, view: IslandPresentation,
    destination: IslandDestinationSlot, gate: InteractionGate, catalogIssue: Boolean, modifier: Modifier,
) {
    Surface(modifier.shadow(12.dp, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp), color = Color.White) {
        Column(Modifier.fillMaxSize().padding(MemorideJourneyLayout.OuterInset),
            verticalArrangement = Arrangement.spacedBy(MemorideJourneyLayout.SectionGap)) {
            IslandWaitingIdentity(snapshot, catalog, view)
            if (!gate.canInteract || catalogIssue) {
                Text(if (!gate.canInteract) gate.reason ?: "현재 이용할 수 없어요." else "섬 정보를 확인해 주세요.",
                    fontSize = 18.sp, fontFamily = MemoridePleosFonts.Body,
                    color = IslandHomeColors.Sub, style = MemoridePleosTextStyle)
            }
            Box(Modifier.weight(1f).fillMaxWidth()) { destination.content() }
        }
    }
}

@Composable
private fun IslandWaitingIdentity(snapshot: GameSnapshot, catalog: Catalog, view: IslandPresentation) {
    val identity = memorideCharacterColors(view.characters.firstOrNull { it.selected }?.id)
    Row(Modifier.fillMaxWidth().height(MemorideJourneyLayout.headerHeight(LocalDensity.current.fontScale)).clip(RoundedCornerShape(22.dp))
        .background(identity.background)
        .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(72.dp).clip(CircleShape).background(Color.White)
            .border(2.dp, identity.accent.copy(alpha = .35f), CircleShape), contentAlignment = Alignment.Center) {
            AvatarView(snapshot.avatar, catalog, "idle", Modifier.size(66.dp), avatarSize = 66.dp)
        }
        Text("${view.characterName}의 섬", Modifier.weight(1f).offset(x = 8.dp), fontFamily = MemoridePleosFonts.CharacterName,
            fontSize = 40.sp, lineHeight = 50.sp, color = Color(0xFF1E3A5F),
            maxLines = 1, overflow = TextOverflow.Ellipsis, style = MemoridePleosTextStyle)
        view.growth?.let { growth ->
            val index = (growth.ordinal - 1).coerceIn(0, 2)
            val labels = listOf("새싹 섬", "숲 섬", "마을 섬")
            val icons = listOf(R.drawable.island_ic_sprout_small, R.drawable.island_ic_trees, R.drawable.island_ic_house)
            Row(Modifier.clip(CircleShape).background(Color.White.copy(alpha = .94f))
                .padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(painterResource(icons[index]), null, Modifier.size(24.dp), tint = Color(0xFF3E9A63))
                Text("${labels[index]} · ${growth.ordinal}/3", fontFamily = MemoridePleosFonts.Body,
                    fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold,
                    color = Color(0xFF3E9A63), style = MemoridePleosTextStyle)
            }
        }
    }
}

@Composable
internal fun IslandWaitingStageChip(
    label: String, animate: Boolean, modifier: Modifier = Modifier, characterName: String? = null,
) {
    val pulse = if (animate) {
        val transition = rememberInfiniteTransition(label = "waiting-status")
        val value by transition.animateFloat(0f, 1f,
            infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "waiting-pulse")
        value
    } else 0f
    Row(modifier.shadow(6.dp, CircleShape).clip(CircleShape).background(Color.White.copy(alpha = .92f))
        .padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Canvas(Modifier.size(20.dp)) {
            drawCircle(Color(0xFF2FB36B).copy(alpha = .35f * (1f - pulse)), radius = 6.dp.toPx() * (1f + .6f * pulse))
            drawCircle(Color(0xFF2FB36B), radius = 6.dp.toPx())
        }
        val companion = characterName?.takeIf { it.isNotBlank() }?.let { "$it${waitingCompanionParticle(it)} 함께 " } ?: ""
        Text("$companion$label${waitingDirectionParticle(label)} 이동 중", fontFamily = MemoridePleosFonts.Body,
            fontSize = 23.sp, fontWeight = FontWeight.Bold, color = IslandHomeColors.Text,
            maxLines = 1, overflow = TextOverflow.Ellipsis, style = MemoridePleosTextStyle)
    }
}

private fun waitingDirectionParticle(label: String): String {
    val syllable = label.lastOrNull()?.code?.minus(0xAC00)
    val final = syllable?.takeIf { it in 0..11171 }?.rem(28)
    return if (final != null && final != 0 && final != 8) "으로" else "로"
}

private fun waitingCompanionParticle(name: String): String {
    val syllable = name.lastOrNull()?.code?.minus(0xAC00)
    return if (syllable != null && syllable in 0..11171 && syllable % 28 != 0) "과" else "와"
}
