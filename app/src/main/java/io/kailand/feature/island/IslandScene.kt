package io.kailand.feature.island

import android.content.res.AssetManager
import android.graphics.ImageDecoder
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.kailand.core.contract.AvatarPreview
import io.kailand.core.contract.Catalog

private val islandFiles = listOf(
    "island_base_v3.png",
    "growth_path_v3.png",
    "growth_flowers_v3.png",
    "growth_rest_area_v3.png",
)

internal fun islandLayerFiles(growth: IslandGrowth): List<String> = islandFiles.take(growth.ordinal + 1)

private sealed interface IslandImages {
    data object Loading : IslandImages
    data object Failed : IslandImages
    data class Ready(val layers: Map<String, ImageBitmap>) : IslandImages
}

private fun readIslandLayer(
    assets: AssetManager,
    name: String,
): ImageBitmap {
    val path = "assets/starter/$name"

    val bitmap = ImageDecoder.decodeBitmap(
        ImageDecoder.createSource(assets, path)
    ) { decoder, info, _ ->
        require(info.size.width == 1024 && info.size.height == 1024) {
            "Invalid island image dimensions: $name"
        }
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
    }

    require(bitmap.hasAlpha()) {
        "Island image requires alpha: $name"
    }

    return bitmap.asImageBitmap()
}

@Composable
internal fun IslandSceneCard(
    growth: IslandGrowth?,
    avatar: AvatarPreview,
    catalog: Catalog,
    characterName: String,
    eating: Boolean,
    slots: List<IslandSlot>,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = growth?.let {
                    "성장 ${it.ordinal}단계 · ${it.label}"
                } ?: "성장 상태 확인 필요",
                style = MaterialTheme.typography.titleMedium,
            )

            IslandScene(growth, avatar, catalog, characterName, eating, slots)
        }
    }
}

@Composable
internal fun IslandScene(
    growth: IslandGrowth?,
    avatar: AvatarPreview,
    catalog: Catalog,
    characterName: String,
    eating: Boolean,
    slots: List<IslandSlot>,
    avatarScale: Float = 1f,
) {
    val assets = LocalContext.current.applicationContext.assets

    val images by produceState<IslandImages>(
        initialValue = IslandImages.Loading,
        key1 = assets,
    ) {
        value = withContext(Dispatchers.IO) {
            try {
                IslandImages.Ready(
                    islandFiles.associateWith { readIslandLayer(assets, it) }
                )
            } catch (error: Exception) {
                Log.e("MemorideIsland", "Island layer load failure", error)
                IslandImages.Failed
            }
        }
    }

    when {
        growth == null -> {
            Text("저장된 성장 단계를 확인해 주세요.")
        }
        images is IslandImages.Loading -> {
            Text("섬 이미지를 불러오는 중…")
        }
        images is IslandImages.Failed -> {
            Text("섬 이미지를 불러오지 못했어요.")
        }
        images is IslandImages.Ready -> {
            val layers = (images as IslandImages.Ready).layers

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .semantics {
                        contentDescription =
                            "성장 ${growth.ordinal}단계 섬. ${growth.label}. " +
                                "캐릭터 $characterName"
                    },
            ) {
                // All images share one 1024px alpha canvas and fixed Blender camera.
                islandLayerFiles(growth).forEach { file ->
                    Image(
                        bitmap = layers.getValue(file),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.matchParentSize(),
                    )
                }

                // Overlay the CURRENT saved avatar, not a hardcoded character.
                // The avatar remains independently composited body/face/outfit.
                val originalSide = (maxWidth * 0.22f).coerceIn(58.dp, 92.dp)
                val side = originalSide * avatarScale
                val x = maxWidth * 0.48f - side / 2f
                // Keep the feet grounded at the same point while increasing the avatar.
                val y = maxWidth * 0.47f + originalSide * 0.395f - side * 0.895f
                // Fixed-slot render is placed behind the character.
                IslandDecorationLayers(
                    slots = slots,
                    catalog = catalog,
                    modifier = Modifier.matchParentSize(),
                )
                // MEMORIDE_SOFT_CONTACT_SHADOW
                // One smoothly fading ellipse. Same avatar anchor; no saved-state change.
                val shadowWidth = side * 0.70f
                val shadowHeight = side * 0.16f
                Canvas(
                    modifier = Modifier
                        .offset(
                            x = x + (side - shadowWidth) / 2f,
                            y = y + side * 0.895f,
                        )
                        .size(shadowWidth, shadowHeight),
                ) {
                    val midpoint = Offset(size.width / 2f, size.height / 2f)
                    val r = size.width / 2f
                    // Radial shader and circle are both compressed vertically.
                    // Unlike layered drawOval calls, there are no visible rings.
                    withTransform({
                        scale(
                            scaleX = 1f,
                            scaleY = size.height / size.width,
                            pivot = midpoint,
                        )
                    }) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                0.0f to Color(0xFF304333).copy(alpha = 0.18f),
                                0.35f to Color(0xFF304333).copy(alpha = 0.11f),
                                0.72f to Color(0xFF304333).copy(alpha = 0.035f),
                                1.0f to Color.Transparent,
                                center = midpoint,
                                radius = r,
                            ),
                            radius = r,
                            center = midpoint,
                        )
                    }
                }

                AvatarView(
                    avatar = avatar,
                    catalog = catalog,
                    expressionKey = if (eating) "eat" else "idle",
                    modifier = Modifier.offset(x = x, y = y),
                    avatarSize = side,
                    highResolution = avatarScale > 1f,
                )
            }
        }
    }
}
