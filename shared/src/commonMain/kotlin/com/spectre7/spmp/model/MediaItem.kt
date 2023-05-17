package com.spectre7.spmp.model

import SpMp
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntSize
import com.beust.klaxon.*
import com.spectre7.spmp.api.DEFAULT_CONNECT_TIMEOUT
import com.spectre7.spmp.api.DataApi
import com.spectre7.spmp.api.TextRun
import com.spectre7.spmp.api.loadMediaItemData
import com.spectre7.spmp.platform.PlatformContext
import com.spectre7.spmp.platform.ProjectPreferences
import com.spectre7.spmp.platform.toByteArray
import com.spectre7.spmp.platform.toImageBitmap
import com.spectre7.spmp.resources.getString
import com.spectre7.spmp.ui.component.MediaItemLayout
import com.spectre7.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.spectre7.spmp.ui.layout.mainpage.PlayerViewContext
import com.spectre7.utils.*
import com.spectre7.utils.composable.SubtleLoadingIndicator
import kotlinx.coroutines.*
import java.io.File
import java.io.FileNotFoundException
import java.io.Reader
import java.net.SocketTimeoutException
import java.net.URL
import java.time.Duration
import java.util.*
import kotlin.concurrent.thread

open class MediaItemData(open val data_item: MediaItem) {

    private var changes_made: Boolean = false
    protected fun onChanged(cached: Boolean) {
        if (!cached) {
            changes_made = true
        }
    }

    var original_title: String? by mutableStateOf(null)

    val title_listeners = Listeners<(String?) -> Unit>()
    fun supplyTitle(value: String?, certain: Boolean = false, cached: Boolean = false): MediaItem {
        if (value != original_title && (original_title == null || certain)) {
            original_title = value
            title_listeners.call { it(data_item.title) }
            onChanged(cached)
        }
        return data_item
    }

    var artist: Artist? by mutableStateOf(null)
        private set

    val artist_listeners = Listeners<(Artist?) -> Unit>()
    fun supplyArtist(value: Artist?, certain: Boolean = false, cached: Boolean = false): MediaItem {
        if (data_item !is Artist && value != artist && (artist == null || certain)) {
            artist = value
            artist_listeners.call { it(artist) }
            onChanged(cached)
        }
        return data_item
    }

    var description: String? by mutableStateOf(null)
        private set

    fun supplyDescription(value: String?, certain: Boolean = false, cached: Boolean = false): MediaItem {
        if (value != description && (description == null || certain)) {
            description = value
            onChanged(cached)
        }
        return data_item
    }

    var thumbnail_provider: MediaItem.ThumbnailProvider? by mutableStateOf(null)
        private set

    fun supplyThumbnailProvider(value: MediaItem.ThumbnailProvider?, certain: Boolean = false, cached: Boolean = false): MediaItem {
        if (value != thumbnail_provider && (thumbnail_provider == null || certain)) {
            thumbnail_provider = value
            onChanged(cached)
        }
        return data_item
    }

    open fun supplyDataFromSubtitle(runs: List<TextRun>) {
        var artist_found = false
        for (run in runs) {
            val type = run.browse_endpoint_type ?: continue
            when (MediaItem.Type.fromBrowseEndpointType(type)) {
                MediaItem.Type.ARTIST -> {
                    val artist = run.navigationEndpoint?.browseEndpoint?.getMediaItem()
                    if (artist != null) {
                        supplyArtist(artist as Artist, true)
                        artist_found = true
                    }
                }
                MediaItem.Type.PLAYLIST -> {
                    check(this is SongItemData)

                    val playlist = run.navigationEndpoint?.browseEndpoint?.getMediaItem() as Playlist?
                    if (playlist != null) {
                        check(playlist.playlist_type == Playlist.PlaylistType.ALBUM)

                        playlist.editData {
                            supplyTitle(run.text, true)
                        }
                        supplyAlbum(playlist, true)
                    }
                }
                else -> {}
            }
        }

        if (!artist_found && data_item !is Artist) {
            val artist = Artist.createForItem(data_item)
            artist.editData {
                supplyTitle(runs[1].text)
            }
            supplyArtist(artist)
        }
    }

    fun save() {
        if (!changes_made) {
            return
        }

        // TODO
        Cache.setString(data_item.cache_key, DataApi.mediaitem_klaxon.toJsonString(data_item), MediaItem.CACHE_LIFETIME)
    }
}

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
abstract class MediaItem(id: String) {

