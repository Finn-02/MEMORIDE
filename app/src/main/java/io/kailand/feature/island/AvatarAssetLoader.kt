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

/** Shares successful decodes and bounds memory to the nine starter avatar layers. */
internal class AvatarAssetLoader<T : Any>(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val decode: (String) -> T?,
) {
    private val mutex = Mutex()
    private val cache = LinkedHashMap<String, T>(9, 0.75f, true)

    suspend fun loadFirst(candidates: List<String>): T? {
        for (path in candidates) {
            currentCoroutineContext().ensureActive()
            if (!isAvatarAssetPath(path)) continue
            val image = withContext(dispatcher) {
                mutex.withLock {
                    cache[path] ?: decodeSafely(path)?.also {
                        currentCoroutineContext().ensureActive()
                        cache[path] = it
                        if (cache.size > 9) cache.remove(cache.keys.first())
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

@Synchronized
internal fun androidAvatarAssetLoader(assets: AssetManager): AvatarAssetLoader<ImageBitmap> {
    if (cachedAssets === assets) sharedLoader?.let { return it }
    return AvatarAssetLoader { path -> decodeAvatarPng(assets, path) }.also {
        cachedAssets = assets
        sharedLoader = it
    }
}

private fun decodeAvatarPng(assets: AssetManager, path: String): ImageBitmap? {
    val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(assets, path)) { decoder, info, _ ->
        if (info.mimeType != "image/png" ||
            info.size.width != AVATAR_CANVAS_SIZE || info.size.height != AVATAR_CANVAS_SIZE
        ) {
            throw IOException("Avatar layer must be a 256 × 256 PNG")
        }
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        decoder.setTargetSize(AVATAR_CANVAS_SIZE, AVATAR_CANVAS_SIZE)
        decoder.setOnPartialImageListener { false }
    }
    return bitmap.takeIf { it.width == AVATAR_CANVAS_SIZE && it.height == AVATAR_CANVAS_SIZE && it.hasAlpha() }
        ?.asImageBitmap()
}
