package com.toasterofbread.spmp.ui.layout.apppage.searchpage

import LocalPlayerState
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.*
import androidx.compose.ui.unit.Dp
import dev.toastbits.composekit.util.composable.copy
import dev.toastbits.composekit.components.utils.modifier.*
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.component.LargeFilterList
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.MINIMISED_NOW_PLAYING_HEIGHT_DP
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.LayoutSlot
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingTopOffsetSection
import dev.toastbits.ytmkt.endpoint.*

@Composable
internal fun SearchAppPage.VerticalSearchSecondaryBar(
    suggestions: List<SearchSuggestion>,
    slot: LayoutSlot,
    distance_to_page: Dp,
    modifier: Modifier,
    content_padding: PaddingValues
) {
    val player: PlayerState = LocalPlayerState.current
    val focus_requester: FocusRequester = remember { FocusRequester() }

    var show_search_bar: Boolean by remember { mutableStateOf(true) }
    var is_focused: Boolean by remember { mutableStateOf(false) }

    val bar_width: Dp = 70.dp
    val search_button_size: Dp = 40.dp

    val suggestions_width: Dp = player.screen_size.width / 2
    val suggestions_direction: Int = if (slot.is_start) 1 else -1
    val suggestions_side_padding: Dp =
        if (slot.is_start) content_padding.calculateEndPadding(LocalLayoutDirection.current)
        else content_padding.calculateStartPadding(LocalLayoutDirection.current)

    val gap: Dp = MINIMISED_NOW_PLAYING_HEIGHT_DP.dp * (if (player.player_showing) 2 else 1)

    Column(
        modifier
            .width(bar_width)
            .padding(content_padding)
            .padding(bottom = 25.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.requiredSize(search_button_size)) {
            val search_bar_offset_modifier: Modifier =
                Modifier
                    .offset(
                        y = gap - ((SEARCH_BAR_HEIGHT_DP.dp - search_button_size) / 2f)
                    )

            IconButton(
                { show_search_bar = !show_search_bar },
                player.nowPlayingTopOffset(
                    search_bar_offset_modifier,
                    NowPlayingTopOffsetSection.SEARCH_PAGE_BAR,
                    displaying = show_search_bar
                )
            ) {
                Icon(Icons.Default.Search, null)
            }

            var suggestions_height: Int by remember { mutableStateOf(0) }
            val outer_content_modifier: Modifier =
                Modifier
                    .requiredWidth(suggestions_width)
                    .offset(
                        x = (((suggestions_width + bar_width + suggestions_side_padding) / 2) + distance_to_page + 10.dp) * suggestions_direction
                    )

            val show_suggestions: Boolean = show_search_bar && is_focused && suggestions.isNotEmpty()

            Box(
                outer_content_modifier
                    .wrapContentHeight(unbounded = true)
                    .offset(
                        y = gap + (search_button_size / 2)
                    )
                    .offset {
                        IntOffset(0, -suggestions_height / 2)
                    }
                    .then(
                        player.nowPlayingTopOffset(
                            Modifier,
                            NowPlayingTopOffsetSection.SEARCH_PAGE_SUGGESTIONS,
                            displaying = show_suggestions,
                            apply_spacing = false
                        )
                    )
                    .zIndex(-1f)
                    .onSizeChanged {
                        suggestions_height = it.height
                    }
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    show_suggestions,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
                    SearchSuggestionsColumn(
                        suggestions,
                        Alignment.Bottom,
                        // Modifier.padding(bottom = 15.dp)
                    )
                }
            }

            Box(
                outer_content_modifier
                    .wrapContentHeight(unbounded = true)
                    .then(search_bar_offset_modifier)
                    .then(
                        player.nowPlayingTopOffset(
                            Modifier,
                            NowPlayingTopOffsetSection.SEARCH_PAGE_BAR,
                            displaying = show_search_bar
                        )
                    )
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    show_search_bar,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    SearchBar(
                        Modifier.fillMaxWidth(),
                        apply_padding = false
                    ) {
                        is_focused = it
                    }
                }
            }
        }
    }
}
