package com.spectre7.spmp.model.mediaitem

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.spectre7.spmp.ui.component.multiselect.MediaItemMultiSelectContext

data class MediaItemPreviewParams(
    val modifier: Modifier = Modifier,
    val contentColour: (() -> Color)? = null,
    val enable_long_press_menu: Boolean = true,
    val show_type: Boolean = true,
    val multiselect_context: MediaItemMultiSelectContext? = null
)
