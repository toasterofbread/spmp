package com.toasterofbread.spmp.ui.layout.library

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.toasterofbread.spmp.model.mediaitem.MediaItemPreviewParams
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.platform.PlayerDownloadManager
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibrarySongsPage(
    downloads: List<PlayerDownloadManager.DownloadStatus>,
    multiselect_context: MediaItemMultiSelectContext,
    content_padding: PaddingValues,
    inline: Boolean,
    openPage: (LibrarySubPage?) -> Unit,
    onSongClicked: (songs: List<Song>, song: Song, index: Int) -> Unit
) {
    var filter: String? by remember { mutableStateOf(null) }
    var sorted_songs: List<Song> by remember { mutableStateOf(emptyList()) }

    LaunchedEffect(filter, downloads.size) {
        sorted_songs = downloads.mapNotNull {  download ->
            if (download.progress != 1f) return@mapNotNull null

            filter?.also { filter ->
                if (download.song.title?.contains(filter, true) != true) return@mapNotNull null
            }

            return@mapNotNull download.song
        }
    }

    Crossfade(multiselect_context.is_active) { active ->
        if (!inline && active) {
            multiselect_context.InfoDisplay()
        }
        else {
            TextField(
                filter ?: "",
                { filter = it },
                Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !active,
                label = { Text("Filter songs") }
            )
        }
    }

    val player = LocalPlayerState.current
    CompositionLocalProvider(LocalPlayerState provides remember { player.copy(onClickedOverride = { item, index ->
        onSongClicked(sorted_songs, item as Song, index!!)
    }) }) {
        LazyColumn(contentPadding = content_padding) {
            itemsIndexed(sorted_songs, { _, item -> item.id }) { index, song ->
                song.PreviewLong(MediaItemPreviewParams(multiselect_context = multiselect_context), queue_index = index)
            }
        }
    }
}
