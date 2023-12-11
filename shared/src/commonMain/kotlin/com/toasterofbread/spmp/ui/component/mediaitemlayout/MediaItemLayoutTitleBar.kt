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
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.toasterofbread.composekit.utils.composable.PlatformClickableIconButton
import com.toasterofbread.composekit.utils.composable.WidthShrinkText
import com.toasterofbread.spmp.model.mediaitem.MediaItemHolder
import com.toasterofbread.spmp.model.mediaitem.layout.ViewMore
import com.toasterofbread.spmp.model.mediaitem.layout.shouldShowTitleBar
import com.toasterofbread.spmp.resources.uilocalisation.LocalisedString
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val BUTTON_SCROLL_AMOUNT_DP: Float = 200f
private const val BUTTON_SCROLL_ITEMS: Int = 2

@Composable
fun TitleBar(
    items: List<MediaItemHolder>,
    title: LocalisedString?,
    subtitle: LocalisedString?,
    modifier: Modifier = Modifier,
    view_more: ViewMore? = null,
    font_size: TextUnit? = null,
    multiselect_context: MediaItemMultiSelectContext? = null,
    scrollable_state: ScrollableState? = null
) {
    val player: PlayerState = LocalPlayerState.current

    AnimatedVisibility(
        shouldShowTitleBar(title, subtitle, view_more, scrollable_state),
        enter = slideInVertically(),
        exit = slideOutVertically()
    ) {
        val title_string: String? = remember { title?.getString(player.context) }
        val subtitle_string: String? = remember { subtitle?.getString(player.context) }

        Row(
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.Center, modifier = modifier.weight(1f)) {
                if (subtitle_string != null) {
                    WidthShrinkText(subtitle_string, style = MaterialTheme.typography.titleSmall.copy(color = player.theme.on_background))
                }

                if (title_string != null) {
                    WidthShrinkText(
                        title_string,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.headlineMedium.let { style ->
                            style.copy(
                                color = player.theme.on_background,
                                fontSize = font_size ?: style.fontSize
                            )
                        }
                    )
                }
            }

            Row {
                if (view_more != null) {
                    IconButton({ view_more.execute(player, title) }) {
                        Icon(Icons.Default.MoreHoriz, null)
                    }
                }

                scrollable_state?.ScrollButtons()

                multiselect_context?.CollectionToggleButton(items)
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
            if (orientation == Orientation.Horizontal) Icons.Default.KeyboardArrowLeft
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
            if (orientation == Orientation.Horizontal) Icons.Default.KeyboardArrowRight
            else Icons.Default.KeyboardArrowUp,
            null
        )
    }
}
