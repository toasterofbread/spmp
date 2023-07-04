package com.toasterofbread.spmp.ui.layout.mainpage

import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.mediaitem.MediaItemHolder
import com.toasterofbread.spmp.ui.component.MediaItemGrid

@Composable
fun MainPageScrollableTopContent(
    pinned_items: List<MediaItemHolder>,
    modifier: Modifier = Modifier,
    visible: Boolean = pinned_items.isNotEmpty()
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        AnimatedVisibility(
            visible,
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
