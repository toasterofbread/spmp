package com.toasterofbread.spmp.model.mediaitem.playlist

import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemSortType
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.model.mediaitem.song.SongData
import com.toasterofbread.spmp.platform.PlatformContext

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

    var custom_image_url: String? = null,
    var image_width: Float? = null,
    var sort_type: MediaItemSortType? = null,

    var item_set_ids: List<String>? = null
): MediaItem.DataWithArtist(), Playlist {
    abstract suspend fun savePlaylist(context: PlatformContext)

    override fun getDataValues(): Map<String, Any?> =
        super.getDataValues() + mapOf(
            "items" to items,
            "item_count" to item_count,
            "playlist_type" to playlist_type,
            "browse_params" to browse_params,
            "total_duration" to total_duration,
            "year" to year,
            "owner" to owner,
            "custom_image_provider" to custom_image_url,
            "image_width" to image_width,
            "sort_type" to sort_type,
            "item_set_ids" to item_set_ids
        )
}
