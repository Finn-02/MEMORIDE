package io.kailand.core.ui

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Coastal Morning, matching docs/ux/design_tokens.json. Night candidates are out of scope. */
object MemorideColors {
    val BackgroundTop = Color(0xFFE3F2FF)
    val BackgroundMiddle = Color(0xFFF4F8FF)
    val BackgroundBottom = Color(0xFFFFF6EA)
    val Navy = Color(0xFF1F4E79)
    val Ocean = Color(0xFF2E86AB)
    val Sky = Color(0xFF38BDF8)
    val Coral = Color(0xFFFF8A5B)
    val CoralDeep = Color(0xFFFF5E62)
    val ChipText = Color(0xFFB8431F)
    val ChipBackground = Coral.copy(alpha = .14f)
    val TextPrimary = Color(0xFF13233A)
    val TextSecondary = Color(0xFF5B6B82)
    val OnPrimary = Color.White
    val Surface = Color.White
    val Panel = Color.White.copy(alpha = .92f)
    val SurfaceSubtle = Color(0xFFEEF6FB)
    val SurfaceLocked = Color(0xFFF3F7FA)
    val BorderDefault = Color(0xFFD6E2EE)
    val BorderCard = Color(0xFFE3ECF4)
    val BorderDisabled = Color(0xFF8FA6BE) // Borders only; not disabled text.
    val ProgressEmpty = Color(0xFFD6E6F0)
    val Dim = TextPrimary.copy(alpha = .45f)
    val Sand = Color(0xFFF6E3B4)
    val SandLight = Color(0xFFFFF1D0)
    val MapLand = Color(0xFFFFF8E6)
    val MapLandBorder = Color(0xFFE8D5A6)
    val MapRoad = Color(0xFFF0DDB0)
}

object MemorideBrushes {
    val Background = Brush.verticalGradient(
        0f to MemorideColors.BackgroundTop,
        .5f to MemorideColors.BackgroundMiddle,
        1f to MemorideColors.BackgroundBottom,
    )
    val Primary = Brush.horizontalGradient(listOf(MemorideColors.Navy, MemorideColors.Ocean))
    val Progress = Brush.horizontalGradient(listOf(MemorideColors.Sky, MemorideColors.Navy))
    val Sea = Brush.verticalGradient(listOf(Color(0xFFDDF3FB), Color(0xFFA8DCEB)))
    val MapSea = Brush.verticalGradient(listOf(Color(0xFFBFE8F2), Color(0xFF9ED3E6)))
    val CharacterStage = Brush.verticalGradient(listOf(Color(0xFFDDF3FB), Color(0xFFFFF3E3)))
}

object MemorideDimens {
    val PrimaryRadius = 22.dp
    val SecondaryRadius = 18.dp
    val IconRadius = 16.dp
    val PanelRadius = 24.dp
    val MinimumTouchTarget = 56.dp
    val PrimaryMinHeight = 88.dp
    val SecondaryMinHeight = 72.dp
    val ScreenPadding = 24.dp
    val WideBreakpoint = 840.dp
}
