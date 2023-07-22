package com.toasterofbread.spmp.model.mediaitem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.toasterofbread.Database
import com.toasterofbread.spmp.api.cast
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MediaItemLayout

interface Playlist: MediaItem, WithArtist {
    val items: List<MediaItem>?
    val playlist_type: PlaylistType?
    val browse_params: String?
    val item_count: Int?
    val year: Int?
    val total_duration: Long?

    val continuation: MediaItemLayout.Continuation?
    val item_set_ids: List<String>?

    override fun getType(): MediaItemType = if (playlist_type == PlaylistType.LOCAL) MediaItemType.PLAYLIST_LOC else MediaItemType.PLAYLIST_ACC
    override fun getURL(): String = "https://music.youtube.com/playlist?list=$id"

    companion object {
        fun formatYoutubeId(id: String): String = id.removePrefix("VL")
    }
}

class PlaylistData(
    override var id: String,
    override var artist: Artist? = null,

    override var items: List<MediaItem>? = null,
    override var playlist_type: PlaylistType? = null,
    override var browse_params: String? = null,
    override var item_count: Int? = null,
    override var year: Int? = null,
    override var total_duration: Long? = null,

    override var continuation: MediaItemLayout.Continuation? = null,
    override var item_set_ids: List<String>? = null
): MediaItemData(), Playlist, DataWithArtist {

    suspend fun loadItems(database: Database): Result<Pair<List<MediaItem>, String?>> {
        MediaItemLoader.withPlaylistLock {
            val current_items: List<MediaItem>? = items ?: database.transactionWithResult {
                val loaded = database.playlistQueries.areItemsLoadedById(id).executeAsOne().items_loaded != null
                if (!loaded) {
                    return@transactionWithResult null
                }

                database.playlistItemQueries.byPlaylistId(id).executeAsList().map {
                    SongData(it.song_id)
                }
            }

            if (current_items != null) {
                return Result.success(Pair(current_items, continuation?.token))
            }

            val load_result = MediaItemLoader.loadPlaylist(this)
            val item = load_result.getOrNull() ?: return load_result.cast()

            return item.items.let { loaded_items ->
                if (loaded_items == null) {
                    return@let Result.failure(IllegalStateException("loadPlaylist failed to load items but didn't return an error"))
                }

                items = loaded_items
                continuation = item.continuation

                return@let Result.success(Pair(loaded_items, continuation?.token))
            }
        }
    }

//    companion object {
//        fun loadFromId(database: Database, playlist_id: String): PlaylistData = database.transactionWithResult {
//            val data = database.playlistQueries.byId(playlist_id).executeAsOne()
//            val items = database.playlistItemQueries.byPlaylistId(playlist_id).executeAsList()
//
//            PlaylistData(
//                data.id,
//                data.artist,
//                items.map { it.song_id },
//                data.playlist_type?.let { PlaylistType.values()[it.toInt()] },
//                data.browse_params,
//                data.item_count?.toInt(),
//                data.year?.toInt(),
//                data.total_duration
//            )
//        }
//    }
}

class ObservablePlaylist(
    id: String,
    db: Database,
    base: MediaItemObservableState,
    playlist_type_state: MutableState<PlaylistType?>
): ObservableMediaItem(id, db, base), Playlist {

    override var playlist_type: PlaylistType? by playlist_type_state

    companion object {
        @Composable
        fun create(id: String, db: Database) =
            with(db.playlistQueries) {
                ObservablePlaylist(
                    id,
                    db,
                    MediaItemObservableState.create(id, db),

                    playlistTypeById(id).observeAsState(
                        { it.executeAsOne().playlist_type?.let { type ->
                            PlaylistType.values()[type.toInt()]
                        }},
                        { updatePlaylistTypeById(it?.ordinal?.toLong(), id) }
                    )
                )
            }
    }
}

class PlaylistDataRegistryEntry: MediaItemDataRegistry.Entry() {
    var playlist_page_thumb_width: Float? by mutableStateOf(null)
    var image_item_uid: String? by mutableStateOf(null)

    override fun clear() {
        super.clear()
        playlist_page_thumb_width = null
        image_item_uid = null
    }
}

