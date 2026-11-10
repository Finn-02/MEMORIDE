package io.kailand.feature.island

import io.kailand.core.contract.AvatarPreview
import io.kailand.core.contract.Catalog
import io.kailand.core.contract.ItemType

internal const val AVATAR_CANVAS_SIZE = 256

internal data class AvatarLayerPaths(
    val body: String?,
    val faceCandidates: List<String> = emptyList(),
    val outfit: String? = null,
)

private val starterPngPath = Regex("assets/starter/[a-zA-Z0-9_][a-zA-Z0-9_-]*\\.png")
private val approvedPngPath = Regex(
    "avatars/approved/(tori|momo|lulu)/(idle|happy|eat)__(none|bread_hat|ginkgo_crown|wave_scarf)\\.png",
)

internal fun isAvatarAssetPath(path: String): Boolean =
    starterPngPath.matches(path) || approvedPngPath.matches(path)

internal object AvatarAssetResolver {
    fun resolve(avatar: AvatarPreview, catalog: Catalog, expressionKey: String): AvatarLayerPaths {
        fun path(assetKey: String?, kind: String): String? =
            catalog.assets[assetKey]?.takeIf { it.kind == kind && starterPngPath.matches(it.path) }?.path

        val character = catalog.characters.singleOrNull { it.id == avatar.characterId && it.id.isNotBlank() }
        val body = path(character?.assetKey, "body") ?: return AvatarLayerPaths(body = null)

        val expression = when (expressionKey) {
            "happy", "eat" -> expressionKey
            else -> "idle"
        }
        val faces = listOf(expression, "idle").mapNotNull { path(catalog.expressions[it], "face") }.distinct()

        // Check uniqueness before type so an ambiguous ID can never select a layer.
        val item = catalog.items.singleOrNull { it.id == avatar.outfitItemId && it.id.isNotBlank() }
        val outfit = path(item?.takeIf { it.type == ItemType.OUTFIT }?.assetKey, "outfit")
        return AvatarLayerPaths(body, faces, outfit)
    }

    fun approvedFrames(avatar: AvatarPreview, expressionKey: String, layers: AvatarLayerPaths): List<String> {
        val character = when (avatar.characterId) {
            "tori", "momo", "lulu" -> avatar.characterId
            else -> return emptyList()
        }
        if (layers.body != "assets/starter/char_$character.png") return emptyList()
        val expression = when (expressionKey) {
            "happy", "eat" -> expressionKey
            else -> "idle"
        }
        val face = when (layers.faceCandidates.firstOrNull()) {
            "assets/starter/face_$expression.png" -> expression
            else -> "idle"
        }
        // The saved ID is resolved through the Catalog, including type and uniqueness checks.
        val outfit = when (layers.outfit) {
            "assets/starter/outfit_bread_hat.png" -> "bread_hat"
            "assets/starter/outfit_ginkgo_crown.png" -> "ginkgo_crown"
            "assets/starter/outfit_wave_scarf.png" -> "wave_scarf"
            else -> "none"
        }
        val base = "avatars/approved/$character"
        return listOf("$base/${face}__${outfit}.png", "$base/${face}__none.png", "$base/idle__none.png").distinct()
    }
}
