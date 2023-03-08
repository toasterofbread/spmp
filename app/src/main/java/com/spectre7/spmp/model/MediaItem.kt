package com.spectre7.spmp.model

import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FeaturedPlayList
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntSize
import androidx.core.content.edit
import androidx.palette.graphics.Palette
import com.beust.klaxon.*
import com.spectre7.spmp.R
import com.spectre7.spmp.api.DataApi
import com.spectre7.spmp.api.cast
import com.spectre7.spmp.api.loadMediaItemData
import com.spectre7.spmp.ui.component.MediaItemLayout
import com.spectre7.spmp.ui.layout.PlayerViewContext
import com.spectre7.utils.SubtleLoadingIndicator
import com.spectre7.utils.getString
import com.spectre7.utils.getThemeColour
import com.spectre7.utils.printJson
import java.io.Reader
import java.net.URL
import java.time.Duration
import kotlin.concurrent.thread

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
abstract class MediaItem(id: String) {

    val reg_entry: DataRegistry.Entry
    open fun getDefaultRegistryEntry(): DataRegistry.Entry = DataRegistry.Entry()

    private val _id: String = id
    val id: String get() {
        requireValid()
        return _id
    }
    val uid: String get() = "${type.ordinal}$_id"

    var title: String? by mutableStateOf(null)
        private set

    fun supplyTitle(value: String?, certain: Boolean = false): MediaItem {
        if (value != null && (title == null || certain)) {
            title = value
        }
        return this
    }

    var artist: Artist? by mutableStateOf(null)
        private set

    fun supplyArtist(value: Artist?, certain: Boolean = false): MediaItem {
        assert(this !is Artist || value == this)

        if (value != null && (artist == null || certain)) {
            artist = value
        }
        return this
    }

    var description: String? by mutableStateOf(null)
        private set

    fun supplyDescription(value: String?, certain: Boolean = false): MediaItem {
        if (value != null && (description == null || certain)) {
            description = value
        }
        return this
    }

    var thumbnail_provider: ThumbnailProvider? by mutableStateOf(null)
        protected set
    open fun canLoadThumbnail(): Boolean = thumbnail_provider != null

    fun supplyThumbnailProvider(value: ThumbnailProvider?, certain: Boolean = false): MediaItem {
        if (value != null && (thumbnail_provider == null || certain)) {
            thumbnail_provider = value
        }
        return this
    }

    private var replaced_with: MediaItem? = null

    enum class Type {
        SONG, ARTIST, PLAYLIST;

        fun getIcon(): ImageVector {
            return when (this) {
                SONG     -> Icons.Filled.MusicNote
                ARTIST   -> Icons.Filled.Person
                PLAYLIST -> Icons.Filled.FeaturedPlayList
            }
        }

        fun getReadable(plural: Boolean = false): String {
            return when (this) {
                SONG -> getString(if (plural) R.string.songs else R.string.song)
                ARTIST -> getString(if (plural) R.string.artists else R.string.artist)
                PLAYLIST -> getString(if (plural) R.string.playlists else R.string.playlist)
            }
        }

        fun parseRegistryEntry(obj: JsonObject): DataRegistry.Entry {
            return when (this) {
                SONG -> DataApi.klaxon.parseFromJsonObject<Song.SongDataRegistryEntry>(obj)!!
                else -> DataApi.klaxon.parseFromJsonObject(obj)!!
            }
        }
    }
    val type: Type get() = when(this) {
        is Song -> Type.SONG
        is Artist -> Type.ARTIST
        is Playlist -> Type.PLAYLIST
        else -> throw NotImplementedError(this.javaClass.name)
    }

    protected fun stringToJson(string: String?): String {
        return DataApi.klaxon.toJsonString(string)
    }
    open fun getJsonMapValues(klaxon: Klaxon = DataApi.klaxon): String {
        return """
            "title": ${stringToJson(title)},
            "artist": ${stringToJson(artist?.id)},
            "desc": ${stringToJson(description)},
            "thumb": ${klaxon.toJsonString(thumbnail_provider)},
        """
    }

    open fun supplyFromJsonObject(data: JsonObject, klaxon: Klaxon): MediaItem {
        assert(data.int("type") == type.ordinal)
        data.string("title")?.also { title = it }
        data.string("artist")?.also { artist = Artist.fromId(it) }
        data.string("desc")?.also { description = it }
        data.obj("thumb")?.also { thumbnail_provider = ThumbnailProvider.fromJsonObject(it, klaxon) }
        return this
    }

