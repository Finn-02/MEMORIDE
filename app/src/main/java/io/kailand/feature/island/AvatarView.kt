package io.kailand.feature.island

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.kailand.core.contract.AvatarPreview
import io.kailand.core.contract.Catalog

@Composable
fun AvatarView(
    avatar: AvatarPreview,
    catalog: Catalog,
    expressionKey: String,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 128.dp,
    highResolution: Boolean = false,
) {
    val assets = LocalContext.current.applicationContext.assets
    val loader = remember(assets, highResolution) { androidAvatarAssetLoader(assets, highResolution) }
    val paths = remember(avatar, catalog, expressionKey) { AvatarAssetResolver.resolve(avatar, catalog, expressionKey) }
    val approved = remember(avatar, expressionKey, paths) { AvatarAssetResolver.approvedFrames(avatar, expressionKey, paths) }
    val candidates = remember(approved, highResolution) {
        if (highResolution) approved.map { it.replace("avatars/approved/", "avatars/approved_hd/") } + approved
        else approved
    }
    val frame = avatarFrame(loader, candidates)
    val placeholder = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)

    Canvas(modifier.size(avatarSize)) {
        val side = size.minDimension.toInt()
        if (side <= 0) return@Canvas
        if (frame == null) {
            drawCircle(placeholder, side * 0.16f, Offset(center.x, center.y - side * 0.14f))
            drawCircle(placeholder, side * 0.22f, Offset(center.x, center.y + side * 0.18f))
        } else {
            val origin = IntOffset(((size.width - side) / 2).toInt(), ((size.height - side) / 2).toInt())
            val destination = IntSize(side, side)
            // Keep the approved frame's full transparent canvas at the existing placement.
            drawImage(frame, dstOffset = origin, dstSize = destination,
                filterQuality = if (highResolution) FilterQuality.High else FilterQuality.Low)
        }
    }
}

@Composable
private fun avatarFrame(loader: AvatarAssetLoader<ImageBitmap>, candidates: List<String>): ImageBitmap? =
    key(loader, candidates) {
        val image by produceState(candidates.firstOrNull()?.let(loader::cached)) {
            value = loader.loadFirst(candidates)
        }
        image
    }
