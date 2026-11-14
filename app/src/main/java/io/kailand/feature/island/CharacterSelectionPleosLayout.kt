package io.kailand.feature.island

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kailand.R
import io.kailand.core.contract.AvatarPreview
import io.kailand.core.contract.Catalog
import io.kailand.core.ui.MemorideBrandLogo
import io.kailand.core.ui.MemoridePleosTextStyle
import io.kailand.core.ui.MemoridePleosFonts

private object CharacterColors {
    val Primary = Color(0xFF1F4E79)
    val Accent = Color(0xFF2A7F9E)
    val Text = Color(0xFF1E2B3A)
    val Sub = Color(0xFF5B6B7B)
    val Border = Color(0xFFE3EAF1)
    val Gradient = Brush.horizontalGradient(listOf(Primary, Color(0xFF2A8FAE)))
}

/** Display-only fallback copy: catalog IDs, names, save schema and renderer remain authoritative. */
private data class CharacterCardStyle(
    val type: String, val intro: String, val icon: Int,
    val ink: Color, val chip: Color, val sky: List<Color>,
)

private val CharacterCardStyles = mapOf(
    "tori" to CharacterCardStyle("여행 캐릭터", "처음 가는 길도 설레게", R.drawable.character_ic_plane,
        Color(0xFFC9782F), Color(0xFFFFF0E2), listOf(Color(0xFFFFE6CF), Color(0xFFFFF7EE))),
    "momo" to CharacterCardStyle("출퇴근 캐릭터", "매일 같은 길도 특별하게", R.drawable.character_ic_briefcase,
        Color(0xFF2A7F9E), Color(0xFFE3F2FA), listOf(Color(0xFFD6EDFA), Color(0xFFF0F8FA))),
    "lulu" to CharacterCardStyle("일상 캐릭터", "가까운 외출도 추억으로", R.drawable.character_ic_coffee,
        Color(0xFF7B5BB0), Color(0xFFF1EAFB), listOf(Color(0xFFECE3FA), Color(0xFFF8F4FC))),
)
private val UnknownCharacterStyle = CharacterCardStyle("함께할 캐릭터", "함께 달리며 추억을 만들어요",
    R.drawable.character_ic_island, CharacterColors.Accent, Color(0xFFE3F2FA),
    listOf(Color(0xFFD6EDFA), Color(0xFFF0F8FA)))

@Composable
internal fun CharacterSelectionPleosLayout(
    choices: List<IslandCharacter>, catalog: Catalog, selectedId: String?, enabled: Boolean,
    busy: Boolean, notice: String?, retry: Boolean,
    onChoose: (String) -> Unit, onStart: () -> Unit, modifier: Modifier,
    onDemoReset: (() -> Unit)? = null,
) {
    BoxWithConstraints(modifier.fillMaxSize().background(Brush.verticalGradient(
        listOf(Color(0xFFE3F1FB), Color(0xFFFFF6EC))))
        .padding(start = 32.dp, end = 32.dp, top = 24.dp, bottom = 28.dp)) {
        // Capture constraints before entering Column/Row scopes (Compose layout DSL receivers).
        val wide = useWideCharacterCards(maxWidth + 64.dp, maxHeight + 52.dp, LocalDensity.current.fontScale)
        val roomy = wide && maxHeight >= 650.dp
        val gap = if (roomy) 24.dp else 16.dp
        val selected = choices.firstOrNull { it.id == selectedId }
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(gap)) {
            MemorideBrandLogo(Modifier.fillMaxWidth().padding(start = 20.dp), markSize = if (roomy) 80.dp else 64.dp,
                wordmarkSize = if (roomy) 46 else 40, showSlogan = true, sloganSize = 22)
            Column(Modifier.fillMaxWidth().padding(start = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CharacterText("함께 달릴 캐릭터를 골라 주세요", if (roomy) 40 else 32,
                    Modifier.fillMaxWidth().semantics { heading() }, weight = FontWeight.Normal,
                    fontFamily = MemoridePleosFonts.CharacterName, textAlign = TextAlign.Start)
                CharacterText("캐릭터마다 자라는 섬이 달라요. 고른 뒤 출발 버튼을 눌러 주세요.", 24,
                    modifier = Modifier.fillMaxWidth(), color = CharacterColors.Sub, weight = FontWeight.Normal,
                    fontFamily = MemoridePleosFonts.CharacterName, textAlign = TextAlign.Start)
            }
            if (notice != null) {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CharacterIcon(R.drawable.inventory_ic_info, 28.dp, CharacterColors.Accent)
                    CharacterText(notice, 22, Modifier.weight(1f), color = CharacterColors.Sub)
                }
            }
            if (wide) {
                Row(Modifier.weight(1f).fillMaxWidth().selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    for (choice in choices) {
                        CharacterPleosCard(choice, catalog, selectedId == choice.id, enabled,
                            { onChoose(choice.id) }, Modifier.weight(1f).fillMaxHeight(), vertical = true)
                    }
                }
            } else {
                // Only the choices scroll on compact/large-font windows; header and CTA stay visible.
                LazyColumn(Modifier.weight(1f).fillMaxWidth().selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(choices, key = { it.id }) { choice ->
                        CharacterPleosCard(choice, catalog, selectedId == choice.id, enabled,
                            { onChoose(choice.id) }, Modifier.fillMaxWidth(), vertical = false)
                    }
                }
            }
            CharacterSelectionBottomBar(selected, enabled, busy, retry, wide, onStart, Modifier.padding(top = 16.dp), onDemoReset)
        }
    }
}

