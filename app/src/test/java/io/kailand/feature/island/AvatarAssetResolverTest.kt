package io.kailand.feature.island

import io.kailand.core.contract.AssetDefinition
import io.kailand.core.contract.AvatarPreview
import io.kailand.core.contract.ItemType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AvatarAssetResolverTest {
    private val catalog = islandTestCatalog

    @Test fun all36CombinationsResolveTheExpectedFullCanvasLayers() {
        val characters = mapOf("tori" to png("char_tori"), "momo" to png("char_momo"), "lulu" to png("char_lulu"))
        val expressions = mapOf("idle" to png("face_idle"), "happy" to png("face_happy"), "eat" to png("face_eat"))
        val outfits = mapOf(null to null, "daejeon_hat" to png("outfit_bread_hat"),
            "wonju_crown" to png("outfit_ginkgo_crown"), "sokcho_scarf" to png("outfit_wave_scarf"))
        var cases = 0
        for ((character, body) in characters) {
            for ((expression, face) in expressions) {
                for ((outfitId, outfit) in outfits) {
                    val paths = AvatarAssetResolver.resolve(AvatarPreview(character, outfitId), catalog, expression)
                    assertEquals(
                        "$character/$expression/$outfitId",
                        AvatarLayerPaths(body, listOf(face, png("face_idle")).distinct(), outfit),
                        paths,
                    )
                    cases++
                }
            }
        }
        assertEquals(36, cases)
    }

    @Test fun unknownOrDuplicateBodyUsesPlaceholderWithoutDetachedLayers() {
        val missing = AvatarAssetResolver.resolve(AvatarPreview("unknown", "daejeon_hat"), catalog, "eat")
        val ambiguous = catalog.copy(characters = catalog.characters + catalog.characters.first { it.id == "tori" })
        val duplicate = AvatarAssetResolver.resolve(AvatarPreview("tori", "daejeon_hat"), ambiguous, "eat")
        assertEquals(AvatarLayerPaths(null), missing)
        assertEquals(AvatarLayerPaths(null), duplicate)
    }

    @Test fun unknownExpressionUsesIdleEvenIfCatalogDefinesThatKey() {
        val extended = catalog.copy(expressions = catalog.expressions + ("custom" to "face_eat"))
        val paths = AvatarAssetResolver.resolve(AvatarPreview("tori", null), extended, "custom")
        assertEquals(listOf(png("face_idle")), paths.faceCandidates)
    }

    @Test fun missingOrWrongKindFaceUsesIdleThenCanBeOmitted() {
        val wrongKind = catalog.copy(expressions = catalog.expressions + ("eat" to "char_tori"))
        val paths = AvatarAssetResolver.resolve(AvatarPreview("tori", null), wrongKind, "eat")
        assertEquals(listOf(png("face_idle")), paths.faceCandidates)
        val missing = wrongKind.copy(assets = wrongKind.assets - "face_idle")
        assertTrue(AvatarAssetResolver.resolve(AvatarPreview("tori", null), missing, "eat").faceCandidates.isEmpty())
    }

    @Test fun outfitMustBeAUniqueOutfitItemWithAnOutfitAsset() {
        val item = catalog.items.single { it.id == "daejeon_hat" }
        val duplicate = catalog.copy(items = catalog.items + item.copy(type = ItemType.FOOD))
        val wrongKind = catalog.copy(assets = catalog.assets + (item.assetKey to AssetDefinition(png("face_idle"), "face")))
        val missing = catalog.copy(assets = catalog.assets - item.assetKey)
        for (invalid in listOf(duplicate, wrongKind, missing)) {
            assertNull(AvatarAssetResolver.resolve(AvatarPreview("tori", item.id), invalid, "idle").outfit)
        }
        for (id in listOf("daejeon_bread", "unknown", null)) {
            assertNull(AvatarAssetResolver.resolve(AvatarPreview("tori", id), catalog, "idle").outfit)
        }
    }

    @Test fun unsafePathsNeverResolve() {
        val paths = listOf("assets/starter/../char_tori.png", "assets/starter/nested/char_tori.png",
            "assets/starter\\char_tori.png", "/assets/starter/char_tori.png", "assets/starter/char_tori.svg",
            "https://example.com/char_tori.png")
        for (path in paths) {
            val invalid = catalog.copy(assets = catalog.assets + ("char_tori" to AssetDefinition(path, "body")))
            assertEquals(path, AvatarLayerPaths(null),
                AvatarAssetResolver.resolve(AvatarPreview("tori", null), invalid, "idle"))
        }
    }

    private fun png(key: String) = "assets/starter/$key.png"
}