//abstract class Playlist protected constructor (id: String, context: PlatformContext): MediaItem(id, context), MediaItemWithLayouts {
//    abstract override val data: PlaylistItemData
//    val playlist_reg_entry: PlaylistDataRegistryEntry get() = registry_entry as PlaylistDataRegistryEntry
//
//    abstract val is_editable: Boolean?
//    abstract val playlist_type: PlaylistType?
//    abstract val total_duration: Long?
//    abstract val item_count: Int?
//
//    override val feed_layouts: List<MediaItemLayout>?
//        @Composable
//        get() = getLayout()?.let { listOf(it) }
//
//    override suspend fun getFeedLayouts(): Result<List<MediaItemLayout>> =
//        getGeneralValue {
//            data.items?.let {
//                listOf(MediaItemLayout(null, null, MediaItemLayout.Type.LIST, it))
//            }
//        }
//
//    @Composable
//    fun getLayout(): MediaItemLayout? {
//        checkNotDeleted()
//        return remember(data.items) {
//            data.items?.let {
//                MediaItemLayout(null, null, MediaItemLayout.Type.LIST, it)
//            }
//        }
//    }
//
//    open val items: MutableList<MediaItem>? get() = checkNotDeleted(data.items)
//    val year: Int? get() = checkNotDeleted(data.year)
//
//    override fun getHolder(): PlaylistHolder = PlaylistHolder(this)
//
//    var is_deleted: Boolean = false
//        private set
//    fun checkNotDeleted() {
//        check(!is_deleted) { toString() }
//    }
//    fun <T> checkNotDeleted(result: T): T {
//        check(!is_deleted)
//        return result
//    }
//
//    open fun addItem(item: MediaItem) {
//        checkNotDeleted()
//        check(is_editable == true)
//        data.items!!.add(item)
//    }
//
//    open fun removeItem(index: Int) {
//        checkNotDeleted()
//        check(is_editable == true)
//        data.items!!.removeAt(index)
//    }
//
//    open fun moveItem(from: Int, to: Int) {
//        checkNotDeleted()
//        check(is_editable == true)
//        data.items!!.add(to, data.items!!.removeAt(from))
//    }
//
//    abstract suspend fun deletePlaylist(): Result<Unit>
//    abstract suspend fun saveItems(): Result<Unit>
//
//    @Composable
//    override fun getThumbnailHolder(): MediaItem {
//        checkNotDeleted()
//        var item: MediaItem by remember { mutableStateOf(this) }
//        LaunchedEffect(playlist_reg_entry.image_item_uid) {
//            val uid = playlist_reg_entry.image_item_uid
//            item = if (uid != null) fromUid(uid)
//            else this@Playlist
//        }
//        return item
//    }
//
//    @Composable
//    override fun PreviewSquare(params: MediaItemPreviewParams) {
//        checkNotDeleted()
//        PlaylistPreviewSquare(this, params)
//    }
//
//    @Composable
//    override fun PreviewLong(params: MediaItemPreviewParams) {
//        checkNotDeleted()
//        PlaylistPreviewLong(this, params)
//    }
//
//    interface Listener {
//        fun onReplaced(with: Playlist)
//        fun onDeleted()
//    }
//    private val listeners: MutableList<Listener> = mutableListOf()
//    fun addListener(listener: Listener) {
//        checkNotDeleted()
//        listeners.addUnique(listener)
//    }
//    fun removeListener(listener: Listener) {
//        listeners.remove(listener)
//    }
//
//    override fun getDefaultRegistryEntry(): PlaylistDataRegistryEntry = PlaylistDataRegistryEntry()
//
//    protected fun onDeleted() {
//        checkNotDeleted()
//
//        if (pinned_to_home) {
//            setPinnedToHome(false)
//        }
//
//        is_deleted = true
//        listeners.forEach { it.onDeleted() }
//        listeners.clear()
//    }
//
//    protected fun onReplaced(with: Playlist) {
//        checkNotDeleted()
//
//        if (pinned_to_home) {
//            var found = false
//            val new_pinned = Settings.INTERNAL_PINNED_ITEMS.get<Set<String>>().map {
//                if (it == uid) {
//                    found = true
//                    with.uid
//                }
//                else it
//            }
//
//            check(found)
//            Settings.INTERNAL_PINNED_ITEMS.set(new_pinned.toSet())
//        }
//
//        is_deleted = true
//
//        for (listener in listeners) {
//            listener.onReplaced(with)
//        }
//        listeners.clear()
//    }
//}
