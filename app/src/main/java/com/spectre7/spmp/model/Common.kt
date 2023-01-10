package com.spectre7.spmp.model

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.spectre7.spmp.api.DataApi
import java.net.URL
import kotlin.concurrent.thread

abstract class YtItem {
    protected var thumbnail: Bitmap? = null
    protected var thumbnail_hq: Bitmap? = null

    private var thumbnails: ServerInfoResponse.Thumbnails? = null

    private var _loaded: Boolean = false
    var loaded: Boolean
        get() = _loaded
        private set(value) { _loaded = value }

    private var on_loaded_callbacks: MutableList<(Boolean) -> Unit>? = null

    val id: String get() = _getId()
    val url: String get() = _getUrl()

    data class ServerInfoResponse(
        val id: String,
        var original_id: String? = null,
        var type: String = "",
        val stream_url: String? = null,
        val snippet: Snippet? = null,
        val statistics: Statistics? = null,
        val contentDetails: ContentDetails? = null,
        val error: String? = null,
    ) {
        data class Snippet(val title: String, val description: String? = null, val publishedAt: String, val channelId: String? = null, val defaultLanguage: String? = null, val country: String? = null, val thumbnails: Thumbnails)
        data class Statistics(val viewCount: String, val subscriberCount: String? = null, val hiddenSubscriberCount: Boolean = false, val videoCount: String? = null)
        data class ContentDetails(val duration: String? = null)
        data class Thumbnails(val default: Thumbnail? = null, val medium: Thumbnail? = null, val high: Thumbnail? = null)
        data class Thumbnail(val url: String)

        init {
            if (original_id == null) {
                original_id = id
            }
        }
    }

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

    fun loadData(process_queue: Boolean = true, get_stream_url: Boolean = false, onFinished: ((YtItem?) -> Unit)? = null): YtItem {
        if (loaded) {
            onFinished?.invoke(this)
        }
        else if (on_loaded_callbacks != null) {
            if (onFinished != null) {
                on_loaded_callbacks?.add { onFinished(if (it) this else null) }
            }
        }
        else {
            on_loaded_callbacks = mutableListOf()
            if (onFinished != null) {
                on_loaded_callbacks!!.add { onFinished(if (it) this else null) }
            }

            thread {
                DataApi.queueYtItemDataLoad(this, get_stream_url) {
                    if (it == null) {
                        throw RuntimeException("Server info response is null (id: $id)")
                    }

                    fun callCallbacks(success: Boolean) {
                        for (callback in on_loaded_callbacks!!) {
                            callback(success)
                        }
                        on_loaded_callbacks = null
                    }

                    try {
                        initWithData(it) {
                            callCallbacks(true)
                        }
                    }
                    catch (_: RuntimeException) {
                        callCallbacks(false)
                    }
                }
                if (process_queue) {
                    DataApi.processYtItemLoadQueue()
                }
            }
        }
        return this
    }

    abstract fun _getId(): String
    abstract fun _getUrl(): String

    fun getThumbUrl(hq: Boolean): String? {
        return (if (hq) thumbnails?.high else thumbnails?.medium)?.url
    }

    fun initWithData(data: ServerInfoResponse, onFinished: () -> Unit) {
        if (loaded) {
            onFinished()
        }
        thumbnails = data.snippet?.thumbnails
        subInitWithData(data) {
            loaded = true
            onFinished()
        }
    }

    protected abstract fun subInitWithData(data: ServerInfoResponse, onFinished: () -> Unit)
}