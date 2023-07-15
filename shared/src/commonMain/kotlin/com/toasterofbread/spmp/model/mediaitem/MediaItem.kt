package com.toasterofbread.spmp.model.mediaitem

import SpMp
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import com.beust.klaxon.*
import com.toasterofbread.spmp.api.*
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.data.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.model.mediaitem.enums.SongType
import com.toasterofbread.spmp.model.mediaitem.enums.getReadable
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.platform.ProjectPreferences
import com.toasterofbread.spmp.platform.toImageBitmap
import com.toasterofbread.utils.*
import com.toasterofbread.utils.composable.SubtleLoadingIndicator
import kotlinx.coroutines.*
import java.io.File
import java.net.URL
import java.time.Duration
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

abstract class MediaItem(val id: String, context: PlatformContext): MediaItemHolder {
    val uid: String get() = "${type.ordinal}$id"
    abstract val url: String?

    val type: MediaItemType
        get() = when(this) {
            is Song -> MediaItemType.SONG
            is Artist -> MediaItemType.ARTIST
            is AccountPlaylist -> MediaItemType.PLAYLIST_ACC
            is LocalPlaylist -> MediaItemType.PLAYLIST_LOC
            is BrowseParamsPlaylist -> MediaItemType.PLAYLIST_BROWSEPARAMS
            else -> throw NotImplementedError(this.javaClass.name)
        }

    fun getReadableType(plural: Boolean): String =
        when(this) {
            is Song ->
                if (song_type == SongType.PODCAST)
                    PlaylistType.PODCAST.getReadable(plural)
                else if (album?.playlist_type == PlaylistType.PODCAST || album?.playlist_type == PlaylistType.AUDIOBOOK)
                    album?.playlist_type.getReadable(plural)
                else MediaItemType.SONG.getReadable(plural)
            else -> type.getReadable(plural)
        }

    abstract val data: MediaItemData
    val registry_entry: MediaItemDataRegistry.Entry

    var pinned_to_home: Boolean by mutableStateOf(false)
        private set
    var loading: Boolean by mutableStateOf(false)
        private set

    val original_title: String? get() = data.original_title
    val title: String? get() = registry_entry.title ?: original_title
    val title_listeners: ValueListeners<String?> get() = data.title_listeners
    val artist: Artist? get() =
        if (this is Artist) this
        else data.artist
    val artist_listeners: ValueListeners<Artist?> get() = data.artist_listeners
    val description: String? get() = data.description
    val thumbnail_provider: MediaItemThumbnailProvider? get() = data.thumbnail_provider

    suspend fun getTitle(): Result<String> =
        getGeneralValue { title }

    suspend fun getDescription(): Result<String> =
        getGeneralValue { description }

    suspend fun getArtist(): Result<Artist> =
        getGeneralValue { artist }

    suspend fun getArtistOrNull(): Result<Artist?> =
        getGeneralValueOrNull { artist }

    // TODO remove
    suspend fun getThumbnailProvider(): Result<MediaItemThumbnailProvider> =
        getGeneralValue { thumbnail_provider }

    open fun canLoadThumbnail(): Boolean = thumbnail_provider != null

    override fun toString(): String {
        val artist_str = if (this is Artist) "" else ", artist=$artist"
        return "$type(id=$id, title=$title$artist_str)"
    }

    override val item: MediaItem get() = this
    open fun getHolder(): MediaItemHolder = this

    fun supplyDataFromSubtitle(runs: List<TextRun>) {
        data.supplyDataFromSubtitle(runs)
    }

    fun getSerialisedData(klaxon: Klaxon = Api.klaxon): List<String> {
        return data.getSerialisedData(klaxon)
    }

    fun supplyFromSerialisedData(data: MutableList<Any?>, klaxon: Klaxon): MediaItem {
        this@MediaItem.data.supplyFromSerialisedData(data, klaxon)
        return this
    }

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

    fun loadFromCache() {
        data.load()
    }

    private fun saveToCache(): MediaItem {
        data.save()
        return this
    }

    fun getThumbUrl(quality: MediaItemThumbnailProvider.Quality): String? =
        thumbnail_provider?.getThumbnailUrl(quality)

    fun isThumbnailLoaded(quality: MediaItemThumbnailProvider.Quality): Boolean =
        thumb_states[quality]!!.image != null

    suspend fun loadThumbnail(quality: MediaItemThumbnailProvider.Quality, context: PlatformContext = SpMp.context, allow_lower: Boolean = false): ImageBitmap? {
        if (!canLoadThumbnail()) {
            return null
        }

        for (q in quality.ordinal downTo 0) {
            val state = thumb_states[quality]!!
            state.load(context)

            if (state.image != null) {
                return state.image
            }

            if (!allow_lower) {
                break
            }
        }

        return null
    }

    fun getThumbnail(quality: MediaItemThumbnailProvider.Quality): ImageBitmap? =
        thumb_states[quality]!!.image

    fun getThumbnailLocalFile(quality: MediaItemThumbnailProvider.Quality, context: PlatformContext = SpMp.context): File =
        thumb_states[quality]!!.getCacheFile(context)

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

