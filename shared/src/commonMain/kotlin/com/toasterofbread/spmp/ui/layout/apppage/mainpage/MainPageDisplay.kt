package com.toasterofbread.spmp.ui.layout.apppage.mainpage

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.toasterofbread.composekit.utils.composable.getEnd
import com.toasterofbread.composekit.utils.composable.getStart
import com.toasterofbread.composekit.utils.composable.getTop
import com.toasterofbread.spmp.platform.FormFactor
import com.toasterofbread.spmp.platform.form_factor
import com.toasterofbread.spmp.platform.getDefaultHorizontalPadding
import com.toasterofbread.spmp.platform.getDefaultVerticalPadding
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.component.WAVE_BORDER_HEIGHT_DP
import com.toasterofbread.spmp.ui.layout.apppage.AppPageSidebar

@Composable
fun MainPageDisplay(bottom_padding: Dp = 0.dp) {
    val player: PlayerState = LocalPlayerState.current
    val horizontal_padding: Dp by animateDpAsState(player.getDefaultHorizontalPadding())

    Row {
        val top_padding: Dp = WindowInsets.getTop()

        if (player.form_factor == FormFactor.LANDSCAPE) {
            AppPageSidebar(
                Modifier.fillMaxHeight().zIndex(1f),
                content_padding = PaddingValues(
                    top = 10.dp + top_padding,
                    bottom = 10.dp,
                    start = 10.dp,
                    end = 10.dp
                ),
                multiselect_context = player.main_multiselect_context
            )
        }

        Crossfade(player.app_page, Modifier.fillMaxWidth().weight(1f)) { page ->
            Column {
                val vertical_padding: Dp = player.getDefaultVerticalPadding()

                if (page.showTopBar()) {
                    MainPageTopBar(PaddingValues(horizontal = horizontal_padding), Modifier.padding(top = top_padding).zIndex(1f))
                }

                with(page) {
                    Page(
                        player.main_multiselect_context,
                        Modifier,
                        PaddingValues(
                            top = if (page.showTopBar()) WAVE_BORDER_HEIGHT_DP.dp / 2 else (top_padding + vertical_padding),
                            bottom = player.nowPlayingBottomPadding(true) + vertical_padding + bottom_padding,
                            start = horizontal_padding + WindowInsets.getStart(),
                            end = horizontal_padding + WindowInsets.getEnd()
                        )
                    ) { player.navigateBack() }
                }
            }
        }
    }
}
