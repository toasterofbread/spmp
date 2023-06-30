package com.toasterofbread.spmp.api.model

import com.toasterofbread.spmp.api.MusicResponsiveListItemRenderer
import com.toasterofbread.spmp.api.MusicTwoRowItemRenderer
import com.toasterofbread.spmp.resources.uilocalisation.parseYoutubeDurationString
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.AccountPlaylist
import com.toasterofbread.spmp.model.mediaitem.Artist
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.model.mediaitem.data.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.model.mediaitem.enums.SongType

data class YoutubeiShelfContentsItem(val musicTwoRowItemRenderer: MusicTwoRowItemRenderer? = null, val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer? = null) {
    // Pair(item, playlistSetVideoId)
    fun toMediaItem(hl: String): Pair<MediaItem, String?>? {
        if (musicTwoRowItemRenderer != null) {
            val renderer = musicTwoRowItemRenderer

            // Video
            if (renderer.navigationEndpoint.watchEndpoint?.videoId != null) {
                val first_thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer.thumbnail.thumbnails.first()
                return Pair(
                    Song.fromId(renderer.navigationEndpoint.watchEndpoint.videoId).editSongData {
                        // Is this the best way of checking?
                        supplySongType(if (first_thumbnail.height == first_thumbnail.width) SongType.SONG else SongType.VIDEO)
                        supplyTitle(renderer.title.first_text)
                        supplyArtist(renderer.getArtist(data_item))
                        supplyThumbnailProvider(renderer.thumbnailRenderer.toThumbnailProvider())
                    },
                    null
                )
            }

            val item: MediaItem

            if (renderer.navigationEndpoint.watchPlaylistEndpoint != null) {
                if (!Settings.get<Boolean>(Settings.KEY_FEED_SHOW_RADIOS)) {
                    return null
                }

                item = AccountPlaylist.fromId(renderer.navigationEndpoint.watchPlaylistEndpoint.playlistId)
                    .editPlaylistData {
                        supplyPlaylistType(PlaylistType.RADIO, true)
                        supplyTitle(renderer.title.first_text)
                        supplyThumbnailProvider(renderer.thumbnailRenderer.toThumbnailProvider())
                    }
            }
            else {
                // Playlist or artist
                val browse_id = renderer.navigationEndpoint.browseEndpoint!!.browseId
                val page_type = renderer.navigationEndpoint.browseEndpoint.getPageType()!!

                item = when (page_type) {
                    "MUSIC_PAGE_TYPE_ALBUM", "MUSIC_PAGE_TYPE_PLAYLIST", "MUSIC_PAGE_TYPE_AUDIOBOOK" -> {
                        if (AccountPlaylist.formatId(browse_id).startsWith("RDAT") && !Settings.get<Boolean>(Settings.KEY_FEED_SHOW_RADIOS)) {
                            return null
                        }

                        AccountPlaylist.fromId(browse_id)
                            .editPlaylistData {
                                supplyPlaylistType(when (page_type) {
                                    "MUSIC_PAGE_TYPE_ALBUM" -> PlaylistType.ALBUM
                                    "MUSIC_PAGE_TYPE_PLAYLIST" -> PlaylistType.PLAYLIST
                                    else -> PlaylistType.AUDIOBOOK
                                }, true)
                                supplyArtist(renderer.getArtist(data_item))
                            }
                            .apply {
                                is_editable = renderer.menu?.menuRenderer?.items
                                    ?.any { it.menuNavigationItemRenderer?.icon?.iconType == "DELETE" } == true
                            }
                    }
                    "MUSIC_PAGE_TYPE_ARTIST" -> Artist.fromId(browse_id)
                    else -> throw NotImplementedError("$page_type ($browse_id)")
                }

                item.editData {
                    supplyTitle(renderer.title.first_text)
                    supplyThumbnailProvider(renderer.thumbnailRenderer.toThumbnailProvider())
                }
            }

            return Pair(item, null)
        }
        else if (musicResponsiveListItemRenderer != null) {
            val renderer = musicResponsiveListItemRenderer

            var video_id: String? = renderer.playlistItemData?.videoId ?: renderer.navigationEndpoint?.watchEndpoint?.videoId
            var video_is_main: Boolean = true

            var title: String? = null
            var artist: Artist? = null
            var playlist: AccountPlaylist? = null
            var duration: Long? = null

            if (video_id == null) {
                val page_type = renderer.navigationEndpoint?.browseEndpoint?.getPageType()
                when (page_type) {
                    "MUSIC_PAGE_TYPE_ALBUM", "MUSIC_PAGE_TYPE_PLAYLIST" -> {
                        video_is_main = false
                        playlist = AccountPlaylist.fromId(renderer.navigationEndpoint.browseEndpoint.browseId)
                            .editPlaylistData {
                                supplyPlaylistType(PlaylistType.fromTypeString(page_type), true)
                            }
                    }
                    "MUSIC_PAGE_TYPE_ARTIST", "MUSIC_PAGE_TYPE_USER_CHANNEL" -> {
                        video_is_main = false
                        artist = Artist.fromId(renderer.navigationEndpoint.browseEndpoint.browseId)
                    }
                }
            }

            if (renderer.flexColumns != null) {
                for (column in renderer.flexColumns.withIndex()) {
                    val text = column.value.musicResponsiveListItemFlexColumnRenderer.text
                    if (text.runs == null) {
                        continue
                    }

                    if (column.index == 0) {
                        title = text.first_text
                    }

                    for (run in text.runs!!) {
                        if (run.navigationEndpoint == null) {
                            continue
                        }

                        if (run.navigationEndpoint.watchEndpoint != null) {
                            if (video_id == null) {
                                video_id = run.navigationEndpoint.watchEndpoint.videoId!!
                            }
                            continue
                        }

                        val browse_endpoint = run.navigationEndpoint.browseEndpoint
                        when (browse_endpoint?.getPageType()) {
                            "MUSIC_PAGE_TYPE_ARTIST", "MUSIC_PAGE_TYPE_USER_CHANNEL" -> {
                                if (artist == null) {
                                    artist = Artist.fromId(browse_endpoint.browseId).editArtistData { supplyTitle(run.text) }
                                }
                            }
                        }
                    }
                }
            }

            if (renderer.fixedColumns != null) {
                for (column in renderer.fixedColumns) {
                    val text = column.musicResponsiveListItemFixedColumnRenderer.text.first_text
                    val parsed = parseYoutubeDurationString(text, hl)
                    if (parsed != null) {
                        duration = parsed
                        break
                    }
                }
            }

            val item_data: MediaItemData
            if (video_id != null) {
                item_data = Song.fromId(video_id).editSongDataManual {
                    supplyDuration(duration, true)
                    renderer.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.firstOrNull()?.also {
                        supplySongType(if (it.height == it.width) SongType.SONG else SongType.VIDEO)
                    }
                }
            }
            else if (video_is_main) {
                return null
            }
            else {
                item_data = (playlist?.data?.apply { supplyTotalDuration(duration, true) }) ?: artist?.data ?: return null
            }

            // Handle songs with no artist (or 'Various artists')
            if (artist == null) {
                if (renderer.flexColumns != null && renderer.flexColumns.size > 1) {
                    val text = renderer.flexColumns[1].musicResponsiveListItemFlexColumnRenderer.text
                    if (text.runs != null) {
                        artist = Artist.createForItem(item_data.data_item).editArtistData { supplyTitle(text.first_text) }
                    }
                }

                if (artist == null && renderer.menu != null) {
                    for (item in renderer.menu.menuRenderer.items) {
                        val browse_endpoint = (item.menuNavigationItemRenderer ?: continue).navigationEndpoint.browseEndpoint ?: continue
                        if (browse_endpoint.getMediaItemType() == MediaItemType.ARTIST) {
                            artist = Artist.fromId(browse_endpoint.browseId)
                            break
                        }
                    }
                }
            }

            with(item_data) {
                supplyTitle(title)
                supplyArtist(artist)
                supplyThumbnailProvider(renderer.thumbnail?.toThumbnailProvider())
                save()
            }

            return Pair(item_data.data_item, renderer.playlistItemData?.playlistSetVideoId)
        }

        throw NotImplementedError()
    }
}
