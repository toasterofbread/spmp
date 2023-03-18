package com.spectre7.spmp.ui.layout

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.spectre7.spmp.api.RadioBuilderArtist
import com.spectre7.spmp.ui.component.PillMenu
import com.spectre7.utils.getString

@Composable
fun RadioBuilderPage(
    pill_menu: PillMenu,
    playerProvider: () -> PlayerViewContext,
    close: () -> Unit,
) {
    var selected_artists: List<RadioBuilderArtist>? by remember { mutableStateOf(null) }

    Crossfade(selected_artists) { artists ->
        Column(Modifier.fillMaxSize()) {
            Text(getString("Radio builder"), fontSize = 20.sp)

            if (artists == null) {
                Text(getString("Select artists"), fontSize = 25.sp)

            }
            else {

            }
        }
    }
}