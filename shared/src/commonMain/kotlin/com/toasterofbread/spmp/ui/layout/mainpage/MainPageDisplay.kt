package com.toasterofbread.spmp.ui.layout.mainpage

import LocalPlayerState
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
import com.toasterofbread.spmp.ui.component.WAVE_BORDER_DEFAULT_HEIGHT

@Composable
fun MainPageDisplay() {
    val player = LocalPlayerState.current
    val padding by animateDpAsState(player.getDefaultHorizontalPadding())

    Column(Modifier.padding(horizontal = padding)) {
        val top_padding = WAVE_BORDER_DEFAULT_HEIGHT.dp
        MainPageTopBar(Modifier.padding(top = player.context.getStatusBarHeightDp()).zIndex(1f))

        with(player.main_page) {
            Page(
                player.main_multiselect_context,
                Modifier,
                PaddingValues(
                    top = top_padding,
                    bottom = player.bottom_padding_dp
                )
            ) { player.navigateBack() }
        }
    }
}
