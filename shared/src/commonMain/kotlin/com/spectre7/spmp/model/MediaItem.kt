package com.spectre7.spmp.model

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.unit.Dp
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntSize
import com.beust.klaxon.*
import com.spectre7.spmp.api.DataApi
import com.spectre7.spmp.api.TextRun
import com.spectre7.spmp.api.loadMediaItemData
import com.spectre7.spmp.ui.component.MediaItemLayout
import com.spectre7.spmp.ui.layout.PlayerViewContext
import com.spectre7.spmp.ui.theme.Theme
import com.spectre7.spmp.platform.ProjectPreferences
import com.spectre7.spmp.platform.toImageBitmap
import com.spectre7.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.io.Reader
import java.net.URL
import java.time.Duration
import java.util.*
import kotlin.concurrent.thread

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
abstract class MediaItem(id: String) {

    val registry_entry: DataRegistry.Entry
    open fun getDefaultRegistryEntry(): DataRegistry.Entry = DataRegistry.Entry()

    private val _id: String = id
    val id: String get() {
        requireValid()
        return _id
    }
    val uid: String get() = "${type.ordinal}$_id"

    var original_title: String? by mutableStateOf(null)
    var title: String?
        get() = registry_entry.title ?: original_title
        private set(value) { original_title = value }

