package com.toasterofbread.spmp.model.mediaitem.song

import LocalPlayerState
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntOffset
import com.toasterofbread.spmp.db.Database
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import dev.toastbits.ytmkt.model.external.ThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistRef
import com.toasterofbread.spmp.model.mediaitem.db.*
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylist
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistRef
import com.toasterofbread.spmp.model.settings.category.ThemeSettings
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.playerservice.PlayerService
import com.toasterofbread.spmp.youtubeapi.lyrics.LyricsReference
import com.toasterofbread.spmp.youtubeapi.lyrics.toLyricsReference
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import dev.toastbits.ytmkt.model.external.SongLikedStatus
import dev.toastbits.ytmkt.model.external.mediaitem.YtmSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlin.math.roundToInt
import PlatformIO
import com.toasterofbread.spmp.platform.crop
import io.ktor.client.HttpClient

const val STATIC_LYRICS_SYNC_OFFSET: Long = 1000

interface Song: MediaItem.WithArtists {
    override fun getType(): MediaItemType = MediaItemType.SONG
    override suspend fun getUrl(context: AppContext): String = "https://music.youtube.com/watch?v=$id"
    override fun getReference(): SongRef

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
        data.loudness_db = LoudnessDb.get(db)
    }

    override suspend fun loadData(context: AppContext, populate_data: Boolean, force: Boolean, save: Boolean): Result<SongData> {
        return super.loadData(context, populate_data, force, save) as Result<SongData>
    }

    override suspend fun downloadThumbnailData(url: String, client: HttpClient): Result<ImageBitmap> = withContext(Dispatchers.PlatformIO) {
        return@withContext kotlin.runCatching {
            val image: ImageBitmap = super.downloadThumbnailData(url, client).getOrThrow()
            if (image.width == image.height) {
                return@runCatching image
            }

            // Crop image to 1:1
            val size: Int = (image.width * (9f/16f)).toInt()
            return@runCatching image.crop((image.width - size) / 2, (image.height - size) / 2, size, size)
        }
    }

    val TypeOfSong: Property<YtmSong.Type?>
        get() = property_rememberer.rememberSingleQueryProperty(
            "TypeOfSong",
            { songQueries.songTypeById(id) },
            { song_type?.let { YtmSong.Type.entries[it.toInt()] } },
            { songQueries.updateSongTypeById(it?.ordinal?.toLong(), id) }
        )
    val Duration: Property<Long?>
        get() = property_rememberer.rememberSingleQueryProperty(
            "Duration", { songQueries.durationById(id) }, { duration }, { songQueries.updateDurationById(it, id) }
        )

    override val Artists: AltSetterProperty<List<ArtistRef>?, List<Artist>?>
        get() = property_rememberer.rememberAltSetterSingleQueryProperty(
            "Artists",
            { songQueries.artistsById(id) },
            { artists?.let { Json.decodeFromString<List<String>>(it).map { ArtistRef(it) } } },
            { songQueries.updateArtistsById(it?.map { it.id }?.let { Json.encodeToString(it) }, id) },
            { songQueries.updateArtistsById(it?.map { it.id }?.let { Json.encodeToString(it) }, id) }
        )

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
    val LoudnessDb: Property<Float?>
        get() = property_rememberer.rememberSingleQueryProperty(
            "LoudnessDbById", { songQueries.loudnessDbById(id) }, { loudness_db?.toFloat() }, { songQueries.updateLoudnessDbById(it?.toDouble(), id) }
        )
    val Explicit: Property<Boolean?>
        get() = property_rememberer.rememberSingleQueryProperty(
            "Explicit", { songQueries.explicitById(id) }, { explicit.fromNullableSQLBoolean() }, { songQueries.updateExplicitDbById(it.toNullableSQLBoolean(), id) }
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
    val ThumbnailRounding: Property<Float?>
        get() = property_rememberer.rememberSingleQueryProperty(
            "ThumbnailRounding", { songQueries.thumbnailRoundingById(id) }, { thumbnail_rounding?.toFloat() }, { songQueries.updateThumbnailRoundingById(it?.toDouble(), id) }
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
    val BackgroundImageOpacity: Property<Float?>
        get() = property_rememberer.rememberSingleQueryProperty(
            "BackgroundImageOpacity", { songQueries.backgroundImageOpacityById(id) }, { background_image_opacity?.toFloat() }, { songQueries.updateBackgroundImageOpacityById(it?.toDouble(), id) }
        )
    val VideoPosition: Property<ThemeSettings.VideoPosition?>
        get() = property_rememberer.rememberSingleQueryProperty(
            "VideoPosition", { songQueries.videoPositionById(id) }, { video_position?.let { ThemeSettings.VideoPosition.entries[it.toInt()] } }, { songQueries.updateVideoPositionById(it?.ordinal?.toLong(), id) }
        )
    val LandscapeQueueOpacity: Property<Float?>
        get() = property_rememberer.rememberSingleQueryProperty(
            "LandscapeQueueOpacity", { songQueries.landscapeQueueOpacityById(id) }, { landscape_queue_opacity?.toFloat() }, { songQueries.updateLandscapeQueueOpacityById(it?.toDouble(), id) }
        )
    val ShadowRadius: Property<Float?>
        get() = property_rememberer.rememberSingleQueryProperty(
            "ImageShadowRadius", { songQueries.imageShadowRadiusById(id) }, { image_shadow_radius?.toFloat() }, { songQueries.updateImageShadowRadiusById(it?.toDouble(), id) }
        )
    val BackgroundWaveSpeed: Property<Float?>
        get() = property_rememberer.rememberSingleQueryProperty(
            "BackgroundWaveSpeed", { songQueries.backgroundWaveSpeedById(id) }, { background_wave_speed?.toFloat() }, { songQueries.updateBackgroundWaveSpeedById(it?.toDouble(), id) }
        )
    val BackgroundWaveOpacity: Property<Float?>
        get() = property_rememberer.rememberSingleQueryProperty(
            "BackgroundWaveOpacity", { songQueries.backgroundWaveOpacityById(id) }, { background_wave_opacity?.toFloat() }, { songQueries.updateBackgroundWaveOpacityById(it?.toDouble(), id) }
        )
    val Liked: Property<SongLikedStatus?>
        get() = property_rememberer.rememberSingleQueryProperty(
            "Liked", { songQueries.likedById(id) }, { liked.toSongLikedStatus() }, { songQueries.updatelikedById(it.toLong(), id) }
        )

    @Composable
    fun getLyricsSyncOffset(database: Database, is_topbar: Boolean): State<Long> {
        val player: PlayerState = LocalPlayerState.current
        val controller: PlayerService? = player.controller

        val internal_offset: Long? by LyricsSyncOffset.observe(database)
        val settings_delay: Float by player.settings.Lyrics.SYNC_DELAY.observe()
        val settings_delay_topbar: Float by player.settings.Lyrics.SYNC_DELAY_TOPBAR.observe()
        val settings_delay_bt: Float by player.settings.Lyrics.SYNC_DELAY_BLUETOOTH.observe()

        return remember(controller, is_topbar) { derivedStateOf {
            var delay: Float = settings_delay

            if (is_topbar) {
                delay += settings_delay_topbar
            }

            // Ensure recomposition on value change, as device change is not observed directly
            @Suppress("UNUSED_EXPRESSION")
            settings_delay_bt

            if (controller?.isPlayingOverLatentDevice() == true) {
                delay += settings_delay_bt
            }

            return@derivedStateOf (internal_offset ?: 0) - (delay * 1000L).toLong() + STATIC_LYRICS_SYNC_OFFSET
        } }
    }

    override val ThumbnailProvider: Property<ThumbnailProvider?>
        get() = object : Property<ThumbnailProvider?> {
            private val provider = SongThumbnailProvider(id)
            override fun get(db: Database): ThumbnailProvider = provider

            override fun set(value: ThumbnailProvider?, db: Database) {}

            @Composable
            override fun observe(db: Database): MutableState<ThumbnailProvider?> =
                remember(this) { mutableStateOf(get(db)) }
        }

    companion object {
        fun isSongIdRegistered(context: AppContext, id: String): Boolean {
            return context.database.songQueries.countById(id).executeAsOne() > 0
        }
    }
}

private data class SongThumbnailProvider(val id: String): ThumbnailProvider {
    override fun getThumbnailUrl(quality: ThumbnailProvider.Quality): String? =
        when (quality) {
            ThumbnailProvider.Quality.LOW -> "https://img.youtube.com/vi/$id/mqdefault.jpg"
            ThumbnailProvider.Quality.HIGH -> "https://img.youtube.com/vi/$id/maxresdefault.jpg"
        }
}

@Composable
fun Song?.observeThumbnailRounding(): Int {
    val player: PlayerState = LocalPlayerState.current
    val default: Float by player.settings.Theme.NOWPLAYING_DEFAULT_IMAGE_CORNER_ROUNDING.observe()
    val corner_rounding: Float? = this?.ThumbnailRounding?.observe(player.database)?.value
    return ((corner_rounding ?: default) * 50f).roundToInt()
}
