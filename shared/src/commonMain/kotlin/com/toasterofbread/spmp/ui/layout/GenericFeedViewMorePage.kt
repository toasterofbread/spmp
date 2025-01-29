package com.toasterofbread.spmp.ui.layout

import LocalPlayerState
import SpMp.isDebugBuild
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.toastbits.composekit.components.utils.composable.SubtleLoadingIndicator
import dev.toastbits.composekit.components.utils.composable.spanItem
import dev.toastbits.composekit.components.utils.modifier.horizontal
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.layout.getDefaultMediaItemPreviewSize
import com.toasterofbread.spmp.model.mediaitem.layout.getMediaItemPreviewSquareAdditionalHeight
import com.toasterofbread.spmp.model.mediaitem.toMediaItemData
import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.component.ErrorInfoDisplay
import com.toasterofbread.spmp.ui.component.WAVE_BORDER_HEIGHT_DP
import com.toasterofbread.spmp.ui.component.mediaitempreview.MEDIA_ITEM_PREVIEW_SQUARE_DEFAULT_MAX_LINES
import com.toasterofbread.spmp.ui.component.mediaitempreview.MEDIA_ITEM_PREVIEW_SQUARE_LINE_HEIGHT_SP
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewSquare
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import kotlin.math.absoluteValue

@Composable
fun GenericFeedViewMorePage(browse_id: String, modifier: Modifier = Modifier, content_padding: PaddingValues = PaddingValues(), title: String? = null) {
    val player: PlayerState = LocalPlayerState.current
    check(player.context.ytapi.GenericFeedViewMorePage.isImplemented())

    var items_result: Result<List<MediaItem>>? by remember { mutableStateOf(null) }
    LaunchedEffect(browse_id) {
        items_result = null
        items_result =
            player.context.ytapi.GenericFeedViewMorePage.getGenericFeedViewMorePage(browse_id).fold(
                {
                    val item_data: List<MediaItemData> = player.database.transactionWithResult {
                        it.map {
                            val data: MediaItemData = it.toMediaItemData()
                            data.saveToDatabase(player.database)
                            return@map data
                        }
                    }

                    Result.success(item_data)
                },
                { Result.failure(it) }
            )
    }

    Column(modifier) {
        items_result?.fold(
            { items ->
                val multiselect_context: MediaItemMultiSelectContext = remember { MediaItemMultiSelectContext(player.context) }

                val item_size: DpSize =
                    getDefaultMediaItemPreviewSize(false) +
                        DpSize(0.dp,
                            getMediaItemPreviewSquareAdditionalHeight(MEDIA_ITEM_PREVIEW_SQUARE_DEFAULT_MAX_LINES, MEDIA_ITEM_PREVIEW_SQUARE_LINE_HEIGHT_SP.sp)
                        )

                val item_spacing: Dp = (item_size.width - item_size.height).value.absoluteValue.dp
                val item_arrangement: Arrangement.HorizontalOrVertical = Arrangement.spacedBy(item_spacing)

                Column(Modifier.fillMaxSize()) {
                    multiselect_context.InfoDisplay(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = WAVE_BORDER_HEIGHT_DP.dp)
                            .padding(content_padding.horizontal)
                    )

                    LazyVerticalGrid(
                        GridCells.Adaptive(maxOf(item_size.width, item_size.height)),
                        Modifier.fillMaxWidth(),
                        contentPadding = content_padding,
                        verticalArrangement = item_arrangement
                    ) {
                        if (title != null) {
                            spanItem {
                                Text(title, Modifier.fillMaxWidth(), style = MaterialTheme.typography.headlineMedium)
                            }
                        }

                        items(items) { item ->
                            MediaItemPreviewSquare(
                                item,
                                Modifier.requiredSize(item_size),
                                multiselect_context = multiselect_context
                            )
                        }
                    }
                }

            },
            { error ->
                ErrorInfoDisplay(error, isDebugBuild(), Modifier.fillMaxWidth().padding(content_padding), onDismiss = null)
            }
        ) ?: Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
            SubtleLoadingIndicator()
        }
    }
}