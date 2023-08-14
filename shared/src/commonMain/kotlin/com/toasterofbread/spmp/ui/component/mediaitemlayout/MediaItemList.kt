package com.toasterofbread.spmp.ui.component.mediaitemlayout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.mediaitem.MediaItemHolder
import com.toasterofbread.spmp.model.mediaitem.rememberFilteredItems
import com.toasterofbread.spmp.resources.uilocalisation.LocalisedYoutubeString
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewLong
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext

@Composable
fun MediaItemList(
    layout: MediaItemLayout,
    modifier: Modifier = Modifier,
    numbered: Boolean = false,
    multiselect_context: MediaItemMultiSelectContext? = null,
    apply_filter: Boolean = false
) {
    MediaItemList(layout.items, modifier, numbered, layout.title, layout.subtitle, layout.view_more, multiselect_context, apply_filter)
}

@Composable
fun MediaItemList(
    items: List<MediaItemHolder>,
    modifier: Modifier = Modifier,
    numbered: Boolean = false,
    title: LocalisedYoutubeString? = null,
    subtitle: LocalisedYoutubeString? = null,
    view_more: MediaItemLayout.ViewMore? = null,
    multiselect_context: MediaItemMultiSelectContext? = null,
    apply_filter: Boolean = false
) {
    val filtered_items = items.rememberFilteredItems(apply_filter)

    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TitleBar(
            items,
            title,
            subtitle,
            Modifier.padding(bottom = 5.dp),
            view_more = view_more,
            multiselect_context = multiselect_context
        )

        for (item in filtered_items.withIndex()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (numbered) {
                    Text((item.index + 1).toString().padStart((filtered_items.size + 1).toString().length, '0'), fontWeight = FontWeight.Light)
                }

                item.value.item?.also {
                    MediaItemPreviewLong(it, multiselect_context = multiselect_context)
                }
            }
        }
    }
}
