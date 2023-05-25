package com.spectre7.spmp.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import com.spectre7.spmp.platform.PlatformContext
import com.spectre7.spmp.platform.toByteArray
import com.spectre7.spmp.platform.toImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import java.io.File
import java.net.SocketTimeoutException
import java.util.concurrent.ExecutorService

class ThumbState(
    private val item: MediaItem,
    val quality: MediaItemThumbnailProvider.Quality,
    private val downloadThumbnail: (MediaItemThumbnailProvider.Quality) -> Result<ImageBitmap>
) {
    var loading: Boolean by mutableStateOf(false)
    var loaded: Boolean by mutableStateOf(false)
    var image: ImageBitmap? by mutableStateOf(null)

    private val load_lock = Object()

    suspend fun load(context: PlatformContext): Result<ImageBitmap> = withContext(Dispatchers.IO) {
        synchronized(load_lock) {
            if (image != null) {
                return@withContext Result.success(image!!)
            }

            if (loading) {
                load_lock.wait()
                return@withContext Result.success(image!!)
            }
            loading = true
            loaded = true
        }

        this.coroutineContext.job.invokeOnCompletion {
            synchronized(load_lock) {
                loading = false
                load_lock.notifyAll()
            }
        }

        return@withContext performLoad(context)
    }

    private fun performLoad(context: PlatformContext): Result<ImageBitmap> {
        val cache_file = getCacheFile(context)
        if (cache_file.exists()) {
            cache_file.readBytes().toImageBitmap().also {
                image = it
                loading = false
                return Result.success(it)
            }
        }

        val result: Result<ImageBitmap>
        try {
            result = downloadThumbnail(quality)
            result.fold(
                {
                    image = it
                },
                {
                    return result
                }
            )
        }
        catch (e: SocketTimeoutException) {
            loading = false
            return Result.failure(e)
        }

        if (Settings.KEY_THUMB_CACHE_ENABLED.get()) {
            cache_file.parentFile.mkdirs()
            cache_file.writeBytes(image!!.toByteArray())
        }

        return result
    }

    fun getCacheFile(context: PlatformContext): File =
        context.getCacheDir().resolve("thumbnails/${item.id}.${quality.ordinal}.png")
}