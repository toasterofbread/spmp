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

    var loaded: Boolean = false
    private var on_loaded_callbacks: MutableList<() -> Unit>? = null
    val loading: Boolean get() = !loaded && on_loaded_callbacks != null

    class ServerInfoResponse(
        val id: String,
        var type: String = "",
        val snippet: Snippet,
        val statistics: Statistics,
        val contentDetails: ContentDetails,
        val localizations: Map<String, Localisation> = emptyMap()
    ) {
        data class Snippet(val title: String, val description: String? = null, val publishedAt: String, val channelId: String? = null, val defaultLanguage: String? = null, val country: String? = null, val thumbnails: Thumbnails)
        data class Statistics(val viewCount: String, val subscriberCount: String? = null, val hiddenSubscriberCount: Boolean = false, val videoCount: String? = null)
        data class ContentDetails(val duration: String? = null)
        data class Localisation(val title: String? = null, val description: String? = null)
        data class Thumbnails(val default: Thumbnail, val medium: Thumbnail, val high: Thumbnail)
        data class Thumbnail(val url: String)
    }

    data class SimpleIdentifier(val id: String, val type: String)

    fun getSimpleIdentifier(): SimpleIdentifier {
        return SimpleIdentifier(getId(), when (this) {
            is Song -> "video"
            else -> "channel"
        })
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

    fun loadData(process_queue: Boolean = true, onFinished: ((YtItem) -> Unit)? = null): YtItem {
        if (loaded) {
            onFinished?.invoke(this)
        }
        else if (loading) {
            if (onFinished != null) {
                on_loaded_callbacks?.add { onFinished(this) }
            }
        }
        else {
            on_loaded_callbacks = mutableListOf()
            if (onFinished != null) {
                on_loaded_callbacks!!.add { onFinished(this) }
            }

            thread {
                DataApi.queueYtItemDataLoad(this) {
                    initWithData(it!!) {
                        for (callback in on_loaded_callbacks!!) {
                            callback()
                        }
                        on_loaded_callbacks = null
                    }
                }
                if (process_queue) {
                    DataApi.processYtItemLoadQueue()
                }
            }
        }
        return this
    }

    abstract fun getId(): String
    abstract fun getThumbUrl(hq: Boolean): String
    abstract fun initWithData(data: ServerInfoResponse, onFinished: () -> Unit)
}