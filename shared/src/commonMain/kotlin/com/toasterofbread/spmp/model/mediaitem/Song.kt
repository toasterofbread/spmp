package com.toasterofbread.spmp.model.mediaitem

import androidx.compose.ui.unit.IntOffset
import com.toasterofbread.Database
import com.toasterofbread.spmp.api.lyrics.LyricsReference
import com.toasterofbread.spmp.api.lyrics.toLyricsReference
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.enums.SongType

class SongRef(override val id: String): Song

interface Song: MediaItem.WithArtist {
    override fun getType(): MediaItemType = MediaItemType.SONG
    override fun getURL(): String = "https://music.youtube.com/watch?v=$id"

    override fun getEmptyData(): SongData = SongData(id)
    override fun populateData(data: MediaItemData, db: Database) {
        super.populateData(data, db)
        (data as SongData).apply {
            song_type = TypeOfSong.get(db)
            duration = Duration.get(db)
            album = Album.get(db)
            related_browse_id = RelatedBrowseId.get(db)
        }
    }

    override suspend fun loadData(db: Database): Result<SongData> {
        return super.loadData(db) as Result<SongData>
    }

    val TypeOfSong: Property<SongType?> get() = SingleProperty(
        { songQueries.songTypeById(id) },
        { song_type?.let { SongType.values()[it.toInt()] } },
        { songQueries.updateSongTypeById(it?.ordinal?.toLong(), id) }
    )
    val Duration: Property<Long?> get() = SingleProperty(
        { songQueries.durationById(id) }, { duration }, { songQueries.updateDurationById(it, id) }
    )
    override val Artist: Property<Artist?> get() = SingleProperty(
        { songQueries.artistById(id) }, { artist?.let { ArtistRef(it) } }, { songQueries.updateArtistById(it?.id, id) }
    )
    val Album: Property<Playlist?> get() = SingleProperty(
        { songQueries.albumById(id) }, { album?.let { AccountPlaylistRef(it) } }, { songQueries.updateAlbumById(it?.id, id) }
    )
    val RelatedBrowseId: Property<String?> get() = SingleProperty(
        { songQueries.relatedBrowseIdById(id) }, { related_browse_id }, { songQueries.updateRelatedBrowseIdById(it, id) }
    )

    val Lyrics: Property<LyricsReference?> get() = SingleProperty(
        { songQueries.lyricsById(id) }, { this.toLyricsReference() }, { songQueries.updateLyricsById(it?.source_idx?.toLong(), it?.id, id) }
    )
    val LyricsSyncOffset: Property<Long?> get() = SingleProperty(
        { songQueries.lyricsSyncOffsetById(id) }, { lyrics_sync_offset }, { songQueries.updateLyricsSyncOffsetById(it, id) }
    )
    val PlayerGradientDepth: Property<Float?> get() = SingleProperty(
        { songQueries.npGradientDepthById(id) }, { np_gradient_depth?.toFloat() }, { songQueries.updateNpGradientDepthById(it?.toDouble(), id) }
    )
    val ThumbnailRounding: Property<Int?> get() = SingleProperty(
        { songQueries.thumbnailRoundingById(id) }, { thumbnail_rounding?.toInt() }, { songQueries.updateThumbnailRoundingById(it?.toLong(), id) }
    )
    val NotificationImageOffset: Property<IntOffset?> get() = SingleProperty(
        { songQueries.notifImageOffsetById(id) },
        {
            if (notif_image_offset_x != null || notif_image_offset_y != null) IntOffset(
                notif_image_offset_x?.toInt() ?: 0,
                notif_image_offset_y?.toInt() ?: 0
            )
            else null
        },
        { songQueries.updateNotifImageOffsetById(it?.x?.toLong(), it?.y?.toLong(), id) }
    )
    val Liked: Property<SongLikedStatus?> get() = SingleProperty(
        { songQueries.likedById(id) }, { liked.toSongLikedStatus() }, { songQueries.updatelikedById(it.toLong(), id) }
    )
}

class SongData(
    override var id: String,

    var song_type: SongType? = null,
    var duration: Long? = null,
    override var artist: Artist? = null,
    var album: Playlist? = null,
    var related_browse_id: String? = null
): MediaItemData(), Song, MediaItem.DataWithArtist
