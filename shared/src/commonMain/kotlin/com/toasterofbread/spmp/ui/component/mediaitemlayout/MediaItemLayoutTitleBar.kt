package com.toasterofbread.spmp.ui.component.mediaitemlayout

import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import dev.toastbits.composekit.components.utils.composable.PlatformClickableIconButton
import dev.toastbits.composekit.util.composable.WidthShrinkText
import com.toasterofbread.spmp.model.getString
import com.toasterofbread.spmp.model.mediaitem.MediaItemHolder
import com.toasterofbread.spmp.model.mediaitem.layout.open
import com.toasterofbread.spmp.model.mediaitem.layout.shouldShowTitleBar
import com.toasterofbread.spmp.model.MediaItemLayoutParams
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import dev.toastbits.ytmkt.model.external.YoutubePage
import dev.toastbits.ytmkt.uistrings.UiString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val BUTTON_SCROLL_AMOUNT_DP: Float = 200f
private const val BUTTON_SCROLL_ITEMS: Int = 2

@Composable
fun TitleBar(
    items: List<MediaItemHolder>,
    layout_params: MediaItemLayoutParams,
    modifier: Modifier = Modifier,
    font_size: TextUnit? = null,
    scrollable_state: ScrollableState? = null
) {
    val player: PlayerState = LocalPlayerState.current
    val coroutine_scope: CoroutineScope = rememberCoroutineScope()

    AnimatedVisibility(
        shouldShowTitleBar(layout_params, scrollable_state),
        modifier,
        enter = slideInVertically(),
        exit = slideOutVertically()
    ) {
        var title_string: String? by remember { mutableStateOf(null) }
        var subtitle_string: String? by remember { mutableStateOf(null) }

        LaunchedEffect(layout_params) {
            title_string = layout_params.title?.getString(player.context)
            subtitle_string = layout_params.subtitle?.getString(player.context)
        }

        Row(
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = modifier.weight(1f)
            ) {
                subtitle_string?.also { subtitle ->
                    WidthShrinkText(subtitle, style = layout_params.getTitleTextStyle(MaterialTheme.typography.titleSmall.copy(color = player.theme.onBackground)))
                }

                title_string?.also { title ->
                    WidthShrinkText(
                        title,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.headlineMedium.let { style ->
                            layout_params.getTitleTextStyle(
                                style.copy(
                                    color = player.theme.onBackground,
                                    fontSize = font_size ?: style.fontSize
                                )
                            )
                        }
                    )
                }
            }

            Row {
                if (layout_params.view_more != null) {
                    IconButton({
                        coroutine_scope.launch {
                            layout_params.view_more.open(player, layout_params.title)
                        }
                    }) {
                        Icon(Icons.Default.MoreHoriz, null)
                    }
                }

                scrollable_state?.ScrollButtons()

                layout_params.multiselect_context?.CollectionToggleButton(
                    remember(items) {
                        items.mapNotNull { it.item?.let { Pair(it, null) } }
                    }
                )
            }
        }
    }
}

private suspend fun ScrollableState.scrollToNext(direction: Int, density: Density, current_scroll_direction: MutableState<Int>) {
    if (this is LazyGridState) {
        val size: Int = layoutInfo.visibleItemsInfo.maxOf {
            if (layoutInfo.orientation == Orientation.Vertical) it.column else it.row
        } + 1

        val offset: Int =
            if (direction == 1 && current_scroll_direction.value == 1) size
            else 0

        current_scroll_direction.value = direction
        animateScrollToItem((firstVisibleItemIndex + ((size + offset) * direction * BUTTON_SCROLL_ITEMS)).coerceAtLeast(0))
        current_scroll_direction.value = 0
    }
    else {
        animateScrollBy(with (density) { BUTTON_SCROLL_AMOUNT_DP.dp.toPx() * direction })
    }
}

@Composable
private fun ScrollableState.ScrollButtons() {
    val coroutine_scope: CoroutineScope = rememberCoroutineScope()
    val density: Density = LocalDensity.current
    val current_scroll_direction: MutableState<Int> = remember { mutableStateOf(0) }

    val orientation: Orientation? =
        when (this) {
            is LazyGridState -> layoutInfo.orientation
            else -> null
        }

    PlatformClickableIconButton(
        onClick = {
            coroutine_scope.launch {
                scrollToNext(-1, density, current_scroll_direction)
            }
        },
        enabled = canScrollBackward
    ) {
        Icon(
            if (orientation == Orientation.Horizontal) Icons.AutoMirrored.Default.KeyboardArrowLeft
            else Icons.Default.KeyboardArrowDown,
            null
        )
    }

    PlatformClickableIconButton(
        onClick = {
            coroutine_scope.launch {
                scrollToNext(1, density, current_scroll_direction)
            }
        },
        enabled = canScrollForward
    ) {
        Icon(
            if (orientation == Orientation.Horizontal) Icons.AutoMirrored.Default.KeyboardArrowRight
            else Icons.Default.KeyboardArrowUp,
            null
        )
    }
}
