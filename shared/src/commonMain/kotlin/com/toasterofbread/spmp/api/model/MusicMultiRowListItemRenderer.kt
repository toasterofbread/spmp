package com.toasterofbread.spmp.api.model

import com.toasterofbread.spmp.api.TextRuns
import com.toasterofbread.spmp.api.ThumbnailRenderer
import com.toasterofbread.spmp.api.radio.YoutubeiNextResponse
import com.toasterofbread.spmp.model.mediaitem.ArtistData
import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.PlaylistData
import com.toasterofbread.spmp.model.mediaitem.SongData
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.model.mediaitem.enums.SongType
import com.toasterofbread.spmp.resources.uilocalisation.parseYoutubeDurationString

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
    val secondTitle: TextRuns? = null
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

            var podcast_data: PlaylistData? = null

            val podcast_text = secondTitle?.runs?.firstOrNull()
            if (podcast_text != null) {
                podcast_data = PlaylistData(
                    podcast_text.navigationEndpoint!!.browseEndpoint!!.browseId
                ).also { data ->
                    data.title = podcast_text.text
                }
            }
            else {
                for (item in menu.menuRenderer.items) {
                    val browse_endpoint = item.menuNavigationItemRenderer?.navigationEndpoint?.browseEndpoint ?: continue
                    if (browse_endpoint.getPageType() == "MUSIC_PAGE_TYPE_PODCAST_SHOW_DETAIL_PAGE") {
                        podcast_data = PlaylistData(
                            browse_endpoint.browseId
                        )
                        break
                    }
                }
            }

            if (podcast_data != null) {
                podcast_data.playlist_type = PlaylistType.PODCAST
                song.album_id = podcast_data
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
