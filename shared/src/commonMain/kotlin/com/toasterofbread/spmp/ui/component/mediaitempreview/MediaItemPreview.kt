package com.toasterofbread.spmp.ui.component.mediaitempreview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toasterofbread.spmp.model.mediaitem.Artist
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemPreviewParams
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.mediaItemPreviewInteraction
import com.toasterofbread.spmp.ui.component.longpressmenu.LongPressMenuData
import com.toasterofbread.spmp.ui.component.longpressmenu.longPressMenuIcon

const val MEDIA_ITEM_PREVIEW_LONG_HEIGHT: Float = 40f

@Composable
fun MediaItemPreviewSquare(
    item: MediaItem,
    params: MediaItemPreviewParams,
    long_press_menu_data: LongPressMenuData
) {
    Column(
        params.modifier.mediaItemPreviewInteraction(item, long_press_menu_data),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            item.Thumbnail(
                MediaItemThumbnailProvider.Quality.LOW,
                Modifier.longPressMenuIcon(long_press_menu_data, params.enable_long_press_menu).aspectRatio(1f),
                contentColourProvider = params.contentColour
            )

            params.multiselect_context?.also { ctx ->
                ctx.SelectableItemOverlay(item, Modifier.fillMaxWidth().aspectRatio(1f), key = long_press_menu_data.multiselect_key)
            }
        }

        Text(
            item.title ?: "",
//            Modifier.fillMaxSize().weight(1f),
            fontSize = 12.sp,
            color = params.contentColour?.invoke() ?: Color.Unspecified,
            maxLines = params.square_item_max_text_rows ?: 1,
            lineHeight = 14.sp,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun MediaItemPreviewLong(
    item: MediaItem,
    params: MediaItemPreviewParams,
    long_press_menu_data: LongPressMenuData
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = params.modifier
            .fillMaxWidth()
            .mediaItemPreviewInteraction(item, long_press_menu_data)
            .height(MEDIA_ITEM_PREVIEW_LONG_HEIGHT.dp)
    ) {
        Box(Modifier.width(IntrinsicSize.Min).height(IntrinsicSize.Min), contentAlignment = Alignment.Center) {
            item.Thumbnail(
                MediaItemThumbnailProvider.Quality.LOW,
                Modifier
                    .longPressMenuIcon(long_press_menu_data, params.enable_long_press_menu)
                    .size(MEDIA_ITEM_PREVIEW_LONG_HEIGHT.dp),
                contentColourProvider = params.contentColour
            )

            params.multiselect_context?.also { ctx ->
                ctx.SelectableItemOverlay(item, Modifier.fillMaxSize(), key = long_press_menu_data.multiselect_key)
            }
        }

        Column(
            Modifier
                .padding(horizontal = 10.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                item.title ?: "",
                fontSize = 15.sp,
                color = params.contentColour?.invoke() ?: Color.Unspecified,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                if (params.show_type) {
                    InfoText(item.getReadableType(false), params)
                }

                if (item !is Artist && item.artist?.title != null) {
                    if (params.show_type) {
                        InfoText("\u2022", params)
                    }
                    InfoText(item.artist?.title!!, params)
                }
            }
        }
    }
}

@Composable
private fun InfoText(text: String, params: MediaItemPreviewParams) {
    Text(
        text,
        Modifier.alpha(0.5f),
        fontSize = 11.sp,
        color = params.contentColour?.invoke() ?: Color.Unspecified,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}
