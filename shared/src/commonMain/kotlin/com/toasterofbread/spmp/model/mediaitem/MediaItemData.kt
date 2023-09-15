package com.toasterofbread.spmp.model.mediaitem

import androidx.compose.ui.graphics.Color
import com.toasterofbread.Database
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistData
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistData
import com.toasterofbread.spmp.model.mediaitem.song.SongData
import com.toasterofbread.spmp.youtubeapi.model.TextRun

abstract class MediaItemData: MediaItem {
    var loaded: Boolean = false
    var title: String? = null
    var custom_title: String? = null
    var description: String? = null
    var thumbnail_provider: MediaItemThumbnailProvider? = null

    var theme_colour: Color? = null
    var hidden: Boolean = false

    open fun getDataValues(): Map<String, Any?> =
        mapOf(
            "id" to id,
            "loaded" to loaded,
            "title" to title,
            "custom_title" to custom_title,
            "description" to description,
            "thumbnail_provider" to thumbnail_provider,
            "theme_colour" to theme_colour,
            "hidden" to hidden
        )

    open fun setDataActiveTitle(value: String) {
        custom_title = value
    }

    open fun saveToDatabase(db: Database, apply_to_item: MediaItem = this, uncertain: Boolean = false) {
        db.transaction { with(apply_to_item) {
            createDbEntry(db)

            if (loaded) {
                Loaded.set(true, db)
            }
            Title.setNotNull(title, db, uncertain)
            CustomTitle.setNotNull(custom_title, db, uncertain)
            Description.setNotNull(description, db, uncertain)
            ThumbnailProvider.setNotNull(thumbnail_provider, db, uncertain)
        }}
    }

    open fun supplyDataFromSubtitle(runs: List<TextRun>) {
        var artist_found = false
        for (run in runs) {
            val type = run.browse_endpoint_type ?: continue
            when (MediaItemType.fromBrowseEndpointType(type)) {
                MediaItemType.ARTIST -> {
                    if (this is MediaItem.DataWithArtist) {
                        val item = run.navigationEndpoint?.browseEndpoint?.getMediaItem()
                        if (item is Artist) {
                            artist = item
                        }
                    }
                    artist_found = true
                }
                MediaItemType.PLAYLIST_REM -> {
                    if (this is SongData) {
                        val playlist = run.navigationEndpoint?.browseEndpoint?.getMediaItem()
                        if (playlist is RemotePlaylistData) {
                            assert(playlist.playlist_type == PlaylistType.ALBUM, { "$playlist (${playlist.playlist_type}) | ${run.navigationEndpoint}" })
                            playlist.title = run.text
                            album = playlist
                        }
                    }
                }
                else -> {}
            }
        }

        if (!artist_found && this is MediaItem.DataWithArtist) {
            artist = ArtistData(com.toasterofbread.spmp.model.mediaitem.artist.Artist.getForItemId(this)).also {
                it.title = runs.getOrNull(1)?.text
            }
        }
    }

    companion object {
        fun fromBrowseEndpointType(page_type: String, id: String): MediaItemData {
            val data = MediaItemType.fromBrowseEndpointType(page_type).referenceFromId(id).getEmptyData()
            if (data is RemotePlaylistData) {
                data.playlist_type = PlaylistType.fromBrowseEndpointType(page_type)
            }
            return data
        }
    }
}
