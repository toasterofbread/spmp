package com.toasterofbread.spmp.ui.layout

import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.db.rememberPinnedItems
import com.toasterofbread.spmp.model.mediaitem.db.setPinned
import com.toasterofbread.spmp.platform.form_factor
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MediaItemGrid
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.MEDIAITEM_PREVIEW_SQUARE_SIZE_LARGE
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.MEDIAITEM_PREVIEW_SQUARE_SIZE_SMALL
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.getMainPageItemSize

@Composable
fun PinnedItemsRow(
    modifier: Modifier = Modifier
) {
    val player: PlayerState = LocalPlayerState.current
    val pinned_items: List<MediaItem>? = rememberPinnedItems()

    AnimatedVisibility(pinned_items == null || pinned_items.isNotEmpty(), enter = slideInVertically(), exit = slideOutVertically()) {
        if (pinned_items == null) {
            return@AnimatedVisibility
        }

        MediaItemGrid(
            pinned_items,
            modifier,
            rows = Pair(1, 1),
            startContent = {
                item {
                    Column(
                        Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceEvenly,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.PushPin, null, Modifier.alpha(0.5f))
                        }
                        IconButton(
                            {
                                player.database.transaction {
                                    for (item in pinned_items.toList()) {
                                        item.setPinned(false, player.context)
                                    }
                                }
                            },
                            Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Filled.Delete, null)
                        }
                    }
                }
            },
            multiselect_context = player.main_multiselect_context,
            itemSizeProvider = {
                val width: Dp = 100.dp
                return@MediaItemGrid DpSize(
                    width,
                    width + 50.dp
                )
            }
        )
    }
}
