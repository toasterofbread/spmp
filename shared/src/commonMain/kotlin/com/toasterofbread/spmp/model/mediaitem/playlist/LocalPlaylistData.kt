package com.toasterofbread.spmp.model.mediaitem.playlist

import LocalPlayerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.toasterofbread.spmp.db.Database
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemSortType
import dev.toastbits.ytmkt.model.external.ThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.PropertyRememberer
import com.toasterofbread.spmp.model.mediaitem.UnsupportedPropertyRememberer
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistRef
import com.toasterofbread.spmp.model.mediaitem.db.AltSetterProperty
import com.toasterofbread.spmp.model.mediaitem.db.ListProperty
import com.toasterofbread.spmp.model.mediaitem.db.Property
import com.toasterofbread.spmp.model.mediaitem.library.MediaItemLibrary
import com.toasterofbread.spmp.model.mediaitem.playlist.PlaylistFileConverter.saveToFile
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.platform.AppContext
import dev.toastbits.ytmkt.model.external.mediaitem.YtmPlaylist
import dev.toastbits.composekit.context.PlatformFile

class LocalPlaylistData(id: String): PlaylistData(id), LocalPlaylist {
    var play_count: Int = 0

    override fun toString(): String = "LocalPlaylistData($id)"
    override fun getReference(): LocalPlaylistRef = LocalPlaylistRef(id)

    override val property_rememberer: PropertyRememberer =
        UnsupportedPropertyRememberer(can_read = true) {
            "Use a PlaylistEditor instead."
        }

    override fun getActiveTitle(db: Database): String? {
        return name
    }

    @Composable
    override fun observeActiveTitle(): MutableState<String?> {
        val player = LocalPlayerState.current
        val state: MutableState<String?> = remember(this) { mutableStateOf(getActiveTitle(player.database)) }
        var launched: Boolean by remember(this) { mutableStateOf(false) }

        LaunchedEffect(this, state.value) {
            if (!launched) {
                launched = true
                return@LaunchedEffect
            }

            setDataActiveTitle(state.value ?: "")
            setActiveTitle(state.value, player.context)
        }
        return state
    }

    override fun setDataActiveTitle(value: String) {
        name = value
    }

    override fun createDbEntry(db: Database) {
        throw UnsupportedOperationException()
    }

    override suspend fun savePlaylist(context: AppContext) {
        val file: PlatformFile = MediaItemLibrary.getLocalPlaylistFile(this, context) ?: return
        saveToFile(file, context)
    }

    override fun saveToDatabase(db: Database, apply_to_item: MediaItem, uncertain: Boolean, subitems_uncertain: Boolean) {
        throw UnsupportedOperationException()
    }
    override suspend fun loadData(context: AppContext, populate_data: Boolean, force: Boolean, save: Boolean): Result<LocalPlaylistData> {
        return Result.success(this)
    }

    override val Loaded: Property<Boolean>
        get() = property_rememberer.rememberLocalSingleProperty(
            "Loaded", { loaded }, { loaded = it }
        )
    override val Title: Property<String?>
        get() = property_rememberer.rememberLocalSingleProperty(
            "Title", { name }, { name = it }
        )
    override val CustomTitle: Property<String?>
        get() = property_rememberer.rememberLocalSingleProperty(
            "CustomTitle", { custom_name }, { custom_name = it }
        )
    override val Description: Property<String?>
        get() = property_rememberer.rememberLocalSingleProperty(
            "Description", { description }, { description = it }
        )
    override val ThumbnailProvider: Property<ThumbnailProvider?>
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
    override val Artists: AltSetterProperty<List<ArtistRef>?, List<Artist>?>
        get() = property_rememberer.rememberAltSetterLocalSingleProperty(
            "Artists", { artists?.map { ArtistRef(it.id) } }, { artists = it }, { artists = it }
        )
    override val Owner: Property<Artist?>
        get() = property_rememberer.rememberLocalSingleProperty(
            "Owner", { owner }, { owner = it }
        )

    override val CustomImageUrl: Property<String?>
        get() = property_rememberer.rememberLocalSingleProperty(
            "CustomImageUrl", { custom_image_url }, { custom_image_url = it }
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
