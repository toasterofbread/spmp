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
        return item_layouts.orEmpty().mapNotNull { layout ->
            layout.Items.get(player.database)?.map {
                Pair(it, null)
            }
        }
    }
    else {
        val row: ArtistWithParamsRow? = browse_params_rows.firstOrNull()
        return (
            listOfNotNull(
                row?.items?.map { Pair(it, null) }
            )
        )
    }
}
