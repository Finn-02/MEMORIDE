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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.kailand.core.contract.AvatarPreview
import io.kailand.core.contract.Catalog

@Composable
fun AvatarView(
    avatar: AvatarPreview,
    catalog: Catalog,
    expressionKey: String,
    modifier: Modifier = Modifier,
) {
    val assets = LocalContext.current.applicationContext.assets
    val loader = remember(assets) { androidAvatarAssetLoader(assets) }
    val paths = remember(avatar, catalog, expressionKey) { AvatarAssetResolver.resolve(avatar, catalog, expressionKey) }
    val body = avatarLayer(loader, listOfNotNull(paths.body))
    val face = if (body != null) avatarLayer(loader, paths.faceCandidates) else null
    val outfit = if (body != null) avatarLayer(loader, listOfNotNull(paths.outfit)) else null
    val placeholder = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)

    Canvas(modifier.size(128.dp)) {
        val side = size.minDimension.toInt()
        if (side <= 0) return@Canvas
        if (body == null) {
            drawCircle(placeholder, side * 0.16f, Offset(center.x, center.y - side * 0.14f))
            drawCircle(placeholder, side * 0.22f, Offset(center.x, center.y + side * 0.18f))
        } else {
            val origin = IntOffset(((size.width - side) / 2).toInt(), ((size.height - side) / 2).toInt())
            val destination = IntSize(side, side)
            // Every layer keeps its full transparent canvas and uses exactly the same destination.
            drawImage(body, dstOffset = origin, dstSize = destination)
            face?.let { drawImage(it, dstOffset = origin, dstSize = destination) }
            outfit?.let { drawImage(it, dstOffset = origin, dstSize = destination) }
        }
    }
}

@Composable
private fun avatarLayer(loader: AvatarAssetLoader<ImageBitmap>, candidates: List<String>): ImageBitmap? =
    key(loader, candidates) {
        val image by produceState<ImageBitmap?>(null) { value = loader.loadFirst(candidates) }
        image
    }
