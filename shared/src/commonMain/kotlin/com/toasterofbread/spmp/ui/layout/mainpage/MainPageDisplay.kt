package com.toasterofbread.spmp.ui.layout.mainpage

import LocalPlayerState
import SpMp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.platform.getDefaultHorizontalPadding
import com.toasterofbread.spmp.ui.component.WAVE_BORDER_DEFAULT_HEIGHT
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.zIndex
import androidx.compose.runtime.getValue
import com.toasterofbread.spmp.api.Api
import com.toasterofbread.spmp.model.FilterChip

@Composable
fun MainPageDisplay() {
    val player = LocalPlayerState.current
    val padding by animateDpAsState(SpMp.context.getDefaultHorizontalPadding())

    Column(Modifier.padding(horizontal = padding)) {
        val top_padding = WAVE_BORDER_DEFAULT_HEIGHT.dp
        MainPageTopBar(Modifier.padding(top = SpMp.context.getStatusBarHeight()).zIndex(1f))

        player.main_page.Page(
            player.main_multiselect_context,
            Modifier,
            PaddingValues(
                top = top_padding,
                bottom = player.bottom_padding_dp
            )
        ) { player.navigateBack() }
    }
}
