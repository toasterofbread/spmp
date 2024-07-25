package com.toasterofbread.spmp.ui.layout.nowplaying.maintab

import LocalAppState
import LocalPlayerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.github.krottv.compose.sliders.DefaultThumb
import com.github.krottv.compose.sliders.DefaultTrack
import com.github.krottv.compose.sliders.SliderValueHorizontal

@Composable
internal fun VolumeSlider(colour: Color, modifier: Modifier = Modifier, reverse: Boolean = false) {
    val state: SpMp.State = LocalAppState.current

    var layout_direction = LocalLayoutDirection.current
    if (reverse) {
        layout_direction =
            if (layout_direction == LayoutDirection.Ltr) LayoutDirection.Rtl
            else LayoutDirection.Ltr
    }

    CompositionLocalProvider(LocalLayoutDirection provides layout_direction) {
        SliderValueHorizontal(
            value = state.session.status.m_volume,
            onValueChange = { state.session.controller?.volume = it },
            thumbSizeInDp = DpSize(12.dp, 12.dp),
            track = { a, b, c, d, e ->
                DefaultTrack(
                    a, b, c, d, e,
                    colorTrack = colour.copy(alpha = 0.5f),
                    colorProgress = colour
                )
            },
            thumb = { a, b, c, d, e -> DefaultThumb(a, b, c, d, e, colour, 1f) },
            modifier = modifier
        )
    }
}
