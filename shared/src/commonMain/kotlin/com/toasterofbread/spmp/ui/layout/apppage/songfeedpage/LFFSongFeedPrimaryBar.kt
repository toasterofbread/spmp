package com.toasterofbread.spmp.ui.layout.apppage.songfeedpage

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.toasterofbread.composekit.platform.Platform
import com.toasterofbread.composekit.utils.common.*
import com.toasterofbread.composekit.utils.composable.*
import com.toasterofbread.composekit.utils.composable.RowOrColumn
import com.toasterofbread.composekit.utils.modifier.horizontal
import com.toasterofbread.spmp.model.mediaitem.*
import com.toasterofbread.spmp.model.mediaitem.layout.MediaItemLayout
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewSquare
import com.toasterofbread.spmp.ui.layout.contentbar.LayoutSlot
import kotlin.Unit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SongFeedAppPage.LFFSongFeedPrimaryBar(
    slot: LayoutSlot,
    modifier: Modifier,
    content_padding: PaddingValues
): Boolean {
    val size: Dp = when (Platform.current) {
        Platform.ANDROID -> 100.dp
        Platform.DESKTOP -> 125.dp
    }

    val player: PlayerState = LocalPlayerState.current

    val artists: List<MediaItem>? by artists_layout?.items?.rememberFilteredItems()
    var show_filters: Boolean by remember { mutableStateOf(false) }

    val can_show_artists: Boolean = !artists.isNullOrEmpty()
    val can_show_filters: Boolean = !filter_chips.isNullOrEmpty()

    Crossfade(
        show_filters,
        modifier.then(
            if (slot.is_vertical) Modifier.width(size)
            else Modifier.height(size)
        )
    ) { filters ->
        RowOrColumn(
            !slot.is_vertical,
            Modifier.padding(top = content_padding.calculateTopPadding()),
            alignment = 0,
            arrangement = Arrangement.spacedBy(10.dp)
        ) {
            RowOrColumn(
                slot.is_vertical,
                Modifier.padding(content_padding.horizontal),
                arrangement = Arrangement.spacedBy(10.dp)
            ) {
                val selected_colours: IconButtonColors =
                    IconButtonDefaults.iconButtonColors(
                        containerColor = player.theme.vibrant_accent.copy(alpha = 0.85f),
                        contentColor = player.theme.vibrant_accent.getContrasted()
                    )

                ShapedIconButton(
                    { show_filters = false },
                    if (!filters) selected_colours
                    else IconButtonDefaults.iconButtonColors(),
                    enabled = can_show_artists || !filters
                ) {
                    Icon(Icons.Default.Person, null)
                }

                ShapedIconButton(
                    { show_filters = true },
                    if (filters) selected_colours
                    else IconButtonDefaults.iconButtonColors(),
                    enabled = can_show_filters || filters
                ) {
                    Icon(Icons.Default.FilterAlt, null)
                }
            }

            ScrollBarLazyRowOrColumn(
                !slot.is_vertical,
                Modifier.weight(1f),
                contentPadding = content_padding.copy(top = 0.dp, start = 0.dp),
                arrangement = Arrangement.spacedBy(15.dp),
                reverseScrollBarLayout = slot.is_vertical,
                scrollBarColour = LocalContentColor.current.copy(alpha = 0.6f)
            ) {
                if (filters) {
                    itemsIndexed(filter_chips ?: emptyList()) { index, filter ->
                        val selected: Boolean = index == selected_filter_chip

                        Card(
                            { selectFilterChip(index) },
                            Modifier.aspectRatio(1f),
                            colors =
                                if (selected) CardDefaults.cardColors(
                                    containerColor = player.theme.vibrant_accent,
                                    contentColor = player.theme.vibrant_accent.getContrasted()
                                )
                                else CardDefaults.cardColors(
                                    containerColor = player.theme.accent.blendWith(player.theme.background, 0.05f),
                                    contentColor = player.theme.on_background
                                ),
                            shape = RoundedCornerShape(25.dp)
                        ) {
                            Column(Modifier.fillMaxSize().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                val icon: ImageVector? = filter.getIcon()
                                if (icon != null) {
                                    Icon(
                                        icon,
                                        null,
                                        Modifier.aspectRatio(1f).fillMaxHeight().weight(1f).padding(10.dp),
                                        tint =
                                            if (selected) LocalContentColor.current
                                            else player.theme.vibrant_accent
                                    )
                                }

                                WidthShrinkText(
                                    filter.text.getString(player.context),
                                    style = MaterialTheme.typography.labelLarge,
                                    alignment = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                else {
                    items(artists ?: emptyList()) { item ->
                        MediaItemPreviewSquare(
                            item,
                            multiselect_context = player.main_multiselect_context,
                            apply_size = false
                        )
                    }
                }
            }
        }
    }

    return true
}
