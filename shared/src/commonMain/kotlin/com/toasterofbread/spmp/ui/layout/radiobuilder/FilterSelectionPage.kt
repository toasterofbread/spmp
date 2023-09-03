package com.toasterofbread.spmp.ui.layout.radiobuilder

import LocalPlayerState
import SpMp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistData
import com.toasterofbread.spmp.resources.getString
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
) {
    val player = LocalPlayerState.current

    val builder_endpoint = player.context.ytapi.RadioBuilder
    check(builder_endpoint.isImplemented())

    var is_loading by remember { mutableStateOf(false) }
    var preview_loading by remember { mutableStateOf(false) }
    var preview_playlist: RemotePlaylistData? by remember { mutableStateOf(null) }
    var invalid_modifiers: Boolean by remember { mutableStateOf(false) }
    val coroutine_scope = rememberCoroutineScope()

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

        val radio_token = builder_endpoint.buildRadioToken(
            selected_artists.map { artists[it] }.toSet(),
            setOf(selection_type.value, artist_variety.value, filter_a.value, filter_b.value)
        )

        invalid_modifiers = false
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
            val result = builder_endpoint.getBuiltRadio(radio_token, SpMp.context)
            result.fold(
                { playlist ->
                    if (playlist == null) {
                        invalid_modifiers = true
                    }
                    else {
                        setRadioMetadata(playlist, artists, selected_artists)
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
                    if (error is InvalidRadioException) {
                        invalid_modifiers = true
                        preview_playlist = null
                    }
                    else {
                        SpMp.error_manager.onError("radio_builder_load_radio", error)
                    }
                }
            )

            is_loading = false
            preview_loading = false
        }
    }

    Box {
        var action_buttons_visible: Boolean by remember { mutableStateOf(true) }

        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Crossfade(Triple(preview_loading, preview_playlist, invalid_modifiers)) {
                val (loading, playlist, invalid) = it
                if (invalid) {
                    Column(
                        Modifier
                            .fillMaxHeight()
                            .padding(content_padding),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(getString("radio_builder_no_songs_match_criteria"), Modifier.padding(10.dp))

                        Row {
                            SpMp.context.CopyShareButtons(name = "") {
                                builder_endpoint.buildRadioToken(
                                    selected_artists.map { i -> artists[i] }.toSet(),
                                    setOf(selection_type.value, artist_variety.value, filter_a.value, filter_b.value)
                                )
                            }
                        }
                    }
                }
                else if (loading) {
                    SubtleLoadingIndicator(Modifier.offset(y = -content_padding.calculateBottomPadding())) { Theme.on_background }
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

                        Column {
                            AnimatedVisibility(multiselect_context.is_active) {
                                multiselect_context.InfoDisplay()
                            }
                            LazyColumn(
                                Modifier.fillMaxSize(),
                                contentPadding = content_padding,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(items) { item ->
                                    MediaItemPreviewLong(item, multiselect_context = multiselect_context)
                                }
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            action_buttons_visible,
            Modifier.align(Alignment.TopEnd),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column {
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

private fun setRadioMetadata(radio_playlist: RemotePlaylistData, artists: List<RadioBuilderArtist>, selected_artists: Collection<Int>) {
    assert(selected_artists.isNotEmpty())

    val artists_string = StringBuilder()
    val splitter = getString("radio_title_artists_splitter")

    for (artist in selected_artists.withIndex()) {
        artists_string.append(artists[artist.value].name)
        if (artist.index + 1 < selected_artists.size) {
            artists_string.append(splitter)
        }
    }

    radio_playlist.title = getString("radio_of_\$artists_title").replace("\$artists", artists_string.toString())
}
