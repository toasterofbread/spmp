package com.toasterofbread.spmp.model.mediaitem.loader

import LocalPlayerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import dev.toastbits.composekit.util.addUnique
import dev.toastbits.composekit.context.PlatformFile
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import dev.toastbits.ytmkt.model.external.ThumbnailProvider
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.toByteArray
import com.toasterofbread.spmp.platform.toImageBitmap
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import PlatformIO
import okio.buffer
import okio.use
import io.ktor.client.HttpClient
import kotlin.concurrent.withLock

internal data class MediaItemThumbnailLoaderKey(
    val provider: ThumbnailProvider,
    val quality: ThumbnailProvider.Quality,
    val item_id: String
)

internal object MediaItemThumbnailLoader: ListenerLoader<MediaItemThumbnailLoaderKey, ImageBitmap>() {
    private var loaded_images: MutableMap<MediaItemThumbnailLoaderKey, ImageBitmap> = mutableMapOf()
    private val client: HttpClient = HttpClient()

    fun getLoadedItemThumbnail(item: MediaItem, quality: ThumbnailProvider.Quality, thumbnail_provider: ThumbnailProvider): ImageBitmap? {
        val key = MediaItemThumbnailLoaderKey(thumbnail_provider, quality, item.id)
        synchronized(loaded_images) {
            return loaded_images[key]
        }
    }

    suspend fun loadItemThumbnail(
        item: MediaItem,
        quality: ThumbnailProvider.Quality,
        context: AppContext
    ): Result<ImageBitmap> {
        val thumbnail_provider = item.ThumbnailProvider.get(context.database)
        if (thumbnail_provider == null) {
            return Result.failure(RuntimeException("Item has no ThumbnailProvider"))
        }
        return loadItemThumbnail(item, thumbnail_provider, quality, context)
    }

    suspend fun loadItemThumbnail(
        item: MediaItem,
        thumbnail_provider: ThumbnailProvider,
        quality: ThumbnailProvider.Quality,
        context: AppContext,
        disable_cache_read: Boolean = false,
        disable_cache_write: Boolean = false
    ): Result<ImageBitmap> {
        val key = MediaItemThumbnailLoaderKey(thumbnail_provider, quality, item.id)

        val loaded: ImageBitmap? = loaded_images[key]
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
                        loaded_images[key] = image
                    }
                }
            }

            return@performLoad result
        }
    }

    suspend fun invalidateCache(item: MediaItem, context: AppContext) = withContext(Dispatchers.PlatformIO) {
        for (quality in ThumbnailProvider.Quality.entries) {
            val file: PlatformFile? = getCacheFile(item, quality, context)
            file?.delete()
        }

        synchronized(loaded_images) {
            loaded_images = loaded_images.filterKeys { key ->
                key.item_id != item.id
            } as MutableMap<MediaItemThumbnailLoaderKey, ImageBitmap>
        }
    }

    private fun getCacheFile(item: MediaItem, quality: ThumbnailProvider.Quality, context: AppContext): PlatformFile? =
        context.getCacheDir()?.resolve("thumbnails/${item.id}.${quality.ordinal}.png")

    private suspend fun performLoad(
        item: MediaItem,
        quality: ThumbnailProvider.Quality,
        thumbnail_url: String,
        context: AppContext,
        disable_cache_read: Boolean = false,
        disable_cache_write: Boolean = false
    ): Result<ImageBitmap> = withContext(Dispatchers.PlatformIO) {
        val cache_file: PlatformFile? = getCacheFile(item, quality, context)
        if (!disable_cache_read && cache_file?.is_file == true) {
            return@withContext runCatching {
                cache_file.inputStream().buffer().use { stream ->
                    stream.readByteArray().toImageBitmap()
                }
            }
        }

        val result: Result<ImageBitmap> = item.downloadThumbnailData(thumbnail_url, client)
        result.onSuccess { image ->
            if (cache_file != null && !disable_cache_write && context.settings.Misc.THUMB_CACHE_ENABLED.get()) {
                cache_file.parent_file.mkdirs()
                cache_file.outputStream().buffer().use {
                    it.write(image.toByteArray())
                }
            }
        }

        return@withContext result
    }

    interface ItemState {
        val loaded_images: Map<ThumbnailProvider.Quality, ImageBitmap>
        val loading_images: List<ThumbnailProvider.Quality>

        fun getHighestQuality(): ImageBitmap? =
            loaded_images.maxByOrNull { it.key.ordinal }?.value
    }

    @Composable
    fun rememberItemState(item: MediaItem): ItemState {
        val player: PlayerState = LocalPlayerState.current
        val state = remember(item) {
            object : ItemState {
                override val loaded_images: MutableMap<ThumbnailProvider.Quality, ImageBitmap> = mutableStateMapOf()
                override val loading_images: MutableList<ThumbnailProvider.Quality> = mutableStateListOf()
            }
        }

        LaunchedEffect(state) {
            withContext(Dispatchers.Default) {
                MediaItemThumbnailLoader.lock.withLock {
                    val provider = item.ThumbnailProvider.get(player.database) ?: return@withContext

                    for (quality in ThumbnailProvider.Quality.entries) {
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
                        state.loaded_images[key.quality] = value
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
