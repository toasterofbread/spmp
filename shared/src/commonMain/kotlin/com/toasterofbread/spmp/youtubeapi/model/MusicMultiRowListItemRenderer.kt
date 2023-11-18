package com.toasterofbread.spmp.youtubeapi.model

import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistData
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.model.mediaitem.enums.SongType
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistData
import com.toasterofbread.spmp.model.mediaitem.song.SongData
import com.toasterofbread.spmp.resources.uilocalisation.parseYoutubeDurationString
import com.toasterofbread.spmp.youtubeapi.radio.YoutubeiNextResponse

class OnTap(
    val watchEndpoint: WatchEndpoint
) {
    class WatchEndpoint(val watchEndpointMusicSupportedConfigs: WatchEndpointMusicSupportedConfigs)
    class WatchEndpointMusicSupportedConfigs(val watchEndpointMusicConfig: WatchEndpointMusicConfig)
    class WatchEndpointMusicConfig(val musicVideoType: String)

    fun getMusicVideoType(): String =
        watchEndpoint.watchEndpointMusicSupportedConfigs.watchEndpointMusicConfig.musicVideoType
}

class MusicMultiRowListItemRenderer(
    val title: TextRuns,
    val subtitle: TextRuns,
    val thumbnail: ThumbnailRenderer,
    val menu: YoutubeiNextResponse.Menu,
    val onTap: OnTap,
    val secondTitle: TextRuns?
) {
    fun toMediaItem(hl: String): MediaItemData {
        val title = title.runs!!.first()
        return SongData(
            title.navigationEndpoint!!.browseEndpoint!!.browseId.removePrefix("MPED")
        ).also { song ->
            song.title = title.text
            song.thumbnail_provider = thumbnail.toThumbnailProvider()

            song.duration = subtitle.runs?.lastOrNull()?.text?.let { text ->
                parseYoutubeDurationString(text, hl)
            }

            if (onTap.getMusicVideoType() == "MUSIC_VIDEO_TYPE_PODCAST_EPISODE") {
                song.song_type = SongType.PODCAST
            }

            var podcast_data: RemotePlaylistData? = null

            val podcast_text = secondTitle?.runs?.firstOrNull()
            if (podcast_text != null) {
                podcast_data = RemotePlaylistData(
                    podcast_text.navigationEndpoint!!.browseEndpoint!!.browseId
                ).also { data ->
                    data.title = podcast_text.text
                }
            }

            for (item in menu.menuRenderer.items) {
                val browse_endpoint: BrowseEndpoint = item.menuNavigationItemRenderer?.navigationEndpoint?.browseEndpoint ?: continue

                if (podcast_data == null && browse_endpoint.getPageType() == "MUSIC_PAGE_TYPE_PODCAST_SHOW_DETAIL_PAGE") {
                    podcast_data = RemotePlaylistData(browse_endpoint.browseId)
                }
                else if (browse_endpoint.getMediaItemType() == MediaItemType.PLAYLIST_REM) {
                    song.album = RemotePlaylistData(browse_endpoint.browseId)
                }
            }

            if (podcast_data != null) {
                podcast_data.playlist_type = PlaylistType.PODCAST
                song.album = podcast_data
            }

            for (run in subtitle.runs ?: emptyList()) {
                if (run.navigationEndpoint?.browseEndpoint?.getMediaItemType() == MediaItemType.ARTIST) {
                    song.artist = ArtistData(run.navigationEndpoint.browseEndpoint.browseId)
                        .also { artist ->
                            artist.title = run.text
                        }
                }
            }
        }
    }
}
