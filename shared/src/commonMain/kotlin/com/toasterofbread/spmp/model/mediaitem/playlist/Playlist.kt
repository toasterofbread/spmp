package com.toasterofbread.spmp.model.mediaitem.playlist

import androidx.compose.ui.graphics.Color
import com.toasterofbread.Database
import com.toasterofbread.spmp.model.mediaitem.Artist
import com.toasterofbread.spmp.model.mediaitem.ArtistRef
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProviderImpl
import com.toasterofbread.spmp.model.mediaitem.PropertyRememberer
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.model.mediaitem.SongData
import com.toasterofbread.spmp.model.mediaitem.SongRef
import com.toasterofbread.spmp.model.mediaitem.UnsupportedPropertyRememberer
import com.toasterofbread.spmp.model.mediaitem.db.ListProperty
import com.toasterofbread.spmp.model.mediaitem.db.Property
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.model.mediaitem.library.MediaItemLibrary
import com.toasterofbread.spmp.model.mediaitem.toThumbnailProvider
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.utils.lazyAssert

class RemotePlaylistRef(override val id: String): RemotePlaylist {
    override fun toString(): String = "RemotePlaylistRef($id)"

    override fun getEmptyData(): RemotePlaylistData = RemotePlaylistData(id)
    override fun createDbEntry(db: Database) {
        db.playlistQueries.insertById(id, null)
    }

    override val property_rememberer: PropertyRememberer = PropertyRememberer()
    init {
        lazyAssert { id.isNotBlank() }
    }
}

class LocalPlaylistRef(override val id: String): LocalPlaylist {
    override fun toString(): String = "LocalPlaylistRef($id)"

    override fun createDbEntry(db: Database) {
        throw IllegalStateException(id)
    }

    override suspend fun loadData(context: PlatformContext, populate_data: Boolean): Result<LocalPlaylistData> {
        return runCatching {
            val file = MediaItemLibrary.getLocalPlaylistFile(this, context)
            PlaylistFileConverter.loadFromFile(file, context)
        }
    }

    override val property_rememberer: PropertyRememberer =
        UnsupportedPropertyRememberer { is_read ->
            if (is_read) "Local playlist must be loaded from file into a LocalPlaylistData."
            else "Use a PlaylistEditor instead."
        }

    init {
        lazyAssert { id.isNotBlank() }
    }
}

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
}

sealed interface LocalPlaylist: Playlist {
    override fun getType(): MediaItemType = MediaItemType.PLAYLIST_LOC
    override fun getURL(context: PlatformContext): String =
        "file://" + MediaItemLibrary.getLocalPlaylistFile(this, context).absolute_path
    override fun getEmptyData(): LocalPlaylistData = LocalPlaylistData(id)

    override suspend fun loadData(context: PlatformContext, populate_data: Boolean): Result<LocalPlaylistData>
}

abstract class PlaylistData(
    override var id: String,
    override var artist: Artist? = null,

    var items: List<SongData>? = null,
    var item_count: Int? = null,
    var playlist_type: PlaylistType? = null,
    var browse_params: String? = null,
    var total_duration: Long? = null,
    var year: Int? = null,
    var owner: Artist? = null,

    var custom_image_provider: MediaItemThumbnailProvider? = null,
    var image_width: Float? = null,

    var item_set_ids: List<String>? = null
): MediaItem.DataWithArtist(), Playlist

class LocalPlaylistData(id: String): PlaylistData(id), LocalPlaylist {
    var play_count: Int = 0

    override fun toString(): String = "LocalPlaylistData($id)"

    override val property_rememberer: PropertyRememberer =
        UnsupportedPropertyRememberer(can_read = true) { is_read ->
            "Use a PlaylistEditor instead."
        }

    override fun createDbEntry(db: Database) {
        throw UnsupportedOperationException()
    }
    override fun saveToDatabase(db: Database, apply_to_item: MediaItem) {
        throw UnsupportedOperationException()
    }
    override suspend fun loadData(context: PlatformContext, populate_data: Boolean): Result<LocalPlaylistData> {
        return Result.success(this)
    }

    override val Loaded: Property<Boolean>
        get() = property_rememberer.rememberLocalSingleProperty(
            "Loaded", { loaded }, { loaded = it }
        )
    override val Title: Property<String?>
        get() = property_rememberer.rememberLocalSingleProperty(
            "Title", { title }, { title = it }
        )
    override val OriginalTitle: Property<String?>
        get() = property_rememberer.rememberLocalSingleProperty(
            "OriginalTitle", { original_title }, { original_title = it }
        )
    override val Description: Property<String?>
        get() = property_rememberer.rememberLocalSingleProperty(
            "Description", { description }, { description = it }
        )
    override val ThumbnailProvider: Property<MediaItemThumbnailProvider?>
        get() = property_rememberer.rememberLocalSingleProperty(
            "ThumbnailProvider", { thumbnail_provider }, { thumbnail_provider = it }
        )

    override val ThemeColour: Property<Color?>
        get() = property_rememberer.rememberLocalSingleProperty(
            "ThemeColour", { theme_colour }, { theme_colour = it }
        )
    override val Hidden: Property<Boolean>
        get() = property_rememberer.rememberLocalSingleProperty(
            "Hidden", { hidden }, { hidden = it }
        )

    override val Items: ListProperty<Song>
        get() = property_rememberer.rememberLocalListProperty(
            "Items", { items ?: emptyList() }
        )
    override val ItemCount: Property<Int?>
        get() = property_rememberer.rememberLocalSingleProperty(
            "ItemCount", { item_count }, { item_count = it }
        )
    override val TypeOfPlaylist: Property<PlaylistType?>
        get() = property_rememberer.rememberLocalSingleProperty(
            "TypeOfPlaylist", { playlist_type }, { playlist_type = it }
        )
    override val TotalDuration: Property<Long?>
        get() = property_rememberer.rememberLocalSingleProperty(
            "TotalDuration", { total_duration }, { total_duration = it }
        )
    override val Year: Property<Int?>
        get() = property_rememberer.rememberLocalSingleProperty(
            "Year", { year }, { year = it }
        )
    override val Artist: Property<Artist?>
        get() = property_rememberer.rememberLocalSingleProperty(
            "Artist", { artist }, { artist = it }
        )
    override val Owner: Property<Artist?>
        get() = property_rememberer.rememberLocalSingleProperty(
            "Owner", { owner }, { owner = it }
        )

    override val CustomImageProvider: Property<MediaItemThumbnailProvider?>
        get() = property_rememberer.rememberLocalSingleProperty(
            "CustomImageProvider", { custom_image_provider }, { custom_image_provider = it }
        )
    override val ImageWidth: Property<Float?>
        get() = property_rememberer.rememberLocalSingleProperty(
            "ImageWidth", { image_width }, { image_width = it }
        )
}
