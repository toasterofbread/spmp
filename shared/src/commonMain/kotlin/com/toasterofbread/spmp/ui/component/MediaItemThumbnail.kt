package com.toasterofbread.spmp.ui.component

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import dev.toastbits.composekit.util.platform.launchSingle
import dev.toastbits.composekit.util.composable.OnChangedEffect
import dev.toastbits.composekit.components.utils.composable.SubtleLoadingIndicator
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import dev.toastbits.ytmkt.model.external.ThumbnailProvider
import dev.toastbits.ytmkt.model.external.ThumbnailProvider.Quality
import com.toasterofbread.spmp.model.mediaitem.loader.MediaItemThumbnailLoader
import com.toasterofbread.spmp.model.mediaitem.playlist.LocalPlaylist
import com.toasterofbread.spmp.model.mediaitem.playlist.LocalPlaylistDefaultThumbnail
import com.toasterofbread.spmp.model.mediaitem.playlist.LocalPlaylistRef
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import dev.toastbits.ytmkt.model.external.ThumbnailProvider.Companion.fromImageUrl
import kotlinx.coroutines.CoroutineScope

private suspend inline fun MediaItem.loadThumb(
    player: PlayerState,
    target_quality: Quality,
    base_provider: ThumbnailProvider?,
    disable_cache: Boolean,
    onLoaded: (ImageBitmap, Quality) -> Unit
) {
    var provider: ThumbnailProvider? = base_provider
    if (provider == null) {
        loadData(player.context)
        provider = ThumbnailProvider.get(player.database)
    }

    if (provider != null) {
        for (quality in Quality.byQuality(target_quality)) {
            val load_result = MediaItemThumbnailLoader.loadItemThumbnail(
                this@loadThumb,
                provider,
                quality,
                player.context,
                disable_cache_read = disable_cache,
                disable_cache_write = disable_cache
            )
            load_result.onSuccess { loaded_image ->
                onLoaded(loaded_image, quality)
                return
            }
        }
    }
}

@Composable
fun MediaItem.Thumbnail(
    target_quality: Quality,
    modifier: Modifier = Modifier,
    load_failed_icon: ImageVector? = Icons.Default.CloudOff,
    provider_override: ThumbnailProvider? = null,
    getContentColour: (() -> Color)? = null,
    container_modifier: Modifier = Modifier,
    disable_cache: Boolean = false,
    show: Boolean = true,
    contentOverride: (@Composable (ImageBitmap?) -> Unit)? = null,
    onLoaded: ((ImageBitmap?) -> Unit)? = null
): Boolean {
    require(this !is LocalPlaylistRef) { "LocalPlaylistRef must be loaded and passed as a LocalPlaylistData" }

    val player: PlayerState = LocalPlayerState.current
    var loading: Boolean by remember { mutableStateOf(true) }
    val coroutine_scope: CoroutineScope = rememberCoroutineScope()

    val custom_image_url: State<String?>? = (this as? Playlist)?.CustomImageUrl?.observe(player.database)
    val thumbnail_provider: ThumbnailProvider? by ThumbnailProvider.observe(player.database)

    fun getThumbnailProvider(): ThumbnailProvider? =
        provider_override ?: custom_image_url?.value?.let { fromImageUrl(it) } ?: thumbnail_provider

    var image: Pair<ImageBitmap, Quality>? by remember(id) {
        val provider: ThumbnailProvider? = getThumbnailProvider()
        if (provider != null) {
            for (quality in Quality.byQuality(target_quality)) {
                val loaded_image = MediaItemThumbnailLoader.getLoadedItemThumbnail(this@Thumbnail, quality, provider)
                if (loaded_image != null) {
                    onLoaded?.invoke(loaded_image)
                    return@remember mutableStateOf(Pair(loaded_image, quality))
                }
            }
        }

        onLoaded?.invoke(null)

        return@remember mutableStateOf(null)
    }

    OnChangedEffect(id, provider_override ?: custom_image_url ?: thumbnail_provider) {
        coroutine_scope.launchSingle {
            loading = true
            image = null
            loadThumb(player, target_quality, getThumbnailProvider(), disable_cache) { loaded_image, quality ->
                image = Pair(loaded_image, quality)
                onLoaded?.invoke(loaded_image)
            }
            loading = false
        }
    }

    LaunchedEffect(target_quality) {
        image?.also { im ->
            if (im.second.ordinal >= target_quality.ordinal) {
                return@LaunchedEffect
            }
        }

        coroutine_scope.launchSingle {
            loading = true
            loadThumb(player, target_quality, getThumbnailProvider(), disable_cache) { loaded_image, quality ->
                image = Pair(loaded_image, quality)
                onLoaded?.invoke(loaded_image)
            }
            loading = false
        }
    }

    if (show) {
        if (contentOverride != null) {
            contentOverride(image?.first)
            return true
        }

        Crossfade(image?.first ?: loading, container_modifier) { state ->
            if (state is ImageBitmap) {
                Image(
                    state,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = modifier
                )
            }
            else if (state == true) {
                SubtleLoadingIndicator(modifier.fillMaxSize(), getColour = getContentColour)
            }
            else if (this is LocalPlaylist) {
                LocalPlaylistDefaultThumbnail(modifier)
            }
            else if (load_failed_icon != null) {
                Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(load_failed_icon, null)
                }
            }
        }
    }

    return show && !loading && image?.first != null
}