    private val _id: String = id
    val id: String get() {
        requireValid()
        return _id
    }
    val uid: String get() = "${type.ordinal}$_id"

    protected abstract val data: MediaItemData
    val registry_entry: DataRegistry.Entry
    open fun getDefaultRegistryEntry(): DataRegistry.Entry = DataRegistry.Entry().apply { item = this@MediaItem }

    val original_title: String? get() = data.original_title
    val title: String? get() = registry_entry.title ?: original_title
    val title_listeners: Listeners<(String?) -> Unit> get() = data.title_listeners
    val artist: Artist? get() =
        if (this is Artist) this
        else data.artist
    val artist_listeners: Listeners<(Artist?) -> Unit> get() = data.artist_listeners
    val description: String? get() = data.description
    val thumbnail_provider: ThumbnailProvider? get() = data.thumbnail_provider

    open fun canLoadThumbnail(): Boolean = thumbnail_provider != null

    fun supplyDataFromSubtitle(runs: List<TextRun>) {
        data.supplyDataFromSubtitle(runs)
    }

    // Remove?
    private var replaced_with: MediaItem? = null
    
    var pinned_to_home: Boolean by mutableStateOf(false)
        private set

    inner class ThumbState(val quality: ThumbnailQuality) {
        var loading: Boolean by mutableStateOf(false)
        var image: ImageBitmap? by mutableStateOf(null)

        private val load_lock = Object()

        fun load(context: PlatformContext): Result<ImageBitmap> {
            synchronized(load_lock) {
                if (image != null) {
                    return Result.success(image!!)
                }

                if (loading) {
                    load_lock.wait()
                    return Result.success(image!!)
                }
                loading = true
            }

            val result = performLoad(context)

            synchronized(load_lock) {
                loading = false
                load_lock.notifyAll()
            }

            return result
        }

        private fun performLoad(context: PlatformContext): Result<ImageBitmap> {
            val cache_file = getCacheFile(context)
            if (cache_file.exists()) {
                cache_file.readBytes().toImageBitmap().also {
                    image = it
                    return Result.success(it)
                }
            }

            try {
                image = downloadThumbnail(quality) ?: return Result.failure(RuntimeException("No image loaded"))
            }
            catch (e: SocketTimeoutException) {
                return Result.failure(e) 
            }

            if (Settings.KEY_THUMB_CACHE_ENABLED.get()) {
                cache_file.parentFile.mkdirs()
                cache_file.writeBytes(image!!.toByteArray())
            }

            return Result.success(image!!)
        }

        fun getCacheFile(context: PlatformContext): File =
            context.getCacheDir().resolve("thumbnails/$id.${quality.ordinal}.png")
    }
    private val thumb_states: Map<ThumbnailQuality, ThumbState>