@Composable
private fun CharacterPleosCard(
    choice: IslandCharacter, catalog: Catalog, selected: Boolean, enabled: Boolean,
    onClick: () -> Unit, modifier: Modifier, vertical: Boolean,
) {
    val style = CharacterCardStyles[choice.id] ?: UnknownCharacterStyle
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .98f else 1f, tween(120), label = "character-press")
    val stroke by animateDpAsState(if (selected) 3.dp else 1.5.dp, tween(200), label = "character-border-width")
    val border by animateColorAsState(if (selected) CharacterColors.Primary else CharacterColors.Border,
        tween(200), label = "character-border")
    val elevation by animateDpAsState(if (selected) 12.dp else 4.dp, tween(200), label = "character-shadow")
    Surface(modifier.graphicsLayer { scaleX = scale; scaleY = scale }
        .heightIn(min = 56.dp)
        .selectable(selected = selected, enabled = enabled, role = Role.RadioButton,
            interactionSource = interaction, indication = null, onClick = onClick),
        shape = RoundedCornerShape(28.dp), color = Color.White,
        border = BorderStroke(stroke, border), shadowElevation = elevation) {
        if (vertical) {
            Column(Modifier.fillMaxSize().padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CharacterPleosStage(choice, catalog, style, selected, Modifier.weight(1f).fillMaxWidth())
                CharacterPleosDetails(choice, style, Modifier.padding(vertical = 16.dp))
            }
        } else {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                CharacterPleosStage(choice, catalog, style, selected, Modifier.weight(.4f).height(176.dp))
                CharacterPleosDetails(choice, style, Modifier.weight(.6f).padding(vertical = 16.dp))
            }
        }
    }
}

