package com.toasterofbread.spmp.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.utils.copy

@Composable
fun MultiselectAndMusicTopBar(
    multiselect_context: MediaItemMultiSelectContext,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(),
    show_wave_border: Boolean = true
): PaddingValues {
    val top_padding = padding.calculateTopPadding()
    var top_bar_showing by remember { mutableStateOf(false) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MusicTopBar(
            Settings.KEY_LYRICS_SHOW_IN_SEARCH,
            Modifier.fillMaxWidth().zIndex(1f),
            padding = padding.copy(bottom = 0.dp),
            onShowingChanged = { top_bar_showing = it }
        )

        AnimatedVisibility(multiselect_context.is_active) {
            multiselect_context.InfoDisplay(
                Modifier.padding(
                    padding.copy(
                        top = animateDpAsState(if (!top_bar_showing) top_padding else 0.dp).value,
                        bottom = 0.dp
                    )
                )
            )
        }

        if (show_wave_border) {
            WaveBorder(Modifier.fillMaxWidth())
        }
    }

    val layout_direction = LocalLayoutDirection.current
    val out_top_padding by animateDpAsState(if (!top_bar_showing && !multiselect_context.is_active) top_padding else WAVE_BORDER_DEFAULT_HEIGHT.dp)

    return padding.copy(
        layout_direction,
        top = out_top_padding,
        bottom = padding.calculateBottomPadding() + out_top_padding
    )
}
