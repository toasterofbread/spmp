package com.spectre7.spmp.model

import java.util.concurrent.Executors

class MediaItemThumbnailLoader() {
    private val executor = Executors.newFixedThreadPool(3)
    private val loaded_callbacks: Map<ThumbState, (Result<ImageBitmap>) -> Unit>

    fun loadThumbnail(thumb_state: ThumbState, context: PlatformContext = SpMp.context, onLoaded: (Result<ImageBitmap>) -> Unit) {
        synchronized(executor) {
            val callbacks = loaded_callbacks[thumb_state]
            if (callbacks != null) {
                callbacks.add(onLoaded)
                return
            }

            executor.submit {
                val result = thumb_state.load()
                for (callback in loaded_callbacks) {
                    callback(result)
                }
            }
        }
    }
}
