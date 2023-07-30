package com.toasterofbread.spmp.ui.component

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
import com.toasterofbread.spmp.model.mediaitem.loader.MediaItemLoader
import com.toasterofbread.spmp.model.mediaitem.loader.MediaItemThumbnailLoader
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.toThumbnailProvider
import com.toasterofbread.utils.composable.SubtleLoadingIndicator

@Composable
fun MediaItem.Thumbnail(
    quality: MediaItemThumbnailProvider.Quality,
    modifier: Modifier = Modifier,
    failure_icon: ImageVector? = Icons.Default.CloudOff,
    contentColourProvider: (() -> Color)? = null,
    onLoaded: ((ImageBitmap) -> Unit)? = null
) {
    val thumbnail_holder = this//getThumbnailHolder()
    var image: ImageBitmap? by remember { mutableStateOf(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(thumbnail_holder, quality) {
        loading = true
        image = null

        val db = SpMp.context.database

        var provider = thumbnail_holder.ThumbnailProvider.get(db)
        if (provider == null) {
            val loaded = thumbnail_holder.Loaded.get(db)
            if (loaded) {
                loading = false
                return@LaunchedEffect
            }

            provider = MediaItemLoader.loadUnknown(thumbnail_holder.getEmptyData(), db).fold(
                { it.thumbnail_provider },
                { null }
            )
        }

        if (provider != null) {
            val load_result = MediaItemThumbnailLoader.loadItemThumbnail(this@Thumbnail, provider, quality, SpMp.context)
            load_result.onSuccess {
                image = it
                onLoaded?.invoke(it)
            }
        }

        loading = false
    }
//
//    var loaded by remember { mutableStateOf(false) }
//    LaunchedEffect(thumbnail_holder) {
//        loaded = false
//    }
//
//    val state = thumbnail_holder.thumb_states.values.lastOrNull { state ->
//        state.quality <= quality && state.image != null
//    } ?: thumbnail_holder.thumb_states[quality]!!
//
//    if (state.loading || (state.image == null && !state.loaded)) {
//        SubtleLoadingIndicator(modifier.fillMaxSize(), contentColourProvider)
//    }
//    else if (state.image != null) {
//        state.image!!.also { thumbnail ->
//            if (!loaded) {
//                onLoaded?.invoke(thumbnail)
//                loaded = true
//            }
//
//            Image(
//                thumbnail,
//                contentDescription = null,
//                contentScale = ContentScale.Crop,
//                modifier = modifier
//            )
//        }
//    }
//    else if (state.loaded) {
//        if (failure_icon != null) {
//            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//                Icon(failure_icon, null)
//            }
//        }
//    }

    if (loading) {
        SubtleLoadingIndicator(modifier.fillMaxSize(), contentColourProvider)
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
