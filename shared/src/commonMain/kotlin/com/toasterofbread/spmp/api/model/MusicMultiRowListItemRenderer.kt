package com.toasterofbread.spmp.api.model

import com.toasterofbread.spmp.api.TextRuns
import com.toasterofbread.spmp.api.ThumbnailRenderer
import com.toasterofbread.spmp.api.radio.YoutubeiNextResponse
import com.toasterofbread.spmp.model.mediaitem.AccountPlaylist
import com.toasterofbread.spmp.model.mediaitem.Artist
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.model.mediaitem.data.AccountPlaylistItemData
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
    fun toMediaItem(hl: String): MediaItem {
        val title = title.runs!!.first()
        return Song.fromId(
            title.navigationEndpoint!!.browseEndpoint!!.browseId.removePrefix("MPED")
        ).editSongData {
            var podcast_data: AccountPlaylistItemData? = null

            val podcast_text = secondTitle?.runs?.firstOrNull()
            if (podcast_text != null) {
                podcast_data = AccountPlaylist.fromId(
                    podcast_text.navigationEndpoint!!.browseEndpoint!!.browseId
                ).editPlaylistDataManual {
                    supplyTitle(podcast_text.text)
                }
            }
            else {
                for (item in menu.menuRenderer.items) {
                    val browse_endpoint = item.menuNavigationItemRenderer?.navigationEndpoint?.browseEndpoint ?: continue
                    if (browse_endpoint.getPageType() == "MUSIC_PAGE_TYPE_PODCAST_SHOW_DETAIL_PAGE") {
                        podcast_data = AccountPlaylist.fromId(
                            browse_endpoint.browseId
                        ).data
                        break
                    }
                }
            }

            if (podcast_data != null) {
                podcast_data.supplyPlaylistType(PlaylistType.PODCAST, true)
                podcast_data.save()
                supplyAlbum(podcast_data.data_item, true)
            }

            for (run in subtitle.runs ?: emptyList()) {
                if (run.navigationEndpoint?.browseEndpoint?.getMediaItemType() == MediaItemType.ARTIST) {
                    supplyArtist(
                        Artist.fromId(run.navigationEndpoint.browseEndpoint.browseId)
                            .editArtistData {
                                supplyTitle(run.text, true)
                            },
                        true
                    )
                }
            }

            if (onTap.getMusicVideoType() == "MUSIC_VIDEO_TYPE_PODCAST_EPISODE") {
                supplySongType(SongType.PODCAST, true)
            }

            val duration = subtitle.runs?.lastOrNull()?.text?.let { text ->
                parseYoutubeDurationString(text, hl)
            }
            supplyDuration(duration, duration != null)

            supplyTitle(title.text, true)
            supplyThumbnailProvider(thumbnail.toThumbnailProvider(), true)
        }
    }
}
