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
import dev.toastbits.composekit.util.getContrasted
import dev.toastbits.composekit.components.utils.modifier.background
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.fromUid
import com.toasterofbread.spmp.model.mediaitem.getMediaItemFromUid
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.radio.RadioState
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewLong
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.component.multiselect.MultiSelectItem
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.radiobuilder.getReadable
import dev.toastbits.ytmkt.endpoint.RadioBuilderModifier
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.radio_filter_all

@Composable
internal fun CurrentRadioIndicator(
    getAccentColour: () -> Color,
    multiselect_context: MediaItemMultiSelectContext,
    modifier: Modifier = Modifier,
    getAllSelectableItems: () -> List<MultiSelectItem>
) {
    val player: PlayerState = LocalPlayerState.current
    val horizontal_padding: Dp = 15.dp

    val radio_state: RadioState? = player.controller?.radio_instance?.state

    val filters: List<List<RadioBuilderModifier>>? = radio_state?.filters
    var show_radio_info: Boolean by remember { mutableStateOf(false) }
    val radio_item: MediaItem? =
        radio_state?.source?.getMediaItem()
            ?.takeIf { item ->
                item !is Song || radio_state.item_queue_index == null
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

@Composable
private fun RadioFilterChip(
    selected: Boolean,
    getAccentColour: () -> Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
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
            selectedBorderColor = Color.Transparent,
            enabled = true,
            selected = selected
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
        val current_filter_index: Int? = player.controller?.radio_instance?.state?.current_filter_index

        item {
            RadioFilterChip(
                current_filter_index == -1,
                getAccentColour,
                onClick = {
                    player.withPlayer {
                        radio.setRadioFilter(-1)
                    }
                },
                modifier = Modifier.width(40.dp)
            ) {
                Icon(
                    MediaItemType.ARTIST.getIcon(),
                    null,
                    Modifier.requiredSize(18.dp)
                )
            }
        }

        itemsIndexed(listOf(null) + filters) { i, filter ->
            val index = if (filter == null) null else i - 1

            RadioFilterChip(
                current_filter_index == index,
                getAccentColour,
                onClick = {
                    player.withPlayer {
                        if (radio.instance.state.current_filter_index != index) {
                            radio.setRadioFilter(index)
                        }
                    }
                }
            ) {
                @Suppress("SimplifiableCallChain")
                Text(
                    filter?.map { it.getReadable() }?.joinToString("|")
                        ?: stringResource(Res.string.radio_filter_all)
                )
            }
        }
    }
}
