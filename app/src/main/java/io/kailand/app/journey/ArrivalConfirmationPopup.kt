package io.kailand.app.journey

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kailand.R
import io.kailand.core.contract.Catalog
import io.kailand.core.contract.DestinationSpec
import io.kailand.core.contract.GameSnapshot
import io.kailand.core.ui.MemorideModal
import io.kailand.core.ui.MemoridePleosFonts
import io.kailand.core.ui.MemoridePleosTextStyle

private val ArrivalPrimary = Color(0xFF1F4E79)
private val ArrivalText = Color(0xFF1E2B3A)
private val ArrivalSub = Color(0xFF5B6B7B)
private val ArrivalSurface = Color(0xFFF4F8FB)
private val ArrivalGradient = Brush.horizontalGradient(listOf(ArrivalPrimary, Color(0xFF2A8FAE)))

@Composable
internal fun ArrivalConfirmationPopup(
    visible: Boolean, destination: DestinationSpec?, catalog: Catalog, snapshot: GameSnapshot,
    enabled: Boolean, saving: Boolean, error: String?, onBack: () -> Unit,
    onContinue: () -> Unit, onArrive: () -> Unit, address: String? = null,
) {
    // Retain display-only values for the 200ms exit, without retaining an actionable screen lease.
    val model = ArrivalPopupModel(destination?.label.orEmpty(), address?.takeIf { it.isNotBlank() },
        arrivalRewardHint(destination, catalog, snapshot), error)
    var lastModel by remember { mutableStateOf(model) }
    SideEffect { if (visible) lastModel = model }
    val display = if (visible) model else lastModel
    var submitted by remember { mutableStateOf(false) }
    LaunchedEffect(visible) { if (visible) submitted = false }
    val busy = saving || submitted
    MemorideModal(visible, "목적지에 도착했나요?", onBack) {
        ArrivalConfirmationContent(display, enabled && visible && !busy, busy,
            onContinue = onContinue,
            onArrive = {
                if (!submitted && !saving && visible) { submitted = true; onArrive() }
            })
    }
}

internal data class ArrivalPopupModel(
    val destination: String, val address: String?, val reward: ArrivalRewardHint?, val error: String?,
)

