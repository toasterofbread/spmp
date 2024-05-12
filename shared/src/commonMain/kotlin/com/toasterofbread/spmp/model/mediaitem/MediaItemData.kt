package com.toasterofbread.spmp.model.mediaitem

import androidx.compose.ui.graphics.Color
import com.toasterofbread.spmp.db.Database
import com.toasterofbread.spmp.model.mediaitem.artist.toArtistData
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistData
import com.toasterofbread.spmp.model.mediaitem.playlist.toRemotePlaylistData
import com.toasterofbread.spmp.model.mediaitem.song.toSongData
import dev.toastbits.ytmkt.model.external.ThumbnailProvider
import dev.toastbits.ytmkt.model.external.mediaitem.YtmArtist
import dev.toastbits.ytmkt.model.external.mediaitem.YtmMediaItem
import dev.toastbits.ytmkt.model.external.mediaitem.YtmPlaylist
import dev.toastbits.ytmkt.model.external.mediaitem.YtmSong

abstract class MediaItemData: MediaItem, YtmMediaItem {
    var loaded: Boolean = false
    override var name: String? = null
    var custom_name: String? = null
    override var description: String? = null
    override var thumbnail_provider: ThumbnailProvider? = null

    var theme_colour: Color? = null
    var hidden: Boolean = false

    open fun getDataValues(): Map<String, Any?> =
        mapOf(
            "id" to id,
            "loaded" to loaded,
            "title" to name,
            "custom_title" to custom_name,
            "description" to description,
            "thumbnail_provider" to thumbnail_provider,
            "theme_colour" to theme_colour,
            "hidden" to hidden
        )

    open fun setDataActiveTitle(value: String) {
        custom_name = value
    }

    open fun saveToDatabase(db: Database, apply_to_item: MediaItem = this, uncertain: Boolean = false, subitems_uncertain: Boolean = uncertain) {
        db.transaction { with(apply_to_item) {
            createDbEntry(db)

            if (loaded) {
                Loaded.set(true, db)
            }

            Title.setNotNull(name, db, uncertain)
            CustomTitle.setNotNull(custom_name, db, uncertain)
            Description.setNotNull(description, db, uncertain)
            ThumbnailProvider.setNotNull(thumbnail_provider, db, uncertain)
        }}
    }

    companion object {
        fun fromBrowseEndpointType(page_type: String, id: String): MediaItemData {
            val data = MediaItemType.fromBrowseEndpointType(page_type).referenceFromId(id).getEmptyData()
            if (data is RemotePlaylistData) {
                data.playlist_type = PlaylistType.fromYtmPlaylistType(
                    YtmPlaylist.Type.fromBrowseEndpointType(page_type)
                )
            }
            return data
        }
    }
}

fun YtmMediaItem.toMediaItemData(): MediaItemData =
    when (this) {
        is MediaItemData -> this
        is MediaItem -> getEmptyData()
        is YtmSong -> toSongData()
        is YtmPlaylist -> toRemotePlaylistData()
        is YtmArtist -> toArtistData()
        else -> throw NotImplementedError(this::class.toString())
    }
