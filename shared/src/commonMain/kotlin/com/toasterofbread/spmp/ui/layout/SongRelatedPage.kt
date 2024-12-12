package com.toasterofbread.spmp.ui.layout

import LocalPlayerState
import SpMp.isDebugBuild
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import dev.toastbits.composekit.util.getContrasted
import dev.toastbits.composekit.components.utils.composable.SubtleLoadingIndicator
import dev.toastbits.composekit.components.utils.modifier.horizontal
import dev.toastbits.composekit.components.utils.modifier.vertical
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.toMediaItemData
import com.toasterofbread.spmp.model.MediaItemLayoutParams
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.component.ErrorInfoDisplay
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MediaItemGrid
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewLong
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import dev.toastbits.ytmkt.endpoint.SongRelatedContentEndpoint
import dev.toastbits.ytmkt.model.external.RelatedGroup
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.song_related_page_no_content
import spmp.shared.generated.resources.song_related_page_empty_row

@Composable
fun SongRelatedPage(
    song: Song,
    related_endpoint: SongRelatedContentEndpoint,
    modifier: Modifier = Modifier,
    previous_item: MediaItem? = null,
    content_padding: PaddingValues = PaddingValues(),
    title_text_style: TextStyle = MaterialTheme.typography.headlineMedium,
    description_text_style: TextStyle = MaterialTheme.typography.bodyLarge,
    accent_colour: Color = LocalContentColor.current,
    close: () -> Unit
) {
    require(related_endpoint.isImplemented())

    val player: PlayerState = LocalPlayerState.current
    val multiselect_context: MediaItemMultiSelectContext = remember { MediaItemMultiSelectContext(player.context) }

    var related_result: Result<List<RelatedGroup>>? by remember { mutableStateOf(null) }
    var retry: Boolean by remember { mutableStateOf(false) }

    LaunchedEffect(song, retry) {
        related_result = null
        related_result = related_endpoint.getSongRelated(song.id)
    }

    Crossfade(related_result, modifier) { result ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val related: List<RelatedGroup>? = result?.getOrNull()
            if (result == null) {
                SubtleLoadingIndicator()
            }
            else if (related == null) {
                ErrorInfoDisplay(result.exceptionOrNull()!!, isDebugBuild(), onDismiss = null, onRetry = { retry = !retry })
            }
            else if (related.isEmpty()) {
                Text(stringResource(Res.string.song_related_page_no_content))
            }
            else {
                val horizontal_padding: PaddingValues = content_padding.horizontal

                Column {
                    multiselect_context.InfoDisplay(
                        Modifier
                            .padding(horizontal_padding)
                            .padding(top = content_padding.calculateTopPadding())
                    )

                    LazyColumn(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        contentPadding =
                            if (multiselect_context.is_active) PaddingValues(top = 10.dp, bottom = content_padding.calculateBottomPadding())
                            else content_padding.vertical
                    ) {
                        item {
                            Box(Modifier.padding(horizontal_padding).background(accent_colour, RoundedCornerShape(16.dp))) {
                                MediaItemPreviewLong(
                                    song,
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 6.dp, vertical = 3.dp),
                                    contentColour = { accent_colour.getContrasted() }
                                )
                            }
                        }

                        items(related) { group ->
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(group.title ?: "", Modifier.padding(horizontal_padding), style = title_text_style)

                                val items: List<MediaItemData>? = remember(group) { group.items?.map { it.toMediaItemData() } }
                                if (items != null) {
                                    MediaItemGrid(
                                        MediaItemLayoutParams(
                                            items,
                                            multiselect_context = multiselect_context,
                                            content_padding = horizontal_padding
                                            )
                                    )
                                }
                                else if (group.description != null) {
                                    Text(group.description ?: "", Modifier.padding(horizontal_padding), style = description_text_style)
                                }
                                else {
                                    Text(stringResource(Res.string.song_related_page_empty_row), Modifier.padding(horizontal_padding))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
