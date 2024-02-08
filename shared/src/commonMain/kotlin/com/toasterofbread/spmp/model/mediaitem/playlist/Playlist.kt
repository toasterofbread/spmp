package com.toasterofbread.spmp.model.mediaitem.playlist

import com.toasterofbread.db.Database
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.MediaItemSortType
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistRef
import com.toasterofbread.spmp.model.mediaitem.db.AltSetterProperty
import com.toasterofbread.spmp.model.mediaitem.db.ListProperty
import com.toasterofbread.spmp.model.mediaitem.db.Property
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongData
import com.toasterofbread.spmp.model.mediaitem.song.SongRef
import com.toasterofbread.spmp.platform.AppContext

sealed interface Playlist: MediaItem.WithArtist {
    override fun getEmptyData(): PlaylistData

    val Items: ListProperty<Song>
        get() = property_rememberer.rememberListQueryProperty(
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
            },
            prerequisite = Loaded
        )
    val ItemCount: Property<Int?>
        get() = property_rememberer.rememberSingleQueryProperty(
            "ItemCount", { playlistQueries.itemCountById(id) }, { item_count?.toInt() }, { playlistQueries.updateItemCountById(it?.toLong(), id) }
        )
    val TypeOfPlaylist: Property<PlaylistType?>
        get() = property_rememberer.rememberSingleQueryProperty(
            "TypeOfPlaylist",
            { playlistQueries.playlistTypeById(id) },
            { playlist_type?.let { PlaylistType.entries[it.toInt()] } },
            { playlistQueries.updatePlaylistTypeById(it?.ordinal?.toLong(), id) }
        )
    val TotalDuration: Property<Long?>
        get() = property_rememberer.rememberSingleQueryProperty(
            "TotalDuration", { playlistQueries.totalDurationById(id) }, { total_duration }, { playlistQueries.updateTotalDurationById(it, id) }
        )
    val Year: Property<Int?>
        get() = property_rememberer.rememberSingleQueryProperty(
            "Year", { playlistQueries.yearById(id) }, { year?.toInt() }, { playlistQueries.updateYearById(it?.toLong(), id) }
        )
    override val Artist: AltSetterProperty<ArtistRef?, Artist?>
        get() = property_rememberer.rememberAltSetterSingleQueryProperty(
            "Artist", { playlistQueries.artistById(id) }, { artist?.let { ArtistRef(it) } }, { playlistQueries.updateArtistById(it?.id, id) }, { playlistQueries.updateArtistById(it?.id, id) }
        )
    val Owner: Property<Artist?>
        get() = property_rememberer.rememberSingleQueryProperty(
            "Owner", { playlistQueries.ownerById(id) }, { owner?.let { ArtistRef(it) } }, { playlistQueries.updateOwnerById(it?.id, id) }
        )

    val CustomImageUrl: Property<String?>
        get() = property_rememberer.rememberSingleQueryProperty(
            "CustomImageUrl",
            { playlistQueries.customImageUrlById(id) },
            { custom_image_url },
            { playlistQueries.updateCustomImageUrlById(it, id) }
        )
    val ImageWidth: Property<Float?>
        get() = property_rememberer.rememberSingleQueryProperty(
            "ImageWidth", { playlistQueries.imageWidthById(id) }, { image_width?.toFloat() }, { playlistQueries.updateImageWidthById(it?.toDouble(), id) }
        )
    val SortType: Property<MediaItemSortType?>
        get() = property_rememberer.rememberSingleQueryProperty(
            "SortType", { playlistQueries.sortTypeById(id) }, { sort_type?.let { MediaItemSortType.entries[it.toInt()] } }, { playlistQueries.updateSortTypeById(it?.ordinal?.toLong(), id) }
        )

    override fun populateData(data: MediaItemData, db: Database) {
        require(data is PlaylistData)

        super.populateData(data, db)

        data.items = Items.get(db)?.map {
            SongData(it.id)
        }
        data.item_count = ItemCount.get(db)
        data.playlist_type = TypeOfPlaylist.get(db)
        data.total_duration = TotalDuration.get(db)
        data.year = Year.get(db)
        data.owner = Owner.get(db)

        data.custom_image_url = CustomImageUrl.get(db)
        data.image_width = ImageWidth.get(db)
        data.sort_type = SortType.get(db)
    }

    suspend fun setSortType(sort_type: MediaItemSortType?, context: AppContext): Result<Unit>

    override suspend fun loadData(context: AppContext, populate_data: Boolean, force: Boolean, save: Boolean): Result<PlaylistData> {
        return super.loadData(context, populate_data, force, save) as Result<PlaylistData>
    }
}
