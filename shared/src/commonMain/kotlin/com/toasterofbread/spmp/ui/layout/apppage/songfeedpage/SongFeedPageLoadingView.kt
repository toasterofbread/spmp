package com.toasterofbread.spmp.ui.layout.apppage.songfeedpage

import LocalAppState
import LocalPlayerState
import LocalTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.toastbits.composekit.settings.ui.ThemeValues
import dev.toastbits.composekit.utils.composable.SubtleLoadingIndicator
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.loading_feed

@Composable
internal fun SongFeedPageLoadingView(modifier: Modifier = Modifier) {
    val theme: ThemeValues = LocalTheme.current
    Box(modifier, contentAlignment = Alignment.Center) {
        SubtleLoadingIndicator(message = stringResource(Res.string.loading_feed), getColour = { theme.on_background })
    }
}
