package com.spectre7.spmp.model

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntSize
import androidx.palette.graphics.Palette
import com.spectre7.spmp.api.loadMediaItemData
import com.spectre7.spmp.ui.layout.PlayerViewContext
import com.spectre7.utils.getThemeColour
import java.net.URL
import kotlin.concurrent.thread

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
abstract class MediaItem(id: String) {
    private val _id: String = id
    val id: String get() {
        requireValid()
        return _id
    }

    private var _title: String? by mutableStateOf(null)
    var title: String? 
        get() = _title
        protected set(value) {
            _title = value
        }

    fun supplyTitle(value: String?) {
        if (_title == null && value != null) {
            _title = value
        }
    }

    private var _artist: Artist? by mutableStateOf(null)
    var artist: Artist?
        get() = _artist
        protected set(value) {
            _artist = value
        }

    fun supplyArtist(value: Artist?) {
        if (_artist == null && value != null) {
            _artist = value
        }
    }

    private var _description: String? by mutableStateOf(null)
    var description: String?
        get() = _description
        protected set(value) {
            _description = value
        }

    fun supplyDescription(value: String?) {
        if (_description == null && value != null) {
            _description = value
        }
    }

    private var replaced_with: MediaItem? = null

    enum class Type {
        SONG, ARTIST, PLAYLIST
    }
    val type: Type get() = when(this) {
        is Song -> Type.SONG
        is Artist -> Type.ARTIST
        is Playlist -> Type.PLAYLIST
        else -> throw NotImplementedError(this.javaClass.name)
    }
    
    protected inline fun stringToJson(string: String?): String {
        return if (string == null) "null" else "\"$string\""
    }
    fun toJsonString(klaxon: Klaxon = DataApi.klaxon): String {
        return """
        {
            "type": $type,
            "title": ${stringToJson(title)},
            "artist": ${stringToJson(artist)},
            "description": ${stringToJson(description)},
            ${getJsonValues(klaxon)}
        }
        """
    }
    open protected fun getJsonValues(klaxon: Klaxon): String = ""

