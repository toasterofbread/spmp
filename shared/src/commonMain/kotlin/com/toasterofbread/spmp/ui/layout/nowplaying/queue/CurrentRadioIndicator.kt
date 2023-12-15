package com.toasterofbread.spmp.ui.layout.nowplaying.queue

import LocalPlayerState
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.toasterofbread.composekit.utils.common.getContrasted
import com.toasterofbread.composekit.utils.modifier.background
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.fromUid
import com.toasterofbread.spmp.model.mediaitem.getMediaItemFromUid
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewLong
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.component.multiselect.MultiSelectItem
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState
import com.toasterofbread.spmp.youtubeapi.RadioBuilderModifier

@Composable
internal fun CurrentRadioIndicator(
    getAccentColour: () -> Color,
    multiselect_context: MediaItemMultiSelectContext,
    modifier: Modifier = Modifier,
    getAllSelectableItems: () -> List<MultiSelectItem>
) {
    val player: PlayerState = LocalPlayerState.current
    val horizontal_padding: Dp = 15.dp

    val filters: List<List<RadioBuilderModifier>>? = player.controller?.radio_state?.filters
    var show_radio_info: Boolean by remember { mutableStateOf(false) }
    val radio_item: MediaItem? =
        player.controller?.radio_state?.item
            ?.takeIf { item ->
                MediaItemType.fromUid(item.first) != MediaItemType.SONG || item.second == null
            }
            ?.let { item ->
                getMediaItemFromUid(item.first)
            }

    LaunchedEffect(radio_item) {
        if (radio_item == null) {
            show_radio_info = false
        }
    }

    AnimatedVisibility(
        filters != null || radio_item != null || multiselect_context.is_active,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Row(modifier.animateContentSize()) {
            AnimatedVisibility(radio_item != null && filters != null) {
                IconButton(
                    { show_radio_info = !show_radio_info },
                    Modifier.padding(start = horizontal_padding)
                ) {
                    Box {
                        Icon(Icons.Default.Radio, null)
                        val content_colour = LocalContentColor.current
                        Icon(
                            Icons.Default.Info, null,
                            Modifier
                                .align(Alignment.BottomEnd)
                                .offset(5.dp, 5.dp)
                                .size(18.dp)
                                // Fill gap in info icon
                                .drawBehind {
                                    drawCircle(content_colour, size.width / 4)
                                },
                            tint = getAccentColour()
                        )
                    }
                }
            }

            multiselect_context.InfoDisplay(
                getAllItems = { listOf(getAllSelectableItems()) },
                content_modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontal_padding),
                show_alt_content = radio_item != null || filters != null,
                altContent = {
                    Crossfade(
                        if (show_radio_info) radio_item
                        else filters ?: radio_item,
                        Modifier.fillMaxWidth()
                    ) { state ->
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            when (state) {
                                is MediaItem ->
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = horizontal_padding)
                                            .background(RoundedCornerShape(45), getAccentColour)
                                            .padding(horizontal = 10.dp, vertical = 7.dp)
                                    ) {
                                        MediaItemPreviewLong(state, Modifier.height(35.dp))
                                    }
                                is List<*> ->
                                    FiltersRow(
                                        state as List<List<RadioBuilderModifier>>,
                                        getAccentColour,
                                        content_padding = PaddingValues(horizontal = horizontal_padding)
                                    )
                            }
                        }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RadioFilterChip(selected: Boolean, getAccentColour: () -> Color, onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    FilterChip(
        selected,
        modifier = modifier.height(32.dp),
        onClick = onClick,
        label = content,
        colors = FilterChipDefaults.filterChipColors(
            labelColor = LocalContentColor.current,
            selectedContainerColor = getAccentColour(),
            selectedLabelColor = getAccentColour().getContrasted()
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = LocalContentColor.current.copy(alpha = 0.25f),
            selectedBorderColor = Color.Transparent
        )
    )
}

@Composable
private fun FiltersRow(
    filters: List<List<RadioBuilderModifier>>,
    getAccentColour: () -> Color,
    modifier: Modifier = Modifier,
    content_padding: PaddingValues = PaddingValues(),
) {
    val player = LocalPlayerState.current
    LazyRow(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(15.dp),
        contentPadding = content_padding
    ) {
        val current_filter = player.controller?.radio_state?.current_filter

        item {
            RadioFilterChip(
                current_filter == -1,
                getAccentColour,
                onClick = {
                    player.withPlayer {
                        radio.setRadioFilter(-1)
                    }
                },
                modifier = Modifier.width(48.dp)
            ) {
                Icon(MediaItemType.ARTIST.getIcon(), null, Modifier.offset(x = (-4).dp))
            }
        }

        itemsIndexed(listOf(null) + filters) { i, filter ->
            val index = if (filter == null) null else i - 1

            RadioFilterChip(
                current_filter == index,
                getAccentColour,
                onClick = {
                    player.withPlayer {
                        if (radio.instance.state.current_filter != index) {
                            radio.setRadioFilter(index)
                        }
                    }
                }
            ) {
                Text(
                    filter?.joinToString("|") { it.getReadable() }
                        ?: getString("radio_filter_all")
                )
            }
        }
    }
}
