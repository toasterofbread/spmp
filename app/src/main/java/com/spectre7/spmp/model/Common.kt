package com.spectre7.spmp.model

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import java.net.URL
import com.spectre7.spmp.sendToast

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

    override fun equals(other: Any?): Boolean {
        sendToast("EQ")
        return other is Previewable && other.getId() == getId()
    }

    @Composable
    abstract fun Preview(large: Boolean, modifier: Modifier)
    abstract fun getId(): String
    abstract fun getThumbUrl(hq: Boolean): String
}