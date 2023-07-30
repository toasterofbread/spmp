package com.toasterofbread.spmp.model.mediaitem

import com.toasterofbread.Database
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MediaItemLayout
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import mediaitem.PlaylistItem

sealed interface PlaylistRef: Playlist

class AccountPlaylistRef(override val id: String): PlaylistRef {
    override fun getType(): MediaItemType = MediaItemType.PLAYLIST_ACC
    override fun getEmptyData(): PlaylistData = PlaylistData(id)
}
class LocalPlaylistRef(override val id: String): PlaylistRef {
    override fun getType(): MediaItemType = MediaItemType.PLAYLIST_LOC
    override fun getEmptyData(): PlaylistData = PlaylistData(id, playlist_type = PlaylistType.LOCAL)
}

sealed interface Playlist: MediaItem.WithArtist {
//    val is_editable: Boolean?
//    val item_set_ids: List<String>?
//    val continuation: MediaItemLayout.Continuation?

    override fun getHolder(): PlaylistHolder = PlaylistHolder(this)
    override fun getURL(): String = "https://music.youtube.com/playlist?list=$id"
    override fun getEmptyData(): PlaylistData

    override fun populateData(data: MediaItemData, db: Database) {
        super.populateData(data, db)
        (data as PlaylistData).apply {
            items = Items.get(db)
            item_count = ItemCount.get(db)
            playlist_type = TypeOfPlaylist.get(db)
            browse_params = BrowseParams.get(db)
            total_duration = TotalDuration.get(db)
            year = Year.get(db)
        }
    }

    override suspend fun loadData(db: Database): Result<PlaylistData> {
        return super.loadData(db) as Result<PlaylistData>
    }

    val Items get() = ListProperty<Song, PlaylistItem>(
        getQuery = { playlistItemQueries.byPlaylistId(id) },
        getValue = { this.map { SongRef(it.song_id) } },
        getSize = { playlistItemQueries.itemCount(id).executeAsOne() },
        addItem = { item, index ->
            playlistItemQueries.insertItemAtIndex(id, item.id, index)
        },
        removeItem = { index ->
            playlistItemQueries.removeItemAtIndex(id, index)
        },
        setItemIndex = { from, to ->
            playlistItemQueries.updateItemIndex(from = from, to = to, playlist_id = id)
        },
        clearItems = { from_index ->
            playlistItemQueries.clearItems(id, from_index)
        }
    )
    val ItemCount: Property<Int?> get() = SingleProperty(
        { playlistQueries.itemCountById(id) }, { item_count?.toInt() }, { playlistQueries.updateItemCountById(it?.toLong(), id) }
    )
    val TypeOfPlaylist: Property<PlaylistType?> get() = SingleProperty(
        { playlistQueries.playlistTypeById(id) },
        { playlist_type?.let { PlaylistType.values()[it.toInt()] } },
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

    var items: List<Song>? = null,
    var item_count: Int? = null,
    var playlist_type: PlaylistType? = null,
    var browse_params: String? = null,
    var total_duration: Long? = null,
    var year: Int? = null,

    var continuation: MediaItemLayout.Continuation? = null,
    var item_set_ids: List<String>? = null
): MediaItemData(), Playlist, MediaItem.DataWithArtist {
    fun isLocalPlaylist(): Boolean = playlist_type == PlaylistType.LOCAL
    override fun getType(): MediaItemType = if (isLocalPlaylist()) MediaItemType.PLAYLIST_LOC else MediaItemType.PLAYLIST_ACC

    override fun getEmptyData(): PlaylistData =
        PlaylistData(id, playlist_type = if (isLocalPlaylist()) PlaylistType.LOCAL else null)

    override fun saveToDatabase(db: Database, apply_to_item: MediaItem) {
        db.transaction { with(apply_to_item as Playlist) {
            super.saveToDatabase(db, apply_to_item)

            Items.overwriteItems(items ?: emptyList(), db)
            ItemCount.set(item_count, db)
            TypeOfPlaylist.set(playlist_type, db)
            BrowseParams.set(browse_params, db)
            TotalDuration.set(total_duration, db)
            Year.set(year, db)
        }}
    }
}
