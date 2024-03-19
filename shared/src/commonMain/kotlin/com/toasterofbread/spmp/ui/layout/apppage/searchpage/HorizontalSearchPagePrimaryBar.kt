package com.toasterofbread.spmp.ui.layout.apppage.searchpage

import LocalPlayerState
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.*
import com.toasterofbread.composekit.utils.common.getContrasted
import com.toasterofbread.composekit.utils.composable.*
import com.toasterofbread.composekit.utils.modifier.bounceOnClick
import com.toasterofbread.spmp.model.settings.category.BehaviourSettings
import com.toasterofbread.spmp.platform.form_factor
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.appTextField
import com.toasterofbread.spmp.ui.layout.contentbar.LayoutSlot
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingExpansionState
import com.toasterofbread.spmp.ui.theme.appHover
import com.toasterofbread.spmp.youtubeapi.endpoint.SearchSuggestion

@Composable
internal fun SearchAppPage.HorizontalSearchPrimaryBar(
    suggestions: List<SearchSuggestion>,
    slot: LayoutSlot,
    modifier: Modifier,
    content_padding: PaddingValues
) {
    val player: PlayerState = LocalPlayerState.current

    var is_focused: Boolean by remember { mutableStateOf(false) }

    val bar_height: Dp = 70.dp
    val suggestions_height: Dp = player.screen_size.height
    val suggestions_direction: Int = if (slot.is_start) 1 else -1

    Box(
        modifier
            .padding(content_padding)
            .requiredHeight(bar_height)
    ) {
        Box(
            Modifier
                .requiredHeight(suggestions_height)
                .offset(
                    y = (suggestions_height + bar_height) / 2 * suggestions_direction
                )
                .zIndex(-1f)
        ) {
            AnimatedVisibility(
                is_focused && suggestions.isNotEmpty(),
                Modifier.fillMaxHeight(),
                enter = slideInVertically { (it / -2) * suggestions_direction } + fadeIn(),
                exit = slideOutVertically { (it / -2) * suggestions_direction } + fadeOut()
            ) {
                SearchSuggestionsColumn(
                    suggestions,
                    if (suggestions_direction == 1) Alignment.Top
                    else Alignment.Bottom
                )
            }
        }

        SearchBar(Modifier.height(IntrinsicSize.Max).fillMaxWidth()) {
            is_focused = it
        }
    }
}
