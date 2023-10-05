package com.toasterofbread.spmp.youtubeapi.endpoint

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.graphics.vector.ImageVector
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.layout.MediaItemLayout
import com.toasterofbread.spmp.youtubeapi.YoutubeApi

abstract class SearchEndpoint: YoutubeApi.Endpoint() {
    abstract suspend fun searchMusic(query: String, params: String?): Result<SearchResults>
}

enum class SearchType {
    SONG, VIDEO, PLAYLIST, ALBUM, ARTIST;

    fun getIcon(): ImageVector {
        return when (this) {
            SONG -> MediaItemType.SONG.getIcon()
            PLAYLIST -> MediaItemType.ARTIST.getIcon()
            ARTIST -> MediaItemType.PLAYLIST_REM.getIcon()
            VIDEO -> Icons.Filled.PlayArrow
            ALBUM -> Icons.Filled.Album
        }
    }

    fun getDefaultParams(): String {
        return when (this) {
            SONG -> "EgWKAQIIAUICCAFqDBAOEAoQAxAEEAkQBQ%3D%3D"
            PLAYLIST -> "EgWKAQIoAUICCAFqDBAOEAoQAxAEEAkQBQ%3D%3D"
            ARTIST -> "EgWKAQIgAUICCAFqDBAOEAoQAxAEEAkQBQ%3D%3D"
            VIDEO -> "EgWKAQIQAUICCAFqDBAOEAoQAxAEEAkQBQ%3D%3D"
            ALBUM -> "EgWKAQIYAUICCAFqDBAOEAoQAxAEEAkQBQ%3D%3D"
        }
    }
}
data class SearchFilter(val type: SearchType, val params: String)
data class SearchResults(val categories: List<Pair<MediaItemLayout, SearchFilter?>>, val suggested_correction: String?)
