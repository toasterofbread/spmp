package com.toasterofbread.spmp.ui.layout.apppage.mainpage

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.toasterofbread.spmp.platform.getDefaultHorizontalPadding
import com.toasterofbread.spmp.platform.getDefaultVerticalPadding
import com.toasterofbread.spmp.ui.component.WAVE_BORDER_DEFAULT_HEIGHT

@Composable
fun MainPageDisplay() {
    val player = LocalPlayerState.current
    val horizontal_padding by animateDpAsState(player.getDefaultHorizontalPadding())

    Crossfade(player.app_page) { page ->
        Column {
            val vertical_padding = player.getDefaultVerticalPadding()
            val status_bar_height = player.context.getStatusBarHeightDp()

            if (page.showTopBar()) {
                MainPageTopBar(PaddingValues(horizontal = horizontal_padding), Modifier.padding(top = status_bar_height).zIndex(1f))
            }

            with(page) {
                Page(
                    player.main_multiselect_context,
                    Modifier,
                    PaddingValues(
                        top = if (page.showTopBar()) WAVE_BORDER_DEFAULT_HEIGHT.dp else (status_bar_height + vertical_padding),
                        bottom = player.nowPlayingBottomPadding(true) + vertical_padding,
                        start = horizontal_padding,
                        end = horizontal_padding
                    )
                ) { player.navigateBack() }
            }
        }
    }
}