    open fun isFullyLoaded(): Boolean {
        return title != null && artist != null && thumbnail_provider != null
    }

    fun toJsonData(): String {
        return Klaxon().converter(json_converter).toJsonString(this)
    }

    fun loadFromCache() {
        val cached = Cache.get(cache_key)
        if (cached != null) {
            thread {
                val klaxon = DataApi.klaxon.converter(json_converter)

                val str = cached.readText()
                cached.close()
                try {
                    val obj = klaxon.parseJsonObject(str.reader())
                    supplyFromJsonObject(obj, klaxon)
                }
                catch (e: KlaxonException) {
                    printJson(str)
                    println(this)
                    throw e
                }
            }
        }
    }

    fun saveToCache() {
        Cache.setString(cache_key, toJsonData(), CACHE_LIFETIME)
    }

    companion object {
        val CACHE_LIFETIME: Duration = Duration.ofDays(1)

        fun getCacheKey(type: Type, id: String): String {
            return "M/${type.name}/$id"
        }

        val data_registry: DataRegistry = DataRegistry()

        fun init(prefs: SharedPreferences) {
            data_registry.load(prefs)
        }

        fun fromJsonData(reader: Reader): MediaItem {
            return Klaxon().converter(json_converter).parse(reader)!!
        }

        fun fromJsonObject(obj: JsonObject, klaxon: Klaxon = DataApi.klaxon, ref_only: Boolean = false): MediaItem {
            val id = obj.string("id")!!
            val item = when (Type.values()[obj.int("type")!!]) {
                Type.SONG -> Song.fromId(id)
                Type.ARTIST -> Artist.fromId(id)
                Type.PLAYLIST -> Playlist.fromId(id)
            }

            if (!ref_only) {
                item.supplyFromJsonObject(obj, klaxon)
            }
            return item
        }

        protected val json_converter = object : Converter {
            private val json_ref_converter = object : Converter {
                override fun canConvert(cls: Class<*>): Boolean {
                    return MediaItem::class.java.isAssignableFrom(cls)
                }

                override fun fromJson(jv: JsonValue): Any {
                    if (jv.obj == null) {
                        throw KlaxonException("Couldn't parse MediaItem as it isn't an object ($jv)")
                    }

                    try {
                        return fromJsonObject(jv.obj!!, DataApi.klaxon.converter(this), true)
                    }
                    catch (e: Exception) {
                        throw RuntimeException("Couldn't parse MediaItem ($jv)", e)
                    }
                }

                override fun toJson(value: Any): String {
                    if (value !is MediaItem) {
                        throw KlaxonException("Value $value is not a MediaItem")
                    }
                    return """{
                        "type": ${value.type.ordinal},
                        "id": "${value.id}"
                    }"""
                }
            }

            override fun canConvert(cls: Class<*>): Boolean {
                return MediaItem::class.java.isAssignableFrom(cls)
            }

            override fun fromJson(jv: JsonValue): Any {
                if (jv.obj == null) {
                    throw KlaxonException("Couldn't parse MediaItem as it isn't an object ($jv)")
                }

                try {
                    return fromJsonObject(jv.obj!!, DataApi.klaxon.converter(json_ref_converter))
                }
                catch (e: Exception) {
                    println("aaa")
                    println(jv.obj?.get("type"))
                    throw RuntimeException("Couldn't parse MediaItem (${jv.obj})", e)
                }
            }

            override fun toJson(value: Any): String {
                if (value !is MediaItem) {
                    throw KlaxonException("Value $value is not a MediaItem")
                }
                return """{
                    "type": ${value.type.ordinal},
                    "id": "${value.id}",${value.getJsonMapValues(DataApi.klaxon.converter(json_ref_converter))}
                }"""
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

    private val _loading_lock = Object()
    val loading_lock: Object get() = getOrReplacedWith()._loading_lock
    var loading: Boolean by mutableStateOf(false)

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

        data class DynamicProvider(val url_a: String, val url_b: String): ThumbnailProvider() {
            override fun getThumbnail(quality: ThumbnailQuality): String {
                val target_size = quality.getTargetSize()
                return "$url_a${target_size.width}-h${target_size.height}$url_b"
            }

            companion object {
                fun fromDynamicUrl(url: String, width: Int, height: Int): DynamicProvider? {
                    val w_index = url.lastIndexOf("w$width")
                    val h_index = url.lastIndexOf("-h$height")

                    if (w_index == -1 || h_index == -1) {
                        return null
                    }

                    return DynamicProvider(
                        url.substring(0, w_index + 1),
                        url.substring(h_index + 2 + height.toString().length)
                    )
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

            fun fromJsonObject(obj: JsonObject, klaxon: Klaxon): ThumbnailProvider? {
                if (obj.containsKey("thumbnails")) {
                    return klaxon.parseFromJsonObject<SetProvider>(obj)
                }
                return klaxon.parseFromJsonObject<DynamicProvider>(obj)
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
    var thumbnail_palette: Palette? by mutableStateOf(null)

    private class ThumbState {
        var image: Bitmap? by mutableStateOf(null)
        var loading by mutableStateOf(false)
    }
    private val thumb_states: Map<ThumbnailQuality, ThumbState>

    fun getOrReplacedWith(): MediaItem {
        return replaced_with?.getOrReplacedWith() ?: this
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
        reg_entry = data_registry.getEntry(this)
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
        if (!canLoadThumbnail()) {
            return null
        }

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
    fun Thumbnail(quality: ThumbnailQuality, modifier: Modifier = Modifier, content_colour: Color = Color.White) {
        LaunchedEffect(quality, canLoadThumbnail()) {
            getThumbnail(quality)
        }

        Crossfade(thumb_states[quality]!!.image) { thumbnail ->
            if (thumbnail == null) {
                SubtleLoadingIndicator(content_colour, modifier.fillMaxSize())
            }
            else {
                Image(
                    thumbnail.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = modifier
                )
            }
        }
    }

    fun loadData(force: Boolean = false): Result<MediaItem> {
        if (!force && isFullyLoaded()) {
            return Result.success(getOrReplacedWith())
        }
        return loadMediaItemData(getOrReplacedWith())
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
        return "MediaItem(type=$type, id=$_id, title=$title)"
    }

    val cache_key: String get() = getCacheKey(type, id)

    fun saveRegistry() {
        MediaItem.data_registry.save()
    }

    class DataRegistry {
        private val entries: MutableMap<String, Entry> = mutableMapOf()

        open class Entry {
            var title: String? by mutableStateOf(null)
        }

        @Synchronized
        fun getEntry(item: MediaItem): Entry {
            return entries.getOrPut(item.uid) {
                item.getDefaultRegistryEntry()
            }
        }

        @Synchronized
        fun load(prefs: SharedPreferences = Settings.prefs) {
            val data = prefs.getString("data_registry", null)
            if (data == null) {
                return
            }

            entries.clear()

            val parsed = DataApi.klaxon.parseJsonObject(data.reader())
            for (item in parsed.entries) {
                val type = Type.values()[item.key.take(1).toInt()]
                entries[item.key] = type.parseRegistryEntry(item.value as JsonObject)
            }
        }

        @Synchronized
        fun save(prefs: SharedPreferences = Settings.prefs) {
            prefs.edit {
                putString("data_registry", DataApi.klaxon.toJsonString(entries))
            }
        }
    }
}

abstract class MediaItemWithLayouts(id: String): MediaItem(id) {
    var feed_layouts: List<MediaItemLayout>? by mutableStateOf(null)
        private set

    fun supplyFeedLayouts(value: List<MediaItemLayout>?, certain: Boolean): MediaItem {
        if (value != null && (feed_layouts == null || certain)) {
            feed_layouts = value
        }
        return this
    }

    override fun getJsonMapValues(klaxon: Klaxon): String {
        return super.getJsonMapValues(klaxon) + "\"feed_layouts\": ${klaxon.toJsonString(feed_layouts)},"
    }

    override fun supplyFromJsonObject(data: JsonObject, klaxon: Klaxon): MediaItem {
        data.array<JsonObject>("feed_layouts")?.also { feed_layouts = klaxon.parseFromJsonArray(it) }
        return super.supplyFromJsonObject(data, klaxon)
    }

    override fun isFullyLoaded(): Boolean {
        return super.isFullyLoaded() && feed_layouts != null
    }
}
