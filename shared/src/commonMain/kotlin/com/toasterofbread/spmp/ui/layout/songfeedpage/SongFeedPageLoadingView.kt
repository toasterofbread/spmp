package com.toasterofbread.spmp.ui.layout.songfeedpage

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.utils.composable.SubtleLoadingIndicator

@Composable
internal fun SongFeedPageLoadingView(modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        SubtleLoadingIndicator(message = getString("loading_feed"), getColour = Theme.on_background_provider)
    }
}
