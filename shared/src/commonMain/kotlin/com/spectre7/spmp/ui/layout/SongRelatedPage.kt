package com.spectre7.spmp.ui.layout

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spectre7.spmp.api.RelatedGroup
import com.spectre7.spmp.api.getMediaItemRelated
import com.spectre7.spmp.model.mediaitem.MediaItem
import com.spectre7.spmp.model.mediaitem.Song
import com.spectre7.spmp.ui.component.ErrorInfoDisplay
import com.spectre7.spmp.ui.component.MediaItemGrid
import com.spectre7.spmp.ui.component.PillMenu
import com.spectre7.utils.composable.SubtleLoadingIndicator

@Composable
fun SongRelatedPage(
    pill_menu: PillMenu,
    song: Song,
    modifier: Modifier = Modifier,
    previous_item: MediaItem? = null,
    bottom_padding: Dp = 0.dp,
    close: () -> Unit
) {
    var related_result: Result<List<RelatedGroup<MediaItem>>>? by remember { mutableStateOf(null) }
    LaunchedEffect(song) {
        related_result = null
        related_result = getMediaItemRelated(song)
    }

    Crossfade(related_result, modifier) { result ->
        if (result == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                SubtleLoadingIndicator()
            }
            return@Crossfade
        }

        val related: List<RelatedGroup<MediaItem>>? = result.getOrNull()
        if (related == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ErrorInfoDisplay(result.exceptionOrNull()!!)
            }
        }
        else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(related) { group ->
                    Column {
                        Text(group.title)
                        MediaItemGrid(group.contents)
                    }
                }
            }
        }
    }
}
