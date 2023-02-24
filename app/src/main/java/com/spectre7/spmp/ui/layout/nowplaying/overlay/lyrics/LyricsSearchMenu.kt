package com.spectre7.spmp.ui.layout.nowplaying.overlay.lyrics

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
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
import com.spectre7.spmp.R
import com.spectre7.spmp.api.LyricsSearchResult
import com.spectre7.spmp.api.getOrThrowHere
import com.spectre7.spmp.api.searchForLyrics
import com.spectre7.spmp.model.Song
import com.spectre7.utils.Theme
import com.spectre7.utils.setAlpha
import kotlin.concurrent.thread

val check_lock = Object()
var checking by mutableStateOf(false)

@Composable
fun LyricsSearchMenu(song: Song, lyrics: Song.Lyrics?, close: (changed: Boolean) -> Unit) {

    val on_accent = Theme.current.accent

    val text_field_colours = TextFieldDefaults.textFieldColors(
        containerColor = on_accent.setAlpha(0.75f),
        textColor = on_accent,
        focusedLabelColor = on_accent,
        unfocusedLabelColor = on_accent,
        focusedTrailingIconColor = on_accent,
        unfocusedTrailingIconColor = on_accent,
        cursorColor = on_accent,
        focusedIndicatorColor = on_accent,
        unfocusedIndicatorColor = on_accent.setAlpha(0.5f)
    )

    val focus = LocalFocusManager.current

    var title = remember (song.title) { mutableStateOf(TextFieldValue(song.title ?: "")) }
    var artist = remember (song.artist?.title) { mutableStateOf(TextFieldValue(song.artist?.title ?: "")) }

    var search_requested by remember { mutableStateOf(false) }
    var search_results: List<LyricsSearchResult>? by remember { mutableStateOf(null) }
    var edit_page_open by remember { mutableStateOf(false) }

    fun performSearch() {
        thread {
            synchronized(check_lock) {
                if (checking) {
                    return@thread
                }
                checking = true
            }

            search_results = searchForLyrics(title.value.text, if (artist.value.text.trim().isEmpty()) null else artist.value.text).getOrThrowHere()

            synchronized(check_lock) {
                checking = false
                edit_page_open = false
            }
        }
    }

    LaunchedEffect(Unit) {
        performSearch()
    }

    LaunchedEffect(search_requested) {
        if (search_requested) {
            search_requested = false
            performSearch()
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
                        Text("Search for lyrics", color = on_accent)
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
                            colors = text_field_colours
                        )
                    }

                    Field(title, com.spectre7.utils.getString(R.string.song_name))
                    Field(artist, com.spectre7.utils.getString(R.string.artist))
                }
            }
            else if (search_results != null) {
                LyricsSearchResults(search_results!!) { index ->
                    if (index != null) {
                        val selected = search_results!![index]
                        if (selected.id != song.registry.get("lyrics_id") && selected.source != song.registry.get("lyrics_source")) {
                            song.registry.set("lyrics_id", selected.id)
                            song.registry.set("lyrics_source", selected.source)
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
                        .weight(1f), contentAlignment = Alignment.Center) {
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
                    Text(com.spectre7.utils.getString(R.string.action_close))
                }

                IconButton(
                    {
                        if (edit_page_open) {
                            search_requested = true
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
