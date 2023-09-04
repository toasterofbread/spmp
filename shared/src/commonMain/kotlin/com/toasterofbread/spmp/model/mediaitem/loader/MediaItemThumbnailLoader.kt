package com.toasterofbread.spmp.model.mediaitem.loader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.platform.toByteArray
import com.toasterofbread.spmp.platform.toImageBitmap
import com.toasterofbread.utils.common.addUnique
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.ref.WeakReference
import kotlin.concurrent.withLock

internal data class MediaItemThumbnailLoaderKey(
    val provider: MediaItemThumbnailProvider,
    val quality: MediaItemThumbnailProvider.Quality,
    val item_id: String
)

internal object MediaItemThumbnailLoader: ListenerLoader<MediaItemThumbnailLoaderKey, ImageBitmap>() {
    private var loaded_images: MutableMap<MediaItemThumbnailLoaderKey, WeakReference<ImageBitmap>> = mutableMapOf()

    fun getLoadedItemThumbnail(item: MediaItem, quality: MediaItemThumbnailProvider.Quality, thumbnail_provider: MediaItemThumbnailProvider): ImageBitmap? {
        val key = MediaItemThumbnailLoaderKey(thumbnail_provider, quality, item.id)
        synchronized(loaded_images) {
            return loaded_images[key]?.get()
        }
    }

    suspend fun loadItemThumbnail(
        item: MediaItem,
        quality: MediaItemThumbnailProvider.Quality,
        context: PlatformContext
    ): Result<ImageBitmap> {
        val thumbnail_provider = item.ThumbnailProvider.get(context.database)
        if (thumbnail_provider == null) {
            return Result.failure(RuntimeException("Item has no ThumbnailProvider"))
        }
        return loadItemThumbnail(item, thumbnail_provider, quality, context)
    }

    suspend fun loadItemThumbnail(
        item: MediaItem,
        thumbnail_provider: MediaItemThumbnailProvider,
        quality: MediaItemThumbnailProvider.Quality,
        context: PlatformContext,
        disable_cache_read: Boolean = false,
        disable_cache_write: Boolean = false
    ): Result<ImageBitmap> {
        val key = MediaItemThumbnailLoaderKey(thumbnail_provider, quality, item.id)

        val loaded = loaded_images[key]?.get()
        if (loaded != null) {
            return Result.success(loaded)
        }

        val thumbnail_url = thumbnail_provider.getThumbnailUrl(quality)
            ?: return Result.failure(RuntimeException("No thumbnail URL available"))

        return performLoad(key) {
            val result = performLoad(
                item,
                quality,
                thumbnail_url,
                context,
                disable_cache_read,
                disable_cache_write
            )

            if (!disable_cache_write) {
                result.onSuccess { image ->
                    synchronized(loaded_images) {
                        loaded_images[key] = WeakReference(image)
                    }
                }
            }

            return@performLoad result
        }
    }

    suspend fun invalidateCache(item: MediaItem, context: PlatformContext) = withContext(Dispatchers.IO) {
        for (quality in MediaItemThumbnailProvider.Quality.values()) {
            val file = getCacheFile(item, quality, context)
            file.delete()
        }

        synchronized(loaded_images) {
            loaded_images = loaded_images.filterKeys { key ->
                key.item_id != item.id
            } as MutableMap<MediaItemThumbnailLoaderKey, WeakReference<ImageBitmap>>
        }
    }

    private fun getCacheFile(item: MediaItem, quality: MediaItemThumbnailProvider.Quality, context: PlatformContext): File =
        context.getCacheDir().resolve("thumbnails/${item.id}.${quality.ordinal}.png")

    private suspend fun performLoad(
        item: MediaItem,
        quality: MediaItemThumbnailProvider.Quality,
        thumbnail_url: String,
        context: PlatformContext,
        disable_cache_read: Boolean = false,
        disable_cache_write: Boolean = false
    ): Result<ImageBitmap> = withContext(Dispatchers.IO) {
        val cache_file = getCacheFile(item, quality, context)
        if (!disable_cache_read && cache_file.exists()) {
            return@withContext runCatching {
                cache_file.readBytes().toImageBitmap()
            }
        }

        val result: Result<ImageBitmap> = item.downloadThumbnailData(thumbnail_url)
        result.onSuccess { image ->
            if (!disable_cache_write && Settings.KEY_THUMB_CACHE_ENABLED.get()) {
                try {
                    cache_file.parentFile.mkdirs()
                    cache_file.writeBytes(image.toByteArray())
                }
                finally {}
            }
        }

        return@withContext result
    }

    interface ItemState {
        val loaded_images: Map<MediaItemThumbnailProvider.Quality, WeakReference<ImageBitmap>>
        val loading_images: List<MediaItemThumbnailProvider.Quality>
    }

    @Composable
    fun rememberItemState(item: MediaItem, context: PlatformContext): ItemState {
        val state = remember(item) {
            object : ItemState {
                override val loaded_images: MutableMap<MediaItemThumbnailProvider.Quality, WeakReference<ImageBitmap>> = mutableStateMapOf()
                override val loading_images: MutableList<MediaItemThumbnailProvider.Quality> = mutableStateListOf()
            }
        }

        LaunchedEffect(state) {
            withContext(Dispatchers.Default) {
                MediaItemThumbnailLoader.lock.withLock {
                    val provider = item.ThumbnailProvider.get(context.database) ?: return@withContext

                    for (quality in MediaItemThumbnailProvider.Quality.values()) {
                        val key = MediaItemThumbnailLoaderKey(provider, quality, item.id)

                        val loaded = loaded_images[key]
                        if (loaded != null) {
                            state.loaded_images[quality] = loaded
                            return@withContext
                        }

                        val loading = MediaItemThumbnailLoader.loading_items.contains(key)
                        if (loading) {
                            state.loading_images.add(quality)
                        }
                    }
                }
            }
        }

        DisposableEffect(state) {
            val listener = object : Listener<MediaItemThumbnailLoaderKey, ImageBitmap> {
                override fun onLoadStarted(key: MediaItemThumbnailLoaderKey) {
                    if (key.item_id != item.id) {
                        return
                    }
                    synchronized(state) {
                        state.loading_images.addUnique(key.quality)
                    }
                }

                override fun onLoadFinished(key: MediaItemThumbnailLoaderKey, value: ImageBitmap) {
                    if (key.item_id != item.id) {
                        return
                    }
                    synchronized(state) {
                        state.loading_images.remove(key.quality)
                        state.loaded_images[key.quality] = WeakReference(value)
                    }
                }

                override fun onLoadFailed(key: MediaItemThumbnailLoaderKey, error: Throwable) {
                    if (key.item_id != item.id) {
                        return
                    }
                    synchronized(state) {
                        state.loading_images.remove(key.quality)
                    }
                }
            }

            addListener(listener)
            onDispose {
                removeListener(listener)
            }
        }

        return state
    }

    override val listeners: MutableList<Listener<MediaItemThumbnailLoaderKey, ImageBitmap>> = mutableListOf()
}
