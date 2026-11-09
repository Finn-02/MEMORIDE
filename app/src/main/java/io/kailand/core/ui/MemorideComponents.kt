package io.kailand.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** AppRoot owns system insets once. Set applyInsets=false inside an already inset host.
 * Content owns scrolling; this shell never forces a fixed Figma canvas or a fake system bar.
 */
@Composable
fun MemorideScreen(
    modifier: Modifier = Modifier,
    applyInsets: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier.fillMaxSize().background(MemorideBrushes.Background)) {
        CompositionLocalProvider(LocalContentColor provides MemorideColors.TextPrimary) {
            Box(
                Modifier.fillMaxSize().then(if (applyInsets) Modifier.safeDrawingPadding() else Modifier),
                content = content,
            )
        }
    }
}

@Composable
fun MemoridePanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(MemorideDimens.PanelRadius),
        color = MemorideColors.Panel,
        contentColor = MemorideColors.TextPrimary,
        shadowElevation = 4.dp,
    ) {
        Column(Modifier.padding(MemorideDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

/** An icon is optional; feature owners supply their own design asset in the leading slot.
 * Minimum heights grow with text. Do not put this in a fixed-height parent at large font scale.
 */
@Composable
fun MemoridePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    supportingText: String? = null,
    leading: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(MemorideDimens.PrimaryRadius)
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = MemorideDimens.PrimaryMinHeight),
        shape = shape,
        color = Color.Transparent,
        contentColor = if (enabled) MemorideColors.OnPrimary else MemorideColors.TextSecondary,
        shadowElevation = if (enabled) 6.dp else 0.dp,
    ) {
        Row(
            Modifier.background(if (enabled) MemorideBrushes.Primary else
                androidx.compose.ui.graphics.Brush.horizontalGradient(
                    listOf(MemorideColors.ProgressEmpty, MemorideColors.ProgressEmpty)))
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading?.invoke()
            Column(Modifier.weight(1f, fill = false)) {
                Text(text, style = MaterialTheme.typography.titleLarge)
                if (!supportingText.isNullOrBlank()) Text(
                    supportingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) MemorideColors.OnPrimary.copy(alpha = .78f)
                        else MemorideColors.TextSecondary,
                )
            }
        }
    }
}

@Composable
fun MemorideSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = MemorideDimens.SecondaryMinHeight),
        shape = RoundedCornerShape(MemorideDimens.SecondaryRadius),
        border = BorderStroke(1.5.dp, MemorideColors.BorderDefault),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MemorideColors.Surface,
            contentColor = MemorideColors.TextPrimary,
            disabledContainerColor = MemorideColors.SurfaceLocked,
            disabledContentColor = MemorideColors.TextSecondary,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            leading?.invoke()
            Text(text, modifier = Modifier.weight(1f, fill = false))
        }
    }
}

/** Icon-only actions need a spoken label; the child icon should be decorative. */
@Composable
fun MemorideIconButton(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick, enabled = enabled,
        modifier = modifier.sizeIn(minWidth = MemorideDimens.MinimumTouchTarget,
            minHeight = MemorideDimens.MinimumTouchTarget)
            .semantics { this.contentDescription = contentDescription },
        shape = RoundedCornerShape(MemorideDimens.IconRadius),
        border = BorderStroke(1.5.dp, MemorideColors.BorderDefault),
        color = MemorideColors.Surface,
        contentColor = MemorideColors.TextPrimary,
    ) {
        Box(Modifier.padding(14.dp), contentAlignment = Alignment.Center) { icon() }
    }
}

@Composable
fun MemorideTextButton(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
) {
    TextButton(onClick = onClick, enabled = enabled,
        modifier = modifier.heightIn(min = MemorideDimens.SecondaryMinHeight),
        colors = ButtonDefaults.textButtonColors(contentColor = MemorideColors.TextSecondary,
            disabledContentColor = MemorideColors.TextSecondary)) { Text(text) }
}
