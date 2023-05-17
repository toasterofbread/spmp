package com.spectre7.spmp.model

import androidx.compose.ui.graphics.ImageBitmap
import java.util.concurrent.Executors
import com.spectre7.spmp.platform.PlatformContext

class MediaItemThumbnailLoader {
    private val executor = Executors.newFixedThreadPool(3)
    private val loaded_callbacks: MutableMap<MediaItem.ThumbState, MutableList<(Result<ImageBitmap>) -> Unit>> = mutableMapOf()

    fun loadThumbnail(thumb_state: MediaItem.ThumbState, context: PlatformContext = SpMp.context, onLoaded: (Result<ImageBitmap>) -> Unit) {
        synchronized(executor) {
            thumb_state.image?.also {
                onLoaded(Result.success(it))
                return
            }

            var callbacks = loaded_callbacks[thumb_state]
            if (callbacks != null) {
                callbacks.add(onLoaded)
                return
            }

            callbacks = mutableListOf(onLoaded)
            loaded_callbacks[thumb_state] = callbacks

            executor.submit {
                val result = thumb_state.load(context)
                for (callback in callbacks) {
                    callback(result)
                }
            }
        }
    }
}
