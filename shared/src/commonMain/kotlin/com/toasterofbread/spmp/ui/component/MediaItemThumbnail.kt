package com.toasterofbread.spmp.ui.component

import SpMp
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
import com.toasterofbread.spmp.model.mediaitem.loader.MediaItemThumbnailLoader
import com.toasterofbread.utils.composable.SubtleLoadingIndicator

@Composable
fun MediaItem.Thumbnail(
    target_quality: MediaItemThumbnailProvider.Quality,
    modifier: Modifier = Modifier,
    failure_icon: ImageVector? = Icons.Default.CloudOff,
    contentColourProvider: (() -> Color)? = null,
    onLoaded: ((ImageBitmap) -> Unit)? = null
) {
    var image: ImageBitmap? by remember { mutableStateOf(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(target_quality) {
        loading = true
        image = null

        var thumbnail_provider = ThumbnailProvider.get(SpMp.context.database)
        if (thumbnail_provider == null) {
            loadData(SpMp.context.database)
            thumbnail_provider = ThumbnailProvider.get(SpMp.context.database)
        }

        if (thumbnail_provider != null) {
            for (quality in MediaItemThumbnailProvider.Quality.byQuality(target_quality)) {
                val load_result = MediaItemThumbnailLoader.loadItemThumbnail(this@Thumbnail, thumbnail_provider, quality, SpMp.context)
                if (load_result.isSuccess) {
                    image = load_result.getOrThrow()
                    onLoaded?.invoke(image!!)
                    break
                }
            }
        }

        loading = false
    }

    if (loading) {
        SubtleLoadingIndicator(modifier.fillMaxSize(), getColour = contentColourProvider)
    }
    else if (image != null) {
        image?.also { thumbnail ->
            Image(
                thumbnail,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = modifier
            )
        }
    }
    else if (failure_icon != null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(failure_icon, null)
        }
    }
}
