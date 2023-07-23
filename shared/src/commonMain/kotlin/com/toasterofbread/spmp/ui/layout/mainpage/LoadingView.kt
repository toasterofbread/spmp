package com.toasterofbread.spmp.ui.layout.mainpage

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.utils.composable.SubtleLoadingIndicator

@Composable
fun MainPageLoadingView(modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        SubtleLoadingIndicator(message = getString("loading_feed"), getColour = Theme.on_background_provider)
    }
}
