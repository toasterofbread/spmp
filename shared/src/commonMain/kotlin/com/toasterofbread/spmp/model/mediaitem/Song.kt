package com.toasterofbread.spmp.model.mediaitem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntOffset
import com.toasterofbread.Database
import com.toasterofbread.spmp.api.DEFAULT_CONNECT_TIMEOUT
import com.toasterofbread.spmp.api.lyrics.LyricsReference
import com.toasterofbread.spmp.api.lyrics.toLyricsReference
import com.toasterofbread.spmp.model.mediaitem.db.Property
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.enums.SongType
import com.toasterofbread.spmp.model.mediaitem.song.SongLikedStatus
import com.toasterofbread.spmp.model.mediaitem.song.toLong
import com.toasterofbread.spmp.model.mediaitem.song.toSongLikedStatus
import com.toasterofbread.spmp.platform.crop
import com.toasterofbread.spmp.platform.toImageBitmap
import com.toasterofbread.utils.lazyAssert
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

class SongRef(override val id: String): Song {
    override val creation: Throwable = Exception()
    override fun toString(): String = "SongRef($id)"

    override val property_rememberer: PropertyRememberer = PropertyRememberer()

    init {
        lazyAssert { id.isNotBlank() }
    }
}

interface Song: MediaItem.WithArtist {
    override fun getType(): MediaItemType = MediaItemType.SONG
    override fun getURL(): String = "https://music.youtube.com/watch?v=$id"

    override fun createDbEntry(db: Database) {
        db.songQueries.insertById(id)
    }
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

    override suspend fun loadData(db: Database, populate_data: Boolean): Result<SongData> {
        return super.loadData(db, populate_data) as Result<SongData>
    }

    override suspend fun downloadThumbnailData(url: String): Result<ImageBitmap> = withContext(Dispatchers.IO) {
        return@withContext kotlin.runCatching {
            val connection = URL(url).openConnection()
            connection.connectTimeout = DEFAULT_CONNECT_TIMEOUT

            val stream = connection.getInputStream()
            val bytes = stream.readBytes()
            stream.close()

            val image = bytes.toImageBitmap()
            if (image.width == image.height) {
                return@runCatching image
            }

            // Crop image to 1:1
            val size = (image.width * (9f/16f)).toInt()
            return@runCatching image.crop((image.width - size) / 2, (image.height - size) / 2, size, size)
        }
    }

    val TypeOfSong: Property<SongType?>
        get() = property_rememberer.rememberSingleProperty(
        "TypeOfSong",
        { songQueries.songTypeById(id) },
        { song_type?.let { SongType.values()[it.toInt()] } },
        { songQueries.updateSongTypeById(it?.ordinal?.toLong(), id) }
    )
    val Duration: Property<Long?>
        get() = property_rememberer.rememberSingleProperty(
        "Duration", { songQueries.durationById(id) }, { duration }, { songQueries.updateDurationById(it, id) }
    )
    override val Artist: Property<Artist?>
        get() = property_rememberer.rememberSingleProperty(
        "Artist", { songQueries.artistById(id) }, { artist?.let { ArtistRef(it) } }, { songQueries.updateArtistById(it?.id, id) }
    )
    val Album: Property<Playlist?>
        get() = property_rememberer.rememberSingleProperty(
        "Album", { songQueries.albumById(id) }, { album?.let { AccountPlaylistRef(it) } }, { songQueries.updateAlbumById(it?.id, id) }
    )
    val RelatedBrowseId: Property<String?>
        get() = property_rememberer.rememberSingleProperty(
        "RelatedBrowseId", { songQueries.relatedBrowseIdById(id) }, { related_browse_id }, { songQueries.updateRelatedBrowseIdById(it, id) }
    )

    val Lyrics: Property<LyricsReference?>
        get() = property_rememberer.rememberSingleProperty(
        "Lyrics", { songQueries.lyricsById(id) }, { this.toLyricsReference() }, { songQueries.updateLyricsById(it?.source_idx?.toLong(), it?.id, id) }
    )
    val LyricsSyncOffset: Property<Long?>
        get() = property_rememberer.rememberSingleProperty(
        "LyricsSyncOffset", { songQueries.lyricsSyncOffsetById(id) }, { lyrics_sync_offset }, { songQueries.updateLyricsSyncOffsetById(it, id) }
    )
    val PlayerGradientDepth: Property<Float?>
        get() = property_rememberer.rememberSingleProperty(
        "PlayerGradientDepth", { songQueries.npGradientDepthById(id) }, { np_gradient_depth?.toFloat() }, { songQueries.updateNpGradientDepthById(it?.toDouble(), id) }
    )
    val ThumbnailRounding: Property<Int?>
        get() = property_rememberer.rememberSingleProperty(
        "ThumbnailRounding", { songQueries.thumbnailRoundingById(id) }, { thumbnail_rounding?.toInt() }, { songQueries.updateThumbnailRoundingById(it?.toLong(), id) }
    )
    val NotificationImageOffset: Property<IntOffset?>
        get() = property_rememberer.rememberSingleProperty(
        "NotificationImageOffset",
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
    val Liked: Property<SongLikedStatus?>
        get() = property_rememberer.rememberSingleProperty(
        "Liked", { songQueries.likedById(id) }, { liked.toSongLikedStatus() }, { songQueries.updatelikedById(it.toLong(), id) }
    )

    override val ThumbnailProvider: Property<MediaItemThumbnailProvider?>
        get() = object : Property<MediaItemThumbnailProvider?> {
            override fun get(db: Database): MediaItemThumbnailProvider =
                MediaItemThumbnailProvider { quality ->
                    when (quality) {
                        MediaItemThumbnailProvider.Quality.LOW -> "https://img.youtube.com/vi/$id/0.jpg"
                        MediaItemThumbnailProvider.Quality.HIGH -> "https://img.youtube.com/vi/$id/maxresdefault.jpg"
                    }
                }

            override fun set(value: MediaItemThumbnailProvider?, db: Database) {}

            @Composable
            override fun observe(db: Database): MutableState<MediaItemThumbnailProvider?> =
                mutableStateOf(get(db))
        }

    val creation: Throwable
}

class SongData(
    override var id: String,
    override var artist: Artist? = null,

    var song_type: SongType? = null,
    var duration: Long? = null,
    var album: Playlist? = null,
    var related_browse_id: String? = null
): MediaItem.DataWithArtist(), Song {
    override val creation: Throwable = Exception()
    override fun toString(): String = "SongData($id)"

    override fun saveToDatabase(db: Database, apply_to_item: MediaItem) {
        db.transaction { with(apply_to_item as Song) {
            super.saveToDatabase(db, apply_to_item)

            TypeOfSong.setNotNull(song_type, db)
            Duration.setNotNull(duration, db)
            Album.setNotNull(album, db)
            RelatedBrowseId.setNotNull(related_browse_id, db)
        }}
    }

    override val ThumbnailProvider: Property<MediaItemThumbnailProvider?>
        get() = super<MediaItem.DataWithArtist>.ThumbnailProvider

    override val property_rememberer: PropertyRememberer = PropertyRememberer()

    init {
        lazyAssert { id.isNotBlank() }
    }
}
