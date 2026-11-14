package io.kailand.feature.island

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kailand.core.ui.memorideCharacterColors
import io.kailand.R
import io.kailand.core.contract.Catalog
import io.kailand.core.contract.GameSnapshot
import io.kailand.core.contract.InteractionGate
import io.kailand.core.ui.IslandDestinationSlot
import io.kailand.core.ui.MemorideBrandLogo
import io.kailand.core.ui.MemoridePleosTextStyle
import io.kailand.core.ui.MemoridePleosFonts
import io.kailand.core.ui.MemoridePleosTheme

/** Local S02 tokens: these do not change other screens' shared theme. */
internal object IslandHomeColors {
    val Background = Color(0xFFF3F6F9)
    val Primary = Color(0xFF1F4E79)
    val Accent = Color(0xFF2A7F9E)
    val Text = Color(0xFF1E2B3A)
    val Sub = Color(0xFF5B6B7B)
    val Muted = Color(0xFF9AA8B6)
    val Border = Color(0xFFDCE4EC)
    val Track = Color(0xFFE6ECF2)
    val PrimaryBrush = Brush.horizontalGradient(listOf(Primary, Color(0xFF2A8FAE)))
    val ProgressBrush = Brush.horizontalGradient(listOf(Color(0xFF3FB6D6), Primary))
}

@Composable
internal fun IslandHomeTheme(content: @Composable () -> Unit) {
    MemoridePleosTheme(content)
}

@Composable
private fun HomeText(
    text: String, size: Int, modifier: Modifier = Modifier,
    color: Color = IslandHomeColors.Text, weight: FontWeight = FontWeight.Normal,
) {
    Text(text, modifier, color = color, fontSize = size.coerceAtLeast(18).sp, lineHeight = (size.coerceAtLeast(18) * 1.3f).sp,
        fontWeight = weight, fontFamily = MemoridePleosFonts.Body,
        style = MemoridePleosTextStyle)
}

@Composable
internal fun IslandHomePanel(
    snapshot: GameSnapshot, catalog: Catalog, view: IslandPresentation, gate: InteractionGate,
    catalogIssue: Boolean, destination: IslandDestinationSlot?, showActions: Boolean,
    onSelectCharacter: () -> Unit, onStartJourney: () -> Unit,
    onOpenInventory: () -> Unit, onOpenWorld: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (destination?.destinationLabel != null) {
        IslandWaitingPanel(snapshot, catalog, view, destination, gate, catalogIssue, modifier)
        return
    }
    Surface(modifier.shadow(12.dp, RoundedCornerShape(28.dp),
        ambientColor = Color(0x0F1A334D), spotColor = Color(0x0F1A334D)),
        shape = RoundedCornerShape(28.dp), color = Color.White) {
        BoxWithConstraints(Modifier.padding(32.dp)) {
            // Keep the action group at the bottom. Only the upper cards scroll when space is tight.
            // Only exceptionally short/narrow windows need a full scrolling fallback.
            // Warnings or destination status must not pull the normal action group upward.
            val scroll = maxHeight < 420.dp * LocalDensity.current.fontScale || maxWidth < 340.dp
            val information: @Composable () -> Unit = {
                IslandHomeLogo()
                IslandHomeName(snapshot, catalog, view)
                IslandHomeGrowth(view.growth)
                IslandHomeWarnings(gate, catalogIssue)
                if (showActions) {
                    HomeSecondaryButton("캐릭터 선택", R.drawable.island_ic_users, gate.canInteract,
                        onSelectCharacter, Modifier.fillMaxWidth(), height = 80.dp,
                        background = Color(0xFFF5F8FB), radius = 18.dp, textSize = 26, iconSize = 28.dp)
                }
            }
            if (scroll) {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    information()
                    destination?.content?.invoke()
                    if (showActions) IslandHomeActions(gate.canInteract, onStartJourney, onOpenInventory, onOpenWorld)
                }
            } else {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        information()
                        destination?.content?.invoke()
                    }
                    if (showActions) IslandHomeActions(gate.canInteract, onStartJourney, onOpenInventory, onOpenWorld)
                }
            }
        }
    }
}

