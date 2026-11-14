package io.kailand.feature.island

import android.content.res.AssetManager
import android.graphics.ImageDecoder
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import io.kailand.core.contract.Catalog
import io.kailand.core.contract.ItemType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Existing catalog IDs only. Art files are keyed by (assetKey, saved slotId).
private val supportedDecorAssets = setOf(
    "decor_bakery_sign", "decor_ginkgo", "decor_shell",
)
private val supportedSlots = setOf("slot_left", "slot_right", "slot_back")

/** Never display decoded images for an older decoration-slot selection. */
internal data class DecorationFrameBatch<T>(
    val paths: List<String>,
    val frames: List<T>,
) {
    fun forRequest(requested: List<String>): List<T> =
        if (paths == requested) frames else emptyList()
}

private sealed interface DecorationImages {
    data object Loading : DecorationImages
    data class Failed(val paths: List<String>) : DecorationImages
    data class Ready(val batch: DecorationFrameBatch<ImageBitmap>) : DecorationImages
}

private fun decodeDecoration(assets: AssetManager, filename: String): ImageBitmap {
    // The filename is derived solely from the fixed asset/slot allowlists below.
    val bitmap = ImageDecoder.decodeBitmap(
        ImageDecoder.createSource(assets, "assets/starter/$filename")
    ) { decoder, info, _ ->
        require(info.mimeType == "image/png" &&
                info.size.width == 1024 && info.size.height == 1024) {
            "Unexpected decoration PNG dimensions: $filename"
        }
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        decoder.setOnPartialImageListener { false }
    }
    require(bitmap.hasAlpha()) { "Decoration layer must have alpha: $filename" }
    return bitmap.asImageBitmap()
}

/** Normalized visible bounds from the same fixed-slot PNG that the scene draws. */
internal data class DecorationPlacementArea(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val centerX: Float get() = (left + right) / 2f
}

/** Read-only marker geometry; no guessed slot coordinates or new saved positions. */
internal suspend fun decorationPlacementAreas(
    assets: AssetManager, assetKey: String?,
): Map<String, DecorationPlacementArea> = withContext(Dispatchers.IO) {
    if (assetKey == null || assetKey !in supportedDecorAssets) return@withContext emptyMap()
    supportedSlots.mapNotNull { slotId ->
        try {
            val bitmap = decodeDecoration(assets, "${assetKey}_${slotId}_v3.png").asAndroidBitmap()
            try {
                var left = bitmap.width
                var top = bitmap.height
                var right = -1
                var bottom = -1
                val row = IntArray(bitmap.width)
                for (y in 0 until bitmap.height) {
                    bitmap.getPixels(row, 0, bitmap.width, 0, y, bitmap.width, 1)
                    for (x in row.indices) {
                        // Ignore the faint contact shadow and transparent padding.
                        if ((row[x] ushr 24) >= 128) {
                            left = minOf(left, x)
                            top = minOf(top, y)
                            right = maxOf(right, x)
                            bottom = maxOf(bottom, y)
                        }
                    }
                }
                if (right < left || bottom < top) null else slotId to DecorationPlacementArea(
                    left.toFloat() / bitmap.width, top.toFloat() / bitmap.height,
                    (right + 1f) / bitmap.width, (bottom + 1f) / bitmap.height,
                )
            } finally {
                // This decode is temporary geometry data, never a displayed/shared image.
                bitmap.recycle()
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.w("MemorideIsland", "Cannot read decoration marker bounds: $slotId", error)
            null
        }
    }.toMap()
}

// IslandPresentation already verifies ownership, known slot IDs and
// unambiguous placement. Revalidate the Catalog asset type and path here.
internal fun decorationPaths(slots: List<IslandSlot>, catalog: Catalog): List<String> =
    slots.mapNotNull { slot ->
        if (!slot.available || slot.savedItemId == null || slot.itemName == null ||
            slot.id !in supportedSlots) return@mapNotNull null
        val item = catalog.items.singleOrNull {
            it.id == slot.savedItemId && it.type == ItemType.DECOR
        } ?: return@mapNotNull null
        val assetKey = item.assetKey
        if (assetKey !in supportedDecorAssets) return@mapNotNull null
        val asset = catalog.assets[assetKey]
        if (asset == null || asset.kind != "decor" || asset.path != "assets/starter/$assetKey.png") {
            return@mapNotNull null
        }
        "${assetKey}_${slot.id}_v3.png"
    }

/** Read-only rendering: the existing inventory owns all place/remove actions. */
@Composable
internal fun IslandDecorationLayers(
    slots: List<IslandSlot>,
    catalog: Catalog,
    modifier: Modifier = Modifier,
) {
    val assets = LocalContext.current.applicationContext.assets
    val requested = decorationPaths(slots, catalog)

    val images by produceState<DecorationImages>(
        initialValue = if (requested.isEmpty()) DecorationImages.Ready(DecorationFrameBatch(requested, emptyList()))
                       else DecorationImages.Loading,
        key1 = assets,
        key2 = requested,
    ) {
        value = try {
            val loaded = withContext(Dispatchers.IO) {
                requested.map { decodeDecoration(assets, it) }
            }
            DecorationImages.Ready(DecorationFrameBatch(requested, loaded))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e("MemorideIsland", "Cannot load saved island decorations", error)
            DecorationImages.Failed(requested)
        }
    }

    Box(modifier) {
        when (val result = images) {
            // produceState retains the last value until a new load completes.
            // Discard that value immediately if the slot paths have changed.
            is DecorationImages.Ready -> result.batch.forRequest(requested).forEach { layer ->
                Image(
                    bitmap = layer,
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Fit,
                )
            }
            is DecorationImages.Failed -> if (result.paths == requested) Text(
                "장식 이미지를 불러오지 못했어요.",
                color = MaterialTheme.colorScheme.error,
            )
            DecorationImages.Loading -> Unit
        }
    }
}
