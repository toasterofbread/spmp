package com.toasterofbread.spmp.model.mediaitem

import com.toasterofbread.Database
import com.toasterofbread.spmp.model.mediaitem.db.ListProperty
import com.toasterofbread.spmp.model.mediaitem.db.Property
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.model.mediaitem.playlist.PlaylistHolder
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MediaItemLayout
import com.toasterofbread.utils.lazyAssert
import mediaitem.PlaylistItem

sealed interface PlaylistRef: Playlist

class AccountPlaylistRef(override val id: String): PlaylistRef {
    override fun isLocalPlaylist(): Boolean = false
    override fun toString(): String = "AccountPlaylistRef($id)"
    override fun getType(): MediaItemType = MediaItemType.PLAYLIST_ACC

    override fun getEmptyData(): PlaylistData = PlaylistData(id)
    override fun createDbEntry(db: Database) {
        db.playlistQueries.insertById(id, null)
    }

    override val property_rememberer: PropertyRememberer = PropertyRememberer()
    init {
        lazyAssert { id.isNotBlank() }
    }
}
class LocalPlaylistRef(override val id: String): PlaylistRef {
    override fun isLocalPlaylist(): Boolean = true

    override fun toString(): String = "LocalPlaylistRef($id)"
    override fun getType(): MediaItemType = MediaItemType.PLAYLIST_LOC

    override fun getEmptyData(): PlaylistData = PlaylistData(id, playlist_type = PlaylistType.LOCAL)
    override fun createDbEntry(db: Database) {
        db.playlistQueries.insertById(id, PlaylistType.LOCAL.ordinal.toLong())
    }

    override val property_rememberer: PropertyRememberer = PropertyRememberer()
    init {
        lazyAssert { id.isNotBlank() }
    }
}

sealed interface Playlist: MediaItem.WithArtist {
//    val is_editable: Boolean?
//    val item_set_ids: List<String>?
//    val continuation: MediaItemLayout.Continuation?

    fun isLocalPlaylist(): Boolean

    override fun getHolder(): PlaylistHolder = PlaylistHolder(this)
    override fun getURL(): String = "https://music.youtube.com/playlist?list=$id"
    override fun getEmptyData(): PlaylistData

    override fun populateData(data: MediaItemData, db: Database) {
        super.populateData(data, db)
        (data as PlaylistData).apply {
            items = Items.get(db)?.map {
                SongData(it.id)
            }
            item_count = ItemCount.get(db)
            playlist_type = TypeOfPlaylist.get(db)
            browse_params = BrowseParams.get(db)
            total_duration = TotalDuration.get(db)
            year = Year.get(db)
            continuation = Continuation.get(db)
        }
    }

    override suspend fun loadData(db: Database, populate_data: Boolean): Result<PlaylistData> {
        return super.loadData(db, populate_data) as Result<PlaylistData>
    }

