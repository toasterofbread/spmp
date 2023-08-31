package com.toasterofbread.spmp.ui.layout

import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.mediaitem.db.rememberPinnedItems
import com.toasterofbread.spmp.model.mediaitem.db.setPinned
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MediaItemGrid
import com.toasterofbread.spmp.ui.layout.mainpage.getMainPageItemSize

@Composable
fun PinnedItemsRow(
    modifier: Modifier = Modifier
) {
    val player = LocalPlayerState.current
    val pinned_items = rememberPinnedItems(player.context)

    AnimatedVisibility(pinned_items.isNotEmpty()) {
        MediaItemGrid(
            pinned_items,
            modifier,
            rows = 1,
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
                            Icon(Icons.Filled.Close, null)
                        }
                    }
                }
            },
            multiselect_context = player.main_multiselect_context,
            itemSizeProvider = { player.getMainPageItemSize() * 0.8f }
        )
    }
}
