package io.kailand.feature.island

import io.kailand.core.contract.AvatarPreview
import io.kailand.core.contract.ItemType
import java.awt.image.BufferedImage
import java.io.FileNotFoundException
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.imageio.ImageIO
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AvatarAssetLoaderTest {
    @Test fun approved36FramesResolveDecodeAndStayCachedUnderTheirExactState() = runBlocking {
        val catalog = islandTestCatalog
        val reads = mutableMapOf<String, Int>()
        val loader = AvatarAssetLoader(Dispatchers.Unconfined, cacheCapacity = 36) { path ->
            reads[path] = reads.getOrDefault(path, 0) + 1
            ImageIO.read(islandTestRoot.resolve("app/src/main/assets").resolve(path))
        }
        val outfits = mapOf(null to "none", "daejeon_hat" to "bread_hat",
            "wonju_crown" to "ginkgo_crown", "sokcho_scarf" to "wave_scarf")
        val hashes = mutableSetOf<Int>()
        for (character in catalog.characters) {
            for (expression in listOf("idle", "happy", "eat")) {
                for ((id, outfit) in outfits) {
                    val avatar = AvatarPreview(character.id, id)
                    val layers = AvatarAssetResolver.resolve(avatar, catalog, expression)
                    val frames = AvatarAssetResolver.approvedFrames(avatar, expression, layers)
                    assertEquals("avatars/approved/${character.id}/${expression}__${outfit}.png", frames.first())
                    val frame = checkNotNull(loader.loadFirst(frames))
                    assertEquals(256, frame.width)
                    assertEquals(256, frame.height)
                    assertTrue(frame.colorModel.hasAlpha())
                    assertEquals(0, frame.getRGB(0, 0) ushr 24)
                    hashes += frame.getRGB(0, 0, 256, 256, null, 0, 256).contentHashCode()
                }
            }
        }
        assertEquals(36, hashes.size)
        assertEquals(36, reads.size)
        reads.keys.toList().forEach { path ->
            assertTrue(loader.cached(path) === loader.loadFirst(listOf(path)))
        }
        assertTrue(reads.values.all { it == 1 })
    }

    @Test fun missingApprovedOutfitUsesSameCharacterWithoutOutfit() = runBlocking {
        val avatar = AvatarPreview("momo", "sokcho_scarf")
        val layers = AvatarAssetResolver.resolve(avatar, islandTestCatalog, "eat")
        val frames = AvatarAssetResolver.approvedFrames(avatar, "eat", layers)
        val loader = AvatarAssetLoader(Dispatchers.Unconfined) { path ->
            if (path.endsWith("wave_scarf.png")) throw FileNotFoundException() else path
        }
        assertEquals(listOf("avatars/approved/momo/eat__wave_scarf.png", "avatars/approved/momo/eat__none.png",
            "avatars/approved/momo/idle__none.png"), frames)
        assertEquals(frames[1], loader.loadFirst(frames))
        assertNull(loader.cached(frames[0]))
        val invalid = AvatarPreview("momo", "daejeon_bread")
        val fallback = AvatarAssetResolver.resolve(invalid, islandTestCatalog, "unknown")
        assertEquals(listOf("avatars/approved/momo/idle__none.png"),
            AvatarAssetResolver.approvedFrames(invalid, "unknown", fallback))
    }

    @Test fun invalidCatalogOrMissingFramesNeverSelectStarterAvatarLayers() = runBlocking {
        val avatar = AvatarPreview("tori", "daejeon_hat")
        val catalog = islandTestCatalog
        val incomplete = catalog.copy(assets = catalog.assets - setOf("face_eat", "face_idle", "outfit_bread_hat"))
        val frames = AvatarAssetResolver.approvedFrames(avatar, "eat",
            AvatarAssetResolver.resolve(avatar, incomplete, "eat"))
        assertEquals(listOf("avatars/approved/tori/idle__none.png"), frames)
        val attempted = mutableListOf<String>()
        val loader = AvatarAssetLoader<String>(Dispatchers.Unconfined) { path -> attempted += path; null }
        assertNull(loader.loadFirst(frames))
        assertEquals(frames, attempted)
        val wrongBody = AvatarAssetResolver.resolve(avatar, catalog, "eat").copy(body = png("char_momo"))
        assertTrue(AvatarAssetResolver.approvedFrames(avatar, "eat", wrongBody).isEmpty())
    }

    @Test fun all36CombinationsCanBeCompositedFromRealPngsOnTheJvm() = runBlocking {
        val catalog = islandTestCatalog
        val reads = mutableMapOf<String, Int>()
        // Host decoder. The Android ImageDecoder adapter requires a device or emulator.
        val loader = AvatarAssetLoader(Dispatchers.Unconfined) { path ->
            reads[path] = reads.getOrDefault(path, 0) + 1
            ImageIO.read(islandTestRoot.resolve(path)).also {
                assertEquals(256, it.width)
                assertEquals(256, it.height)
                assertTrue(it.colorModel.hasAlpha())
            }
        }
        val combinations = mutableSetOf<Int>()
        val outfits = listOf(null) + catalog.items.filter { it.type == ItemType.OUTFIT }.map { it.id }
        for (character in catalog.characters) {
            for (expression in listOf("idle", "happy", "eat")) {
                for (outfit in outfits) {
                    val paths = AvatarAssetResolver.resolve(AvatarPreview(character.id, outfit), catalog, expression)
                    val body = checkNotNull(loader.loadFirst(listOf(checkNotNull(paths.body))))
                    val face = checkNotNull(loader.loadFirst(paths.faceCandidates))
                    val clothing = paths.outfit?.let { checkNotNull(loader.loadFirst(listOf(it))) }
                    val composite = BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB)
                    val canvas = composite.createGraphics()
                    try {
                        for (layer in listOfNotNull(body, face, clothing)) canvas.drawImage(layer, 0, 0, null)
                    } finally {
                        canvas.dispose()
                    }
                    assertEquals(0, composite.getRGB(0, 0) ushr 24)
                    assertEquals(255, composite.getRGB(102, 237) ushr 24)
                    combinations += composite.getRGB(0, 0, 256, 256, null, 0, 256).contentHashCode()
                }
            }
        }
        assertEquals(36, combinations.size)
        assertEquals(9, reads.size)
        assertTrue(reads.values.all { it == 1 })
    }

    @Test fun missingCorruptAndRejectedDecodesFallBackWithoutCachingFailures() = runBlocking {
        val missing = png("missing")
        val corrupt = png("corrupt")
        val invalid = png("invalid_size")
        val idle = png("face_idle")
        val attempts = mutableListOf<String>()
        val loader = AvatarAssetLoader(Dispatchers.Unconfined) { path ->
            attempts += path
            when (path) {
                missing -> throw FileNotFoundException()
                corrupt -> throw IOException("Truncated PNG")
                invalid -> null
                else -> path
            }
        }
        val candidates = listOf(missing, corrupt, invalid, idle)
        assertEquals(idle, loader.loadFirst(candidates))
        assertEquals(idle, loader.loadFirst(candidates))
        assertEquals(candidates + candidates.dropLast(1), attempts)
        assertNull(loader.loadFirst(listOf(missing, corrupt, invalid)))
    }

    @Test fun unsafeAndEmptyCandidateListsNeverReachDecoder() = runBlocking {
        val loader = AvatarAssetLoader<String>(Dispatchers.Unconfined) { error("Unexpected decode") }
        assertNull(loader.loadFirst(emptyList()))
        assertNull(loader.loadFirst(listOf("assets/starter/../char_tori.png", "content/catalog.json")))
    }

    @Test fun cacheEvictsLeastRecentlyUsedLayerAfterNineEntries() = runBlocking {
        val reads = mutableListOf<String>()
        val loader = AvatarAssetLoader(Dispatchers.Unconfined) { path -> reads += path; path }
        val paths = (1..10).map { png("layer_$it") }
        paths.take(9).forEach { assertEquals(it, loader.loadFirst(listOf(it))) }
        loader.loadFirst(listOf(paths[0]))
        loader.loadFirst(listOf(paths[9]))
        loader.loadFirst(listOf(paths[0]))
        loader.loadFirst(listOf(paths[1]))
        assertEquals(paths + paths[1], reads)
    }

    @Test fun cancellingDecodeDoesNotPublishOrCacheTheOldResult() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CountDownLatch(1)
        val reads = AtomicInteger()
        val path = png("char_tori")
        val loader = AvatarAssetLoader(Dispatchers.IO) {
            if (reads.incrementAndGet() == 1) {
                entered.complete(Unit)
                check(release.await(5, TimeUnit.SECONDS))
            }
            it
        }
        var delivered = false
        val job = launch {
            loader.loadFirst(listOf(path))
            delivered = true
        }
        try {
            withTimeout(5_000) { entered.await() }
            job.cancel()
        } finally {
            release.countDown()
            job.cancelAndJoin()
        }
        assertFalse(delivered)
        assertEquals(path, loader.loadFirst(listOf(path)))
        assertEquals(2, reads.get())
    }

    private fun png(key: String) = "assets/starter/$key.png"
}
