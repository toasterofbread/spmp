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
import androidx.compose.ui.unit.IntSize
import androidx.palette.graphics.Palette
import com.spectre7.spmp.api.loadMediaItemData
import java.net.URL
import kotlin.concurrent.thread

class MediaItemRow(val title: String, val subtitle: String?, val items: MutableList<MediaItem> = mutableListOf()) {
    fun add(item: MediaItem): Boolean {
        if (items.any { it.id == item.id }) {
            return false
        }
        items.add(item)
        return true
    }
}

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
abstract class MediaItem(val id: String) {
    enum class Type {
        SONG, ARTIST, PLAYLIST
    }
    val type: Type get() = when(this) {
        is Song -> Type.SONG
        is Artist -> Type.ARTIST
        is Playlist -> Type.PLAYLIST
        else -> throw NotImplementedError(this.javaClass.name)
    }

    data class YTApiDataResponse(
        val id: String = "",
        val snippet: Snippet? = null,
        val statistics: Statistics? = null,
        val contentDetails: ContentDetails? = null,
        val localizations: Map<String, LocalizedSnippet>? = null
    ) {
        fun getLocalisation(lang: String): LocalizedSnippet? {
            return localizations?.getOrElse(lang) {
                for (loc in localizations) {
                    if (loc.key.split('_').first() == lang) {
                        return@getOrElse loc.value
                    }
                }
                return@getOrElse null
            }
        }

        data class Snippet(
            val title: String,
            val description: String? = null,
            val publishedAt: String,
            val channelId: String? = null,
            val defaultLanguage: String? = null,
            val country: String? = null,
            val thumbnails: Map<String, ThumbnailProvider.Thumbnail>,
            val localized: LocalizedSnippet? = null
        )
        data class LocalizedSnippet(val title: String? = null, var description: String? = null) {
            init {
                if (description?.isBlank() == true) {
                    description = null
                }
            }
        }
        data class Statistics(val viewCount: String, val subscriberCount: String? = null, val hiddenSubscriberCount: Boolean = false, val videoCount: String? = null)
        data class ContentDetails(val duration: String? = null)
    }

    data class Serialisable(val type: Int, val id: String) {
        private val enum_type get() = Type.values()[type]
        fun toMediaItem(): MediaItem {
            when (enum_type) {
                Type.SONG -> return Song.fromId(id)
                Type.ARTIST -> return Artist.fromId(id)
                Type.PLAYLIST -> return Playlist.fromId(id)
            }
        }
    }
    fun toSerialisable(): Serialisable {
        return Serialisable(type.ordinal, id)
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

    abstract class ThumbnailProvider {
        fun getThumbnail(quality: ThumbnailQuality): String? {
            return when (this) {
                is SetProvider -> {
                    when (quality) {
                        ThumbnailQuality.HIGH -> thumbnails.maxByOrNull { it.width * it.height }
                        ThumbnailQuality.LOW -> thumbnails.minByOrNull { it.width * it.height }
                    }?.url
                }
                is DynamicProvider -> {
                    val target_size = quality.getTargetSize()
                    provider(target_size.width, target_size.height)
                }
                else -> throw NotImplementedError(this.javaClass.name)
            }
        }

        data class Thumbnail(val url: String, val width: Int, val height: Int)
        data class SetProvider(val thumbnails: List<Thumbnail>): ThumbnailProvider()

        data class DynamicProvider(val provider: (w: Int, h: Int) -> String): ThumbnailProvider()
    }

    enum class ThumbnailQuality {
        LOW, HIGH;

        fun getTargetSize(): IntSize {
            return when (this) {
                LOW -> IntSize(180, 180)
                HIGH -> IntSize(720, 720)
            }
        }
    }
    private var thumbnail_provider: ThumbnailProvider? = null
    var thumbnail_palette: Palette? = null

    private class ThumbState {
        var image: Bitmap? by mutableStateOf(null)
        var loading by mutableStateOf(false)
    }
    private val thumb_states: Map<ThumbnailQuality, ThumbState>

    val url: String get() = _getUrl()

    private val _browse_endpoints = mutableListOf<BrowseEndpoint>()
    val browse_endpoints: List<BrowseEndpoint>
        get() = _browse_endpoints

    private var _is_valid: Boolean = true
    var is_valid: Boolean
        get() = _is_valid
        private set(value) { _is_valid = value }

    fun invalidate() {
        is_valid = false
    }

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
        return thumbnail_provider?.getThumbnail(quality)
    }

    fun isThumbnailLoaded(quality: ThumbnailQuality): Boolean {
        return thumb_states[quality]!!.image != null
    }

    fun getThumbnail(quality: ThumbnailQuality, onLoaded: (Bitmap) -> Unit = {}): Bitmap? {
        val state = thumb_states[quality]!!
        synchronized(state) {
            if (state.loading) {
                thread {
                    synchronized(state) {
                        (state as Object).wait()
                        onLoaded(state.image!!)
                    }
                }
                return state.image
            }
        }
        thread {
            onLoaded(loadThumbnail(quality))
        }
        return state.image
    }

    fun loadThumbnail(quality: ThumbnailQuality): Bitmap {
        val state = thumb_states[quality]!!
        synchronized(state) {
            if (state.loading) {
                (state as Object).wait()
                return state.image!!
            }

            if (state.image != null) {
                return state.image!!
            }

            state.loading = true
        }

        state.image = downloadThumbnail(quality)
        thumbnail_palette = Palette.from(state.image!!.asImageBitmap().asAndroidBitmap()).clearFilters().generate()

        synchronized(state) {
            state.loading = false
            (state as Object).notifyAll()
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

    fun initWithData(data: Any, thumbnail_provider: ThumbnailProvider) {
        if (load_status == LoadStatus.LOADED) {
            return
        }
        this.thumbnail_provider = thumbnail_provider
        subInitWithData(data)
        load_status = LoadStatus.LOADED
    }

    protected abstract fun subInitWithData(data: Any)

    companion object {
        fun getDefaultPaletteColour(palette: Palette, default: Color): Color {
            val unspecified = Color.Unspecified.toArgb()
            var ret: Color? = null

            fun tryColour(colour: Int): Boolean {
                if (colour == unspecified) {
                    return false
                }
                ret = Color(colour)
                return true
            }

            if (tryColour(palette.getVibrantColor(unspecified))) { return ret!! }
            if (tryColour(palette.getLightVibrantColor(unspecified))) { return ret!! }
            if (tryColour(palette.getLightMutedColor(unspecified))) { return ret!! }
            if (tryColour(palette.getDarkVibrantColor(unspecified))) { return ret!! }
            if (tryColour(palette.getDarkMutedColor(unspecified))) { return ret!! }
            if (tryColour(palette.getDominantColor(unspecified))) { return ret!! }

            return default
        }
    }
}