        SpMp.context.player_state.onMediaItemPinnedChanged(this, value)
    }

    @Composable
    abstract fun PreviewSquare(params: MediaItemPreviewParams)
    @Composable
    abstract fun PreviewLong(params: MediaItemPreviewParams)

    @Composable
    open fun getThumbnailHolder(): MediaItem = this

    @Composable
    open fun Thumbnail(
        quality: MediaItemThumbnailProvider.Quality,
        modifier: Modifier = Modifier,
        failure_icon: ImageVector? = Icons.Default.CloudOff,
        contentColourProvider: (() -> Color)? = null,
        onLoaded: ((ImageBitmap) -> Unit)? = null
    ) {
        val thumb_item = getThumbnailHolder()
        LaunchedEffect(thumb_item, quality, thumb_item.canLoadThumbnail()) {
            if (!thumb_item.canLoadThumbnail()) {
                val provider_result = thumb_item.getThumbnailProvider()
                if (provider_result.isFailure) {
                    thumb_item.thumb_states[quality]!!.loaded = true
                    return@LaunchedEffect
                }
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
                if (!loaded) {
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
            if (failure_icon != null) {
                Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(failure_icon, null)
                }
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

    fun saveRegistry() {
        data_registry.save()
    }
    fun editRegistry(action: (MediaItemDataRegistry.Entry) -> Unit) {
        action(registry_entry)
        saveRegistry()
    }

    private val thumb_states: Map<MediaItemThumbnailProvider.Quality, ThumbState>

    private var loaded_callbacks: MutableList<(MediaItem) -> Unit>? = mutableListOf()
    private val loading_lock = ReentrantLock()
    private val load_condition = loading_lock.newCondition()
    private var load_result: Result<Unit>? = null

    init {
        // Populate thumb_states
        val states = mutableMapOf<MediaItemThumbnailProvider.Quality, ThumbState>()
        for (quality in MediaItemThumbnailProvider.Quality.values()) {
            states[quality] = ThumbState(this, quality, ::downloadThumbnail)
        }
        thumb_states = states

        // Get registry
        registry_entry = data_registry.getEntry(this) { getDefaultRegistryEntry() }

        // Get pinned status
        pinned_to_home = Settings.INTERNAL_PINNED_ITEMS.get<Set<String>>(context).contains(uid)
    }

    protected open suspend fun loadGeneralData(item_id: String = id, browse_params: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        loading_lock.withLock {
            if (loading) {
                load_condition.await()
                return@withContext load_result ?: Result.failure(CancellationException())
            }
            loading = true
        }

        coroutineContext.job.invokeOnCompletion {
            loading_lock.withLock {
                loading = false
                load_condition.signalAll()
                load_result = null
            }
        }

        load_result = loadMediaItemData(this@MediaItem, item_id, browse_params)
        loading_lock.withLock {
            loaded_callbacks?.forEach { it.invoke(this@MediaItem) }
            loaded_callbacks = null
        }

        return@withContext load_result!!
    }

    protected suspend inline fun <reified T> getGeneralValueOrNull(getValue: () -> T?): Result<T?> {
        getValue()?.also { return Result.success(it) }
        val result = loadGeneralData()
        if (result.isFailure) {
            return result.cast()
        }
        return Result.success(getValue())
    }

    protected suspend inline fun <reified T> getGeneralValue(getValue: () -> T?): Result<T> {
        return getGeneralValueOrNull(getValue).fold(
            { value ->
                if (value != null) Result.success(value)
                else Result.failure(RuntimeException("Value with type '${T::class.simpleName}' not loaded in item $this"))
            },
            { Result.failure(it) }
        )
    }

    protected open fun getDefaultRegistryEntry(): MediaItemDataRegistry.Entry = MediaItemDataRegistry.Entry().apply { item = this@MediaItem }

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
        catch (e: Throwable) {
            return Result.failure(e)
        }
    }

    companion object {
        val CACHE_LIFETIME: Duration = Duration.ofDays(1)
        private val data_registry: MediaItemDataRegistry = MediaItemDataRegistry()

        fun init(prefs: ProjectPreferences) {
            data_registry.load(prefs)
        }

        suspend fun fromUid(uid: String, context: PlatformContext = SpMp.context): MediaItem {
            val type_index = uid[0].toString().toInt()
            require(type_index in 0 until MediaItemType.values().size) { uid }

            val type = MediaItemType.values()[type_index]
            return type.fromId(uid.substring(1), context)
        }

        fun fromDataItems(data: List<Any?>, klaxon: Klaxon = Api.klaxon): MediaItem {
            require(data.size >= 2)

            val type = MediaItemType.values()[data[0] as Int]
            val id = data[1] as String

            return runBlocking {
                val item = when (type) {
                    MediaItemType.SONG -> Song.fromId(id)
                    MediaItemType.ARTIST -> Artist.fromId(id)
                    MediaItemType.PLAYLIST_ACC -> AccountPlaylist.fromId(id)
                    MediaItemType.PLAYLIST_LOC -> LocalPlaylist.fromId(id)
                    MediaItemType.PLAYLIST_BROWSEPARAMS -> throw IllegalStateException(id)
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
                    AccountPlaylist.fromId(id).editPlaylistData { supplyPlaylistType(PlaylistType.fromTypeString(page_type), true) }
                "MUSIC_PAGE_TYPE_ARTIST", "MUSIC_PAGE_TYPE_USER_CHANNEL" ->
                    Artist.fromId(id)
                else -> throw NotImplementedError(page_type)
            }
        }

        val RELATED_CONTENT_ICON: ImageVector get() = Icons.Default.GridView
    }
}
