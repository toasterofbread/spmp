package com.toasterofbread.spmp.model.mediaitem.playlist

import com.toasterofbread.Database
import com.toasterofbread.spmp.model.mediaitem.Artist
import com.toasterofbread.spmp.model.mediaitem.ArtistRef
import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProviderImpl
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.model.mediaitem.SongData
import com.toasterofbread.spmp.model.mediaitem.SongRef
import com.toasterofbread.spmp.model.mediaitem.db.ListPropertyImpl
import com.toasterofbread.spmp.model.mediaitem.db.Property
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.model.mediaitem.toThumbnailProvider
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MediaItemLayout
import mediaitem.PlaylistItem

sealed interface RemotePlaylist: Playlist {
//    val is_editable: Boolean?
//    val item_set_ids: List<String>?
//    val continuation: MediaItemLayout.Continuation?

    val Continuation: Property<MediaItemLayout.Continuation?>
        get() = property_rememberer.rememberSingleQueryProperty(
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

    override fun getType(): MediaItemType = MediaItemType.PLAYLIST_REM

    override fun getHolder(): PlaylistHolder = PlaylistHolder(this)
    override fun getURL(context: PlatformContext): String = "https://music.youtube.com/playlist?list=$id"
    override fun getEmptyData(): RemotePlaylistData

    override fun populateData(data: MediaItemData, db: Database) {
        super.populateData(data, db)
        (data as RemotePlaylistData).apply {
            items = Items.get(db)?.map {
                SongData(it.id)
            }
            item_count = ItemCount.get(db)
            playlist_type = TypeOfPlaylist.get(db)
            total_duration = TotalDuration.get(db)
            year = Year.get(db)
            owner = Owner.get(db)
            continuation = Continuation.get(db)
        }
    }

    override suspend fun loadData(context: PlatformContext, populate_data: Boolean): Result<RemotePlaylistData> {
        return super.loadData(context, populate_data) as Result<RemotePlaylistData>
    }

    companion object {
        fun formatYoutubeId(id: String): String = id.removePrefix("VL")
    }
}
