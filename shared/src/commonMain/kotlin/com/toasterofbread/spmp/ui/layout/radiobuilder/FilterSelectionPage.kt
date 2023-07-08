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
import com.toasterofbread.spmp.api.InvalidRadioException
import com.toasterofbread.spmp.api.RadioBuilderArtist
import com.toasterofbread.spmp.api.RadioModifier
import com.toasterofbread.spmp.api.buildRadioToken
import com.toasterofbread.spmp.api.getBuiltRadio
import com.toasterofbread.spmp.model.mediaitem.MediaItemPreviewParams
import com.toasterofbread.spmp.model.mediaitem.Playlist
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.utils.composable.ShapedIconButton
import com.toasterofbread.utils.composable.SubtleLoadingIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun FilterSelectionPage(
    selected_artists: Collection<Int>,
    artists: List<RadioBuilderArtist>,
    bottom_padding: Dp = 0.dp,
) {
    val player = LocalPlayerState.current

    val selection_type = remember { mutableStateOf(RadioModifier.SelectionType.BLEND) }
    SelectionTypeRow(selection_type)

    val artist_variety = remember { mutableStateOf(RadioModifier.Variety.MEDIUM) }
    ArtistVarietyRow(artist_variety)

    val filter_a: MutableState<RadioModifier.FilterA?> = remember { mutableStateOf(null) }
    FilterARow(filter_a)

    val filter_b: MutableState<RadioModifier.FilterB?> = remember { mutableStateOf(null) }
    FilterBRow(filter_b)

    var is_loading by remember { mutableStateOf(false) }
    var preview_loading by remember { mutableStateOf(false) }
    var preview_playlist: Playlist? by remember { mutableStateOf(null) }
    var invalid_modifiers: Boolean by remember { mutableStateOf(false) }
    val coroutine_scope = rememberCoroutineScope()

    fun loadRadio(preview: Boolean) {
        if (is_loading || preview_loading) {
            return
        }

        val radio_token = buildRadioToken(
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
            val result = getBuiltRadio(radio_token)
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
                            .padding(bottom = bottom_padding),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(getString("radio_builder_no_songs_match_criteria"), Modifier.padding(10.dp))

                        Row {
                            SpMp.context.CopyShareButtons(name = "") {
                                buildRadioToken(
                                    selected_artists.map { i -> artists[i] }.toSet(),
                                    setOf(selection_type.value, artist_variety.value, filter_a.value, filter_b.value)
                                )
                            }
                        }
                    }
                } else if (loading) {
                    SubtleLoadingIndicator(Modifier.offset(y = -bottom_padding), { Theme.current.on_background })
                } else if (playlist?.feed_layouts?.isNotEmpty() == true) {
                    val layout = playlist.feed_layouts!!.first()
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
                            contentPadding = PaddingValues(bottom = bottom_padding),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(layout.items) { item ->
                                item.PreviewLong(MediaItemPreviewParams(multiselect_context = multiselect_context))
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
                    containerColor = Theme.current.accent,
                    contentColor = Theme.current.on_accent
                )
                ShapedIconButton({ loadRadio(false) }, colors = icon_button_colours) {
                    Crossfade(is_loading) { loading ->
                        if (loading) {
                            SubtleLoadingIndicator(colourProvider = { Theme.current.on_accent })
                        } else {
                            Icon(Icons.Filled.PlayArrow, null)
                        }
                    }
                }
                ShapedIconButton({ loadRadio(true) }, colors = icon_button_colours) {
                    Icon(Icons.Filled.RemoveRedEye, null)
                }
            }
        }
    }
}

private fun setRadioMetadata(radio_playlist: Playlist, artists: List<RadioBuilderArtist>, selected_artists: Collection<Int>) {
    assert(selected_artists.isNotEmpty())

    val artists_string = StringBuilder()
    val splitter = getString("radio_title_artists_splitter")

    for (artist in selected_artists.withIndex()) {
        artists_string.append(artists[artist.value].name)
        if (artist.index + 1 < selected_artists.size) {
            artists_string.append(splitter)
        }
    }

    radio_playlist.editData {
        supplyTitle(getString("radio_of_\$artists_title").replace("\$artists", artists_string.toString()))
    }
}
