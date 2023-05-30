package com.spectre7.spmp.model.mediaitem

import GlobalPlayerState
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
import androidx.compose.ui.layout.ContentScale
import com.beust.klaxon.*
import com.spectre7.spmp.api.*
import com.spectre7.spmp.model.Cache
import com.spectre7.spmp.model.Settings
import com.spectre7.spmp.platform.PlatformContext
import com.spectre7.spmp.platform.ProjectPreferences
import com.spectre7.spmp.platform.toImageBitmap
import com.spectre7.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.spectre7.utils.*
import com.spectre7.utils.composable.SubtleLoadingIndicator
import kotlinx.coroutines.*
import java.io.File
import java.io.FileNotFoundException
import java.io.Reader
import java.net.URL
import java.time.Duration
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock

open class MediaItemData(open val data_item: MediaItem) {

    private var changes_made: Boolean = false
    fun onChanged(cached: Boolean = false) {
        if (!cached) {
            changes_made = true
        }
    }

    var original_title: String? by mutableStateOf(null)

    val title_listeners = ValueListeners<String?>()
    fun supplyTitle(value: String?, certain: Boolean = false, cached: Boolean = false): MediaItem {
        if (value != original_title && (original_title == null || certain)) {
            original_title = value
            title_listeners.call(data_item.title)
            onChanged(cached)
        }
        return data_item
    }

    var artist: Artist? by mutableStateOf(null)
        private set