@Composable
internal fun ArrivalConfirmationContent(
    model: ArrivalPopupModel, enabled: Boolean, busy: Boolean,
    onContinue: () -> Unit, onArrive: () -> Unit,
) {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(start = 44.dp, top = 44.dp, end = 44.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Box(Modifier.size(120.dp).clip(CircleShape).background(Color(0xFFE7F0F8)), contentAlignment = Alignment.Center) {
            Box(Modifier.size(92.dp).clip(CircleShape).background(ArrivalGradient), contentAlignment = Alignment.Center) {
                ArrivalIcon(R.drawable.arrival_ic_flag, 46, Color.White)
            }
        }
        ArrivalLabel("목적지에 도착했나요?", 44, weight = FontWeight.Bold, align = TextAlign.Center)
        Surface(shape = CircleShape, color = ArrivalSurface, border = BorderStroke(1.5.dp, Color(0xFFDCE4EC))) {
            Row(Modifier.padding(start = 24.dp, end = 28.dp, top = 14.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ArrivalIcon(R.drawable.arrival_ic_pin, 32, ArrivalPrimary)
                ArrivalLabel(model.destination.ifBlank { "목적지를 다시 확인해 주세요" }, 32,
                    color = ArrivalPrimary, weight = FontWeight.ExtraBold, modifier = Modifier.weight(1f, fill = false))
                model.address?.let { ArrivalLabel(it, 24, color = ArrivalSub, modifier = Modifier.weight(1f, fill = false)) }
            }
        }
        model.reward?.let { hint ->
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = Color.Transparent,
                border = BorderStroke(1.5.dp, Color(0xFFF3DDA2))) {
                Row(Modifier.background(Brush.horizontalGradient(listOf(Color(0xFFFFF8E8), Color(0xFFFFFDF7))))
                    .padding(start = 20.dp, end = 22.dp, top = 16.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFFFEFC7)),
                        contentAlignment = Alignment.Center) { ArrivalIcon(R.drawable.arrival_ic_gift, 30, Color(0xFFB7791F)) }
                    Text(buildAnnotatedString {
                        if (hint.itemName == null) append("이미 이 지역 기념품을 모두 모았어요")
                        else {
                            append("도착하면 ")
                            withStyle(SpanStyle(color = ArrivalText, fontWeight = FontWeight.Bold)) { append(hint.itemName) }
                            append("${arrivalObjectParticle(hint.itemName)} 받을 수 있어요")
                        }
                    }, Modifier.weight(1f), fontFamily = MemoridePleosFonts.Body, fontSize = 26.sp,
                        lineHeight = 33.8.sp, color = ArrivalSub, fontWeight = FontWeight.Medium, style = MemoridePleosTextStyle)
                }
            }
        }
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(ArrivalSurface)
            .padding(horizontal = 20.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ArrivalIcon(R.drawable.arrival_ic_info, 26, ArrivalSub)
            ArrivalLabel("주차 후 ‘도착했어요’를 누르면 여정이 기록돼요.\n아직 도착 전이라면 ‘아직 아니에요’를 선택해 주세요.",
                22, Modifier.weight(1f), color = ArrivalSub)
        }
        model.error?.let { ArrivalLabel(it, 18, Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }, color = ArrivalSub) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            ArrivalAction("아직 아니에요", R.drawable.arrival_ic_clock, 28, false, enabled, false,
                onContinue, Modifier.weight(1f))
            ArrivalAction("도착했어요", R.drawable.arrival_ic_check, 30, true, enabled, busy,
                onArrive, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ArrivalAction(
    label: String, icon: Int, textSize: Int, primary: Boolean, enabled: Boolean, busy: Boolean,
    onClick: () -> Unit, modifier: Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && enabled) .98f else 1f, tween(120), label = "arrival-press")
    Surface(onClick = onClick, enabled = enabled, interactionSource = interaction,
        modifier = modifier.heightIn(min = 92.dp).scale(scale), shape = RoundedCornerShape(22.dp),
        color = if (primary) Color.Transparent else Color.White,
        border = if (primary) null else BorderStroke(2.dp, Color(0xFFC9D6E2)),
        shadowElevation = if (primary && enabled) 10.dp else 0.dp) {
        val ink = if (primary) Color.White else ArrivalText
        Row((if (primary) Modifier.background(if (enabled || busy) ArrivalGradient else
            Brush.horizontalGradient(listOf(Color(0xFF8A98A6), Color(0xFF8A98A6)))) else Modifier)
            .padding(horizontal = 16.dp, vertical = 26.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)) {
            if (busy) CircularProgressIndicator(Modifier.size(32.dp), color = Color.White, strokeWidth = 2.dp)
            else ArrivalIcon(icon, if (primary) 32 else 30, if (primary) ink else ArrivalSub)
            ArrivalLabel(label, textSize, color = ink, weight = FontWeight.Bold, align = TextAlign.Center)
        }
    }
}

@Composable
private fun ArrivalIcon(resource: Int, size: Int, color: Color) {
    Icon(painterResource(resource), null, Modifier.size(size.dp), tint = color)
}

@Composable
private fun ArrivalLabel(
    text: String, size: Int, modifier: Modifier = Modifier, color: Color = ArrivalText,
    weight: FontWeight = FontWeight.Medium, align: TextAlign? = null,
) {
    Text(text, modifier, fontFamily = MemoridePleosFonts.Body, fontSize = size.sp, lineHeight = (size * 1.3f).sp,
        fontWeight = weight, color = color, textAlign = align, style = MemoridePleosTextStyle)
}