    fun supplyTitle(value: String?, certain: Boolean = false): MediaItem {
        if (value != null && (original_title == null || certain)) {
            original_title = value
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

    fun supplyDataFromSubtitle(runs: List<TextRun>) {
        var artist_found = false
        for (run in runs) {
            val type = run.browse_endpoint_type ?: continue
            when (Type.fromBrowseEndpointType(type)) {
                Type.ARTIST -> {
                    val artist = run.navigationEndpoint?.browseEndpoint?.getMediaItem()
                    if (artist != null) {
                        supplyArtist(artist as Artist, true)
                        artist_found = true
                    }
                }
                Type.PLAYLIST -> TODO()
                else -> {}
            }
        }

        if (!artist_found && this !is Artist) {
            val artist = Artist.createForItem(this)
            artist.supplyTitle(runs[1].text)
            supplyArtist(artist)
        }
    }

    // Remove?
    private var replaced_with: MediaItem? = null
    
    var pinned_to_home: Boolean by mutableStateOf(false)
        private set

    fun setPinnedToHome(value: Boolean, playerProvider: () -> PlayerViewContext) {
        if (value == pinned_to_home) {
            return
        }

        val key = when (type) {
            Type.SONG -> Settings.INTERNAL_PINNED_SONGS
            Type.ARTIST -> Settings.INTERNAL_PINNED_ARTISTS
            Type.PLAYLIST -> Settings.INTERNAL_PINNED_PLAYLISTS
        }

        val set: MutableSet<String> = key.get<Set<String>>().toMutableSet()
        if (value) {
            set.add(id)
        }
        else {
            set.remove(id)
        }
        key.set(set)
        
        pinned_to_home = value

        playerProvider().onMediaItemPinnedChanged(this, value)
    }

    private class ThumbState {
        var image: ImageBitmap? by mutableStateOf(null)
        var loading by mutableStateOf(false)
    }
    private val thumb_states: Map<ThumbnailQuality, ThumbState>

    init {
        // Populate thumb_states
        val states = mutableMapOf<ThumbnailQuality, ThumbState>()
        for (quality in ThumbnailQuality.values()) {
            states[quality] = ThumbState()
        }
        thumb_states = states

        // Get registry
        registry_entry = data_registry.getEntry(this)

        // Get pinned status
        val key = when (type) {
            Type.SONG -> Settings.INTERNAL_PINNED_SONGS
            Type.ARTIST -> Settings.INTERNAL_PINNED_ARTISTS
            Type.PLAYLIST -> Settings.INTERNAL_PINNED_PLAYLISTS
        }
        pinned_to_home = key.get<Set<String>>().contains(id)
    }

    enum class Type {
        SONG, ARTIST, PLAYLIST;

        fun getIcon(): ImageVector {
            return when (this) {
                SONG     -> Icons.Filled.MusicNote
                ARTIST   -> Icons.Filled.Person
                PLAYLIST -> Icons.Filled.PlaylistPlay
            }
        }

        fun getReadable(plural: Boolean = false): String {
            return getString(when (this) {
                SONG -> if (plural) "songs" else "song"
                ARTIST -> if (plural) "artists" else "artist"
                PLAYLIST -> if (plural) "playlists" else "playlist"
            })
        }

        fun parseRegistryEntry(obj: JsonObject): DataRegistry.Entry {
            return when (this) {
                SONG -> DataApi.klaxon.parseFromJsonObject<Song.SongDataRegistryEntry>(obj)!!
                else -> DataApi.klaxon.parseFromJsonObject(obj)!!
            }
        }

        override fun toString(): String {
            return name.lowercase().replaceFirstChar { it.uppercase() }
        }

        companion object {
            fun fromBrowseEndpointType(page_type: String): Type? {
                return when (page_type) {
                    "MUSIC_PAGE_TYPE_PLAYLIST", "MUSIC_PAGE_TYPE_ALBUM", "MUSIC_PAGE_TYPE_AUDIOBOOK" -> PLAYLIST
                    "MUSIC_PAGE_TYPE_ARTIST", "MUSIC_PAGE_TYPE_USER_CHANNEL" -> ARTIST
                    else -> null
                }
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
            "title": ${stringToJson(original_title)},
            "artist": ${stringToJson(artist?.id)},
            "desc": ${stringToJson(description)},
            "thumb": ${klaxon.toJsonString(thumbnail_provider)},
        """
    }

    open fun supplyFromJsonObject(data: JsonObject, klaxon: Klaxon): MediaItem {
        assert(data.int("type") == type.ordinal)
        runBlocking {
            withContext(Dispatchers.Main) {
                data.string("title")?.also { original_title = it }
                data.string("artist")?.also { artist = Artist.fromId(it) }
                data.string("desc")?.also { description = it }
                data.obj("thumb")?.also { thumbnail_provider = ThumbnailProvider.fromJsonObject(it, klaxon) }
            }
        }
        return this
    }

    open fun isFullyLoaded(): Boolean {
        return original_title != null && artist != null && thumbnail_provider != null
    }

    private fun toJsonData(): String {
        return DataApi.mediaitem_klaxon.toJsonString(this)
    }

    val cache_key: String get() = getCacheKey(type, id)

    fun loadFromCache() {
        val cached = Cache.get(cache_key)
        if (cached != null) {
            thread {
                val str = cached.readText()
                cached.close()
                try {
                    val obj = DataApi.klaxon.parseJsonObject(str.reader())
                    supplyFromJsonObject(obj, DataApi.klaxon)
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

        fun init(prefs: ProjectPreferences) {
            data_registry.load(prefs)
        }

        fun fromJsonData(reader: Reader): MediaItem {
            return DataApi.klaxon.parse(reader)!!
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

        fun fromBrowseEndpointType(page_type: String, id: String): MediaItem {
            return when (page_type) {
                "MUSIC_PAGE_TYPE_PLAYLIST", "MUSIC_PAGE_TYPE_ALBUM", "MUSIC_PAGE_TYPE_AUDIOBOOK" ->
                    Playlist.fromId(id).supplyPlaylistType(Playlist.PlaylistType.fromTypeString(page_type), true)
                "MUSIC_PAGE_TYPE_ARTIST", "MUSIC_PAGE_TYPE_USER_CHANNEL" ->
                    Artist.fromId(id)
                else -> throw NotImplementedError(page_type)
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

        data class DynamicProvider(val url_a: String, val url_b: String, val original_url: String): ThumbnailProvider() {
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
                        url.substring(h_index + 2 + height.toString().length),
                        url
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

    fun getThumbnail(quality: ThumbnailQuality): ImageBitmap? {
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

    fun loadThumbnail(quality: ThumbnailQuality): ImageBitmap? {
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

        try {
            state.image = downloadThumbnail(quality)
        }
        finally {
            synchronized(state) {
                state.loading = false
                (state as Object).notifyAll()
            }
        }

        return state.image
    }

    protected open fun downloadThumbnail(quality: ThumbnailQuality): ImageBitmap? {
        val url = getThumbUrl(quality) ?: return null

        try {
            val stream = URL(url).openConnection().getInputStream()
            val bytes = stream.readBytes()
            stream.close()

            return bytes.toImageBitmap()
        }
        catch (_: FileNotFoundException) {
            return null
        }
    }

    data class PreviewParams(
        val playerProvider: () -> PlayerViewContext,
        val modifier: Modifier = Modifier,
        val content_colour: () -> Color = Theme.current.on_background_provider,
        val enable_long_press_menu: Boolean = true,
        val show_type: Boolean = true
    )

    @Composable
    abstract fun PreviewSquare(params: PreviewParams)
    @Composable
    abstract fun PreviewLong(params: PreviewParams)

    @Composable
    fun Thumbnail(quality: ThumbnailQuality, modifier: Modifier = Modifier, contentColourProvider: () -> Color = { Color.White }) {
        LaunchedEffect(quality, canLoadThumbnail()) {
            if (!canLoadThumbnail()) {
                thread { loadData() }
            }
            getThumbnail(quality)
        }

        Crossfade(thumb_states[quality]!!.image) { thumbnail ->
            if (thumbnail == null) {
                SubtleLoadingIndicator(contentColourProvider, modifier.fillMaxSize())
            }
            else {
                Image(
                    thumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = modifier
                )
            }
        }
    }

    fun loadData(force: Boolean = false): Result<MediaItem?> {
        if (!force && isFullyLoaded()) {
            return Result.success(getOrReplacedWith())
        }
        return loadMediaItemData(getOrReplacedWith())
    }

    fun canGetThemeColour(): Boolean = thumb_states.values.any { it.image != null }

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
        val artist_str = if (this is Artist) "" else ", artist=$artist"
        return "$type(id=$_id, title=$title$artist_str)"
    }

    fun saveRegistry() {
        data_registry.save()
    }
    fun editRegistry(action: (DataRegistry.Entry) -> Unit) {
        action(registry_entry)
        saveRegistry()
    }

    class DataRegistry {
        private val entries: MutableMap<String, Entry> = mutableMapOf()

        open class Entry {
            var title: String? by mutableStateOf(null)
            var play_count: Int by mutableStateOf(0)
        }

        @Synchronized
        fun getEntry(item: MediaItem): Entry {
            return entries.getOrPut(item.uid) {
                item.getDefaultRegistryEntry()
            }
        }

        @Synchronized
        fun load(prefs: ProjectPreferences = Settings.prefs) {
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
        fun save(prefs: ProjectPreferences = Settings.prefs) {
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