@Composable
private fun CharacterPleosStage(
    choice: IslandCharacter, catalog: Catalog, style: CharacterCardStyle, selected: Boolean, modifier: Modifier,
) {
    BoxWithConstraints(modifier.clip(RoundedCornerShape(22.dp)).background(Brush.verticalGradient(style.sky))) {
        val avatarSide = minOf(maxWidth, maxHeight) * .88f
        Canvas(Modifier.matchParentSize()) {
            drawOval(Color.White.copy(alpha = .7f), topLeft = Offset(size.width * .18f, size.height * .84f),
                size = Size(size.width * .64f, size.height * .12f))
        }
        AvatarView(AvatarPreview(choice.id, null), catalog, "idle",
            Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp).size(avatarSide), avatarSize = avatarSide)
        if (selected) {
            Row(Modifier.align(Alignment.TopEnd).padding(14.dp).clip(CircleShape).background(CharacterColors.Primary)
                .padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CharacterIcon(R.drawable.character_ic_check, 24.dp, Color.White)
                CharacterText("선택됨", 22, color = Color.White, weight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CharacterPleosDetails(choice: IslandCharacter, style: CharacterCardStyle, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.clip(CircleShape).background(style.chip).padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CharacterIcon(style.icon, 24.dp, style.ink)
            CharacterText(style.type, 22, color = style.ink, weight = FontWeight.Bold)
        }
        Text(choice.name, modifier = Modifier.padding(start = 8.dp, top = 8.dp), fontFamily = MemoridePleosFonts.CharacterName, fontSize = 40.sp, lineHeight = 48.sp,
            color = Color(0xFF1E3A5F), style = MemoridePleosTextStyle)
        CharacterText(style.intro, 24, Modifier.padding(start = 8.dp), color = CharacterColors.Sub)
    }
}

@Composable
private fun CharacterSelectionBottomBar(
    selected: IslandCharacter?, enabled: Boolean, busy: Boolean, retry: Boolean, wide: Boolean, onStart: () -> Unit,
    modifier: Modifier = Modifier, onDemoReset: (() -> Unit)? = null,
) {
    val note: @Composable (Modifier) -> Unit = { modifier ->
        Column(modifier.padding(start = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CharacterIcon(R.drawable.world_ic_info, 28.dp, CharacterColors.Accent)
                CharacterText("캐릭터는 나중에 섬 화면에서 언제든 바꿀 수 있어요.", 25,
                    Modifier.weight(1f), color = Color(0xFF4A5B6C))
            }
            if (onDemoReset != null) Surface(onClick = onDemoReset, enabled = enabled && !busy,
                shape = RoundedCornerShape(14.dp), color = Color.White.copy(alpha = .85f),
                border = BorderStroke(1.dp, CharacterColors.Border)) {
                Box(Modifier.heightIn(min = 56.dp).padding(horizontal = 18.dp), contentAlignment = Alignment.Center) {
                    CharacterText("시연용 초기화", 18, color = CharacterColors.Sub)
                }
            }
        }
    }
    if (wide) Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        note(Modifier.weight(1f))
        CharacterStartButton(selected, enabled, busy, retry, onStart)
    } else Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        note(Modifier.fillMaxWidth())
        CharacterStartButton(selected, enabled, busy, retry, onStart, Modifier.fillMaxWidth())
    }
}

@Composable
private fun CharacterStartButton(
    selected: IslandCharacter?, enabled: Boolean, busy: Boolean, retry: Boolean,
    onStart: () -> Unit, modifier: Modifier = Modifier,
) {
    val active = enabled && selected != null
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .98f else 1f, tween(120), label = "character-start-press")
    val ink = if (active || busy) Color.White else Color(0xFF8A98A6)
    val brush = if (active || busy) CharacterColors.Gradient
        else Brush.horizontalGradient(listOf(Color(0xFFE9EEF3), Color(0xFFE9EEF3)))
    Surface(onClick = onStart, enabled = active, interactionSource = interaction,
        modifier = modifier.heightIn(min = 104.dp).graphicsLayer { scaleX = scale; scaleY = scale },
        shape = RoundedCornerShape(24.dp), color = Color.Transparent, shadowElevation = if (active) 8.dp else 0.dp) {
        Row(Modifier.background(brush).padding(horizontal = 32.dp, vertical = 26.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterHorizontally)) {
            CharacterIcon(R.drawable.character_ic_island, 36.dp, ink)
            CharacterText(when {
                busy -> "선택 저장 중…"
                selected == null -> "캐릭터를 골라 주세요"
                retry -> "다시 시도"
                else -> "${selected.name}${characterCompanionParticle(selected.name)} 섬으로 출발"
            }, 30, color = ink, weight = FontWeight.Bold,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            CharacterIcon(R.drawable.character_ic_arrow_right, 36.dp, ink)
        }
    }
}

private fun characterCompanionParticle(name: String): String {
    val syllable = name.lastOrNull()?.code?.minus(0xAC00)
    return if (syllable != null && syllable in 0..11171 && syllable % 28 != 0) "과" else "와"
}

@Composable
private fun CharacterIcon(resource: Int, size: Dp, tint: Color) {
    Icon(painterResource(resource), null, Modifier.size(size), tint = tint)
}

@Composable
private fun CharacterText(
    text: String, size: Int, modifier: Modifier = Modifier, color: Color = CharacterColors.Text,
    weight: FontWeight = FontWeight.Medium,
    fontFamily: FontFamily = MemoridePleosFonts.Body,
    textAlign: TextAlign? = null,
) {
    Text(text, modifier, fontFamily = fontFamily, fontSize = size.coerceAtLeast(18).sp,
        lineHeight = (size.coerceAtLeast(18) * 1.3f).sp, color = color, fontWeight = weight, textAlign = textAlign,
        style = MemoridePleosTextStyle)
}
