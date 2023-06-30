package com.spectre7.spmp.ui.layout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.spectre7.spmp.api.RelatedGroup
import com.spectre7.spmp.api.getSongRelated
import com.spectre7.spmp.model.mediaitem.MediaItem
import com.spectre7.spmp.model.mediaitem.MediaItemPreviewParams
import com.spectre7.spmp.model.mediaitem.Song
import com.spectre7.spmp.resources.getStringTODO
import com.spectre7.spmp.ui.component.ErrorInfoDisplay
import com.spectre7.spmp.ui.component.MediaItemGrid
import com.spectre7.spmp.ui.component.PillMenu
import com.spectre7.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.spectre7.utils.composable.SubtleLoadingIndicator
import com.spectre7.utils.getContrasted
import com.spectre7.utils.modifier.horizontal
import com.spectre7.utils.modifier.vertical

@Composable
fun SongRelatedPage(
    pill_menu: PillMenu,
    song: Song,
    modifier: Modifier = Modifier,
    previous_item: MediaItem? = null,
    padding: PaddingValues = PaddingValues(),
    title_text_style: TextStyle = MaterialTheme.typography.headlineMedium,
    description_text_style: TextStyle = MaterialTheme.typography.bodyLarge,
    accent_colour: Color = LocalContentColor.current,
    close: () -> Unit
) {
    val multiselect_context = remember { MediaItemMultiSelectContext() }

    var related_result: Result<List<RelatedGroup>>? by remember { mutableStateOf(null) }
    LaunchedEffect(song) {
        related_result = null
        related_result = getSongRelated(song)
    }

    Crossfade(related_result, modifier) { result ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val related: List<RelatedGroup>? = result?.getOrNull()
            if (result == null) {
                SubtleLoadingIndicator()
            }
            else if (related == null) {
                ErrorInfoDisplay(result.exceptionOrNull()!!)
            }
            else if (related.isEmpty()) {
                Text(getStringTODO("Song has no related content"))
            }
            else {
                Column(Modifier.padding(padding.horizontal)) {
                    AnimatedVisibility(multiselect_context.is_active) {
                        multiselect_context.InfoDisplay(
                            Modifier.padding(top = padding.calculateTopPadding() + 5.dp)
                        )
                    }

                    LazyColumn(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        contentPadding = padding.vertical
                    ) {
                        item {
                            Box(Modifier.background(accent_colour, RoundedCornerShape(16.dp))) {
                                song.PreviewLong(MediaItemPreviewParams(
                                    Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 3.dp),
                                    contentColour = { accent_colour.getContrasted() }
                                ))
                            }
                        }

                        items(related) { group ->
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(group.title, style = title_text_style)

                                if (group.items != null) {
                                    MediaItemGrid(group.items, multiselect_context = multiselect_context)
                                }
                                else if (group.description != null) {
                                    Text(group.description, style = description_text_style)
                                }
                                else {
                                    Text(getStringTODO("No content"))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
