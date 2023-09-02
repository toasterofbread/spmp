package com.toasterofbread.spmp.model.mediaitem.playlist

import androidx.compose.ui.graphics.Color
import com.toasterofbread.Database
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemSortType
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.PropertyRememberer
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.UnsupportedPropertyRememberer
import com.toasterofbread.spmp.model.mediaitem.db.ListProperty
import com.toasterofbread.spmp.model.mediaitem.db.Property
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.platform.PlatformContext

class LocalPlaylistData(id: String): PlaylistData(id), LocalPlaylist {
    var play_count: Int = 0

    override fun toString(): String = "LocalPlaylistData($id)"

    override val property_rememberer: PropertyRememberer =
        UnsupportedPropertyRememberer(can_read = true) {
            "Use a PlaylistEditor instead."
        }

    override fun createDbEntry(db: Database) {
        throw UnsupportedOperationException()
    }
    override fun saveToDatabase(db: Database, apply_to_item: MediaItem, uncertain: Boolean) {
        throw UnsupportedOperationException()
    }
    override suspend fun loadData(context: PlatformContext, populate_data: Boolean, force: Boolean): Result<LocalPlaylistData> {
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
    override val SortType: Property<MediaItemSortType?>
        get() = property_rememberer.rememberLocalSingleProperty(
            "SortType", { sort_type }, { sort_type = it }
        )
}
