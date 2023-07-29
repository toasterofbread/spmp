package com.toasterofbread.spmp.model.mediaitem

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import app.cash.sqldelight.Query
import com.toasterofbread.Database
import com.toasterofbread.spmp.api.DEFAULT_CONNECT_TIMEOUT
import com.toasterofbread.spmp.api.model.TextRun
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.platform.toImageBitmap
import com.toasterofbread.spmp.ui.component.Thumbnail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

val MEDIA_ITEM_RELATED_CONTENT_ICON: ImageVector get() = Icons.Default.GridView

sealed interface MediaItem {
    val id: String

    fun getType(): MediaItemType
    fun getURL(): String

    val Loaded: Property<Boolean> get() = SingleProperty(
        { mediaItemQueries.loadedById(id) }, { loaded.fromSQLBoolean() }, { mediaItemQueries.updateLoadedById(it.toSQLBoolean(), id) }
    )
    val Title: Property<String?> get() = SingleProperty(
        { mediaItemQueries.titleById(id) }, { title }, { mediaItemQueries.updateTitleById(it, id) }
    )
    val OriginalTitle: Property<String?> get() = SingleProperty(
        { mediaItemQueries.originalTitleById(id) }, { original_title }, { mediaItemQueries.updateOriginalTitleById(it, id) }
    )
    val Description: Property<String?> get() = SingleProperty(
        { mediaItemQueries.descriptionById(id) }, { description }, { mediaItemQueries.updateDescriptionById(it, id) }
    )

    val ThumbnailProvider: Property<MediaItemThumbnailProvider?> get() = SingleProperty(
        { mediaItemQueries.thumbnailProviderById(id) }, { this.toThumbnailProvider() }, { mediaItemQueries.updateThumbnailProviderById(it?.url_a, it?.url_b, id) }
    )
    val ThemeColour: Property<Color?> get() = SingleProperty(
        { mediaItemQueries.themeColourById(id) }, { theme_colour?.let { Color(it) } }, { mediaItemQueries.updateThemeColourById(it?.toArgb()?.toLong(), id) }
    )
    val PinnedToHome: Property<Boolean> get() = SingleProperty(
        { mediaItemQueries.pinnedToHomeById(id) }, { pinned_to_home.fromSQLBoolean() }, { mediaItemQueries.updatePinnedToHomeById(it.toSQLBoolean(), id) }
    )
    val Hidden: Property<Boolean> get() = SingleProperty(
        { mediaItemQueries.isHiddenById(id) }, { hidden.fromSQLBoolean() }, { mediaItemQueries.updateIsHiddenById(it.toSQLBoolean(), id) }
    )

    suspend fun downloadThumbnailData(url: String): Result<ImageBitmap> = withContext(Dispatchers.IO) {
        return@withContext runCatching {
            val connection = URL(url).openConnection()
            connection.connectTimeout = DEFAULT_CONNECT_TIMEOUT

            val stream = connection.getInputStream()
            val bytes = stream.readBytes()
            stream.close()

            return@runCatching bytes.toImageBitmap()
        }
    }

    interface WithArtist {
        val Artist: Property<Artist?>
    }
    interface DataWithArtist {
        var artist: Artist?
    }
}

sealed class MediaItemData: MediaItem {
    var loaded: Boolean = false
    var title: String? = null
        set(value) {
            field = value
            original_title = value
        }
    var original_title: String? = null
        set(value) {
            field = value
            title = value
        }
    var description: String? = null
    var thumbnail_provider: MediaItemThumbnailProvider? = null
    var theme_colour: Color? = null
    var pinned_to_home: Boolean = false
    var hidden: Boolean = false

    companion object {
        fun fromBrowseEndpointType(page_type: String, id: String): MediaItemData {
            return when (page_type) {
                "MUSIC_PAGE_TYPE_PLAYLIST", "MUSIC_PAGE_TYPE_ALBUM", "MUSIC_PAGE_TYPE_AUDIOBOOK" ->
                    PlaylistData(id).apply { playlist_type = PlaylistType.fromTypeString(page_type) }
                "MUSIC_PAGE_TYPE_ARTIST", "MUSIC_PAGE_TYPE_USER_CHANNEL" ->
                    ArtistData(id)
                else -> throw NotImplementedError(page_type)
            }
        }
    }

    open fun saveToDatabase(db: Database) {
        db.transaction {
            Loaded.set(loaded, db)
            Title.set(title, db)
            OriginalTitle.set(original_title, db)
            Description.set(description, db)
            ThumbnailProvider.set(thumbnail_provider, db)
            ThemeColour.set(theme_colour, db)
            PinnedToHome.set(pinned_to_home, db)
            Hidden.set(hidden, db)
        }
    }

