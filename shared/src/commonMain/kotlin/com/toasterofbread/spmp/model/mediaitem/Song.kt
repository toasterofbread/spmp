package com.toasterofbread.spmp.model.mediaitem

import androidx.compose.ui.unit.IntOffset
import com.toasterofbread.spmp.api.lyrics.LyricsReference
import com.toasterofbread.spmp.api.lyrics.toLyricsReference
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.enums.SongType as SongTypeEnum

class SongRef(override val id: String): Song

interface Song: MediaItem, MediaItem.WithArtist {
    override fun getType(): MediaItemType = MediaItemType.SONG
    override fun getURL(): String = "https://music.youtube.com/watch?v=$id"

    val SongType: Property<SongTypeEnum?> get() = SingleProperty(
        { songQueries.songTypeById(id) },
        { song_type?.let { SongTypeEnum.values()[it.toInt()] } },
        { songQueries.updateSongTypeById(it?.ordinal?.toLong(), id) }
    )
    val Duration: Property<Long?> get() = SingleProperty(
        { songQueries.durationById(id) }, { duration }, { songQueries.updateDurationById(it, id) }
    )
    override val Artist: Property<Artist?> get() = SingleProperty(
        { songQueries.artistById(id) }, { artist?.let { Artist(it) } }, { songQueries.updateArtistById(it?.id, id) }
    )
    val Album: Property<Playlist?> get() = SingleProperty(
        { songQueries.albumById(id) }, { album?.let { Playlist(it) } }, { songQueries.updateAlbumById(it?.id, id) }
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

    var song_type: SongTypeEnum? = null,
    var duration: Long? = null,
    override var artist: Artist? = null,
    var album: Playlist? = null,
    var related_browse_id: String? = null
): MediaItemData(), Song, MediaItem.DataWithArtist

//class Song private constructor(
//    id: String,
//    context: PlatformContext
//): MediaItem(id, context) {
//
//    override val artist: Artist?
//        get() = TODO("Not yet implemented")
//    override val url: String get() = "https://music.youtube.com/watch?v=$id"
//
//    override val data = SongItemData(this)
//    val song_reg_entry: SongDataRegistryEntry = registry_entry as SongDataRegistryEntry
//
//    val like_status = SongLikeStatus(id)
//    val lyrics = SongLyricsHolder(this)
//
//    val song_type: SongType? get() = data.song_type
//    val duration: Long? get() = data.duration
//    val album: Playlist? get() = data.album
//
//    var theme_colour: Color?
//        get() = song_reg_entry.theme_colour?.let { Color(it) }
//        set(value) {
//            editRegistry {
//                (it as SongDataRegistryEntry).theme_colour = value?.toArgb()
//            }
//        }
//
//    override fun canGetThemeColour(): Boolean = theme_colour != null || super.canGetThemeColour()
//    override fun getThemeColour(): Color? = theme_colour ?: super.getThemeColour()
//
//    suspend fun getRelatedBrowseId(): Result<String> =
//        getGeneralValue { data.related_browse_id }
//
//    fun <T> editSongData(action: SongItemData.() -> T): T {
//        val ret = editData {
//            action(this as SongItemData)
//        }
//        return ret
//    }
//
//    suspend fun <T> editSongDataSuspend(action: suspend SongItemData.() -> T): T {
//        val ret = editDataSuspend {
//            action(this as SongItemData)
//        }
//        return ret
//    }
//
//    fun editSongDataManual(action: SongItemData.() -> Unit): SongItemData {
//        action(data)
//        return data
//    }
//
//    fun getFormatByQuality(quality: SongAudioQuality): Result<YoutubeVideoFormat> {
//        val formats = getAudioFormats()
//        if (formats.isFailure) {
//            return formats.cast()
//        }
//
//        return Result.success(formats.getOrThrow().getByQuality(quality))
//    }
//
//    fun getStreamFormat(): Result<YoutubeVideoFormat> {
//        val quality: SongAudioQuality = getTargetStreamQuality()
//        if (stream_format?.matched_quality != quality) {
//            val formats = getAudioFormats()
//            if (formats.isFailure) {
//                return formats.cast()
//            }
//
//            stream_format = formats.getOrThrow().getByQuality(quality)
//        }
//
//        return Result.success(stream_format!!)
//    }
//
//    override fun canLoadThumbnail(): Boolean = true
//
//    @Composable
//    override fun PreviewSquare(params: MediaItemPreviewParams) {
//        SongPreviewSquare(this, params)
//    }
//
//    @Composable
//    fun PreviewSquare(params: MediaItemPreviewParams, queue_index: Int?) {
//        SongPreviewSquare(this, params, queue_index = queue_index)
//    }
//
//    @Composable
//    override fun PreviewLong(params: MediaItemPreviewParams) {
//        SongPreviewLong(this, params)
//    }
//
//    @Composable
//    fun PreviewLong(params: MediaItemPreviewParams, queue_index: Int?) {
//        SongPreviewLong(this, params, queue_index = queue_index)
//    }
//
//    override fun getDefaultRegistryEntry(): MediaItemDataRegistry.Entry = SongDataRegistryEntry()
//
//    private var audio_formats: List<YoutubeVideoFormat>? = null
//    private var stream_format: YoutubeVideoFormat? = null
//
//    override fun downloadThumbnail(quality: MediaItemThumbnailProvider.Quality): Result<ImageBitmap> {
//        val provider = thumbnail_provider ?: return Result.failure(IllegalStateException())
//
//        // Iterate through getThumbUrl URL and ThumbnailQuality URLs for passed quality and each lower quality
//        for (i in 0 .. quality.ordinal + 1) {
//
//            // Some static thumbnails are cropped for some reason
//            if (i == 0 && provider.isStatic()) {
//                continue
//            }
//
//            val url = if (i == 0) provider.getThumbnailUrl(quality) else {
//                when (MediaItemThumbnailProvider.Quality.values()[quality.ordinal - i + 1]) {
//                    MediaItemThumbnailProvider.Quality.LOW -> "https://img.youtube.com/vi/$id/0.jpg"
//                    MediaItemThumbnailProvider.Quality.HIGH -> "https://img.youtube.com/vi/$id/maxresdefault.jpg"
//                }
//            }
//
//            try {
//                val connection = URL(url).openConnection()
//                connection.connectTimeout = DEFAULT_CONNECT_TIMEOUT
//
//                val stream = connection.getInputStream()
//                val bytes = stream.readBytes()
//                stream.close()
//
//                val image = bytes.toImageBitmap()
//                if (image.width == image.height) {
//                    return Result.success(image)
//                }
//
//                // Crop image to 1:1
//                val size = (image.width * (9f/16f)).toInt()
//                return Result.success(image.crop((image.width - size) / 2, (image.height - size) / 2, size, size))
//            }
//            catch (e: Throwable) {
//                if (i == quality.ordinal + 1) {
//                    return Result.failure(e)
//                }
//            }
//        }
//
//        return Result.failure(IllegalStateException())
//    }
//
//    // Expects formats to be sorted by bitrate (descending)
//    private fun List<YoutubeVideoFormat>.getByQuality(quality: SongAudioQuality): YoutubeVideoFormat {
//        check(isNotEmpty())
//        return when (quality) {
//            SongAudioQuality.HIGH -> firstOrNull { it.audio_only } ?: first()
//            SongAudioQuality.MEDIUM -> {
//                val audio_formats = filterList { audio_only }
//                if (audio_formats.isNotEmpty()) {
//                    audio_formats[audio_formats.size / 2]
//                }
//                else {
//                    get(size / 2)
//                }
//            }
//            SongAudioQuality.LOW -> lastOrNull { it.audio_only } ?: last()
//        }.also { it.matched_quality = quality }
//    }
//
//    @Synchronized
//    private fun getAudioFormats(): Result<List<YoutubeVideoFormat>> {
//        if (audio_formats == null) {
//            val result = getVideoFormats(id) { it.audio_only }
//            if (result.isFailure) {
//                return result.cast()
//            }
//
//            if (result.getOrThrow().isEmpty()) {
//                return Result.failure(Exception("No formats returned by getVideoFormats($id)"))
//            }
//
//            audio_formats = result.getOrThrow().sortedByDescending { it.bitrate }
//        }
//        return Result.success(audio_formats!!)
//    }
//
//    companion object {
//        private val songs: MutableMap<String, Song> = mutableMapOf()
//
//        @Synchronized
//        fun fromId(id: String, context: PlatformContext = SpMp.context): Song {
//            return songs.getOrPut(id) {
//                val song = Song(id, context)
//                song.loadFromCache()
//                return@getOrPut song
//            }
//        }
//
//        fun getTargetStreamQuality(): SongAudioQuality {
//            return Settings.getEnum(Settings.KEY_STREAM_AUDIO_QUALITY)
//        }
//
//        fun getTargetDownloadQuality(): SongAudioQuality {
//            return Settings.getEnum(Settings.KEY_DOWNLOAD_AUDIO_QUALITY)
//        }
//    }
//}
