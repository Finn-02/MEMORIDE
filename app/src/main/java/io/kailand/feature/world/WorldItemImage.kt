package io.kailand.feature.world

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private sealed interface WorldImage {
    data object Loading : WorldImage
    data object Missing : WorldImage
    data class Loaded(val bitmap: ImageBitmap) : WorldImage
}

@Composable
internal fun WorldItemImage(path: String?, label: String) {
    val assets = LocalContext.current.assets
    key(path, assets) {
        val result by produceState<WorldImage>(WorldImage.Loading, path, assets) {
            value = WorldImage.Loading
            value = if (path == null || !safeWorldImagePath(path)) WorldImage.Missing else withContext(Dispatchers.IO) {
                try {
                    assets.open(path).use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()?.let { WorldImage.Loaded(it) } ?: WorldImage.Missing
                    }
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { WorldImage.Missing }
            }
        }
        when (val image = result) {
            WorldImage.Loading -> Text("이미지 불러오는 중")
            WorldImage.Missing -> Text("◇ 이미지 없음 · $label")
            is WorldImage.Loaded -> Image(image.bitmap, label, Modifier.size(64.dp))
        }
    }
}
