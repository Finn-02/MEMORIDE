package io.kailand.feature.island

import android.content.res.AssetManager
import android.graphics.ImageDecoder
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Shares successful decodes up to the configured cache capacity. */
internal class AvatarAssetLoader<T : Any>(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val cacheCapacity: Int = 9,
    private val decode: (String) -> T?,
) {
    private val mutex = Mutex()
    private val cache = LinkedHashMap<String, T>(cacheCapacity, 0.75f, true)

    fun cached(path: String): T? = synchronized(cache) { cache[path] }

    suspend fun loadFirst(candidates: List<String>): T? {
        for (path in candidates) {
            currentCoroutineContext().ensureActive()
            if (!isAvatarAssetPath(path)) continue
            cached(path)?.let { return it }
            val image = withContext(dispatcher) {
                mutex.withLock {
                    cached(path) ?: decodeSafely(path)?.also {
                        currentCoroutineContext().ensureActive()
                        synchronized(cache) {
                            cache[path] = it
                            if (cache.size > cacheCapacity) cache.remove(cache.keys.first())
                        }
                    }
                }
            }
            if (image != null) return image
        }
        return null
    }

    private fun decodeSafely(path: String): T? = try {
        decode(path)
    } catch (_: IOException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}

private var cachedAssets: AssetManager? = null
private var sharedLoader: AvatarAssetLoader<ImageBitmap>? = null
private var sharedInventoryLoader: AvatarAssetLoader<ImageBitmap>? = null

@Synchronized
internal fun androidAvatarAssetLoader(assets: AssetManager, highResolution: Boolean = false): AvatarAssetLoader<ImageBitmap> {
    if (cachedAssets !== assets) {
        cachedAssets = assets
        sharedLoader = null
        sharedInventoryLoader = null
    }
    if (highResolution) {
        sharedInventoryLoader?.let { return it }
        // Six 768px frames bound the larger inventory cache to about 13.5MiB.
        return AvatarAssetLoader<ImageBitmap>(cacheCapacity = 6) { path -> decodeAvatarPng(assets, path) }
            .also { sharedInventoryLoader = it }
    }
    sharedLoader?.let { return it }
    return AvatarAssetLoader<ImageBitmap>(cacheCapacity = 36) { path -> decodeAvatarPng(assets, path) }
        .also { sharedLoader = it }
}

private fun decodeAvatarPng(assets: AssetManager, path: String): ImageBitmap? {
    val sourceSize = if (path.startsWith("avatars/approved_hd/")) 1024 else AVATAR_CANVAS_SIZE
    val decodedSize = if (sourceSize == 1024) 768 else AVATAR_CANVAS_SIZE
    val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(assets, path)) { decoder, info, _ ->
        if (info.mimeType != "image/png" ||
            info.size.width != sourceSize || info.size.height != sourceSize
        ) {
            throw IOException("Avatar layer dimensions do not match its approved asset family")
        }
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        decoder.setTargetSize(decodedSize, decodedSize)
        decoder.setOnPartialImageListener { false }
    }
    return bitmap.takeIf { it.width == decodedSize && it.height == decodedSize && it.hasAlpha() }
        ?.asImageBitmap()
}
