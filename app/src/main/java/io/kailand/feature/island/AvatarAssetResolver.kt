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

internal fun isAvatarAssetPath(path: String): Boolean = starterPngPath.matches(path)

internal object AvatarAssetResolver {
    fun resolve(avatar: AvatarPreview, catalog: Catalog, expressionKey: String): AvatarLayerPaths {
        fun path(assetKey: String?, kind: String): String? =
            catalog.assets[assetKey]?.takeIf { it.kind == kind && isAvatarAssetPath(it.path) }?.path

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
}
