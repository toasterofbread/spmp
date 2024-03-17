package com.toasterofbread.spmp.ui.layout.artistpage

import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistLayout
import dev.toastbits.ytmkt.model.external.mediaitem.MediaItemLayout
import com.toasterofbread.spmp.ui.component.multiselect.MultiSelectItem
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import dev.toastbits.ytmkt.endpoint.ArtistWithParamsRow
import dev.toastbits.ytmkt.model.external.ItemLayoutType

internal fun artistPageGetAllItems(player: PlayerState, browse_params_rows: List<ArtistWithParamsRow>?, item_layouts: List<ArtistLayout>?): List<List<MultiSelectItem>> {
    if (browse_params_rows == null) {
        val ret: MutableList<List<MultiSelectItem>> = mutableListOf()
        for (layout in item_layouts ?: emptyList()) {
            val items: List<MediaItem> = layout.Items.get(player.database) ?: continue
            val include_index: Boolean = when (layout.Type.get(player.database)) {
                ItemLayoutType.LIST, ItemLayoutType.NUMBERED_LIST -> true
                else -> false
            }
            
            ret.add(
                items.mapIndexed { index, item ->
                    Pair(item, if (include_index) index else null)
                }
            )
        }
        return ret
    }
    else {
        val row: ArtistWithParamsRow? = browse_params_rows.firstOrNull()
        return (
            listOfNotNull(
               row?.items?.mapIndexed { index, item ->
                   Pair(item, index)
               }
            )
        )
    }
}