@Composable
internal fun IslandHomeCompact(
    snapshot: GameSnapshot, catalog: Catalog, view: IslandPresentation, gate: InteractionGate,
    catalogIssue: Boolean, destination: IslandDestinationSlot?, showActions: Boolean,
    onSelectCharacter: () -> Unit, onStartJourney: () -> Unit,
    onOpenInventory: () -> Unit, onOpenWorld: () -> Unit,
) {
    if (destination?.destinationLabel != null) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            IslandJourneyCarStage(destination.destinationLabel, gate.canInteract,
                Modifier.weight(.38f).fillMaxWidth(), characterName = view.characterName)
            IslandWaitingPanel(snapshot, catalog, view, destination, gate, catalogIssue,
                Modifier.weight(.62f).fillMaxWidth())
        }
        return
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)) {
        IslandHomeStage(snapshot, catalog, view, gate.canInteract && showActions,
            Modifier.fillMaxWidth().height(360.dp))
        // The same panel content flows naturally instead of clipping a fixed 1280px canvas.
        Surface(shape = RoundedCornerShape(28.dp), color = Color.White) {
            Column(Modifier.fillMaxWidth().padding(32.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                IslandHomeLogo()
                IslandHomeName(snapshot, catalog, view)
                IslandHomeGrowth(view.growth)
                IslandHomeWarnings(gate, catalogIssue)
                if (showActions) HomeSecondaryButton("캐릭터 선택", R.drawable.island_ic_users,
                    gate.canInteract, onSelectCharacter, Modifier.fillMaxWidth(), height = 80.dp,
                    background = Color(0xFFF5F8FB), radius = 18.dp, textSize = 26, iconSize = 28.dp)
                destination?.content?.invoke()
                if (showActions) IslandHomeActions(gate.canInteract, onStartJourney, onOpenInventory, onOpenWorld)
            }
        }
    }
}

@Composable
private fun IslandHomeLogo() {
    MemorideBrandLogo(Modifier.fillMaxWidth(), markSize = 76.dp, wordmarkSize = 40,
        showSlogan = true, sloganSize = 20, sloganOffset = (-8).dp)
}

/** Display-only archetypes. Catalog IDs, saves and selection behavior are unchanged. */
private fun islandCharacterType(id: String?): String? = when (id) {
    "momo" -> "출퇴근 캐릭터"
    "tori" -> "여행 캐릭터"
    "lulu" -> "일상 캐릭터"
    else -> null
}

@Composable
private fun IslandHomeName(snapshot: GameSnapshot, catalog: Catalog, view: IslandPresentation) {
    val selected = view.characters.firstOrNull { it.selected }
    val identity = memorideCharacterColors(selected?.id)
    Row(Modifier.fillMaxWidth().heightIn(min = 144.dp).clip(RoundedCornerShape(22.dp))
        .background(identity.background)
        .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(28.dp)) {
        Box(Modifier.size(96.dp).clip(CircleShape).background(Color.White)
            .border(2.dp, identity.accent.copy(alpha = .35f), CircleShape), contentAlignment = Alignment.Center) {
            AvatarView(snapshot.avatar, catalog, "idle", Modifier.size(88.dp), avatarSize = 88.dp)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            islandCharacterType(selected?.id)?.let { type ->
                HomeText(type, 18, Modifier.clip(CircleShape).background(Color.White.copy(alpha = .85f))
                    .padding(horizontal = 10.dp, vertical = 3.dp),
                    color = IslandHomeColors.Accent, weight = FontWeight.Bold)
            }
            Text(selected?.name?.let { "${it}의 섬" } ?: "나의 섬",
                Modifier.semantics { heading() }, fontFamily = MemoridePleosFonts.CharacterName,
                fontSize = 48.sp, lineHeight = 62.sp, color = Color(0xFF1E3A5F),
                style = MemoridePleosTextStyle)
        }
    }
}

@Composable
private fun IslandHomeGrowth(growth: IslandGrowth?) {
    Column(Modifier.fillMaxWidth().heightIn(min = 184.dp).border(1.5.dp, Color(0xFFE3EAF1), RoundedCornerShape(22.dp))
        .padding(horizontal = 20.dp, vertical = 20.dp), verticalArrangement = Arrangement.SpaceBetween) {
        if (growth == null) {
            HomeText("섬 성장 정보를 확인해 주세요.", 22, color = MaterialTheme.colorScheme.error)
            return@Column
        }
        val stage = growth.ordinal
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFE6F6EC)),
                contentAlignment = Alignment.Center) {
                HomeIcon(R.drawable.island_ic_sprout, 30.dp, Color(0xFF3E9A63))
            }
            HomeText("섬 성장", 28, weight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(buildAnnotatedString {
                withStyle(SpanStyle(color = IslandHomeColors.Accent, fontSize = 30.sp)) { append("$stage") }
                withStyle(SpanStyle(color = IslandHomeColors.Muted, fontSize = 22.sp)) { append(" / 3단계") }
            }, fontFamily = MemoridePleosFonts.Body, fontWeight = FontWeight.Bold,
                style = MemoridePleosTextStyle)
        }
        // Keep the progress bar and its labels together at the card bottom.
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth().semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(stage.toFloat(), 0f..3f)
                contentDescription = "섬 성장 $stage / 3단계"
            }, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { index ->
                    // The current save exposes completed integer stages only, not partial progress.
                    val fill by animateFloatAsState(if (stage > index) 1f else 0f, tween(400), label = "growth-$index")
                    Box(Modifier.weight(1f).height(18.dp).clip(RoundedCornerShape(9.dp)).background(IslandHomeColors.Track)) {
                        if (fill > 0f) Box(Modifier.fillMaxWidth(fill).fillMaxHeight().background(IslandHomeColors.ProgressBrush))
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val labels = listOf("새싹 섬", "숲 섬", "마을 섬")
                val icons = listOf(R.drawable.island_ic_sprout_small, R.drawable.island_ic_trees, R.drawable.island_ic_house)
                repeat(3) { index ->
                    val current = stage == index + 1
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        HomeIcon(icons[index], 22.dp, if (current) IslandHomeColors.Accent else Color(0xFFAAB6C2))
                        HomeText(labels[index], 20, color = if (current) IslandHomeColors.Accent else IslandHomeColors.Muted,
                            weight = if (current) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            }
        }
        // No trustworthy next-stage journey count exists in GameSnapshot. Do not invent N.
    }
}

