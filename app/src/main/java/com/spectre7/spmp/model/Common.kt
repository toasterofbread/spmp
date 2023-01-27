package com.spectre7.spmp.model

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.api.loadMediaItemData
import com.spectre7.utils.getContrasted
import java.net.URL

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

    enum class ThumbnailQuality {
        LOW, HIGH
    }
    private var thumbnails: YTApiDataResponse.Thumbnails? = null
    var thumbnail_palette: Palette? = null

    private class ThumbState {
        var image: Bitmap? by mutableStateOf(null)
        var loading by mutableStateOf(false)
    }
    private val thumb_states: Map<ThumbnailQuality, ThumbState>

    val id: String get() = _getId()
    val url: String get() = _getUrl()

    private val _browse_endpoints = mutableListOf<BrowseEndpoint>()
    val browse_endpoints: List<BrowseEndpoint>
        get() = _browse_endpoints

    init {
        val states = mutableMapOf<ThumbnailQuality, ThumbState>()
        for (quality in ThumbnailQuality.values()) {
            states[quality] = ThumbState()
        }
        thumb_states = states
    }

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

    fun getThumbUrl(quality: ThumbnailQuality): String? {
        return when (quality) {
            ThumbnailQuality.HIGH -> thumbnails?.high
            ThumbnailQuality.MEDIUM -> thumbnails?.medium
        }?.url
    }

    fun isThumbnailLoaded(quality: ThumbnailQuality): Boolean {
        return thumb_states[quality].image != null
    }

    fun getThumbnail(quality: ThumbnailQuality, onLoaded: (Bitmap) -> Unit = {}): Bitmap? {
        val state = thumb_states[quality]
        synchronized(state) {
            if (state.loading) {
                onLoaded(state.image!!)
                return state.image
            }
        }
        thread {
            onLoaded(loadThumbnail(quality))
        }
        return state.image
    }

    fun loadThumbnail(quality: ThumbnailQuality): Bitmap {
        val state = thumb_states[quality]
        synchronized(state) {
            if (state.loading) {
                state.wait()
                return state.image!!
            }

            if (state.image != null) {
                return state.image!!
            }

            state.loading = true
        }

        state.image = downloadThumbnail()
        thumbnail_palette = Palette.from(state.image.asImageBitmap().asAndroidBitmap()).clearFilters().generate()

        synchronized(state) {
            state.loading = false
            state.notifyAll()
        }

        return state.image!!
    }

    protected open fun downloadThumbnail(quality: ThumbnailQuality): Bitmap {
        return BitmapFactory.decodeStream(URL(getThumbUrl(quality)).openConnection().getInputStream())!!
    }

    @Composable
    abstract fun PreviewSquare(content_colour: Color, onClick: (() -> Unit)?, onLongClick: (() -> Unit)?, modifier: Modifier)
    @Composable
    abstract fun PreviewLong(content_colour: Color, onClick: (() -> Unit)?, onLongClick: (() -> Unit)?, modifier: Modifier)

    abstract fun _getId(): String
    abstract fun _getUrl(): String

    fun getAssociatedArtist(): Artist? {
        return when (this) {
            is Artist -> this
            is Song -> artist
            is Playlist -> null // TODO?
            else -> throw NotImplementedError(this.javaClass.name)
        }
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

    companion object {
        fun getDefaultPaletteColour(palette: Palette, default: Color): Color {
            val unspecified = Color.Unspecified.toArgb()
            var ret: Color? = null

            fun apply(colour: Int): Boolean {
                if (colour == unspecified) {
                    return false
                }
                ret = Color(colour)
                return true
            }

            if (apply(palette.getVibrantColor(unspecified))) { return ret!! }
            if (apply(palette.getLightVibrantColor(unspecified))) { return ret!! }
            if (apply(palette.getLightMutedColor(unspecified))) { return ret!! }
            if (apply(palette.getDarkVibrantColor(unspecified))) { return ret!! }
            if (apply(palette.getDarkMutedColor(unspecified))) { return ret!! }
            if (apply(palette.getDominantColor(unspecified))) { return ret!! }

            return default
        }
    }
}