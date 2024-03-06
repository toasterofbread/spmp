package com.toasterofbread.spmp.youtubeapi.endpoint

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.model.mediaitem.enums.getReadable
import com.toasterofbread.spmp.model.mediaitem.layout.MediaItemLayout
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.youtubeapi.YoutubeApi

abstract class SearchEndpoint: YoutubeApi.Endpoint() {
    abstract suspend fun searchMusic(query: String, params: String?): Result<SearchResults>
}

enum class SearchType {
    SONG, VIDEO, PLAYLIST, ALBUM, ARTIST;

    fun getIcon(): ImageVector =
        when (this) {
            SONG -> MediaItemType.SONG.getIcon()
            PLAYLIST -> MediaItemType.ARTIST.getIcon()
            ARTIST -> MediaItemType.PLAYLIST_REM.getIcon()
            VIDEO -> Icons.Filled.PlayArrow
            ALBUM -> Icons.Filled.Album
        }

    fun getDefaultParams(): String =
        when (this) {
            SONG -> "EgWKAQIIAUICCAFqDBAOEAoQAxAEEAkQBQ%3D%3D"
            PLAYLIST -> "EgWKAQIoAUICCAFqDBAOEAoQAxAEEAkQBQ%3D%3D"
            ARTIST -> "EgWKAQIgAUICCAFqDBAOEAoQAxAEEAkQBQ%3D%3D"
            VIDEO -> "EgWKAQIQAUICCAFqDBAOEAoQAxAEEAkQBQ%3D%3D"
            ALBUM -> "EgWKAQIYAUICCAFqDBAOEAoQAxAEEAkQBQ%3D%3D"
        }
}

fun SearchType?.getReadable(): String =
    when (this) {
        null -> getString("search_filter_all")
        SearchType.VIDEO -> getString("search_filter_videos")
        SearchType.SONG -> MediaItemType.SONG.getReadable(true)
        SearchType.ARTIST -> MediaItemType.ARTIST.getReadable(true)
        SearchType.PLAYLIST -> PlaylistType.PLAYLIST.getReadable(true)
        SearchType.ALBUM -> PlaylistType.ALBUM.getReadable(true)
    }

fun SearchType?.getIcon(): ImageVector =
    when (this) {
        null -> Icons.Default.SelectAll
        SearchType.VIDEO -> Icons.Default.Movie
        SearchType.SONG -> MediaItemType.SONG.getIcon()
        SearchType.ARTIST -> MediaItemType.ARTIST.getIcon()
        SearchType.PLAYLIST -> MediaItemType.PLAYLIST_REM.getIcon()
        SearchType.ALBUM -> Icons.Default.Album
    }

data class SearchFilter(val type: SearchType, val params: String)
data class SearchResults(val categories: List<Pair<MediaItemLayout, SearchFilter?>>, val suggested_correction: String?)
