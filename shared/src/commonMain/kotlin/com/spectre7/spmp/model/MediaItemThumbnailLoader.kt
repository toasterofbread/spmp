package com.spectre7.spmp.model

import androidx.compose.ui.graphics.ImageBitmap
import com.spectre7.spmp.api.getOrThrowHere
import java.util.concurrent.Executors
import com.spectre7.spmp.platform.PlatformContext
import kotlin.concurrent.thread

class MediaItemThumbnailLoader {
    private val executor = Executors.newFixedThreadPool(5)
    private val loaded_callbacks: MutableMap<ThumbState, MutableList<(Result<ImageBitmap>) -> Unit>> = mutableMapOf()

    fun loadThumbnail(thumb_state: ThumbState, context: PlatformContext = SpMp.context, onLoaded: (Result<ImageBitmap>) -> Unit) {
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

            thumb_state.loadWithExecutor(context, executor) { result ->
                for (callback in callbacks) {
                    callback(result)
                }
            }
        }
    }
}
