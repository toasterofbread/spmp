package com.toasterofbread.spmp.ui.layout.apppage.mainpage

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.toasterofbread.spmp.platform.getDefaultHorizontalPadding
import com.toasterofbread.spmp.platform.getDefaultVerticalPadding
import com.toasterofbread.spmp.ui.component.WAVE_BORDER_DEFAULT_HEIGHT
import com.toasterofbread.toastercomposetools.utils.composable.getEnd
import com.toasterofbread.toastercomposetools.utils.composable.getStart
import com.toasterofbread.toastercomposetools.utils.composable.getTop

@Composable
fun MainPageDisplay() {
    val player = LocalPlayerState.current
    val horizontal_padding by animateDpAsState(player.getDefaultHorizontalPadding())

    Crossfade(player.app_page) { page ->
        Column {
            val vertical_padding = player.getDefaultVerticalPadding()
            val top_padding = WindowInsets.getTop()

            if (page.showTopBar()) {
                MainPageTopBar(PaddingValues(horizontal = horizontal_padding), Modifier.padding(top = top_padding).zIndex(1f))
            }

            with(page) {
                Page(
                    player.main_multiselect_context,
                    Modifier,
                    PaddingValues(
                        top = if (page.showTopBar()) WAVE_BORDER_DEFAULT_HEIGHT.dp else (top_padding + vertical_padding),
                        bottom = player.nowPlayingBottomPadding(true) + vertical_padding,
                        start = horizontal_padding + WindowInsets.getStart(),
                        end = horizontal_padding + WindowInsets.getEnd()
                    )
                ) { player.navigateBack() }
            }
        }
    }
}
