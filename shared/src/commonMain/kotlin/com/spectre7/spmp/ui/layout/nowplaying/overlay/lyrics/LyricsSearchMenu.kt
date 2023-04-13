package com.spectre7.spmp.ui.layout.nowplaying.overlay.lyrics

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.R
import com.spectre7.spmp.api.LyricsSearchResult
import com.spectre7.spmp.api.searchForLyrics
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.ui.theme.Theme
import com.spectre7.utils.getStringTemp
import com.spectre7.utils.sendToast
import com.spectre7.utils.setAlpha
import kotlin.concurrent.thread

@Composable
fun LyricsSearchMenu(song: Song, close: (changed: Boolean) -> Unit) {

    val on_accent = Theme.current.on_accent
    val accent = Theme.current.accent

    val check_lock = remember { Object() }
    var checking by remember { mutableStateOf(false) }

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

    var title = remember (song.title) { mutableStateOf(TextFieldValue(song.title ?: "")) }
    var artist = remember (song.artist?.title) { mutableStateOf(TextFieldValue(song.artist?.title ?: "")) }

    var search_results: List<LyricsSearchResult>? by remember { mutableStateOf(null) }
    var edit_page_open by remember { mutableStateOf(true) }

    fun performSearch() {
        thread {
            synchronized(check_lock) {
                if (checking) {
                    throw IllegalStateException()
                }
                checking = true
            }

            val result = searchForLyrics(title.value.text, if (artist.value.text.trim().isEmpty()) null else artist.value.text)
            result.fold(
                {
                    search_results = it
                },
                {
                    MainActivity.error_manager.onError("performLyricsSearch", it)
                }
            )

            synchronized(check_lock) {
                checking = false

                if (search_results?.isNotEmpty() == true) {
                    edit_page_open = false
                }
                else if (result.isSuccess) {
                    sendToast(getString("no_lyrics_found))
                }
            }
        }
    }

    BackHandler {
        close(false)
    }

    Crossfade(edit_page_open) { edit_page ->
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (edit_page) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(15.dp)
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    Box(
                        Modifier
                            .background(Theme.current.accent, CircleShape)
                            .padding(10.dp)) {
                        Text(getStringTemp("Search for lyrics"), color = on_accent)
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
                                    Icon(Icons.Filled.Close, null)
                                }
                            },
                            colors = text_field_colours,
                            modifier = Modifier.clickable {}
                        )
                    }

                    Field(title, com.spectre7.utils.getString("song_name))
                    Field(artist, com.spectre7.utils.getString("artist))
                }
            }
            else if (search_results != null) {
                LyricsSearchResults(search_results!!) { index ->
                    if (index != null) {
                        val selected = search_results!![index]
                        if (selected.id != song.song_reg_entry.lyrics_id || selected.source != song.song_reg_entry.lyrics_source) {
                            song.song_reg_entry.lyrics_id = selected.id
                            song.song_reg_entry.lyrics_source = selected.source
                            song.saveRegistry()
                            close(true)
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
                        color = Theme.current.accent,
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
                        containerColor = Theme.current.accent,
                        contentColor = on_accent
                    )
                ) {
                    Text(getString("action_close))
                }

                IconButton(
                    {
                        if (edit_page_open) {
                            performSearch()
                        }
                        else {
                            edit_page_open = true
                        }
                    },
                    Modifier.background(Theme.current.accent, CircleShape).requiredSize(40.dp),
                ) {
                    Crossfade(if (checking) 0 else if (edit_page_open) 1 else 2) { icon ->
                        when (icon) {
                            0 -> CircularProgressIndicator(Modifier.requiredSize(22.dp), color = on_accent, strokeWidth = 3.dp)
                            else -> {
                                Icon(
                                    if (icon == 1) Icons.Filled.Search else Icons.Filled.Edit,
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