    init {
        // Populate thumb_states
        val states = mutableMapOf<ThumbnailQuality, ThumbState>()
        for (quality in ThumbnailQuality.values()) {
            states[quality] = ThumbState(quality)
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

    open fun getSerialisedData(klaxon: Klaxon = DataApi.klaxon): List<String> {
        return listOf(stringToJson(original_title), stringToJson(artist?.id), stringToJson(description), klaxon.toJsonString(thumbnail_provider))
    }
    open fun supplyFromSerialisedData(data: MutableList<Any?>, klaxon: Klaxon): MediaItem {
        require(data.size >= 4)
        runBlocking {
            with(this@MediaItem.data) {
                with(Dispatchers.Main) {
                    data[data.size - 4]?.also { supplyTitle(it as String, cached = true) }
                    data[data.size - 3]?.also { supplyArtist(Artist.fromId(it as String), cached = true) }
                    data[data.size - 2]?.also { supplyDescription(it as String, cached = true) }
                    data[data.size - 1]?.also { supplyThumbnailProvider(ThumbnailProvider.fromJsonObject(it as JsonObject, klaxon), cached = true) }
                }
            }
        }
        return this@MediaItem
    }

    open fun isFullyLoaded(): Boolean {
        return original_title != null && artist != null && thumbnail_provider != null
    }

    val cache_key: String get() = getCacheKey(type, id)

    fun <T> editData(action: MediaItemData.() -> T): T {
        val ret = action(data)
        saveToCache()
        return ret
    }

    fun editDataManual(action: (MediaItemData.() -> Unit)? = null): MediaItemData {
        action?.invoke(data)
        return data
    }

    fun loadFromCache() {
        val cached = Cache.get(cache_key)
        if (cached != null) {
            thread {
                val str = cached.readText()
                cached.close()
                try {
                    val array = DataApi.klaxon.parseJsonArray(str.reader())
                    runBlocking {
                        var retries = 5
                        while (retries-- > 0) {
                            try {
                                editData {
                                    supplyFromSerialisedData(array.toMutableList(), DataApi.klaxon)
                                }
                                break
                            }
                            catch (e: IllegalStateException) {
                                delay(100)
                                if (retries == 0) {
                                    throw e
                                }
                            }
                        }
                    }
                }
                catch (e: KlaxonException) {
                    println(this)
                    printJson(str)
                    throw e
                }
            }
        }
    }

    private fun saveToCache(): MediaItem {
        data.save()
        return this
    }

    companion object {
        val CACHE_LIFETIME: Duration = Duration.ofDays(1)
        val data_registry: DataRegistry = DataRegistry()
        val thumb_loader: MediaItemThumbnailLoader = MediaItemThumbnailLoader()

        fun getCacheKey(type: Type, id: String): String {
            return "M/${type.name}/$id"
        }

        fun init(prefs: ProjectPreferences) {
            data_registry.load(prefs)
        }

        fun fromDataItems(data: List<Any?>, klaxon: Klaxon = DataApi.klaxon): MediaItem {
            require(data.size >= 2)

            val type = Type.values()[data[0] as Int]
            val id = data[1] as String

            val item = when (type) {
                Type.SONG -> Song.fromId(id)
                Type.ARTIST -> Artist.fromId(id)
                Type.PLAYLIST -> Playlist.fromId(id)
            }

            if (data.size > 2) {
                item.editData {
                    item.supplyFromSerialisedData(data.toMutableList(), klaxon)
                }
            }
            return item
        }

        fun fromBrowseEndpointType(page_type: String, id: String): MediaItem {
            return when (page_type) {
                "MUSIC_PAGE_TYPE_PLAYLIST", "MUSIC_PAGE_TYPE_ALBUM", "MUSIC_PAGE_TYPE_AUDIOBOOK" ->
                    Playlist.fromId(id).editPlaylistData { supplyPlaylistType(Playlist.PlaylistType.fromTypeString(page_type), true) }
                "MUSIC_PAGE_TYPE_ARTIST", "MUSIC_PAGE_TYPE_USER_CHANNEL" ->
                    Artist.fromId(id)
                else -> throw NotImplementedError(page_type)
            }
        }

//        fun clearStoredItems() {
//            var amount = Song.clearStoredItems() + Artist.clearStoredItems() + Playlist.clearStoredItems()
//            println("Cleared $amount MediaItems")
//        }
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
        abstract fun getThumbnailUrl(quality: ThumbnailQuality): String?

        data class SetProvider(val thumbnails: List<Thumbnail>): ThumbnailProvider() {
            override fun getThumbnailUrl(quality: ThumbnailQuality): String? {
                return when (quality) {
                    ThumbnailQuality.HIGH -> thumbnails.minByOrNull { it.width * it.height }
                    ThumbnailQuality.LOW -> thumbnails.maxByOrNull { it.width * it.height }
                }?.url
            }
        }

        data class DynamicProvider(val url_a: String, val url_b: String): ThumbnailProvider() {
            override fun getThumbnailUrl(quality: ThumbnailQuality): String {
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
        return thumbnail_provider?.getThumbnailUrl(quality)
    }

    fun isThumbnailLoaded(quality: ThumbnailQuality): Boolean {
        return thumb_states[quality]!!.image != null
    }

    fun loadAndGetThumbnail(quality: ThumbnailQuality, context: PlatformContext = SpMp.context): ImageBitmap? {
        val state = thumb_states[quality]!!
        thumb_loader.loadThumbnail(state, context) {
            state.image = it.getOrNull()
        }
        return state.image
    }

    fun loadThumbnail(quality: ThumbnailQuality, context: PlatformContext = SpMp.context): ImageBitmap? {
        if (!canLoadThumbnail()) {
            return null
        }
        return thumb_states[quality]!!.load(context).getOrNull()
    }

    fun getThumbnailLocalFile(quality: ThumbnailQuality, context: PlatformContext = SpMp.context): File = thumb_states[quality]!!.getCacheFile(context)

    protected open fun downloadThumbnail(quality: ThumbnailQuality): ImageBitmap? {
        val url = getThumbUrl(quality) ?: return null

        try {
            val connection = URL(url).openConnection()
            connection.connectTimeout = DEFAULT_CONNECT_TIMEOUT

            val stream = connection.getInputStream()
            val bytes = stream.readBytes()
            stream.close()

            return bytes.toImageBitmap()
        }
        catch (_: FileNotFoundException) {
            return null
        }
    }

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

    data class PreviewParams(
        val playerProvider: () -> PlayerViewContext,
        val modifier: Modifier = Modifier,
        val contentColour: (() -> Color)? = null,
        val enable_long_press_menu: Boolean = true,
        val show_type: Boolean = true,
        val multiselect_context: MediaItemMultiSelectContext? = null
    )

    @Composable
    abstract fun PreviewSquare(params: PreviewParams)
    @Composable
    abstract fun PreviewLong(params: PreviewParams)

    @Composable
    fun Thumbnail(
        quality: ThumbnailQuality,
        modifier: Modifier = Modifier,
        contentColourProvider: (() -> Color)? = null,
        onLoaded: ((ImageBitmap) -> Unit)? = null
    ) {
        LaunchedEffect(quality, canLoadThumbnail()) {
            if (!canLoadThumbnail()) {
                thread { loadData() }
            }
            loadAndGetThumbnail(quality)
        }

        val state = thumb_states.values.lastOrNull { state ->
            state.quality <= quality && state.image != null
        } ?: thumb_states[quality]!!

        var loaded by remember { mutableStateOf(false) }

        if (state.loading) {
            SubtleLoadingIndicator(modifier.fillMaxSize(), contentColourProvider)
        }
        else if (state.image == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.WifiOff, null)
            }
        }
        else {
            state.image?.also { thumbnail ->
                if (!loaded && state.quality == quality) {
                    onLoaded?.invoke(thumbnail)
                    loaded = true
                }

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
        val size: Int get() = entries.size

        open class Entry {
            @Json(ignored = true)
            var item: MediaItem? = null

            @Json(ignored = true)
            val title_state: MutableState<String?> = mutableStateOf(null)

            var title: String?
                get() = title_state.value
                set(value) {
                    title_state.value = value
                    item?.also { i ->
                        i.title_listeners.call { it(i.title) }
                    }
                }
            var play_count: Int by mutableStateOf(0)
        }

        @Synchronized
        fun getEntry(item: MediaItem): Entry {
            return entries.getOrPut(item.uid) {
                item.getDefaultRegistryEntry()
            }.also { it.item = item }
        }

        @Synchronized
        fun load(prefs: ProjectPreferences = Settings.prefs) {
            val data = prefs.getString("data_registry", null)
            if (data == null) {
                return
            }

            entries.clear()

            val parsed = DataApi.klaxon.parseJsonObject(data.reader())
            runBlocking {
                parsed.entries.map { item ->
                    GlobalScope.async {
                        val type = Type.values()[item.key.take(1).toInt()]
                        entries[item.key] = type.parseRegistryEntry(item.value as JsonObject)
                    }
                }.joinAll()
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

abstract class MediaItemWithLayoutsData(item: MediaItem): MediaItemData(item) {
    var feed_layouts: List<MediaItemLayout>? by mutableStateOf(null)
        private set

    fun supplyFeedLayouts(value: List<MediaItemLayout>?, certain: Boolean, cached: Boolean = false): MediaItemWithLayoutsData {
        if (value != feed_layouts && (feed_layouts == null || certain)) {
            feed_layouts = value
            onChanged(cached)
        }
        return this
    }
}

abstract class MediaItemWithLayouts(id: String): MediaItem(id) {
    abstract override val data: MediaItemWithLayoutsData

    val feed_layouts: List<MediaItemLayout>? get() = data.feed_layouts

    override fun getSerialisedData(klaxon: Klaxon): List<String> {
        return super.getSerialisedData(klaxon) + listOf(klaxon.toJsonString(feed_layouts))
    }

    override fun supplyFromSerialisedData(data: MutableList<Any?>, klaxon: Klaxon): MediaItem {
        require(data.size >= 1)
        with(this@MediaItemWithLayouts.data) {
            data.removeLast()?.also { supplyFeedLayouts(klaxon.parseFromJsonArray(it as JsonArray<*>), true) }
        }
        return super.supplyFromSerialisedData(data, klaxon)
    }

    override fun isFullyLoaded(): Boolean {
        return super.isFullyLoaded() && feed_layouts != null
    }
}
