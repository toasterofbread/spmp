package com.toasterofbread.spmp.model.mediaitem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import app.cash.sqldelight.Query
import com.toasterofbread.Database
import com.toasterofbread.spmp.api.model.TextRun
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.utils.composable.OnChangedEffect
import mediaitem.ThumbnailProviderById

sealed interface MediaItem {
    val id: String
    val loaded: Boolean
    val title: String?
    val original_title: String?
    val description: String?
    val theme_colour: Color?
    val thumbnail_provider: MediaItemThumbnailProvider?
    val pinned_to_home: Boolean

    fun getType(): MediaItemType
    fun getURL(): String
}

sealed class MediaItemData(
    override var loaded: Boolean = false,
    override var title: String? = null,
    override var original_title: String? = null,
    override var description: String? = null,
    override var theme_colour: Color? = null,
    override var thumbnail_provider: MediaItemThumbnailProvider? = null,
    override var pinned_to_home: Boolean = false
): MediaItem {
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

    open fun supplyDataFromSubtitle(runs: List<TextRun>) {
        var artist_found = false
        for (run in runs) {
            val type = run.browse_endpoint_type ?: continue
            when (MediaItemType.fromBrowseEndpointType(type)) {
                MediaItemType.ARTIST -> {
                    if (this is DataWithArtist) {
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

        if (!artist_found && this is DataWithArtist) {
            artist = ArtistData.createForItem(this).also {
                it.title = runs.getOrNull(1)?.text
            }
        }
    }
}

interface WithArtist {
    val artist: Artist?
}
interface DataWithArtist: WithArtist {
    override var artist: Artist?
}

class MediaItemObservableState private constructor (
    val loaded_state: MutableState<Boolean>,

    val title_state: MutableState<String?>,
    val original_title_state: MutableState<String?>,
    val description_state: MutableState<String?>,

    val theme_colour_state: MutableState<Long?>,
    val thumbnail_provider_state: MutableState<ThumbnailProviderById?>,
    val pinned_to_home_state: MutableState<Long?>
) {
    companion object {
        @Composable
        internal fun create(id: String, db: Database) =
            with(db.mediaItemQueries) {
                MediaItemObservableState(
                    loadedById(id).observeAsState(
                        { it.executeAsOne().loaded != null },
                        { updateLoadedById(if (it) 0 else null, id) }
                    ),
                    titleById(id).observeAsState(
                        { it.executeAsOne().title },
                        { updateTitleById(it, id) }
                    ),
                    originalTitleById(id).observeAsState(
                        { it.executeAsOne().original_title },
                        { updateOriginalTitleById(it, id) }
                    ),
                    descriptionById(id).observeAsState(
                        { it.executeAsOne().description },
                        { updateDescriptionById(it, id) }
                    ),
                    themeColourById(id).observeAsState(
                        { it.executeAsOne().theme_colour },
                        { updateThemeColourById(it, id) }
                    ),
                    thumbnailProviderById(id).observeAsState(
                        { it.executeAsOne() },
                        { updateThumbnailProviderById(it?.thumb_url_a, it?.thumb_url_b, id) }
                    ),
                    pinnedToHomeById(id).observeAsState(
                        { it.executeAsOne().pinned_to_home },
                        { updatePinnedToHomeById(it, id) }
                    )
                )
            }
    }
}

abstract class ObservableMediaItem internal constructor(
    override val id: String,
    protected val db: Database,
    state: MediaItemObservableState
): MediaItem {
    override var loaded: Boolean by state.loaded_state

    override var title: String? by state.title_state
    override var original_title: String? by state.original_title_state
    override var description: String? by state.description_state

    // TODO Move mapping logic to MediaItemObservableState
    private var theme_colour_value: Long? by state.theme_colour_state
    override var theme_colour: Color?
        get() = theme_colour_value?.let { Color(it) }
        set(value) {
            theme_colour_value = value?.value?.toLong()
        }

    private var thumbnail_provider_value: ThumbnailProviderById? by state.thumbnail_provider_state
    override var thumbnail_provider: MediaItemThumbnailProvider?
        get() = thumbnail_provider_value?.let {
            if (it.thumb_url_a == null) null
            else MediaItemThumbnailProvider(it.thumb_url_a, it.thumb_url_b)
        }
        set(value) {
            db.mediaItemQueries.updateThumbnailProviderById(value?.url_a, value?.url_b, id)
        }

    private var pinned_to_home_value: Long? by state.pinned_to_home_state
    override var pinned_to_home: Boolean
        get() = pinned_to_home_value != null
        set(value) {
            pinned_to_home_value = if (value) 0L else null
        }

    open suspend fun loadDataFromYouTube() {
        TODO()
    }
}

@Composable
fun <T, Q: Any> Query<Q>.observeAsState(
    mapValue: (Query<Q>) -> T = { it as T },
    onChanged: ((T) -> Unit)?
): MutableState<T> {
    val state = remember { mutableStateOf(mapValue(this)) }

    DisposableEffect(Unit) {
        val listener = Query.Listener { state.value = mapValue(this@observeAsState) }
        addListener(listener)

        onDispose {
            removeListener(listener)
        }
    }

    OnChangedEffect(state.value) {
        onChanged?.invoke(state.value)
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
//    @Composable
//    open fun Thumbnail(
//        quality: MediaItemThumbnailProvider.Quality,
//        modifier: Modifier = Modifier,
//        failure_icon: ImageVector? = Icons.Default.CloudOff,
//        contentColourProvider: (() -> Color)? = null,
//        onLoaded: ((ImageBitmap) -> Unit)? = null
//    ) {
//        val thumb_item = getThumbnailHolder()
//        LaunchedEffect(thumb_item, quality, thumb_item.canLoadThumbnail()) {
//            if (!thumb_item.canLoadThumbnail()) {
//                val provider_result = thumb_item.getThumbnailProvider()
//                if (provider_result.isFailure) {
//                    thumb_item.thumb_states[quality]!!.loaded = true
//                    return@LaunchedEffect
//                }
//            }
//            thumb_item.loadThumbnail(quality)
//        }
//
//        var loaded by remember { mutableStateOf(false) }
//        LaunchedEffect(thumb_item) {
//            loaded = false
//        }
//
//        val state = thumb_item.thumb_states.values.lastOrNull { state ->
//            state.quality <= quality && state.image != null
//        } ?: thumb_item.thumb_states[quality]!!
//
//        if (state.loading || (state.image == null && !state.loaded)) {
//            SubtleLoadingIndicator(modifier.fillMaxSize(), contentColourProvider)
//        }
//        else if (state.image != null) {
//            state.image!!.also { thumbnail ->
//                if (!loaded) {
//                    onLoaded?.invoke(thumbnail)
//                    loaded = true
//                }
//
//                Image(
//                    thumbnail,
//                    contentDescription = null,
//                    contentScale = ContentScale.Crop,
//                    modifier = modifier
//                )
//            }
//        }
//        else if (state.loaded) {
//            if (failure_icon != null) {
//                Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//                    Icon(failure_icon, null)
//                }
//            }
//        }
//    }
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
