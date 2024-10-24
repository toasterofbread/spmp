package com.toasterofbread.spmp.widget.component

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.graphics.scale
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.layout.ContentScale
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.ui.component.Thumbnail
import dev.toastbits.ytmkt.model.external.ThumbnailProvider

@Composable
internal fun GlanceSongThumbnail(
    song: Song,
    content_description: String?,
    quality: ThumbnailProvider.Quality,
    modifier: GlanceModifier = GlanceModifier,
    content_scale: ContentScale = ContentScale.Fit,
    scale_to_size: Int? = null,
    onLoaded: (ImageBitmap) -> Unit = {}
): Boolean {
    return song.Thumbnail(
        quality,
        contentOverride = {
            onLoaded(it)

            val image: Bitmap =
                it.asAndroidBitmap().run {
                    if (scale_to_size != null) scale(scale_to_size, scale_to_size, true)
                    else this
                }

            Image(
                ImageProvider(image),
                content_description,
                modifier,
                contentScale = content_scale
            )
        }
    )
}
