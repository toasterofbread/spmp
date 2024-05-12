package com.toasterofbread.spmp.model.mediaitem.layout

import dev.toastbits.ytmkt.model.external.ItemLayoutType
import dev.toastbits.ytmkt.uistrings.UiString
import dev.toastbits.ytmkt.model.external.YoutubePage
import dev.toastbits.ytmkt.model.external.mediaitem.MediaItemLayout
import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.toMediaItemData

data class AppMediaItemLayout(
    val items: List<MediaItemData>,
    val title: UiString?,
    val subtitle: UiString?,
    val type: ItemLayoutType? = null,
    val view_more: YoutubePage? = null
) {
    constructor(layout: MediaItemLayout): this(
        items = layout.items.map { it.toMediaItemData() },
        title = layout.title,
        subtitle = layout.subtitle,
        type = layout.type,
        view_more = layout.view_more
    )
}
