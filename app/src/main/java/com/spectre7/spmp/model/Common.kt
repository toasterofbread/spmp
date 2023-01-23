package com.spectre7.spmp.model

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.spectre7.spmp.api.DataApi
import com.spectre7.spmp.api.loadMediaItemData
import java.net.URL
import kotlin.concurrent.thread

abstract class MediaItem {

    data class YTApiDataResponse(
        val id: String = "",
        val snippet: Snippet? = null,
        val statistics: Statistics? = null,
        val contentDetails: ContentDetails? = null,
    ) {
        data class Snippet(val title: String, val description: String? = null, val publishedAt: String, val channelId: String? = null, val defaultLanguage: String? = null, val country: String? = null, val thumbnails: Thumbnails)
        data class Statistics(val viewCount: String, val subscriberCount: String? = null, val hiddenSubscriberCount: Boolean = false, val videoCount: String? = null)
        data class ContentDetails(val duration: String? = null)
        data class Thumbnails(val default: Thumbnail? = null, val medium: Thumbnail? = null, val high: Thumbnail? = null)
        data class Thumbnail(val url: String)
    }

    class BrowseEndpoint {

        val id: String
        val type: Type

        constructor(id: String, type: Type) {
            this.id = id
            this.type = type
        }

        constructor(id: String, type_name: String) {
            this.id = id
            this.type = Type.fromString(type_name)
        }

        enum class Type {
            CHANNEL,
            ARTIST,
            ALBUM;

            companion object {
                fun fromString(type_name: String): Type {
                    return when (type_name) {
                        "MUSIC_PAGE_TYPE_USER_CHANNEL" -> CHANNEL
                        "MUSIC_PAGE_TYPE_ARTIST" -> ARTIST
                        "MUSIC_PAGE_TYPE_ALBUM" -> ALBUM
                        else -> throw NotImplementedError(type_name)
                    }
                }
            }
        }
    }

    enum class LoadStatus {
        NOT_LOADED,
        LOADING,
        LOADED
    }

    private var _load_status: LoadStatus = LoadStatus.NOT_LOADED
    var load_status: LoadStatus
        get() = _load_status
        private set(value) { _load_status = value }
    val loading_lock = Object()
    
    protected var thumbnail: Bitmap? = null
    protected var thumbnail_hq: Bitmap? = null
    private var thumbnails: YTApiDataResponse.Thumbnails? = null

    val id: String get() = _getId()
    val url: String get() = _getUrl()

    private val _browse_endpoints = mutableListOf<BrowseEndpoint>()
    val browse_endpoints: List<BrowseEndpoint>
        get() = _browse_endpoints

    fun addBrowseEndpoint(id: String, type: BrowseEndpoint.Type): Boolean {
        for (endpoint in _browse_endpoints) {
            if (endpoint.id == id && endpoint.type == type) {
                return false
            }
        }
        _browse_endpoints.add(BrowseEndpoint(id, type))
        return true
    }

    fun addBrowseEndpoint(id: String, type_name: String): Boolean {
        return addBrowseEndpoint(id, BrowseEndpoint.Type.fromString(type_name))
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

    abstract fun _getId(): String
    abstract fun _getUrl(): String

    fun getThumbUrl(hq: Boolean): String? {
        return (if (hq) thumbnails?.high else thumbnails?.medium)?.url
    }

    fun loadData(): MediaItem {
        if (load_status == LoadStatus.LOADED) {
            return this
        }

        val result = loadMediaItemData(this)
        return result.getDataOrThrow()
    }

    fun initWithData(data: YTApiDataResponse) {
        if (load_status == LoadStatus.LOADED) {
            return
        }
        thumbnails = data.snippet?.thumbnails
        subInitWithData(data)
        load_status = LoadStatus.LOADED
    }

    protected abstract fun subInitWithData(data: YTApiDataResponse)
}