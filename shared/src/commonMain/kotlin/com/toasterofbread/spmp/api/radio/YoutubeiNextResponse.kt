package com.toasterofbread.spmp.api.radio

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
    class ResponseRadioItem(val playlistPanelVideoRenderer: PlaylistPanelVideoRenderer? = null)
    class PlaylistPanelVideoRenderer(
        val videoId: String,
        val title: TextRuns,
        val longBylineText: TextRuns,
        val menu: Menu,
        val thumbnail: MusicThumbnailRenderer.Thumbnail
    ) {
        // Artist, certain
        suspend fun getArtist(host_item: Song): Result<Pair<Artist?, Boolean>> {
            // Get artist ID directly
            for (run in longBylineText.runs!! + title.runs!!) {
                if (run.browse_endpoint_type != "MUSIC_PAGE_TYPE_ARTIST" && run.browse_endpoint_type != "MUSIC_PAGE_TYPE_USER_CHANNEL") {
                    continue
                }

                return Result.success(Pair(
                    Artist.fromId(run.navigationEndpoint!!.browseEndpoint!!.browseId).editArtistData { supplyTitle(run.text) },
                    true
                ))
            }

            val menu_artist = menu.menuRenderer.getArtist()?.menuNavigationItemRenderer?.navigationEndpoint?.browseEndpoint?.browseId
            if (menu_artist != null) {
                return Result.success(Pair(
                    Artist.fromId(menu_artist),
                    false
                ))
            }

            // Get artist from album
            for (run in longBylineText.runs!!) {
                if (run.navigationEndpoint?.browseEndpoint?.getPageType() != "MUSIC_PAGE_TYPE_ALBUM") {
                    continue
                }

                val playlist = AccountPlaylist.fromId(run.navigationEndpoint.browseEndpoint.browseId)
                return playlist.getArtistOrNull().fold(
                    { artist ->
                        Result.success(Pair(artist, false))
                    },
                    { Result.failure(it) }
                )
            }

            // Get title-only artist (Resolves to 'Various artists' when viewed on YouTube)
            val artist_title = longBylineText.runs?.firstOrNull { it.navigationEndpoint == null }
            if (artist_title != null) {
                return Result.success(Pair(
                    Artist.createForItem(host_item).editArtistData { supplyTitle(artist_title.text) },
                    false
                ))
            }

            return Result.success(Pair(null, false))
        }
    }
    class Menu(val menuRenderer: MenuRenderer)
    class MenuRenderer(val items: List<MenuItem>) {
        fun getArtist(): MenuItem? {
            return items.firstOrNull {
                it.menuNavigationItemRenderer?.icon?.iconType == "ARTIST"
            }
        }
    }
    class MenuItem(val menuNavigationItemRenderer: MenuNavigationItemRenderer? = null)
    class MenuNavigationItemRenderer(val icon: MenuIcon, val navigationEndpoint: NavigationEndpoint)
    class MenuIcon(val iconType: String)
    class Continuation(val nextContinuationData: ContinuationData? = null, val nextRadioContinuationData: ContinuationData? = null) {
        val data: ContinuationData? get() = nextContinuationData ?: nextRadioContinuationData
    }
    class ContinuationData(val continuation: String)
}

data class YoutubeiNextContinuationResponse(
    val continuationContents: Contents
) {
    data class Contents(val playlistPanelContinuation: YoutubeiNextResponse.PlaylistPanelRenderer)
}