    val Items: ListProperty<Song, PlaylistItem>
        get() = property_rememberer.rememberListProperty(
        "Items",
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
    val ItemCount: Property<Int?>
        get() = property_rememberer.rememberSingleProperty(
        "ItemCount", { playlistQueries.itemCountById(id) }, { item_count?.toInt() }, { playlistQueries.updateItemCountById(it?.toLong(), id) }
    )
    val TypeOfPlaylist: Property<PlaylistType?>
        get() = property_rememberer.rememberSingleProperty(
        "TypeOfPlaylist",
        { playlistQueries.playlistTypeById(id) },
        { playlist_type?.let { PlaylistType.values()[it.toInt()] } },
        { playlistQueries.updatePlaylistTypeById(it?.ordinal?.toLong(), id) }
    )
    val BrowseParams: Property<String?>
        get() = property_rememberer.rememberSingleProperty(
        "BrowseParams", { playlistQueries.browseParamsById(id) }, { browse_params }, { playlistQueries.updateBrowseParamsById(it, id) }
    )
    val TotalDuration: Property<Long?>
        get() = property_rememberer.rememberSingleProperty(
        "TotalDuration", { playlistQueries.totalDurationById(id) }, { total_duration }, { playlistQueries.updateTotalDurationById(it, id) }
    )
    val Year: Property<Int?>
        get() = property_rememberer.rememberSingleProperty(
        "Year", { playlistQueries.yearById(id) }, { year?.toInt() }, { playlistQueries.updateYearById(it?.toLong(), id) }
    )
    override val Artist: Property<Artist?>
        get() = property_rememberer.rememberSingleProperty(
        "Artist", { playlistQueries.artistById(id) }, { artist?.let { ArtistRef(it) } }, { playlistQueries.updateArtistById(it?.id, id) }
    )
    val Continuation: Property<MediaItemLayout.Continuation?>
        get() = property_rememberer.rememberSingleProperty(
        "Continuation",
        { playlistQueries.continuationById(id) },
        { continuation_token?.let {
            MediaItemLayout.Continuation(
                it,
                MediaItemLayout.Continuation.Type.values()[continuation_type!!.toInt()]
            )
        }},
        { playlistQueries.updateContinuationById(it?.token, it?.type?.ordinal?.toLong(), id) }
    )

    val CustomImageProvider: Property<MediaItemThumbnailProvider?>
        get() = property_rememberer.rememberSingleProperty(
        "CustomImageProvider",
        { playlistQueries.customImageProviderById(id) },
        { this.toThumbnailProvider() },
        {
            require(it is MediaItemThumbnailProviderImpl?)
            playlistQueries.updateCustomImageProviderById(it?.url_a, it?.url_b, id)
        }
    )
    val ImageWidth: Property<Float?>
        get() = property_rememberer.rememberSingleProperty(
        "ImageWidth", { playlistQueries.imageWidthById(id) }, { image_width?.toFloat() }, { playlistQueries.updateImageWidthById(it?.toDouble(), id) }
    )

    companion object {
        fun formatYoutubeId(id: String): String = id.removePrefix("VL")
    }
}

class PlaylistData(
    override var id: String,
    override var artist: Artist? = null,

    var items: List<SongData>? = null,
    var item_count: Int? = null,
    var playlist_type: PlaylistType? = null,
    var browse_params: String? = null,
    var total_duration: Long? = null,
    var year: Int? = null,
    var continuation: MediaItemLayout.Continuation? = null,

    var item_set_ids: List<String>? = null
): MediaItem.DataWithArtist(), Playlist {
    override fun toString(): String = "PlaylistData($id, type=$playlist_type)"

    override fun isLocalPlaylist(): Boolean = playlist_type == PlaylistType.LOCAL
    override fun getType(): MediaItemType = if (isLocalPlaylist()) MediaItemType.PLAYLIST_LOC else MediaItemType.PLAYLIST_ACC

    override fun createDbEntry(db: Database) {
        db.playlistQueries.insertById(id, playlist_type?.ordinal?.toLong())
    }
    override fun getEmptyData(): PlaylistData =
        PlaylistData(id, playlist_type = if (isLocalPlaylist()) PlaylistType.LOCAL else null)

    override fun saveToDatabase(db: Database, apply_to_item: MediaItem) {
        db.transaction { with(apply_to_item as Playlist) {
            super.saveToDatabase(db, apply_to_item)

            items?.also { items ->
                for (item in items) {
                    item.saveToDatabase(db)
                }
                Items.overwriteItems(items, db)
            }

            ItemCount.setNotNull(item_count, db)
            TypeOfPlaylist.setNotNull(playlist_type, db)
            BrowseParams.setNotNull(browse_params, db)
            TotalDuration.setNotNull(total_duration, db)
            Year.setNotNull(year, db)
            Continuation.setNotNull(continuation, db)
        }}
    }

    override val property_rememberer: PropertyRememberer = PropertyRememberer()
    init {
        lazyAssert { id.isNotBlank() }
    }
}
