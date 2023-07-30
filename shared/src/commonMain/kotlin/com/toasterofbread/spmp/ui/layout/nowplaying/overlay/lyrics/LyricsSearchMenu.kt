package com.toasterofbread.spmp.ui.layout.nowplaying.overlay.lyrics

import SpMp
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.toasterofbread.spmp.api.lyrics.LyricsSource
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.Artist
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.utils.composable.OnChangedEffect
import com.toasterofbread.utils.setAlpha
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import com.toasterofbread.spmp.platform.LargeDropdownMenu

private const val LYRICS_SEARCH_RETRY_COUNT = 3

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun LyricsSearchMenu(song: Song, modifier: Modifier = Modifier, close: (changed: Boolean) -> Unit) {
    val db = SpMp.context.database

    val song_title: String? by song.Title.observe(db)
    val song_artist_title: String? = song.Artist.observeOn(db) {
        it?.Title
    }

    val on_accent = Theme.on_accent
    val accent = Theme.accent

    val load_lock = remember { Object() }
    var loading by remember { mutableStateOf(false) }

    val text_field_colours = TextFieldDefaults.textFieldColors(
        containerColor = accent.setAlpha(0.75f),
        textColor = on_accent,
        focusedLabelColor = on_accent,
        unfocusedLabelColor = on_accent,
        focusedTrailingIconColor = on_accent,
        unfocusedTrailingIconColor = on_accent,
        cursorColor = on_accent,
        focusedIndicatorColor = accent,
        unfocusedIndicatorColor = accent.setAlpha(0.5f)
    )

    val focus = LocalFocusManager.current
    val keyboard_controller = LocalSoftwareKeyboardController.current

    val title = remember (song_title) { mutableStateOf(TextFieldValue(song_title ?: "")) }
    val artist = remember (song_artist_title) { mutableStateOf(TextFieldValue(song_artist_title ?: "")) }
    var search_state: Boolean by remember { mutableStateOf(false) }
    var selected_source_idx: Int by remember { mutableStateOf(Settings.KEY_LYRICS_DEFAULT_SOURCE.get()) }

    var search_results: Pair<List<LyricsSource.SearchResult>, Int>? by remember { mutableStateOf(null) }
    var edit_page_open by remember { mutableStateOf(true) }

    OnChangedEffect(search_state) {
        keyboard_controller?.hide()

        withContext(Dispatchers.IO) {
            synchronized(load_lock) {
                check(!loading)
                loading = true
            }

            var result: Result<List<LyricsSource.SearchResult>>? = null
            var retry_count = LYRICS_SEARCH_RETRY_COUNT
            val source = LyricsSource.fromIdx(selected_source_idx)

            while (retry_count-- > 0) {
                result = source.searchForLyrics(title.value.text, if (artist.value.text.trim().isEmpty()) null else artist.value.text)
                
                val error = result.exceptionOrNull() ?: break
                if (error !is IOException) {
                    SpMp.reportActionError(error)
                    break
                }
            }

            result?.fold(
                {
                    search_results = Pair(it, source.source_idx)
                    if (search_results?.first?.isNotEmpty() == true) {
                        edit_page_open = false
                    }
                    else {
                        SpMp.context.sendToast(getString("lyrics_none_found"))
                    }
                },
                { SpMp.reportActionError(it) }
            )

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
                    var source_selector_open: Boolean by remember { mutableStateOf(false) }
                    Row(
                        Modifier
                            .background(Theme.accent, CircleShape)
                            .padding(10.dp)
                            .clickable { source_selector_open = !source_selector_open },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ArrowDropDown, null, tint = on_accent)

                        Text(
                            remember(selected_source_idx) {
                                getString("lyrics_search_on_\$source")
                                    .replace("\$source", LyricsSource.fromIdx(selected_source_idx).getReadable())
                            },
                            color = on_accent
                        )

                        LargeDropdownMenu(
                            source_selector_open,
                            { source_selector_open = false },
                            LyricsSource.SOURCE_AMOUNT,
                            selected_source_idx,
                            { source_idx ->
                                LyricsSource.fromIdx(source_idx).getReadable()
                            },
                            selected_item_colour = Theme.vibrant_accent
                        ) { source_idx ->
                            selected_source_idx = source_idx
                            source_selector_open = false
                        }
                    }

                    @Composable
                    fun Field(state: MutableState<TextFieldValue>, label: String) {
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
                                IconButton({ state.value = TextFieldValue() }) {
                                    Icon(Icons.Default.Close, null)
                                }
                            },
                            colors = text_field_colours,
                            modifier = Modifier.clickable {}
                        )
                    }

                    Field(title, getString("song_name"))
                    Field(artist, getString("artist"))
                }
            }
            else if (search_results != null) {
                val results = search_results!!
                LyricsSearchResults(results) { index ->
                    if (index != null) {
                        val selected = results.first[index]
                        val lyrics_source = results.second.toLong()

                        val current_lyrics = SpMp.context.database.songQueries
                            .lyricsById(song.id)
                            .executeAsOne()

                        if (selected.id != current_lyrics.lyrics_id || lyrics_source != current_lyrics.lyrics_source) {
                            SpMp.context.database.songQueries.updateLyricsById(
                                lyrics_source = lyrics_source,
                                lyrics_id = selected.id,
                                id = song.id
                            )
                            close(true)
                        }
                        else {
                            close(false)
                        }
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
                        color = Theme.accent,
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
                        containerColor = Theme.accent,
                        contentColor = on_accent
                    )
                ) {
                    Text(getString("action_close"))
                }

                IconButton(
                    {
                        if (edit_page_open) {
                            search_state = !search_state
                        }
                        else {
                            edit_page_open = true
                        }
                    },
                    Modifier.background(Theme.accent, CircleShape).requiredSize(40.dp),
                ) {
                    Crossfade(if (loading) 0 else if (edit_page_open) 1 else 2) { icon ->
                        when (icon) {
                            0 -> CircularProgressIndicator(Modifier.requiredSize(22.dp), color = on_accent, strokeWidth = 3.dp)
                            else -> {
                                Icon(
                                    if (icon == 1) Icons.Default.Search else Icons.Default.Edit,
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
