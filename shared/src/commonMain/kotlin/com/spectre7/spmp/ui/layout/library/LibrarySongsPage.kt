package com.spectre7.spmp.ui.layout.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.spectre7.spmp.model.mediaitem.MediaItemPreviewParams
import com.spectre7.spmp.model.mediaitem.Song
import com.spectre7.spmp.platform.PlayerDownloadManager
import com.spectre7.spmp.ui.component.multiselect.MediaItemMultiSelectContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibrarySongsPage(
    downloads: List<PlayerDownloadManager.DownloadStatus>,
    multiselect_context: MediaItemMultiSelectContext,
    bottom_padding: Dp,
    inline: Boolean,
    openPage: (LibrarySubPage?) -> Unit
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
                enabled = !active
            )
        }
    }

    LazyColumn(contentPadding = PaddingValues(bottom = bottom_padding)) {
        items(sorted_songs, { it.id }) { song ->
            song.PreviewLong(MediaItemPreviewParams(multiselect_context = multiselect_context))
        }
    }
}
