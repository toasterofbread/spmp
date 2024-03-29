package com.toasterofbread.spmp.ui.layout.radiobuilder

import LocalPlayerState
import SpMp.isDebugBuild
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.toasterofbread.composekit.platform.composable.BackHandler
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.ErrorInfoDisplay
import com.toasterofbread.spmp.ui.component.PillMenu
import com.toasterofbread.spmp.ui.component.WaveBorder
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import dev.toastbits.ytmkt.endpoint.RadioBuilderArtist
import dev.toastbits.ytmkt.endpoint.RadioBuilderEndpoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

const val RADIO_BUILDER_ICON_WIDTH_DP = 37f

@Composable
fun RadioBuilderPage(
    content_padding: PaddingValues,
    modifier: Modifier = Modifier,
    close: () -> Unit
) {
    var artists_result: Result<List<RadioBuilderArtist>>? by remember { mutableStateOf(null) }
    var selected_artists: Set<Int>? by remember { mutableStateOf(null) }

    val player: PlayerState = LocalPlayerState.current
    val builder_endpoint: RadioBuilderEndpoint = player.context.ytapi.RadioBuilder
    check(builder_endpoint.isImplemented())

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            artists_result = builder_endpoint.getRadioBuilderArtists { thumbnails ->
                thumbnails.maxBy { it.width * it.height }
            }
        }
    }

    Box(modifier) {
        val pill_menu = remember {
            PillMenu(follow_player = true)
        }
        pill_menu.PillMenu()

        Column(
            Modifier
                .fillMaxSize()
                .padding(
                    start = content_padding.calculateStartPadding(LocalLayoutDirection.current),
                    end = content_padding.calculateEndPadding(LocalLayoutDirection.current),
                    top = content_padding.calculateTopPadding()
                )
        ) {
            Crossfade(selected_artists, Modifier.fillMaxSize()) { selected ->
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

                            Spacer(Modifier.width(RADIO_BUILDER_ICON_WIDTH_DP.dp))
                        }

                        if (selected == null) {
                            WaveBorder(Modifier.fillMaxWidth(), getOffset = { it })
                        }
                    }

                    if (selected == null) {
                        if (artists_result?.isFailure == true) {
                            artists_result?.exceptionOrNull()?.also {
                                ErrorInfoDisplay(it, isDebugBuild()) { artists_result = null }
                            }
                        }
                        else {
                            RadioArtistSelector(artists_result?.getOrNull(), pill_menu, Modifier.fillMaxSize()) { selected ->
                                if (selected == null) {
                                    close()
                                }
                                else {
                                    selected_artists = selected.toSet()
                                }
                            }
                        }
                    }
                    else {
                        BackHandler {
                            selected_artists = null
                        }

                        FilterSelectionPage(
                            selected,
                            artists_result!!.getOrThrow(),
                            PaddingValues(bottom = content_padding.calculateBottomPadding()),
                            Modifier.fillMaxSize().weight(1f)
                        )
                    }
                }
            }
        }
    }
}
