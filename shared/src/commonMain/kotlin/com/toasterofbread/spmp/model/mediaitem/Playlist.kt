package com.toasterofbread.spmp.model.mediaitem

import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MediaItemLayout
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType as PlaylistTypeEnum

class AccountPlaylistRef(override val id: String): Playlist {
    override fun getType(): MediaItemType = MediaItemType.PLAYLIST_ACC
}
class LocalPlaylistRef(override val id: String): Playlist {
    override fun getType(): MediaItemType = MediaItemType.PLAYLIST_LOC
}

interface Playlist: MediaItem, MediaItem.WithArtist {
//    val is_editable: Boolean?
//    val item_set_ids: List<String>?
//    val continuation: MediaItemLayout.Continuation?
    override fun getURL(): String = "https://music.youtube.com/playlist?list=$id"

    val Items get() = ListProperty(
        getQuery = { playlistItemQueries.byPlaylistId(id) },
        getValue = { this.map { SongRef(it.song_id) } },
        getSize = { playlistItemQueries.itemCount(id).executeAsOne().toInt() },
        addItem = { item, index ->
            playlistItemQueries.insertItemAtIndex(id, item.id, index.toLong())
        },
        removeItem = { index ->
            playlistItemQueries.removeItemAtIndex(id, index.toLong())
        },
        setItemIndex = { from, to ->
            playlistItemQueries.updateItemIndex(from = from.toLong(), to = to.toLong(), playlist_id = id)
        }
    )
    val ItemCount: Property<Int?> get() = SingleProperty(
        { playlistQueries.itemCountById(id) }, { item_count?.toInt() }, { playlistQueries.updateItemCountById(it?.toLong(), id) }
    )
    val PlaylistType: Property<PlaylistTypeEnum?> get() = SingleProperty(
        { playlistQueries.playlistTypeById(id) },
        { playlist_type?.let { PlaylistTypeEnum.values()[it.toInt()] } },
        { playlistQueries.updatePlaylistTypeById(it?.ordinal?.toLong(), id) }
    )
    val BrowseParams: Property<String?> get() = SingleProperty(
        { playlistQueries.browseParamsById(id) }, { browse_params }, { playlistQueries.updateBrowseParamsById(it, id) }
    )
    val TotalDuration: Property<Long?> get() = SingleProperty(
        { playlistQueries.totalDurationById(id) }, { total_duration }, { playlistQueries.updateTotalDurationById(it, id) }
    )
    val Year: Property<Int?> get() = SingleProperty(
        { playlistQueries.yearById(id) }, { year?.toInt() }, { playlistQueries.updateYearById(it?.toLong(), id) }
    )
    override val Artist: Property<Artist?> get() = SingleProperty(
        { playlistQueries.artistById(id) }, { artist?.let { ArtistRef(it) } }, { playlistQueries.updateArtistById(it?.id, id) }
    )

    val CustomImageProvider: Property<MediaItemThumbnailProvider?> get() = SingleProperty(
        { playlistQueries.customImageProviderById(id) }, { this.toThumbnailProvider() }, { playlistQueries.updateCustomImageProviderById(it?.url_a, it?.url_b, id) }
    )
    val ImageWidth: Property<Float?> get() = SingleProperty(
        { playlistQueries.imageWidthById(id) }, { image_width?.toFloat() }, { playlistQueries.updateImageWidthById(it?.toDouble(), id) }
    )

    companion object {
        fun formatYoutubeId(id: String): String = id.removePrefix("VL")
    }
}

class PlaylistData(
    override var id: String,
    override var artist: Artist? = null,

    var items: List<MediaItem>? = null,
    var item_count: Int? = null,
    var playlist_type: PlaylistTypeEnum? = null,
    var browse_params: String? = null,
    var total_duration: Long? = null,
    var year: Int? = null,

    var custom_image_provider: MediaItemThumbnailProvider? = null,
    var image_width: Float? = null,

    var is_editable: Boolean? = null,
    var continuation: MediaItemLayout.Continuation? = null,
    var item_set_ids: List<String>? = null
): MediaItemData(), Playlist, MediaItem.DataWithArtist {
    override fun getType(): MediaItemType = if (playlist_type == PlaylistTypeEnum.LOCAL) MediaItemType.PLAYLIST_LOC else MediaItemType.PLAYLIST_ACC
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
