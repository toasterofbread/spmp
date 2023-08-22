@file:Suppress("MemberVisibilityCanBePrivate")

package com.toasterofbread.spmp.youtubeapi.radio

import com.toasterofbread.spmp.model.mediaitem.ArtistData
import com.toasterofbread.spmp.model.mediaitem.PlaylistData
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.model.mediaitem.db.loadMediaItemValue
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.youtubeapi.model.BrowseEndpoint
import com.toasterofbread.spmp.youtubeapi.model.MusicThumbnailRenderer
import com.toasterofbread.spmp.youtubeapi.model.NavigationEndpoint
import com.toasterofbread.spmp.youtubeapi.model.TextRuns
import com.toasterofbread.spmp.youtubeapi.model.WatchEndpoint

data class YoutubeiNextResponse(
    val contents: Contents
) {
    class Contents(val singleColumnMusicWatchNextResultsRenderer: SingleColumnMusicWatchNextResultsRenderer)
    class SingleColumnMusicWatchNextResultsRenderer(val tabbedRenderer: TabbedRenderer)
    class TabbedRenderer(val watchNextTabbedResultsRenderer: WatchNextTabbedResultsRenderer)
    class WatchNextTabbedResultsRenderer(val tabs: List<Tab>)
    class Tab(val tabRenderer: TabRenderer)
    class TabRenderer(val content: Content? = null, val endpoint: TabRendererEndpoint? = null)
    class TabRendererEndpoint(val browseEndpoint: BrowseEndpoint)
    class Content(val musicQueueRenderer: MusicQueueRenderer)
    class MusicQueueRenderer(val content: MusicQueueRendererContent, val subHeaderChipCloud: SubHeaderChipCloud? = null)

    class SubHeaderChipCloud(val chipCloudRenderer: ChipCloudRenderer)
    class ChipCloudRenderer(val chips: List<Chip>)
    class Chip(val chipCloudChipRenderer: ChipCloudChipRenderer) {
        fun getPlaylistId(): String = chipCloudChipRenderer.navigationEndpoint.queueUpdateCommand.fetchContentsCommand.watchEndpoint.playlistId!!
    }
    class ChipCloudChipRenderer(val navigationEndpoint: ChipNavigationEndpoint)
    class ChipNavigationEndpoint(val queueUpdateCommand: QueueUpdateCommand)
    class QueueUpdateCommand(val fetchContentsCommand: FetchContentsCommand)
    class FetchContentsCommand(val watchEndpoint: WatchEndpoint)

    class MusicQueueRendererContent(val playlistPanelRenderer: PlaylistPanelRenderer)
    class PlaylistPanelRenderer(val contents: List<ResponseRadioItem>, val continuations: List<Continuation>? = null)
    data class ResponseRadioItem(
        val playlistPanelVideoRenderer: PlaylistPanelVideoRenderer? = null,
        val playlistPanelVideoWrapperRenderer: PlaylistPanelVideoWrapperRenderer? = null
    ) {
        fun getRenderer(): PlaylistPanelVideoRenderer {
            if (playlistPanelVideoRenderer != null) {
                return playlistPanelVideoRenderer
            }

            if (playlistPanelVideoWrapperRenderer == null) {
                throw NotImplementedError("Unimplemented renderer object in ResponseRadioItem")
            }

            return playlistPanelVideoWrapperRenderer.primaryRenderer.getRenderer()
        }
    }

    class PlaylistPanelVideoWrapperRenderer(
        val primaryRenderer: ResponseRadioItem
    )

    class PlaylistPanelVideoRenderer(
        val videoId: String,
        val title: TextRuns,
        val longBylineText: TextRuns,
        val menu: Menu,
        val thumbnail: MusicThumbnailRenderer.Thumbnail
    ) {
        suspend fun getArtist(host_item: Song, context: PlatformContext): Result<ArtistData?> {
            // Get artist ID directly
            for (run in longBylineText.runs!! + title.runs!!) {
                val page_type = run.browse_endpoint_type?.let { type ->
                    MediaItemType.fromBrowseEndpointType(type)
                }
                if (page_type != MediaItemType.ARTIST) {
                    continue
                }

                return Result.success(
                    ArtistData(run.navigationEndpoint!!.browseEndpoint!!.browseId).apply {
                        title = run.text
                    }
                )
            }

            val menu_artist = menu.menuRenderer.getArtist()?.menuNavigationItemRenderer?.navigationEndpoint?.browseEndpoint?.browseId
            if (menu_artist != null) {
                return Result.success(ArtistData(menu_artist))
            }

            // Get artist from album
            for (run in longBylineText.runs!!) {
                if (run.navigationEndpoint?.browseEndpoint?.getPageType() != "MUSIC_PAGE_TYPE_ALBUM") {
                    continue
                }

                val playlist_id = run.navigationEndpoint.browseEndpoint.browseId
                var artist = context.database.playlistQueries.artistById(playlist_id).executeAsOneOrNull()?.artist?.let {
                    ArtistData(it)
                }

                if (artist == null) {
                    val artist_load_result = context.loadMediaItemValue(
                        PlaylistData(playlist_id),
                        { artist }
                    )

                    artist_load_result?.fold(
                        { artist = it },
                        { return Result.failure(it) }
                    )
                }

                if (artist != null) {
                    return Result.success(artist)
                }
            }

            // Get title-only artist (Resolves to 'Various artists' when viewed on YouTube)
            val artist_title = longBylineText.runs?.firstOrNull { it.navigationEndpoint == null }
            if (artist_title != null) {
                return Result.success(
                    ArtistData.createForItem(host_item).apply {
                        title = artist_title.text
                    }
                )
            }

            return Result.success(null)
        }
    }
    data class Menu(val menuRenderer: MenuRenderer)
    data class MenuRenderer(val items: List<MenuItem>) {
        fun getArtist(): MenuItem? =
            items.firstOrNull {
                it.menuNavigationItemRenderer?.icon?.iconType == "ARTIST"
            }
    }
    data class MenuItem(val menuNavigationItemRenderer: MenuNavigationItemRenderer? = null)
    data class MenuNavigationItemRenderer(val icon: MenuIcon, val navigationEndpoint: NavigationEndpoint)
    data class MenuIcon(val iconType: String)
    data class Continuation(val nextContinuationData: ContinuationData? = null, val nextRadioContinuationData: ContinuationData? = null) {
        val data: ContinuationData? get() = nextContinuationData ?: nextRadioContinuationData
    }
    data class ContinuationData(val continuation: String)
}

data class YoutubeiNextContinuationResponse(
    val continuationContents: Contents
) {
    data class Contents(val playlistPanelContinuation: YoutubeiNextResponse.PlaylistPanelRenderer)
}
