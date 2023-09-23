package com.toasterofbread.spmp.model.mediaitem.song

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntOffset
import app.cash.sqldelight.Query
import com.toasterofbread.Database
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistRef
import com.toasterofbread.spmp.model.mediaitem.db.AltSetterProperty
import com.toasterofbread.spmp.model.mediaitem.db.Property
import com.toasterofbread.spmp.model.mediaitem.db.PropertyImpl
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.enums.SongType
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylist
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistRef
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.platform.crop
import com.toasterofbread.spmp.platform.toImageBitmap
import com.toasterofbread.spmp.youtubeapi.lyrics.LyricsReference
import com.toasterofbread.spmp.youtubeapi.lyrics.toLyricsReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mediaitem.AlbumById
import mediaitem.song.ArtistById
import java.net.URL

interface Song: MediaItem.WithArtist {
    override fun getType(): MediaItemType = MediaItemType.SONG
    override fun getURL(context: PlatformContext): String = "https://music.youtube.com/watch?v=$id"

    override fun createDbEntry(db: Database) {
        db.songQueries.insertById(id)
    }
    override fun getEmptyData(): SongData = SongData(id)
    override fun populateData(data: MediaItemData, db: Database) {
        require(data is SongData)

        super.populateData(data, db)

        data.song_type = TypeOfSong.get(db)
        data.duration = Duration.get(db)
        data.album = Album.get(db)
        data.related_browse_id = RelatedBrowseId.get(db)
        data.lyrics_browse_id = LyricsBrowseId.get(db)
    }

    override suspend fun loadData(context: PlatformContext, populate_data: Boolean, force: Boolean): Result<SongData> {
        return super.loadData(context, populate_data, force) as Result<SongData>
    }

    override suspend fun downloadThumbnailData(url: String): Result<ImageBitmap> = withContext(Dispatchers.IO) {
        return@withContext kotlin.runCatching {
            val connection = URL(url).openConnection()
            connection.connectTimeout = com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.DEFAULT_CONNECT_TIMEOUT

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
        get() = property_rememberer.rememberSingleQueryProperty(
        "TypeOfSong",
        { songQueries.songTypeById(id) },
        { song_type?.let { SongType.values()[it.toInt()] } },
        { songQueries.updateSongTypeById(it?.ordinal?.toLong(), id) }
    )
    val Duration: Property<Long?>
        get() = property_rememberer.rememberSingleQueryProperty(
        "Duration", { songQueries.durationById(id) }, { duration }, { songQueries.updateDurationById(it, id) }
    )

    override val Artist: AltSetterProperty<ArtistRef?, Artist?>
        get() = object : PropertyImpl<ArtistRef?, Query<ArtistById>>(
            { songQueries.artistById(id) }, { executeAsOne().artist?.let { ArtistRef(it) } }, { songQueries.updateArtistById(it?.id, id) }
        ), AltSetterProperty<ArtistRef?, Artist?> {
            override fun setAlt(value: Artist?, db: Database) {
                db.songQueries.updateArtistById(value?.id, id)
            }
        }

    val Album: Property<RemotePlaylist?>
        get() = property_rememberer.rememberSingleQueryProperty(
        "Album", { songQueries.albumById(id) }, { album?.let { RemotePlaylistRef(it) } }, { songQueries.updateAlbumById(it?.id, id) }
    )
    val RelatedBrowseId: Property<String?>
        get() = property_rememberer.rememberSingleQueryProperty(
        "RelatedBrowseId", { songQueries.relatedBrowseIdById(id) }, { related_browse_id }, { songQueries.updateRelatedBrowseIdById(it, id) }
    )
    val LyricsBrowseId: Property<String?>
        get() = property_rememberer.rememberSingleQueryProperty(
        "LyricsBrowseId", { songQueries.lyricsBrowseIdById(id) }, { lyrics_browse_id }, { songQueries.updateLyricsBrowseIdById(it, id) }
    )

    val Lyrics: Property<LyricsReference?>
        get() = property_rememberer.rememberSingleQueryProperty(
        "Lyrics", { songQueries.lyricsById(id) }, { this.toLyricsReference() }, { songQueries.updateLyricsById(it?.source_index?.toLong(), it?.id, id) }
    )
    val LyricsSyncOffset: Property<Long?>
        get() = property_rememberer.rememberSingleQueryProperty(
        "LyricsSyncOffset", { songQueries.lyricsSyncOffsetById(id) }, { lyrics_sync_offset }, { songQueries.updateLyricsSyncOffsetById(it, id) }
    )
    val PlayerGradientDepth: Property<Float?>
        get() = property_rememberer.rememberSingleQueryProperty(
        "PlayerGradientDepth", { songQueries.npGradientDepthById(id) }, { np_gradient_depth?.toFloat() }, { songQueries.updateNpGradientDepthById(it?.toDouble(), id) }
    )
    val ThumbnailRounding: Property<Int?>
        get() = property_rememberer.rememberSingleQueryProperty(
        "ThumbnailRounding", { songQueries.thumbnailRoundingById(id) }, { thumbnail_rounding?.toInt() }, { songQueries.updateThumbnailRoundingById(it?.toLong(), id) }
    )
    val NotificationImageOffset: Property<IntOffset?>
        get() = property_rememberer.rememberSingleQueryProperty(
        "NotificationImageOffset",
        { songQueries.notifImageOffsetById(id) },
        {
            if (notif_image_offset_x != null || notif_image_offset_y != null)
                IntOffset(
                    notif_image_offset_x?.toInt() ?: 0,
                    notif_image_offset_y?.toInt() ?: 0
                )
            else null
        },
        { songQueries.updateNotifImageOffsetById(it?.x?.toLong(), it?.y?.toLong(), id) }
    )
    val Liked: Property<SongLikedStatus?>
        get() = property_rememberer.rememberSingleQueryProperty(
        "Liked", { songQueries.likedById(id) }, { liked.toSongLikedStatus() }, { songQueries.updatelikedById(it.toLong(), id) }
    )

    override val ThumbnailProvider: Property<MediaItemThumbnailProvider?>
        get() = object : Property<MediaItemThumbnailProvider?> {
            override fun get(db: Database): MediaItemThumbnailProvider =
                SongThumbnailProvider(id)

            override fun set(value: MediaItemThumbnailProvider?, db: Database) {}

            @Composable
            override fun observe(db: Database): MutableState<MediaItemThumbnailProvider?> =
                remember(this) { mutableStateOf(get(db)) }
        }
}

private data class SongThumbnailProvider(val id: String): MediaItemThumbnailProvider {
    override fun getThumbnailUrl(quality: MediaItemThumbnailProvider.Quality): String? =
        when (quality) {
            MediaItemThumbnailProvider.Quality.LOW -> "https://img.youtube.com/vi/$id/0.jpg"
            MediaItemThumbnailProvider.Quality.HIGH -> "https://img.youtube.com/vi/$id/maxresdefault.jpg"
        }
}
