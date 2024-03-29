package com.toasterofbread.spmp.ui.layout.apppage.searchpage

import dev.toastbits.ytmkt.endpoint.SearchType
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.model.mediaitem.enums.getReadable
import com.toasterofbread.spmp.model.getIcon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

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