@Composable
private fun IslandHomeWarnings(gate: InteractionGate, catalogIssue: Boolean) {
    if (catalogIssue) HomeText("캐릭터 또는 섬 자산 정보를 확인해 주세요.", 18, color = MaterialTheme.colorScheme.error)
    if (!gate.canInteract) HomeText(gate.reason ?: "현재 나의 섬의 기본 행동을 사용할 수 없어요.", 18,
        color = IslandHomeColors.Sub)
}

@Composable
private fun IslandHomeActions(enabled: Boolean, onStart: () -> Unit, onInventory: () -> Unit, onWorld: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        val interaction = remember { MutableInteractionSource() }
        val pressed by interaction.collectIsPressedAsState()
        val scale by animateFloatAsState(if (pressed && enabled) .98f else 1f, tween(100), label = "journey-press")
        Surface(onClick = onStart, enabled = enabled, interactionSource = interaction,
            modifier = Modifier.fillMaxWidth().heightIn(min = 152.dp).scale(scale)
                .shadow(if (enabled) 10.dp else 0.dp, RoundedCornerShape(24.dp),
                    ambientColor = IslandHomeColors.Primary.copy(alpha = .3f), spotColor = IslandHomeColors.Primary.copy(alpha = .3f)),
            shape = RoundedCornerShape(24.dp), color = Color.Transparent) {
            Row(Modifier.background(if (enabled) IslandHomeColors.PrimaryBrush else Brush.horizontalGradient(
                listOf(IslandHomeColors.Track, IslandHomeColors.Track))).padding(horizontal = 22.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                val ink = if (enabled) Color.White else IslandHomeColors.Sub
                Box(Modifier.offset(x = 12.dp).size(80.dp).clip(CircleShape).background(ink.copy(alpha = .18f)), contentAlignment = Alignment.Center) {
                    HomeIcon(R.drawable.island_ic_navigation, 44.dp, ink)
                }
                Column(Modifier.weight(1f).offset(x = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    HomeText("여정 출발", 34, color = ink, weight = FontWeight.Bold)
                    HomeText("출발하면 내비가 자동으로 켜져요", 22, color = ink.copy(alpha = .85f))
                }
                HomeIcon(R.drawable.island_ic_chevron_right, 32.dp, ink)
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth >= 400.dp && LocalDensity.current.fontScale <= 1.1f) {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    HomeSecondaryButton("인벤토리", R.drawable.island_ic_backpack, enabled, onInventory, Modifier.weight(1f), textSize = 27)
                    HomeSecondaryButton("월드맵·도감", R.drawable.island_ic_map, enabled, onWorld, Modifier.weight(1f), textSize = 27)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    HomeSecondaryButton("인벤토리", R.drawable.island_ic_backpack, enabled, onInventory, Modifier.fillMaxWidth(), textSize = 27)
                    HomeSecondaryButton("월드맵·도감", R.drawable.island_ic_map, enabled, onWorld, Modifier.fillMaxWidth(), textSize = 27)
                }
            }
        }
    }
}

@Composable
private fun HomeSecondaryButton(
    text: String, icon: Int, enabled: Boolean, onClick: () -> Unit, modifier: Modifier,
    height: Dp = 96.dp, background: Color = Color.White, radius: Dp = 20.dp,
    textSize: Int = 26, iconSize: Dp = 30.dp,
) {
    Surface(onClick = onClick, enabled = enabled,
        modifier = modifier.heightIn(min = height).alpha(if (enabled) 1f else .5f),
        shape = RoundedCornerShape(radius), color = background, border = BorderStroke(1.5.dp, IslandHomeColors.Border)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically) {
            HomeIcon(icon, iconSize)
            HomeText(text, textSize, weight = FontWeight.Bold)
        }
    }
}

@Composable
private fun HomeIcon(resource: Int, size: Dp, tint: Color = IslandHomeColors.Primary) {
    Icon(painterResource(resource), contentDescription = null, modifier = Modifier.size(size), tint = tint)
}
