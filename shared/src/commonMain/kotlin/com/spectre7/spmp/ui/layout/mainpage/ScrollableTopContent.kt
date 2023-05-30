package com.spectre7.spmp.ui.layout.mainpage

import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.spectre7.spmp.model.mediaitem.MediaItem
import com.spectre7.spmp.model.mediaitem.MediaItemHolder
import com.spectre7.spmp.ui.component.MediaItemGrid
import com.spectre7.spmp.ui.component.MediaItemLayout

@Composable
fun MainPageScrollableTopContent(
    pinned_items: List<MediaItemHolder>,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        AnimatedVisibility(
            pinned_items.isNotEmpty(),
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            MediaItemGrid(
                pinned_items,
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
                            IconButton({
                                println(pinned_items.toList())
                                for (item in pinned_items.toList()) {
                                    println(item.item)
                                    item.item?.setPinnedToHome(false)
                                }
                            }, Modifier.size(30.dp)) {
                                Icon(Icons.Filled.CleaningServices, null)
                            }
                        }
                    }
                },
                multiselect_context = LocalPlayerState.current.main_multiselect_context,
                itemSizeProvider = { getMainPageItemSize() * 0.8f }
            )
        }
    }
}
