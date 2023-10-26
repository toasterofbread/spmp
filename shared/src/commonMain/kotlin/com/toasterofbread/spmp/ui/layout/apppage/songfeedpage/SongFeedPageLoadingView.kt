package com.toasterofbread.spmp.ui.layout.apppage.songfeedpage

import LocalPlayerState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.toastercomposetools.settings.ui.Theme
import com.toasterofbread.toastercomposetools.utils.composable.SubtleLoadingIndicator

@Composable
internal fun SongFeedPageLoadingView(modifier: Modifier = Modifier) {
    val player = LocalPlayerState.current
    Box(modifier, contentAlignment = Alignment.Center) {
        SubtleLoadingIndicator(message = getString("loading_feed"), getColour = player.theme.on_background_provider)
    }
}
