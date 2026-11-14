package io.kailand.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kailand.R

/** Bundled fonts: no reliance on device-specific font names or runtime downloads. */
@OptIn(ExperimentalTextApi::class)
object MemoridePleosFonts {
    val Body = FontFamily(
        Font(R.font.memoride_asta_sans, FontWeight.Normal,
            variationSettings = FontVariation.Settings(FontVariation.weight(400))),
        Font(R.font.memoride_asta_sans, FontWeight.Medium,
            variationSettings = FontVariation.Settings(FontVariation.weight(500))),
        Font(R.font.memoride_asta_sans, FontWeight.SemiBold,
            variationSettings = FontVariation.Settings(FontVariation.weight(600))),
        Font(R.font.memoride_asta_sans, FontWeight.Bold,
            variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    )
    val CharacterName = FontFamily(Font(R.font.island_jua))
    val Logo = FontFamily(Font(R.font.island_baloo2, FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(800))))
}

private fun TextStyle.pleos(): TextStyle {
    val size = fontSize.value.takeIf { it.isFinite() }?.coerceAtLeast(18f) ?: 18f
    val height = lineHeight.value.takeIf { it.isFinite() }?.coerceAtLeast(size * 1.3f) ?: size * 1.3f
    return copy(fontFamily = MemoridePleosFonts.Body, fontSize = size.sp, lineHeight = height.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineBreak = MemoridePleosTextStyle.lineBreak, hyphens = MemoridePleosTextStyle.hyphens,
        localeList = MemoridePleosTextStyle.localeList)
}

/** Opt-in theme for home/inventory only; other feature screens keep their own theme. */
@Composable
fun MemoridePleosTheme(content: @Composable () -> Unit) {
    MemorideTheme {
        val t = MaterialTheme.typography
        MaterialTheme(typography = t.copy(
            displayLarge = t.displayLarge.pleos(), displayMedium = t.displayMedium.pleos(), displaySmall = t.displaySmall.pleos(),
            headlineLarge = t.headlineLarge.pleos(), headlineMedium = t.headlineMedium.pleos(), headlineSmall = t.headlineSmall.pleos(),
            titleLarge = t.titleLarge.pleos(), titleMedium = t.titleMedium.pleos(), titleSmall = t.titleSmall.pleos(),
            bodyLarge = t.bodyLarge.pleos(), bodyMedium = t.bodyMedium.pleos(), bodySmall = t.bodySmall.pleos(),
            labelLarge = t.labelLarge.pleos(), labelMedium = t.labelMedium.pleos(), labelSmall = t.labelSmall.pleos(),
        ), content = content)
    }
}

/** One logo implementation, with the accepted larger home configuration and compact inventory header. */
@Composable
fun MemorideBrandLogo(
    modifier: Modifier = Modifier,
    markSize: Dp = 48.dp,
    wordmarkSize: Int = 30,
    showSlogan: Boolean = false,
    sloganSize: Int = 20,
    sloganOffset: Dp = (-8).dp,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Image(painterResource(R.drawable.island_logo_mark), null, Modifier.size(markSize).clip(RoundedCornerShape(markSize * (14f / 48f))))
        Column(Modifier.weight(1f)) {
            Text("MEMORIDE", fontFamily = MemoridePleosFonts.Logo, fontWeight = FontWeight.ExtraBold,
                fontSize = wordmarkSize.coerceAtLeast(18).sp, lineHeight = wordmarkSize.coerceAtLeast(18).sp,
                letterSpacing = (wordmarkSize * .02f).sp,
                style = TextStyle(brush = Brush.horizontalGradient(listOf(Color(0xFF1F4E79), Color(0xFF2A9DB8))),
                    platformStyle = PlatformTextStyle(includeFontPadding = false)),
                modifier = Modifier.semantics { contentDescription = "메모라이드" })
            if (showSlogan) Text("기억을 싣고 달리는 드라이브", Modifier.offset(y = sloganOffset),
                fontFamily = MemoridePleosFonts.Body, fontSize = sloganSize.coerceAtLeast(18).sp,
                lineHeight = (sloganSize.coerceAtLeast(18) * 1.3f).sp, color = Color(0xFF7A8A99),
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)))
        }
    }
}
