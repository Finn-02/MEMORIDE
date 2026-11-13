package io.kailand.feature.island

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/** Presentation only: the existing successful-eat state starts/stops this brief effect. */
@Composable
internal fun InventoryMealEffect(playing: Boolean, modifier: Modifier = Modifier) {
    if (!playing) return
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(1400)) }
    val fraction = progress.value
    Canvas(modifier) {
        val positions = listOf(.18f to .32f, .82f to .28f, .24f to .48f,
            .76f to .46f, .15f to .56f, .85f to .54f)
        val colors = listOf(Color(0xFFF0BC52), Color(0xFFE98BA0), Color(0xFF67B69B))
        positions.forEachIndexed { index, (x, y) ->
            val local = ((fraction - (index % 3) * .08f) / .8f).coerceIn(0f, 1f)
            val glow = sin(local * PI).toFloat().coerceAtLeast(0f)
            val center = Offset(size.width * x, size.height * (y - local * .14f))
            val radius = (7.dp.toPx() + index % 2 * 3.dp.toPx()) * (.75f + glow * .5f)
            val inner = radius * .28f
            val star = Path().apply {
                moveTo(center.x, center.y - radius)
                lineTo(center.x + inner, center.y - inner)
                lineTo(center.x + radius, center.y)
                lineTo(center.x + inner, center.y + inner)
                lineTo(center.x, center.y + radius)
                lineTo(center.x - inner, center.y + inner)
                lineTo(center.x - radius, center.y)
                lineTo(center.x - inner, center.y - inner)
                close()
            }
            drawCircle(colors[index % colors.size].copy(alpha = glow * .14f), radius * 1.9f, center)
            drawPath(star, colors[index % colors.size].copy(alpha = glow))
        }
    }
}