    open fun supplyDataFromSubtitle(runs: List<TextRun>) {
        var artist_found = false
        for (run in runs) {
            val type = run.browse_endpoint_type ?: continue
            when (MediaItemType.fromBrowseEndpointType(type)) {
                MediaItemType.ARTIST -> {
                    if (this is MediaItem.DataWithArtist) {
                        val item = run.navigationEndpoint?.browseEndpoint?.getMediaItem()
                        if (item is Artist) {
                            artist = item
                        }
                    }
                    artist_found = true
                }
                MediaItemType.PLAYLIST_ACC -> {
                    if (this is SongData) {
                        val playlist = run.navigationEndpoint?.browseEndpoint?.getMediaItem()
                        if (playlist is PlaylistData) {
                            assert(playlist.playlist_type == PlaylistType.ALBUM)
                            playlist.title = run.text
                            album = playlist
                        }
                    }
                }
                else -> {}
            }
        }

        if (!artist_found && this is MediaItem.DataWithArtist) {
            artist = ArtistData.createForItem(this).also {
                it.title = runs.getOrNull(1)?.text
            }
        }
    }
}

@Composable
fun <T, Q: Query<*>> Q.observeAsState(
    mapValue: (Q) -> T = { it as T },
    onExternalChange: (suspend (T) -> Unit)?
): MutableState<T> {
    val state = remember(this) { mutableStateOf(mapValue(this)) }
    var current_value by remember(state) { mutableStateOf(state.value) }

    DisposableEffect(state) {
        val listener = Query.Listener {
            current_value = mapValue(this@observeAsState)
            state.value = current_value
        }

        addListener(listener)
        onDispose {
            removeListener(listener)
        }
    }

    LaunchedEffect(state.value) {
        if (state.value != current_value) {
            current_value = state.value
            onExternalChange?.invoke(current_value)
        }
    }

    return state
}

