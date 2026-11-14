package io.kailand.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kailand.R

/** Shared wide-screen frame dimensions: align header/footer edges across the two panels. */
object MemorideJourneyLayout {
    val OuterInset = 24.dp
    val SectionGap = 24.dp
    fun headerHeight(fontScale: Float) = 104.dp * fontScale.coerceAtLeast(1f)
    fun footerHeight(fontScale: Float, compact: Boolean = false) =
        (if (compact) 136.dp else 160.dp) * fontScale.coerceAtLeast(1f)
}

/** Illustrative waiting directions only; no measured vehicle state or interaction permission. */
@Composable
fun MemorideJourneySteps(modifier: Modifier = Modifier, compact: Boolean = false) {
    val green = Color(0xFF3E9A63)
    val primary = Color(0xFF1F4E79)
    val circle = if (compact) 44.dp else 56.dp
    val font = if (compact) 18 else 24
    Column(modifier, verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val labels = listOf("목적지 확인", "운전", "주차 후 마무리")
            val icons = listOf(R.drawable.character_ic_check, R.drawable.journey_ic_car, R.drawable.journey_ic_parking)
            repeat(3) { index ->
                if (index > 0) Box(Modifier.padding(top = circle / 2f).width(if (compact) 16.dp else 24.dp)
                    .height(3.dp).background(if (index == 1) green else Color(0xFFD5DEE7), RoundedCornerShape(2.dp)))
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val ink = when (index) { 0 -> green; 1 -> primary; else -> Color(0xFF7A8A99) }
                    Surface(Modifier.size(circle), shape = CircleShape,
                        color = when (index) { 0 -> green; 1 -> Color(0xFFE7F0F8); else -> Color(0xFFEEF2F6) },
                        border = if (index == 1) BorderStroke(2.dp, primary) else null) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(painterResource(icons[index]), null, Modifier.size(if (compact) 24.dp else 30.dp),
                                tint = if (index == 0) Color.White else ink)
                        }
                    }
                    Text(labels[index], fontFamily = MemoridePleosFonts.Body, fontSize = font.sp,
                        lineHeight = (font * 1.3f).sp, fontWeight = if (index < 2) FontWeight.Bold else FontWeight.Medium,
                        color = ink, textAlign = TextAlign.Center, style = MemoridePleosTextStyle)
                }
            }
        }
        Text("운전 중에는 앱 화면을 보지 마세요.", Modifier.fillMaxWidth(), fontFamily = MemoridePleosFonts.Body,
            fontSize = (if (compact) 18 else 22).sp, color = Color(0xFF5B6B7B),
            textAlign = TextAlign.Center, style = MemoridePleosTextStyle)
    }
}
