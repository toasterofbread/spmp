package com.spectre7.spmp.model

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import java.net.URL

abstract class Previewable {

    protected var thumbnail: Bitmap? = null
    protected var thumbnail_hq: Bitmap? = null

    fun thumbnailLoaded(hq: Boolean): Boolean {
        return (if (hq) thumbnail_hq else thumbnail) != null
    }

    open fun loadThumbnail(hq: Boolean): Bitmap {
        if (!thumbnailLoaded(hq)) {
            val thumb = BitmapFactory.decodeStream(URL(getThumbUrl(hq)).openConnection().getInputStream())!!
            if (hq) {
                thumbnail_hq = thumb
            }
            else {
                thumbnail = thumb
            }
        }
        return (if (hq) thumbnail_hq else thumbnail)!!
    }

    @Composable
    abstract fun Preview(large: Boolean, modifier: Modifier, colour: Color)

    @Composable
    fun Preview(large: Boolean, modifier: Modifier) {
        Preview(large, modifier, MaterialTheme.colorScheme.onBackground)
    }

    @Composable
    fun Preview(large: Boolean) {
        Preview(large, Modifier, MaterialTheme.colorScheme.onBackground)
    }

    abstract fun getId(): String
    abstract fun getThumbUrl(hq: Boolean): String
}