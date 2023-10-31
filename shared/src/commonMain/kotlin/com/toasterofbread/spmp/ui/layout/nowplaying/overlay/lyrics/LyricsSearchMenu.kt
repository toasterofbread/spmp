package com.toasterofbread.spmp.ui.layout.nowplaying.overlay.lyrics

import LocalPlayerState
import SpMp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CommentsDisabled
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.db.observePropertyActiveTitle
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.toastercomposetools.utils.composable.LargeDropdownMenu
import com.toasterofbread.toastercomposetools.platform.composable.PlatformAlertDialog
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.toastercomposetools.settings.ui.Theme
import com.toasterofbread.spmp.youtubeapi.lyrics.LyricsReference
import com.toasterofbread.spmp.youtubeapi.lyrics.LyricsSource
import com.toasterofbread.toastercomposetools.utils.common.getValue
import com.toasterofbread.toastercomposetools.utils.composable.OnChangedEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

private const val LYRICS_SEARCH_RETRY_COUNT = 3

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LyricsSearchMenu(
    song: Song,
    modifier: Modifier = Modifier,
    close: (changed: Boolean) -> Unit,
) {
    val player = LocalPlayerState.current
    val db = player.context.database

    val song_title: String? by song.observeActiveTitle()
    val song_artist_title: String? by song.Artist.observePropertyActiveTitle()

    val on_accent = player.theme.on_accent
    val accent = player.theme.accent

    val load_lock = remember { Object() }
    var loading by remember { mutableStateOf(false) }

    val text_field_colours = TextFieldDefaults.colors(
        focusedContainerColor = accent.copy(alpha = 0.75f),
        unfocusedContainerColor = accent.copy(alpha = 0.75f),
        focusedTextColor = on_accent,
        unfocusedTextColor = on_accent,
        focusedLabelColor = on_accent,
        unfocusedLabelColor = on_accent,
        focusedTrailingIconColor = on_accent,
        unfocusedTrailingIconColor = on_accent,
        cursorColor = on_accent,
        focusedIndicatorColor = accent,
        unfocusedIndicatorColor = accent.copy(alpha = 0.5f)
    )

    val focus = LocalFocusManager.current
    val keyboard_controller = LocalSoftwareKeyboardController.current

    val title = remember (song_title) { mutableStateOf(TextFieldValue(song_title ?: "")) }
    val artist = remember (song_artist_title) { mutableStateOf(TextFieldValue(song_artist_title ?: "")) }
    var search_state: Boolean by remember { mutableStateOf(false) }

    var selected_source: LyricsSource by remember {
        mutableStateOf(
            LyricsSource.fromIdx(Settings.KEY_LYRICS_DEFAULT_SOURCE.get())
        )
    }

    var search_results: Pair<List<LyricsSource.SearchResult>, Int>? by remember { mutableStateOf(null) }
    var edit_page_open by remember { mutableStateOf(true) }

    OnChangedEffect(search_state) {
        keyboard_controller?.hide()

        withContext(Dispatchers.IO) {
            synchronized(load_lock) {
                check(!loading)
                loading = true
            }

            if (selected_source.supportsLyricsBySearching()) {
                var result: Result<List<LyricsSource.SearchResult>>? = null
                var retry_count = LYRICS_SEARCH_RETRY_COUNT

                while (retry_count-- > 0) {
                    result = selected_source.searchForLyrics(
                        title.value.text,
                        if (artist.value.text.trim().isEmpty()) null else artist.value.text
                    )

                    val error = result.exceptionOrNull() ?: break
                    if (error !is IOException) {
                        SpMp.reportActionError(error)
                        break
                    }
                }

                result?.fold(
                    {
                        search_results = Pair(it, selected_source.source_index)
                        if (search_results?.first?.isNotEmpty() == true) {
                            edit_page_open = false
                        }
                        else {
                            player.context.sendToast(getString("lyrics_none_found"))
                        }
                    },
                    { SpMp.reportActionError(it) }
                )
            }
            else if (selected_source.supportsLyricsBySong()) {
                selected_source.getReferenceBySong(song, player.context).fold(
                    { lyrics_reference ->
                        if (lyrics_reference != song.Lyrics.get(db)) {
                            song.Lyrics.set(lyrics_reference, db)
                            close(true)
                        }
                        else {
                            close(false)
                        }
                    },
                    { error ->
                        SpMp.reportActionError(error)
                    }
                )
            }
            else {
                throw NotImplementedError(selected_source::class.toString())
            }

            synchronized(load_lock) {
                loading = false
            }
        }
    }

    Crossfade(edit_page_open, modifier) { edit_page ->
        Column(Modifier.padding(15.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (edit_page) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(15.dp)
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    Row(Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        var source_selector_open: Boolean by remember { mutableStateOf(false) }
                        Row(
                            Modifier
                                .background(player.theme.accent, CircleShape)
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(10.dp)
                                .clickable { source_selector_open = !source_selector_open },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ArrowDropDown, null, tint = on_accent)

                            Text(
                                remember(selected_source) {
                                    getString("lyrics_search_on_\$source")
                                        .replace("\$source", selected_source.getReadable())
                                },
                                color = on_accent
                            )

                            LargeDropdownMenu(
                                source_selector_open,
                                { source_selector_open = false },
                                LyricsSource.SOURCE_AMOUNT,
                                selected_source.source_index,
                                { source_idx ->
                                    remember(source_idx) {
                                        LyricsSource.fromIdx(source_idx).getReadable()
                                    }
                                },
                                selected_border_colour = player.theme.vibrant_accent
                            ) { source_idx ->
                                selected_source = LyricsSource.fromIdx(source_idx)
                                source_selector_open = false
                            }
                        }

                        var confirming_no_lyrics: Boolean by remember { mutableStateOf(false) }
                        IconButton(
                            {
                                confirming_no_lyrics = !confirming_no_lyrics
                            },
                            Modifier.background(player.theme.accent, CircleShape).fillMaxHeight().aspectRatio(1f),
                        ) {
                            Icon(
                                Icons.Default.CommentsDisabled,
                                null,
                                tint = on_accent
                            )
                        }

                        if (confirming_no_lyrics) {
                            PlatformAlertDialog(
                                onDismissRequest = { confirming_no_lyrics = false },
                                confirmButton = {
                                    Button({ song.Lyrics.set(LyricsReference.NONE, player.database) }) {
                                        Text(getString("action_confirm_action"))
                                    }
                                },
                                dismissButton = {
                                    Button({ confirming_no_lyrics = false }) {
                                        Text(getString("action_cancel"))
                                    }
                                },
                                title = {
                                    Text(getString("prompt_confirm_action"))
                                },
                                text = {
                                    Text(getString("lyrics_no_lyrics_set_confirmation_title"))
                                },
                                icon = {
                                    Icon(Icons.Default.CommentsDisabled, null)
                                }
                            )
                        }
                    }

                    @Composable
                    fun Field(state: MutableState<TextFieldValue>, label: String, enabled: Boolean) {
                        TextField(
                            state.value,
                            { state.value = it },
                            label = { Text(label) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                focus.clearFocus()
                            }),
                            trailingIcon = {
                                IconButton({ state.value = TextFieldValue() }, enabled = selected_source.supportsLyricsBySearching()) {
                                    Icon(Icons.Default.Close, null)
                                }
                            },
                            colors = text_field_colours,
                            modifier = Modifier.clickable {},
                            enabled = enabled
                        )
                    }

                    Crossfade(selected_source.supportsLyricsBySearching()) { search_supported ->
                        Column {
                            Field(title, getString("song_name"), search_supported)
                            Spacer(Modifier.height(10.dp))
                            Field(artist, getString("artist"), search_supported)
                        }
                    }

                    AnimatedVisibility(!selected_source.supportsLyricsBySearching()) {
                        Text(
                            getString("lyrics_source_cannot_search"),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
            else if (search_results != null) {
                val results = search_results ?: Pair(emptyList(), 0)
                LyricsSearchResults(results) { index ->
                    if (index == null) {
                        close(false)
                        return@LyricsSearchResults
                    }

                    val selected = results.first[index]
                    val lyrics_source = results.second

                    val current_lyrics = song.Lyrics.get(player.database)

                    if (selected.id != current_lyrics?.id || lyrics_source != current_lyrics.source_index) {
                        song.Lyrics.set(LyricsReference(lyrics_source, selected.id), player.database)
                        close(true)
                    }
                    else {
                        close(false)
                    }

                    search_results = null
                }
            }
            else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        Modifier.requiredSize(22.dp),
                        color = player.theme.accent,
                        strokeWidth = 3.dp
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    { close(false) },
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = player.theme.accent,
                        contentColor = on_accent
                    )
                ) {
                    Text(getString("action_close"))
                }

                IconButton(
                    {
                        if (edit_page_open) {
                            if (!loading) {
                                search_state = !search_state
                            }
                        }
                        else {
                            edit_page_open = true
                        }
                    },
                    Modifier.background(player.theme.accent, CircleShape).requiredSize(40.dp),
                ) {
                    Crossfade(
                        // Loading
                        if (loading) 0
                        else if (edit_page_open) (
                            // Search
                            if (selected_source.supportsLyricsBySearching()) 1
                            // Load directly
                            else 2
                        )
                        // Open edit page
                        else 3
                    ) { icon ->
                        when (icon) {
                            0 -> CircularProgressIndicator(Modifier.requiredSize(22.dp), color = on_accent, strokeWidth = 3.dp)
                            else -> {
                                Icon(
                                    when (icon) {
                                        1 -> Icons.Default.Search
                                        2 -> Icons.Default.Download
                                        else -> Icons.Default.Edit
                                    },
                                    null,
                                    tint = on_accent
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
