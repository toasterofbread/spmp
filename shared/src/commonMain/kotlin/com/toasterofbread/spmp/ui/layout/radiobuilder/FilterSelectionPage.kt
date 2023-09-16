package com.toasterofbread.spmp.ui.layout.radiobuilder

import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistData
import com.toasterofbread.spmp.platform.composable.platformClickable
import com.toasterofbread.spmp.platform.vibrateShort
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.ErrorInfoDisplay
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewLong
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.spmp.youtubeapi.RadioBuilderArtist
import com.toasterofbread.spmp.youtubeapi.RadioBuilderModifier
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.InvalidRadioException
import com.toasterofbread.utils.composable.ShapedIconButton
import com.toasterofbread.utils.composable.SubtleLoadingIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun FilterSelectionPage(
    selected_artists: Collection<Int>,
    artists: List<RadioBuilderArtist>,
    content_padding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val player = LocalPlayerState.current

    val builder_endpoint = player.context.ytapi.RadioBuilder
    check(builder_endpoint.isImplemented())

    var is_loading by remember { mutableStateOf(false) }
    var preview_loading by remember { mutableStateOf(false) }
    var preview_playlist: RemotePlaylistData? by remember { mutableStateOf(null) }
    var load_error: Throwable? by remember { mutableStateOf(null) }
    val coroutine_scope = rememberCoroutineScope()

    Column(modifier) {
        val selection_type = remember { mutableStateOf(RadioBuilderModifier.SelectionType.BLEND) }
        SelectionTypeRow(selection_type)

        val artist_variety = remember { mutableStateOf(RadioBuilderModifier.Variety.MEDIUM) }
        ArtistVarietyRow(artist_variety)

        val filter_a: MutableState<RadioBuilderModifier.FilterA?> = remember { mutableStateOf(null) }
        FilterARow(filter_a)

        val filter_b: MutableState<RadioBuilderModifier.FilterB?> = remember { mutableStateOf(null) }
        FilterBRow(filter_b)

        fun loadRadio(preview: Boolean) {
            if (is_loading || preview_loading) {
                return
            }

            val selected_radio_artists: Set<RadioBuilderArtist> = selected_artists.map { artists[it] }.toSet()
            val radio_token = builder_endpoint.buildRadioToken(
                selected_radio_artists,
                setOf(selection_type.value, artist_variety.value, filter_a.value, filter_b.value)
            )

            load_error = null
            if (preview) {
                preview_loading = true
            }
            else if (preview_playlist?.id == radio_token) {
                player.player?.startRadioAtIndex(0, preview_playlist)
                return
            }
            else {
                is_loading = true
            }

            coroutine_scope.launch {
                val result = builder_endpoint.getBuiltRadio(radio_token, player.context)
                result.fold(
                    { playlist: RemotePlaylistData? ->
                        if (playlist == null) {
                            load_error = InvalidRadioException()
                        }
                        else {
                            withContext(Dispatchers.IO) {
                                setRadioMetadata(playlist, artists, selected_artists)
                                playlist.saveToDatabase(player.database)
                            }

                            if (preview) {
                                preview_playlist = playlist
                            }
                            else {
                                withContext(Dispatchers.Main) {
                                    player.player?.startRadioAtIndex(0, playlist)
                                }
                            }
                        }
                    },
                    { error ->
                        load_error = error
                        preview_playlist = null
                    }
                )

                is_loading = false
                preview_loading = false
            }
        }

        Box(Modifier.fillMaxSize().weight(1f)) {
            var action_buttons_visible: Boolean by remember { mutableStateOf(true) }

            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Crossfade(Triple(preview_loading, preview_playlist, load_error), Modifier.fillMaxSize()) {
                    val (loading, playlist, error) = it
                    if (error != null) {
                        Column(
                            Modifier
                                .fillMaxSize()
                                .padding(content_padding),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            var show_error: Boolean by remember { mutableStateOf(false) }

                            Text(
                                getString("radio_builder_no_songs_match_criteria"),
                                Modifier
                                    .padding(10.dp)
                                    .platformClickable(
                                        onAltClick = {
                                            show_error = !show_error
                                            player.context.vibrateShort()
                                        }
                                    )
                            )

                            AnimatedVisibility(show_error) {
                                ErrorInfoDisplay(error, Modifier.fillMaxWidth())
                            }
                        }
                    }
                    else if (loading) {
                        Box(Modifier.fillMaxSize().padding(content_padding), contentAlignment = Alignment.Center) {
                            SubtleLoadingIndicator { Theme.on_background }
                        }
                    }
                    else {
                        val items = playlist?.items
                        if (items != null) {
                            val multiselect_context = remember { MediaItemMultiSelectContext() {} }

                            DisposableEffect(multiselect_context.is_active) {
                                action_buttons_visible = !multiselect_context.is_active
                                onDispose {
                                    action_buttons_visible = true
                                }
                            }

                            Column(Modifier.fillMaxWidth()) {
                                AnimatedVisibility(multiselect_context.is_active) {
                                    multiselect_context.InfoDisplay()
                                }

                                LazyColumn(
                                    Modifier.fillMaxSize(),
                                    contentPadding = content_padding,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(items) { item ->
                                        MediaItemPreviewLong(item, Modifier.fillMaxWidth(), multiselect_context = multiselect_context)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            this@Column.AnimatedVisibility(
                action_buttons_visible,
                Modifier.align(Alignment.TopEnd),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val icon_button_colours = IconButtonDefaults.iconButtonColors(
                        containerColor = Theme.accent,
                        contentColor = Theme.on_accent
                    )
                    ShapedIconButton({ loadRadio(false) }, colours = icon_button_colours) {
                        Crossfade(is_loading) { loading ->
                            if (loading) {
                                SubtleLoadingIndicator(getColour = { Theme.on_accent })
                            } else {
                                Icon(Icons.Filled.PlayArrow, null)
                            }
                        }
                    }
                    ShapedIconButton({ loadRadio(true) }, colours = icon_button_colours) {
                        Icon(Icons.Filled.RemoveRedEye, null)
                    }
                }
            }
        }
    }
}

private fun setRadioMetadata(radio_playlist: RemotePlaylistData, artists: List<RadioBuilderArtist>, selected_artists: Collection<Int>) {
    val included_artists = selected_artists.take(2)
    val splitter = getString("radio_title_artists_splitter")

    var artists_string = included_artists.joinToString(splitter) { index ->
        artists[index].name
    }

    if (selected_artists.size > included_artists.size || included_artists.isEmpty()) {
        artists_string += getString("radio_title_overflow")
    }

    radio_playlist.title = getString("radio_title_of_\$artists").replace("\$artists", artists_string)
}
