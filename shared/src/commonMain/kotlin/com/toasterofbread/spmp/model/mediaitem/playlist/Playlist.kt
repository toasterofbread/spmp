package com.toasterofbread.spmp.model.mediaitem.playlist

import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistRef
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.MediaItemSortType
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProviderImpl
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongRef
import com.toasterofbread.spmp.model.mediaitem.db.ListProperty
import com.toasterofbread.spmp.model.mediaitem.db.Property
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.model.mediaitem.loader.MediaItemLoader
import com.toasterofbread.spmp.model.mediaitem.toThumbnailProvider
import com.toasterofbread.spmp.platform.PlatformContext

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
            { playlist_type?.let { PlaylistType.values()[it.toInt()] } },
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
    override val Artist: Property<Artist?>
        get() = property_rememberer.rememberSingleQueryProperty(
            "Artist", { playlistQueries.artistById(id) }, { artist?.let { ArtistRef(it) } }, { playlistQueries.updateArtistById(it?.id, id) }
        )
    val Owner: Property<Artist?>
        get() = property_rememberer.rememberSingleQueryProperty(
            "Owner", { playlistQueries.ownerById(id) }, { owner?.let { ArtistRef(it) } }, { playlistQueries.updateOwnerById(it?.id, id) }
        )

    val CustomImageProvider: Property<MediaItemThumbnailProvider?>
        get() = property_rememberer.rememberSingleQueryProperty(
            "CustomImageProvider",
            { playlistQueries.customImageProviderById(id) },
            { this.toThumbnailProvider() },
            {
                require(it is MediaItemThumbnailProviderImpl?)
                playlistQueries.updateCustomImageProviderById(it?.url_a, it?.url_b, id)
            }
        )
    val ImageWidth: Property<Float?>
        get() = property_rememberer.rememberSingleQueryProperty(
            "ImageWidth", { playlistQueries.imageWidthById(id) }, { image_width?.toFloat() }, { playlistQueries.updateImageWidthById(it?.toDouble(), id) }
        )
    val SortType: Property<MediaItemSortType?>
        get() = property_rememberer.rememberSingleQueryProperty(
            "SortType", { playlistQueries.sortTypeById(id) }, { sort_type?.let { MediaItemSortType.values()[it.toInt()] } }, { playlistQueries.updateSortTypeById(it?.ordinal?.toLong(), id) }
        )

    suspend fun setSortType(sort_type: MediaItemSortType?, context: PlatformContext): Result<Unit>

    override suspend fun loadData(context: PlatformContext, populate_data: Boolean, force: Boolean): Result<PlaylistData> {
        return super.loadData(context, populate_data, force) as Result<PlaylistData>
    }
}
