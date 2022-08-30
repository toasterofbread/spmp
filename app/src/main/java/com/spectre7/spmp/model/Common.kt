package com.spectre7.spmp.model

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import java.net.URL

abstract class Previewable {

    private var thumbnail: Bitmap? = null

    fun thumbnailLoaded(): Boolean {
        return thumbnail != null
    }

    fun loadThumbnail(): Bitmap {
        if (!thumbnailLoaded()) {
            thumbnail = BitmapFactory.decodeStream(URL(getThumbUrl()).openConnection().getInputStream())
        }
        return thumbnail!!
    }

    @Composable
    abstract fun getPreview()
    abstract fun getId(): String
    abstract fun getThumbUrl(): String
}