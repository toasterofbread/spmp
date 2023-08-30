package com.toasterofbread.spmp.ui.component

import SpMp
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.Playlist
import com.toasterofbread.spmp.model.mediaitem.loader.MediaItemThumbnailLoader
import com.toasterofbread.spmp.model.mediaitem.playlist.LocalPlaylistDefaultThumbnail
import com.toasterofbread.utils.composable.SubtleLoadingIndicator

@Composable
fun MediaItem.Thumbnail(
    target_quality: MediaItemThumbnailProvider.Quality,
    modifier: Modifier = Modifier,
    load_failed_icon: ImageVector? = Icons.Default.CloudOff,
    getContentColour: (() -> Color)? = null,
    onLoaded: ((ImageBitmap) -> Unit)? = null
) {
    var loading by remember { mutableStateOf(true) }
    var image: Pair<ImageBitmap, MediaItemThumbnailProvider.Quality>? by remember(id) {
        val provider = ThumbnailProvider.get(SpMp.context.database)
        if (provider != null) {
            for (quality in MediaItemThumbnailProvider.Quality.byQuality(target_quality)) {
                val loaded_image = MediaItemThumbnailLoader.getLoadedItemThumbnail(this, quality, provider)
                if (loaded_image != null) {
                    onLoaded?.invoke(loaded_image)
                    return@remember mutableStateOf(Pair(loaded_image, quality))
                }
            }
        }

        return@remember mutableStateOf(null)
    }

    LaunchedEffect(id, target_quality) {
        image?.also { im ->
            if (im.second.ordinal >= target_quality.ordinal) {
                onLoaded?.invoke(im.first)
                return@LaunchedEffect
            }
        }

        loading = true

        var thumbnail_provider = ThumbnailProvider.get(SpMp.context.database)
        if (thumbnail_provider == null) {
            loadData(SpMp.context)
            thumbnail_provider = ThumbnailProvider.get(SpMp.context.database)
        }

        if (thumbnail_provider != null) {
            for (quality in MediaItemThumbnailProvider.Quality.byQuality(target_quality)) {
                val load_result = MediaItemThumbnailLoader.loadItemThumbnail(this@Thumbnail, thumbnail_provider, quality, SpMp.context)
                if (load_result.isSuccess) {
                    val loaded_image = load_result.getOrThrow()
                    image = Pair(loaded_image, quality)
                    onLoaded?.invoke(loaded_image)
                    break
                }
            }
        }

        loading = false
    }

    Crossfade(image?.first ?: loading) { state ->
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
        else if (this is Playlist && isLocalPlaylist()) {
            LocalPlaylistDefaultThumbnail(modifier)
        }
        else if (load_failed_icon != null) {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(load_failed_icon, null)
            }
        }
    }
}
