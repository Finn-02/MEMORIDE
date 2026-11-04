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
