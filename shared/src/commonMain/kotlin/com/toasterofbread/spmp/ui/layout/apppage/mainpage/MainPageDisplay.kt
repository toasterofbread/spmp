package com.toasterofbread.spmp.ui.layout.apppage.mainpage

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.toasterofbread.composekit.utils.common.blendWith
import com.toasterofbread.composekit.utils.composable.getEnd
import com.toasterofbread.composekit.utils.composable.getStart
import com.toasterofbread.composekit.utils.composable.getTop
import com.toasterofbread.spmp.platform.FormFactor
import com.toasterofbread.spmp.platform.form_factor
import com.toasterofbread.spmp.platform.getDefaultHorizontalPadding
import com.toasterofbread.spmp.platform.getDefaultVerticalPadding
import com.toasterofbread.spmp.ui.component.WAVE_BORDER_HEIGHT_DP
import com.toasterofbread.spmp.ui.layout.apppage.AppPageSidebar

@Composable
fun MainPageDisplay() {
    val player = LocalPlayerState.current
    val horizontal_padding by animateDpAsState(player.getDefaultHorizontalPadding())

    Crossfade(player.app_page) { page ->

        Row {
            if (player.form_factor == FormFactor.DESKTOP) {
                AppPageSidebar(
                    Modifier.fillMaxHeight().zIndex(1f),
                    content_padding = PaddingValues(10.dp)
                )
            }

            Column(Modifier.fillMaxWidth().weight(1f)) {
                val vertical_padding: Dp = player.getDefaultVerticalPadding()
                val top_padding: Dp = WindowInsets.getTop()

                if (page.showTopBar()) {
                    MainPageTopBar(PaddingValues(horizontal = horizontal_padding), Modifier.padding(top = top_padding).zIndex(1f))
                }

                with(page) {
                    Page(
                        player.main_multiselect_context,
                        Modifier,
                        PaddingValues(
                            top = if (page.showTopBar()) WAVE_BORDER_HEIGHT_DP.dp else (top_padding + vertical_padding),
                            bottom = player.nowPlayingBottomPadding(true) + vertical_padding,
                            start = horizontal_padding + WindowInsets.getStart(),
                            end = horizontal_padding + WindowInsets.getEnd()
                        )
                    ) { player.navigateBack() }
                }
            }
        }
    }
}
