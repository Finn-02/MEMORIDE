package io.kailand.feature.island

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import io.kailand.core.contract.Catalog
import io.kailand.core.contract.GameSnapshot

/** Atmospheric layers only. The approved alpha island and saved avatar are still rendered by IslandScene. */
@Composable
internal fun IslandHomeStage(
    snapshot: GameSnapshot, catalog: Catalog, view: IslandPresentation,
    animate: Boolean, modifier: Modifier = Modifier, destinationLabel: String? = null,
    statusAnimate: Boolean = animate,
) {
    val drift = if (animate) {
        val transition = rememberInfiniteTransition(label = "island-clouds")
        val value by transition.animateFloat(-10f, 10f,
            infiniteRepeatable(tween(18000), RepeatMode.Reverse), label = "cloud-drift")
        value
    } else 0f
    BoxWithConstraints(modifier.clip(RoundedCornerShape(28.dp)).background(
        Brush.verticalGradient(0f to Color(0xFFCDEBFA), .55f to Color(0xFFE4F5F6), 1f to Color(0xFFBFE3EA))),
        contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            // Coordinates follow the 776x752 Figma stage, scaled with the card.
            val sx = w / 776f
            val sy = h / 752f
            val sun = Offset(w * .88f, h * .12f)
            drawRect(Brush.radialGradient(listOf(Color.White.copy(alpha = .85f), Color.Transparent),
                center = sun, radius = w * .50f))
            drawRect(Brush.verticalGradient(
                0f to Color(0x00A9DCE7), .25f to Color(0x99A9DCE7), 1f to Color(0xFF8CCBDB),
                startY = h * .6f, endY = h), topLeft = Offset(0f, h * .6f), size = Size(w, h * .4f))
            fun cloud(x: Float, y: Float, scale: Float, direction: Float) {
                val dx = drift * sx * direction
                drawOval(Color.White.copy(alpha = .8f), Offset(x * sx + dx, (y + 14f * scale) * sy),
                    Size(120f * scale * sx, 46f * scale * sy))
                drawOval(Color.White.copy(alpha = .8f), Offset((x + 30f * scale) * sx + dx, y * sy),
                    Size(80f * scale * sx, 56f * scale * sy))
            }
            cloud(60f, 70f, 1f, 1f)
            cloud(560f, 130f, .8f, -1f)
            cloud(300f, 40f, .6f, .7f)
            drawOval(Color.White.copy(alpha = .35f), Offset(38f * sx, 355f * sy),
                Size(700f * sx, 270f * sy), style = Stroke(2.dp.toPx()))
            drawOval(Color.White.copy(alpha = .5f), Offset(78f * sx, 370f * sy),
                Size(620f * sx, 240f * sy), style = Stroke(2.dp.toPx()))
        }
        // Keep the existing square alpha canvas, layering, avatar scale and asset resolver.
        // Translate the whole scene slightly down to sit in the illustrated water rings.
        val side = minOf(maxWidth, maxHeight)
        Box(Modifier.size(side).offset(y = maxHeight * .10f)) {
            IslandScene(
                growth = view.growth, avatar = snapshot.avatar, catalog = catalog,
                characterName = view.characterName, eating = false, slots = view.slots,
                avatarScale = 1.7f,
            )
        }
        destinationLabel?.let {
            IslandWaitingStageChip(it, animate = statusAnimate,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 28.dp, start = 16.dp, end = 16.dp))
        }
    }
}
