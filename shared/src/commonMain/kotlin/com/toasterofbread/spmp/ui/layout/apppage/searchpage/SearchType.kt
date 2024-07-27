package com.toasterofbread.spmp.ui.layout.apppage.searchpage

import dev.toastbits.ytmkt.endpoint.SearchType
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.model.mediaitem.enums.getReadable
import com.toasterofbread.spmp.model.getIcon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.search_filter_all
import spmp.shared.generated.resources.search_filter_videos

@Composable
fun SearchType?.getReadable(): String =
    when (this) {
        null -> stringResource(Res.string.search_filter_all)
        SearchType.VIDEO -> stringResource(Res.string.search_filter_videos)
        SearchType.SONG -> stringResource(MediaItemType.SONG.getReadable(true))
        SearchType.ARTIST -> stringResource(MediaItemType.ARTIST.getReadable(true))
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