    companion object {
        fun fromJsonObject(obj: JsonObject, klaxon: DataApi.klaxon): MediaItem {
            val id = obj.string(id!!)
            val type = Type.values()[obj.int("type")!!]

            return when (type) {
                Type.SONG -> Song(id)
                Type.ARTIST -> Artist(id)
                Type.PLAYLIST -> Playlist(id)
            }.initFromJsonObject(obj, klaxon)
        }
        
        val json_converter = object : Converter {
            override fun canConvert(cls: Class<*>): Boolean {
                return MediaItem::class.java.isAssignableFrom(cls)
            }

            override fun fromJson(value: JsonValue): Any {
                if (value.obj == null) {
                    throw KlaxonException("Couldn't parse MediaItem as it isn't an object ($value)")
                }

                try {
                    return fromJsonObject(value)
                }
                catch (e: Exception) {
                    throw KlaxonException("Couldn't parse MediaItem ($value)", e)
                }
            }

            override fun toJson(obj: Any): String {
                return (obj as MediaItem).toJsonString(DataApi.klaxon)
            }
        }
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

    private var _load_status: LoadStatus by mutableStateOf(LoadStatus.NOT_LOADED)
    var load_status: LoadStatus
        get() = _load_status
        private set(value) { _load_status = value }
    val loaded: Boolean get() = _load_status == LoadStatus.LOADED

    private val _loading_lock = Object()
    val loading_lock: Object get() = getOrReplacedWith()._loading_lock

    abstract class ThumbnailProvider {
        abstract fun getThumbnail(quality: ThumbnailQuality): String?

        data class SetProvider(val thumbnails: List<Thumbnail>): ThumbnailProvider() {
            override fun getThumbnail(quality: ThumbnailQuality): String? {
                return when (quality) {
                    ThumbnailQuality.HIGH -> thumbnails.minByOrNull { it.width * it.height }
                    ThumbnailQuality.LOW -> thumbnails.maxByOrNull { it.width * it.height }
                }?.url
            }
        }

        data class DynamicProvider(val provider: (w: Int, h: Int) -> String): ThumbnailProvider() {
            override fun getThumbnail(quality: ThumbnailQuality): String? {
                val target_size = quality.getTargetSize()
                return provider(target_size.width, target_size.height)
            }

            companion object {
                fun fromDynamicUrl(url: String, width: Int, height: Int): DynamicProvider? {
                    val w_index = url.lastIndexOf("w$width")
                    val h_index = url.lastIndexOf("-h$height")

                    if (w_index == -1 || h_index == -1) {
                        return null
                    }

                    val url_a = url.substring(0, w_index + 1)
                    val url_b = url.substring(h_index + 2 + height.toString().length)
                    return DynamicProvider { w, h ->
                        return@DynamicProvider "$url_a$w-h$h$url_b"
                    }
                }
            }
        }
        data class Thumbnail(val url: String, val width: Int, val height: Int)

        companion object {
            fun fromThumbnails(thumbnails: List<Thumbnail>): ThumbnailProvider? {
                if (thumbnails.isEmpty()) {
                    return null
                }

                for (thumbnail in thumbnails) {
                    val dynamic_provider = DynamicProvider.fromDynamicUrl(thumbnail.url, thumbnail.width, thumbnail.height)
                    if (dynamic_provider != null) {
                        return dynamic_provider
                    }
                }
                return SetProvider(thumbnails)
            }
        }

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
    var thumbnail_provider: ThumbnailProvider? = null
    var thumbnail_palette: Palette? by mutableStateOf(null)

    private class ThumbState {
        var image: Bitmap? by mutableStateOf(null)
        var loading by mutableStateOf(false)
    }
    private val thumb_states: Map<ThumbnailQuality, ThumbState>

    fun getOrReplacedWith(): MediaItem {
        return replaced_with?.getOrReplacedWith() ?: this
    }

    fun getLoadedOrNull(): MediaItem? {
        if (!getOrReplacedWith().loaded) {
            return null
        }
        return getOrReplacedWith()
    }

    fun replaceWithItemWithId(new_id: String): MediaItem {
        if (_id == new_id) {
            return this
        }

        if (replaced_with != null) {
            if (replaced_with!!.getOrReplacedWith()._id == new_id) {
                return replaced_with!!.getOrReplacedWith()
            }
            throw IllegalStateException()
        }

        invalidate()

        replaced_with = when (type) {
            Type.SONG -> Song.fromId(new_id)
            Type.ARTIST -> Artist.fromId(new_id)
            Type.PLAYLIST -> Playlist.fromId(new_id)
        }

        return replaced_with!!
    }

    abstract val url: String

    private val _related_endpoints = mutableListOf<BrowseEndpoint>()
    val related_endpoints: List<BrowseEndpoint>
        get() = _related_endpoints

    private var invalidation_exception: Throwable? = null
    val is_valid: Boolean
        get() = invalidation_exception == null

    private fun invalidate() {
        invalidation_exception = RuntimeException()
    }

    fun requireValid() {
        if (invalidation_exception != null) {
            throw IllegalStateException("$this (replaced with $replaced_with) must be valid. Invalidated at cause.", invalidation_exception)
        }
    }

    init {
        val states = mutableMapOf<ThumbnailQuality, ThumbState>()
        for (quality in ThumbnailQuality.values()) {
            states[quality] = ThumbState()
        }
        thumb_states = states
    }

    fun addBrowseEndpoint(id: String, type: BrowseEndpoint.Type): Boolean {
        for (endpoint in _related_endpoints) {
            if (endpoint.id == id && endpoint.type == type) {
                return false
            }
        }
        _related_endpoints.add(BrowseEndpoint(id, type))
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

    fun getThumbnail(quality: ThumbnailQuality): Bitmap? {
        val state = thumb_states[quality]!!
        synchronized(state) {
            if (!state.loading) {
                thread {
                    loadThumbnail(quality)
                }
            }
        }
        return state.image
    }

    fun loadThumbnail(quality: ThumbnailQuality): Bitmap? {
        assert(loaded)

        val state = thumb_states[quality]!!
        synchronized(state) {
            if (state.loading) {
                (state as Object).wait()
                return state.image
            }

            if (state.image != null) {
                return state.image
            }

            state.loading = true
        }

        state.image = downloadThumbnail(quality)
        if (state.image != null) {
            thumbnail_palette = Palette.from(state.image!!.asImageBitmap().asAndroidBitmap()).clearFilters().generate()
        }

        synchronized(state) {
            state.loading = false
            (state as Object).notifyAll()
        }

        return state.image
    }

    protected open fun downloadThumbnail(quality: ThumbnailQuality): Bitmap? {
        val url = getThumbUrl(quality) ?: return null
        return BitmapFactory.decodeStream(URL(url).openConnection().getInputStream())
    }

    @Composable
    abstract fun PreviewSquare(content_colour: () -> Color, playerProvider: () -> PlayerViewContext, enable_long_press_menu: Boolean, modifier: Modifier)
    @Composable
    abstract fun PreviewLong(content_colour: () -> Color, playerProvider: () -> PlayerViewContext, enable_long_press_menu: Boolean, modifier: Modifier)

    @Composable
    fun Thumbnail(quality: ThumbnailQuality, modifier: Modifier) {
        LaunchedEffect(quality) {
            getThumbnail(quality)
        }

        Crossfade(thumb_states[quality]!!.image) { thumbnail ->
            if (thumbnail != null) {
                Image(
                    thumbnail.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = modifier
                )
            }
        }
    }

    fun loadData(): MediaItem {
        return loadMediaItemData(getOrReplacedWith()).getDataOrThrow()
    }

    open fun initWithData(data: JsonObject, klaxon: Klaxon): MediaItem {
        title = data.string("title")
        artist = data.string("artist")
        description = data.string("description")
        return this
    }

    fun getDefaultThemeColour(): Color? {
        for (quality in ThumbnailQuality.values()) {
            val state = thumb_states[quality]!!
            if (state.image != null) {
                return state.image?.getThemeColour()
            }
        }
        return null
    }

    override fun toString(): String {
        return "MediaItem(type=$type, id=$_id)"
    }
}