    val artist_listeners = ValueListeners<Artist?>()
    fun supplyArtist(value: Artist?, certain: Boolean = false, cached: Boolean = false): MediaItem {
        if (data_item !is Artist && value != artist && (artist == null || certain)) {
            artist = value
            artist_listeners.call(artist)
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

    var thumbnail_provider: MediaItemThumbnailProvider? by mutableStateOf(null)
        private set

    fun supplyThumbnailProvider(value: MediaItemThumbnailProvider?, certain: Boolean = false, cached: Boolean = false): MediaItem {
        if (value != thumbnail_provider && (thumbnail_provider == null || certain)) {
            thumbnail_provider = value
            onChanged(cached)
        }
        return data_item
    }

    open fun getSerialisedData(klaxon: Klaxon = DataApi.klaxon): List<String> {
        return listOf(
            klaxon.toJsonString(original_title),
            klaxon.toJsonString(artist?.id),
            klaxon.toJsonString(description),
            klaxon.toJsonString(thumbnail_provider)
        )
    }

    open fun supplyFromSerialisedData(data: MutableList<Any?>, klaxon: Klaxon): MediaItemData {
        require(data.size >= 4) { data }
        data[data.size - 4]?.also { supplyTitle(it as String, cached = true) }
        data[data.size - 3]?.also { supplyArtist(Artist.fromId(it as String), cached = true) }
        data[data.size - 2]?.also { supplyDescription(it as String, cached = true) }
        data[data.size - 1]?.also { supplyThumbnailProvider(MediaItemThumbnailProvider.fromJsonObject(it as JsonObject, klaxon), cached = true) }
        return this
    }

    open fun supplyDataFromSubtitle(runs: List<TextRun>) {
        var artist_found = false
        for (run in runs) {
            val type = run.browse_endpoint_type ?: continue
            when (MediaItemType.fromBrowseEndpointType(type)) {
                MediaItemType.ARTIST -> {
                    val artist = run.navigationEndpoint?.browseEndpoint?.getMediaItem()
                    if (artist != null) {
                        supplyArtist(artist as Artist, true)
                        artist_found = true
                    }
                }
                MediaItemType.PLAYLIST_ACC -> {
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

    fun load() {
        val reader = getDataReader() ?: return
        thread {
            val array = DataApi.klaxon.parseJsonArray(reader)
            reader.close()

            runBlocking {
                var retries = 5
                while (retries-- > 0) {
                    try {
                        data_item.supplyFromSerialisedData(array.toMutableList(), DataApi.klaxon)
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
    }

    protected open fun getDataReader(): Reader? = Cache.get(data_item.cache_key)

    fun save() {
        if (!changes_made) {
            return
        }
        saveData(DataApi.mediaitem_klaxon.toJsonString(data_item))
    }

    protected open fun saveData(data: String) {
        Cache.setString(
            data_item.cache_key,
            DataApi.mediaitem_klaxon.toJsonString(data_item),
            MediaItem.CACHE_LIFETIME
        )
    }
}

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
abstract class MediaItem(id: String): MediaItemHolder {
    override val item: MediaItem get() = this
    open fun getHolder(): MediaItemHolder = this

    private val _id: String = id
    val id: String get() {
        requireValid()
        return _id
    }
    val uid: String get() = "${type.ordinal}$_id"

    abstract val data: MediaItemData
    val registry_entry: MediaItemDataRegistry.Entry

    open fun getDefaultRegistryEntry(): MediaItemDataRegistry.Entry = MediaItemDataRegistry.Entry().apply { item = this@MediaItem }

    val original_title: String? get() = data.original_title
    val title: String? get() = registry_entry.title ?: original_title
    val title_listeners: ValueListeners<String?> get() = data.title_listeners
    val artist: Artist? get() =
        if (this is Artist) this
        else data.artist
    val artist_listeners: ValueListeners<Artist?> get() = data.artist_listeners
    val description: String? get() = data.description
    val thumbnail_provider: MediaItemThumbnailProvider? get() = data.thumbnail_provider

    open fun canLoadThumbnail(): Boolean = thumbnail_provider != null

    fun supplyDataFromSubtitle(runs: List<TextRun>) {
        data.supplyDataFromSubtitle(runs)
    }

    // Remove?
    private var replaced_with: MediaItem? = null

    var pinned_to_home: Boolean by mutableStateOf(false)
        private set

    private val thumb_states: Map<MediaItemThumbnailProvider.Quality, ThumbState>

    init {
        // Populate thumb_states
        val states = mutableMapOf<MediaItemThumbnailProvider.Quality, ThumbState>()
        for (quality in MediaItemThumbnailProvider.Quality.values()) {
            states[quality] = ThumbState(this, quality, ::downloadThumbnail)
        }
        thumb_states = states

        // Get registry
        registry_entry = data_registry.getEntry(this)

        // Get pinned status
        pinned_to_home = Settings.INTERNAL_PINNED_ITEMS.get<Set<String>>().contains(uid)
    }

    val type: MediaItemType
        get() = when(this) {
        is Song -> MediaItemType.SONG
        is Artist -> MediaItemType.ARTIST
        is AccountPlaylist -> MediaItemType.PLAYLIST_ACC
        is LocalPlaylist -> MediaItemType.PLAYLIST_LOC
        else -> throw NotImplementedError(this.javaClass.name)
    }

    fun getSerialisedData(klaxon: Klaxon = DataApi.klaxon): List<String> {
        return data.getSerialisedData(klaxon)
    }

    fun supplyFromSerialisedData(data: MutableList<Any?>, klaxon: Klaxon): MediaItem {
        this@MediaItem.data.supplyFromSerialisedData(data, klaxon)
        return this
    }

    open fun isFullyLoaded(): Boolean {
        return original_title != null && artist != null && canLoadThumbnail()
    }

    val cache_key: String get() = getCacheKey(type, id)

    fun <T> editData(action: MediaItemData.() -> T): T {
        val ret = action(data)
        saveToCache()
        return ret
    }

    suspend fun <T> editDataSuspend(action: suspend MediaItemData.() -> T): T {
        val ret = action(data)
        saveToCache()
        return ret
    }

    fun editDataManual(action: (MediaItemData.() -> Unit)? = null): MediaItemData {
        action?.invoke(data)
        return data
    }

    fun loadFromCache() {
        data.load()
    }

    private fun saveToCache(): MediaItem {
        data.save()
        return this
    }

    private var loaded_callbacks: MutableList<(MediaItem) -> Unit>? = mutableListOf()
    private val loading_lock = ReentrantLock()
    private val load_condition = loading_lock.newCondition()

    var loading: Boolean by mutableStateOf(false)
        private set

    fun onLoaded(action: (MediaItem) -> Unit) {
        loading_lock.withLock {
            if (isFullyLoaded()) {
                action(this)
            }
            else {
                loaded_callbacks!!.add(action)
            }
        }
    }

    open suspend fun loadData(force: Boolean = false): Result<MediaItem?> = withContext(Dispatchers.IO) {
        if (!force && isFullyLoaded()) {
            loaded_callbacks?.forEach { it.invoke(this@MediaItem) }
            loaded_callbacks = null
            return@withContext Result.success(getOrReplacedWith())
        }

        loading_lock.withLock {
            if (loading) {
                load_condition.await()
                return@withContext Result.success(getOrReplacedWith())
            }
            loading = true
        }

        coroutineContext.job.invokeOnCompletion {
            loading_lock.withLock {
                load_condition.signalAll()
            }
        }

        val result = loadMediaItemData(getOrReplacedWith())
        loading_lock.withLock {
            if (isFullyLoaded()) {
                loaded_callbacks?.forEach { it.invoke(this@MediaItem) }
                loaded_callbacks = null
            }
        }

        return@withContext result
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
            MediaItemType.SONG -> Song.fromId(new_id)
            MediaItemType.ARTIST -> Artist.fromId(new_id)
            MediaItemType.PLAYLIST_ACC -> AccountPlaylist.fromId(new_id)
            else -> TODO()
        }

        return replaced_with!!
    }

    abstract val url: String?

    private val _related_endpoints = mutableListOf<MediaItemBrowseEndpoint>()
    val related_endpoints: List<MediaItemBrowseEndpoint>
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

    fun addBrowseEndpoint(id: String, type: MediaItemBrowseEndpoint.Type): Boolean {
        for (endpoint in _related_endpoints) {
            if (endpoint.id == id && endpoint.type == type) {
                return false
            }
        }
        _related_endpoints.add(MediaItemBrowseEndpoint(id, type))
        return true
    }

    fun addBrowseEndpoint(id: String, type_name: String): Boolean {
        return addBrowseEndpoint(id, MediaItemBrowseEndpoint.Type.fromString(type_name))
    }

    fun getThumbUrl(quality: MediaItemThumbnailProvider.Quality): String? {
        return thumbnail_provider?.getThumbnailUrl(quality)
    }

    fun isThumbnailLoaded(quality: MediaItemThumbnailProvider.Quality): Boolean {
        return thumb_states[quality]!!.image != null
    }

    suspend fun loadThumbnail(quality: MediaItemThumbnailProvider.Quality, context: PlatformContext = SpMp.context): ImageBitmap? {
        if (!canLoadThumbnail()) {
            return null
        }

        val state = thumb_states[quality]!!
        state.load(context)
        return state.image
    }

    fun getThumbnail(quality: MediaItemThumbnailProvider.Quality): ImageBitmap? = thumb_states[quality]!!.image

    fun getThumbnailLocalFile(quality: MediaItemThumbnailProvider.Quality, context: PlatformContext = SpMp.context): File = thumb_states[quality]!!.getCacheFile(context)

    protected open fun downloadThumbnail(quality: MediaItemThumbnailProvider.Quality): Result<ImageBitmap> {
        val url = getThumbUrl(quality) ?: return Result.failure(RuntimeException("getThumbUrl returned null"))

        try {
            val connection = URL(url).openConnection()
            connection.connectTimeout = DEFAULT_CONNECT_TIMEOUT

            val stream = connection.getInputStream()
            val bytes = stream.readBytes()
            stream.close()

            return Result.success(bytes.toImageBitmap())
        }
        catch (e: FileNotFoundException) {
            return Result.failure(e)
        }
    }

    fun setPinnedToHome(value: Boolean) {
        if (value == pinned_to_home) {
            return
        }

        val set: MutableSet<String> = Settings.INTERNAL_PINNED_ITEMS.get<Set<String>>().toMutableSet()
        if (value) {
            set.add(uid)
        }
        else {
            set.remove(uid)
        }
        Settings.INTERNAL_PINNED_ITEMS.set(set)

        pinned_to_home = value

        GlobalPlayerState.onMediaItemPinnedChanged(this, value)
    }

    data class PreviewParams(
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
    open fun getThumbnailHolder(): MediaItem = this

    @Composable
    open fun Thumbnail(
        quality: MediaItemThumbnailProvider.Quality,
        modifier: Modifier = Modifier,
        contentColourProvider: (() -> Color)? = null,
        onLoaded: ((ImageBitmap) -> Unit)? = null
    ) {
        val thumb_item = getThumbnailHolder()
        LaunchedEffect(thumb_item, quality, thumb_item.canLoadThumbnail()) {
            if (!thumb_item.canLoadThumbnail()) {
                thumb_item.loadData()
            }
            thumb_item.loadThumbnail(quality)
        }

        var loaded by remember { mutableStateOf(false) }
        LaunchedEffect(thumb_item) {
            loaded = false
        }

        val state = thumb_item.thumb_states.values.lastOrNull { state ->
            state.quality <= quality && state.image != null
        } ?: thumb_item.thumb_states[quality]!!

        if (state.loading || (state.image == null && !state.loaded)) {
            SubtleLoadingIndicator(modifier.fillMaxSize(), contentColourProvider)
        }
        else if (state.image != null) {
            state.image!!.also { thumbnail ->
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
        else if (state.loaded) {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.WifiOff, null)
            }
        }
    }

    open fun canGetThemeColour(): Boolean = thumb_states.values.any { it.image != null }

    open fun getThemeColour(): Color? {
        return getDefaultThemeColour()
    }

    fun getDefaultThemeColour(): Color? {
        for (quality in MediaItemThumbnailProvider.Quality.values()) {
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
    fun editRegistry(action: (MediaItemDataRegistry.Entry) -> Unit) {
        action(registry_entry)
        saveRegistry()
    }

    companion object {
        val CACHE_LIFETIME: Duration = Duration.ofDays(1)
        val data_registry: MediaItemDataRegistry = MediaItemDataRegistry()

        fun getCacheKey(type: MediaItemType, id: String): String {
            return "M/${type.name}/$id"
        }
        
        suspend fun fromUid(uid: String): MediaItem {
            val type_index = uid[0].toString().toInt()
            require(type_index in 0 until MediaItemType.values().size) { uid }

            val type = MediaItemType.values()[type_index]
            return type.fromId(uid.substring(1))
        }
        
        fun init(prefs: ProjectPreferences) {
            data_registry.load(prefs)
        }

        fun fromDataItems(data: List<Any?>, klaxon: Klaxon = DataApi.klaxon): MediaItem {
            require(data.size >= 2)

            val type = MediaItemType.values()[data[0] as Int]
            val id = data[1] as String

            return runBlocking {
                val item = when (type) {
                    MediaItemType.SONG -> Song.fromId(id)
                    MediaItemType.ARTIST -> Artist.fromId(id)
                    MediaItemType.PLAYLIST_ACC -> AccountPlaylist.fromId(id)
                    MediaItemType.PLAYLIST_LOC -> LocalPlaylist.fromId(id)
                }

                if (data.size > 2) {
                    item.editData {
                        item.supplyFromSerialisedData(data.toMutableList(), klaxon)
                    }
                }
                return@runBlocking item
            }
        }

        fun fromBrowseEndpointType(page_type: String, id: String): MediaItem {
            return when (page_type) {
                "MUSIC_PAGE_TYPE_PLAYLIST", "MUSIC_PAGE_TYPE_ALBUM", "MUSIC_PAGE_TYPE_AUDIOBOOK" ->
                    AccountPlaylist.fromId(id).editPlaylistData { supplyPlaylistType(Playlist.PlaylistType.fromTypeString(page_type), true) }
                "MUSIC_PAGE_TYPE_ARTIST", "MUSIC_PAGE_TYPE_USER_CHANNEL" ->
                    Artist.fromId(id)
                else -> throw NotImplementedError(page_type)
            }
        }
    }
}
