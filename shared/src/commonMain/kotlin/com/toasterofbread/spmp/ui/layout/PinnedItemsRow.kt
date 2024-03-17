package com.toasterofbread.spmp.ui.layout

import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.db.rememberPinnedItems
import com.toasterofbread.spmp.model.mediaitem.db.setPinned
import com.toasterofbread.spmp.model.MediaItemGridParams
import com.toasterofbread.spmp.model.MediaItemLayoutParams
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MediaItemGrid
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.service.playercontroller.PlayerState

@Composable
fun PinnedItemsRow(
    modifier: Modifier = Modifier,
    multiselect_context: MediaItemMultiSelectContext? = null
) {
    val player: PlayerState = LocalPlayerState.current
    val pinned_items: List<MediaItem>? = rememberPinnedItems()

    var shown: Boolean by remember { mutableStateOf(false) }
    var prev_pinned_items: List<MediaItem>? by remember { mutableStateOf(null) }

    LaunchedEffect(pinned_items) {
        if (prev_pinned_items != null) {
            shown = true
        }
        prev_pinned_items = pinned_items
    }

    AnimatedVisibility(
        !pinned_items.isNullOrEmpty(),
        enter = if (shown) expandVertically() else EnterTransition.None,
        exit = shrinkVertically()
    ) {
        if (pinned_items == null) {
            return@AnimatedVisibility
        }

        MediaItemGrid(
            MediaItemLayoutParams(
                items = pinned_items,
                modifier = modifier,
                multiselect_context = multiselect_context
            ),
            MediaItemGridParams(
                rows = Pair(1, 1),
                startContent = {
                    item {
                        Column(
                            Modifier.fillMaxHeight(),
                            verticalArrangement = Arrangement.Top,
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
                itemSizeProvider = {
                    DpSize(100.dp, 100.dp)
                }
            )
        )
    }
}
