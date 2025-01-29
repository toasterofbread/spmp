package com.toasterofbread.spmp.ui.layout.apppage.songfeedpage

import LocalPlayerState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.toastbits.composekit.components.utils.composable.SubtleLoadingIndicator
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.loading_feed

@Composable
internal fun SongFeedPageLoadingView(modifier: Modifier = Modifier) {
    val player = LocalPlayerState.current
    Box(modifier, contentAlignment = Alignment.Center) {
        SubtleLoadingIndicator(message = stringResource(Res.string.loading_feed), getColour = { player.theme.onBackground })
    }
}
