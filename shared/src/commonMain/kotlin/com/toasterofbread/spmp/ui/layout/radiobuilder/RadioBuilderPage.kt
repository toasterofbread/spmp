package com.toasterofbread.spmp.ui.layout.radiobuilder

import SpMp
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.toasterofbread.spmp.api.RadioBuilderArtist
import com.toasterofbread.spmp.api.getRadioBuilderArtists
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.platform.composable.BackHandler
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.MusicTopBar
import com.toasterofbread.spmp.ui.component.PillMenu
import com.toasterofbread.spmp.ui.component.WaveBorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

const val RADIO_BUILDER_ICON_WIDTH = 35f

@Composable
fun RadioBuilderPage(
    pill_menu: PillMenu,
    bottom_padding: Dp,
    modifier: Modifier = Modifier,
    close: () -> Unit
) {
    var artists_result: Result<List<RadioBuilderArtist>>? by remember { mutableStateOf(null) }
    var selected_artists: Set<Int>? by remember { mutableStateOf(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            artists_result = getRadioBuilderArtists { thumbnails ->
                thumbnails.maxBy { it.width * it.height }
            }
        }
    }

    Column(modifier.padding(horizontal = 10.dp)) {
        MusicTopBar(
            Settings.KEY_LYRICS_SHOW_IN_RADIOBUILDER,
            Modifier.fillMaxWidth()
        )

        Crossfade(selected_artists) { selected ->
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(Modifier.padding(vertical = 10.dp).fillMaxWidth().zIndex(10f)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioBuilderIcon()

                        Text(
                            getString(
                                if (selected == null) "radio_builder_artists_title"
                                else "radio_builder_modifiers_title"
                            ),
                            style = MaterialTheme.typography.headlineMedium
                        )

                        Spacer(Modifier.width(RADIO_BUILDER_ICON_WIDTH.dp))
                    }

                    if (selected == null) {
                        WaveBorder(Modifier.fillMaxWidth(), getOffset = { it })
                    }
                }

                if (selected == null) {
                    if (artists_result?.isFailure == true) {
                        // TODO
                        SpMp.ErrorDisplay(artists_result?.exceptionOrNull())
                    }
                    else {
                        RadioArtistSelector(artists_result?.getOrNull(), pill_menu, Modifier.fillMaxSize()) { selected_artists = it.toSet() }
                    }
                }
                else {
                    BackHandler {
                        selected_artists = null
                    }

                    FilterSelectionPage(
                        selected,
                        artists_result!!.getOrThrow(),
                        bottom_padding
                    )
                }
            }
        }
    }
}
