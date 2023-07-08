package com.toasterofbread.spmp.ui.component.mediaitempreview

import com.toasterofbread.spmp.model.mediaitem.MediaItem
import androidx.compose.runtime.Composable
import com.toasterofbread.spmp.model.mediaitem.MediaItemPreviewParams
import com.toasterofbread.spmp.model.mediaitem.mediaItemPreviewInteraction
import com.toasterofbread.spmp.ui.component.longpressmenu.LongPressMenuData
import com.toasterofbread.spmp.ui.component.longpressmenu.longPressMenuIcon
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.aspectRatio
import com.toasterofbread.utils.modifier.recomposeHighlighter

@Composable
fun MediaItemPreviewSquare(
    item: MediaItem,
    params: MediaItemPreviewParams,
    long_press_menu_data: LongPressMenuData
) {
    Column(
        params.modifier.mediaItemPreviewInteraction(item, long_press_menu_data).recomposeHighlighter(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
            item.Thumbnail(
                MediaItemThumbnailProvider.Quality.LOW,
                Modifier.longPressMenuIcon(long_press_menu_data, params.enable_long_press_menu).aspectRatio(1f),
                contentColourProvider = params.contentColour
            )

            params.multiselect_context?.also { ctx ->
                ctx.SelectableItemOverlay(item, Modifier.fillMaxSize(), key = long_press_menu_data.multiselect_key)
            }
        }

        Text(
            item.title ?: "",
            fontSize = 12.sp,
            color = params.contentColour?.invoke() ?: Color.Unspecified,
            maxLines = params.square_item_max_text_rows ?: 1,
            lineHeight = 14.sp,
            overflow = TextOverflow.Ellipsis
        )
    }
}
