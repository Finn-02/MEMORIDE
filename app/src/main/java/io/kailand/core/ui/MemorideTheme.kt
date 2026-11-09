package io.kailand.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val CoastalColors = lightColorScheme(
    primary = MemorideColors.Navy,
    onPrimary = MemorideColors.OnPrimary,
    secondary = MemorideColors.Ocean,
    onSecondary = MemorideColors.OnPrimary,
    background = MemorideColors.BackgroundMiddle,
    onBackground = MemorideColors.TextPrimary,
    surface = MemorideColors.Surface,
    onSurface = MemorideColors.TextPrimary,
    surfaceVariant = MemorideColors.SurfaceSubtle,
    onSurfaceVariant = MemorideColors.TextSecondary,
    outline = MemorideColors.BorderDefault,
)

// Use Android's installed Korean fallback. No network font request or unlicensed font bundle.
private val CoastalTypography = Typography(
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 50.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 45.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 39.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 39.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 31.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 20.sp, lineHeight = 28.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 31.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 23.sp),
)

@Composable
fun MemorideTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = CoastalColors, typography = CoastalTypography, content = content)
}