//abstract class MediaItem(
//    val id: String,
//    val context: PlatformContext
//): MediaItemHolder {
//    abstract val title: String?
//    abstract val original_title: String?
//    abstract val description: String?
//    abstract val thumbnail_provider: MediaItemThumbnailProvider?
//
//    abstract val artist: Artist?
//    abstract val url: String?
//
//    val uid: String get() = "${type.ordinal}$id"
//    val type: MediaItemType
//        get() = when(this) {
//            is Song -> MediaItemType.SONG
//            is Artist -> MediaItemType.ARTIST
//            is PlaylistData -> MediaItemType.PLAYLIST_ACC
//            is LocalPlaylist -> MediaItemType.PLAYLIST_LOC
//            is BrowseParamsPlaylist -> MediaItemType.PLAYLIST_BROWSEPARAMS
//            else -> throw NotImplementedError(this.javaClass.name)
//        }
//
//    fun getReadableType(plural: Boolean): String =
//        when(this) {
//            is Song ->
//                if (song_type == SongType.PODCAST)
//                    PlaylistType.PODCAST.getReadable(plural)
//                else if (album?.playlist_type == PlaylistType.PODCAST || album?.playlist_type == PlaylistType.AUDIOBOOK)
//                    album?.playlist_type.getReadable(plural)
//                else MediaItemType.SONG.getReadable(plural)
//            else -> type.getReadable(plural)
//        }
//
//    abstract val data: MediaItemData
//    val registry_entry: MediaItemDataRegistry.Entry
//
//    var pinned_to_home: Boolean by mutableStateOf(false)
//        private set
//    var loading: Boolean by mutableStateOf(false)
//        private set
//
//    val title_listeners = ValueListeners<String?>()
//    val artist_listeners = ValueListeners<Artist?>()
//
//    suspend fun getTitle(): Result<String> =
//        getGeneralValue { title }
//
//    suspend fun getDescription(): Result<String> =
//        getGeneralValue { description }
//
//    suspend fun getArtist(): Result<Artist> =
//        getGeneralValue { artist }
//
//    suspend fun getArtistOrNull(): Result<Artist?> =
//        getGeneralValueOrNull { artist }
//
//    // TODO remove
//    suspend fun getThumbnailProvider(): Result<MediaItemThumbnailProvider> =
//        getGeneralValue { thumbnail_provider }
//
//    open fun canLoadThumbnail(): Boolean = thumbnail_provider != null
//
//    override fun toString(): String {
//        val artist_str = if (this is Artist) "" else ", artist=$artist"
//        return "$type(id=$id, title=$title$artist_str)"
//    }
//
//    override val item: MediaItem get() = this
//    open fun getHolder(): MediaItemHolder = this
//
//    fun supplyDataFromSubtitle(runs: List<TextRun>) {
//        data.supplyDataFromSubtitle(runs)
//    }
//
//    fun getSerialisedData(klaxon: Klaxon = Api.klaxon): List<String> {
//        return data.getSerialisedData(klaxon)
//    }
//
//    fun supplyFromSerialisedData(data: MutableList<Any?>, klaxon: Klaxon): MediaItem {
//        this@MediaItem.data.supplyFromSerialisedData(data, klaxon)
//        return this
//    }
//
//    fun <T> editData(action: MediaItemData.() -> T): T {
//        val ret = action(data)
//        saveToCache()
//        return ret
//    }
//
//    suspend fun <T> editDataSuspend(action: suspend MediaItemData.() -> T): T {
//        val ret = action(data)
//        saveToCache()
//        return ret
//    }
//
//    fun loadFromCache() {
//        data.load()
//    }
//
//    private fun saveToCache(): MediaItem {
//        data.save()
//        return this
//    }
//
//    fun getThumbUrl(quality: MediaItemThumbnailProvider.Quality): String? =
//        thumbnail_provider?.getThumbnailUrl(quality)
//
//    fun isThumbnailLoaded(quality: MediaItemThumbnailProvider.Quality): Boolean =
//        thumb_states[quality]!!.image != null
//
//    suspend fun loadThumbnail(quality: MediaItemThumbnailProvider.Quality, context: PlatformContext = SpMp.context, allow_lower: Boolean = false): ImageBitmap? {
//        if (!canLoadThumbnail()) {
//            return null
//        }
//
//        for (q in quality.ordinal downTo 0) {
//            val state = thumb_states[quality]!!
//            state.load(context)
//
//            if (state.image != null) {
//                return state.image
//            }
//
//            if (!allow_lower) {
//                break
//            }
//        }
//
//        return null
//    }
//
//    fun getThumbnail(quality: MediaItemThumbnailProvider.Quality): ImageBitmap? =
//        thumb_states[quality]!!.image
//
//    fun getThumbnailLocalFile(quality: MediaItemThumbnailProvider.Quality, context: PlatformContext = SpMp.context): File =
//        thumb_states[quality]!!.getCacheFile(context)
//
//    fun setPinnedToHome(value: Boolean) {
//        if (value == pinned_to_home) {
//            return
//        }
//
//        val set: MutableSet<String> = Settings.INTERNAL_PINNED_ITEMS.get<Set<String>>().toMutableSet()
//        if (value) {
//            set.add(uid)
//        }
//        else {
//            set.remove(uid)
//        }
//        Settings.INTERNAL_PINNED_ITEMS.set(set)
//
//        pinned_to_home = value
//
//        SpMp.context.player_state.onMediaItemPinnedChanged(this, value)
//    }
//
//    @Composable
//    abstract fun PreviewSquare(params: MediaItemPreviewParams)
//    @Composable
//    abstract fun PreviewLong(params: MediaItemPreviewParams)
//
//    @Composable
//    open fun getThumbnailHolder(): MediaItem = this
//
//    open fun canGetThemeColour(): Boolean = thumb_states.values.any { it.image != null }
//
//    open fun getThemeColour(): Color? {
//        return getDefaultThemeColour()
//    }
//
//    fun getDefaultThemeColour(): Color? {
//        for (quality in MediaItemThumbnailProvider.Quality.values()) {
//            val state = thumb_states[quality]!!
//            if (state.image != null) {
//                return state.image?.getThemeColour()
//            }
//        }
//        return null
//    }
//
//    fun saveRegistry() {
//        data_registry.save()
//    }
//    fun editRegistry(action: (MediaItemDataRegistry.Entry) -> Unit) {
//        action(registry_entry)
//        saveRegistry()
//    }
//
//    private val thumb_states: Map<MediaItemThumbnailProvider.Quality, ThumbState>
//
//    private var loaded_callbacks: MutableList<(MediaItem) -> Unit>? = mutableListOf()
//    private val loading_lock = ReentrantLock()
//    private val load_condition = loading_lock.newCondition()
//    private var load_result: Result<Unit>? = null
//
//    init {
//        // Populate thumb_states
//        val states = mutableMapOf<MediaItemThumbnailProvider.Quality, ThumbState>()
//        for (quality in MediaItemThumbnailProvider.Quality.values()) {
//            states[quality] = ThumbState(this, quality, ::downloadThumbnail)
//        }
//        thumb_states = states
//
//        // Get registry
//        registry_entry = data_registry.getEntry(this) { getDefaultRegistryEntry() }
//
//        // Get pinned status
//        pinned_to_home = Settings.INTERNAL_PINNED_ITEMS.get<Set<String>>(context).contains(uid)
//    }
//
//    protected open suspend fun loadGeneralData(item_id: String = id, browse_params: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
//        loading_lock.withLock {
//            if (loading) {
//                load_condition.await()
//                return@withContext load_result ?: Result.failure(CancellationException())
//            }
//            loading = true
//        }
//
//        coroutineContext.job.invokeOnCompletion {
//            loading_lock.withLock {
//                loading = false
//                load_condition.signalAll()
//                load_result = null
//            }
//        }
//
//        load_result = loadMediaItemData(this@MediaItem, item_id, browse_params)
//        loading_lock.withLock {
//            loaded_callbacks?.forEach { it.invoke(this@MediaItem) }
//            loaded_callbacks = null
//        }
//
//        return@withContext load_result!!
//    }
//
//    protected suspend inline fun <reified T> getGeneralValueOrNull(getValue: () -> T?): Result<T?> {
//        getValue()?.also { return Result.success(it) }
//        val result = loadGeneralData()
//        if (result.isFailure) {
//            return result.cast()
//        }
//        return Result.success(getValue())
//    }
//
//    protected suspend inline fun <reified T> getGeneralValue(getValue: () -> T?): Result<T> {
//        return getGeneralValueOrNull(getValue).fold(
//            { value ->
//                if (value != null) Result.success(value)
//                else Result.failure(RuntimeException("Value with type '${T::class.simpleName}' not loaded in item $this"))
//            },
//            { Result.failure(it) }
//        )
//    }
//
//    protected open fun getDefaultRegistryEntry(): MediaItemDataRegistry.Entry = MediaItemDataRegistry.Entry().apply { item = this@MediaItem }
//
//    protected open fun downloadThumbnail(quality: MediaItemThumbnailProvider.Quality): Result<ImageBitmap> {
//        val url = getThumbUrl(quality) ?: return Result.failure(RuntimeException("getThumbUrl returned null"))
//
//        try {
//            val connection = URL(url).openConnection()
//            connection.connectTimeout = DEFAULT_CONNECT_TIMEOUT
//
//            val stream = connection.getInputStream()
//            val bytes = stream.readBytes()
//            stream.close()
//
//            return Result.success(bytes.toImageBitmap())
//        }
//        catch (e: Throwable) {
//            return Result.failure(e)
//        }
//    }
//
//    companion object {
//        val CACHE_LIFETIME: Duration = Duration.ofDays(1)
//        private val data_registry: MediaItemDataRegistry = MediaItemDataRegistry()
//
//        fun init(prefs: ProjectPreferences) {
//            data_registry.load(prefs)
//        }
//
//        suspend fun fromUid(uid: String, context: PlatformContext = SpMp.context): MediaItem {
//            val type_index = uid[0].toString().toInt()
//            require(type_index in 0 until MediaItemType.values().size) { uid }
//
//            val type = MediaItemType.values()[type_index]
//            return type.fromId(uid.substring(1), context)
//        }
//
//        fun fromDataItems(data: List<Any?>, klaxon: Klaxon = Api.klaxon): MediaItem {
//            require(data.size >= 2)
//
//            val type = MediaItemType.values()[data[0] as Int]
//            val id = data[1] as String
//
//            return runBlocking {
//                val item = when (type) {
//                    MediaItemType.SONG -> SongData(id)
//                    MediaItemType.ARTIST -> ArtistData(id)
//                    MediaItemType.PLAYLIST_ACC -> PlaylistData(id)
//                    MediaItemType.PLAYLIST_LOC -> LocalPlaylist.fromId(id)
//                    MediaItemType.PLAYLIST_BROWSEPARAMS -> throw IllegalStateException(id)
//                }
//
//                if (data.size > 2) {
//                    item.editData {
//                        item.supplyFromSerialisedData(data.toMutableList(), klaxon)
//                    }
//                }
//                return@runBlocking item
//            }
//        }
//
//        fun fromBrowseEndpointType(page_type: String, id: String): MediaItem {
//            return when (page_type) {
//                "MUSIC_PAGE_TYPE_PLAYLIST", "MUSIC_PAGE_TYPE_ALBUM", "MUSIC_PAGE_TYPE_AUDIOBOOK" ->
//                    PlaylistData(id).editPlaylistData { supplyPlaylistType(PlaylistType.fromTypeString(page_type), true) }
//                "MUSIC_PAGE_TYPE_ARTIST", "MUSIC_PAGE_TYPE_USER_CHANNEL" ->
//                    ArtistData(id)
//                else -> throw NotImplementedError(page_type)
//            }
//        }
//
//        val RELATED_CONTENT_ICON: ImageVector get() = Icons.Default.GridView
//    }
//}
