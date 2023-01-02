package com.spectre7.spmp.ui.layout.nowplaying.overlay.lyrics

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.api.DataApi
import com.spectre7.spmp.model.Song
import com.spectre7.utils.setAlpha
import kotlin.concurrent.thread

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsSearchMenu(song: Song, lyrics: Song.Lyrics?, close: () -> Unit) {

    val source_options = Song.Lyrics.Source.values()
    var source_menu_open by remember { mutableStateOf(false) }
    var selected_source by remember { mutableStateOf(source_options[0]) }

    val lyrics_id: String?
    if (song.registry.overrides.lyrics_id != null) {
        lyrics_id = song.registry.overrides.lyrics_id
    }
    else if (lyrics != null) {
        lyrics_id = lyrics.id
    }
    else {
        lyrics_id = null
    }

    var show_finder by remember { mutableStateOf(false) }

    val ToolButton = @Composable { onClick: () -> Unit, modifier: Modifier, content: @Composable () -> Unit ->
        Box(
            modifier
                .background(MainActivity.theme.getAccent(), CircleShape)
                .padding(10.dp)
                .clickable(remember { MutableInteractionSource() }, null, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(LocalContentColor provides MainActivity.theme.getOnAccent()) {
                content()
            }
        }
    }

    val text_field_colours = TextFieldDefaults.textFieldColors(
        containerColor = MainActivity.theme.getAccent().setAlpha(0.75),
        textColor = MainActivity.theme.getOnAccent(),
        focusedLabelColor = MainActivity.theme.getOnAccent(),
        unfocusedLabelColor = MainActivity.theme.getOnAccent(),
        focusedTrailingIconColor = MainActivity.theme.getOnAccent(),
        unfocusedTrailingIconColor = MainActivity.theme.getOnAccent(),
        cursorColor = MainActivity.theme.getOnAccent(),
        focusedIndicatorColor = MainActivity.theme.getOnAccent(),
        unfocusedIndicatorColor = MainActivity.theme.getOnAccent().setAlpha(0.5)
    )

    Crossfade(show_finder) { finder ->
        Column(
            Modifier.fillMaxSize().padding(15.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (finder) {
                when (selected_source) {
                    Song.Lyrics.Source.PETITLYRICS -> LyricsFinder(song, text_field_colours, Modifier, ToolButton) {
                        show_finder = false
                    }
                }
            }
            else {
                Column(
                    Modifier
                        .fillMaxHeight()
                        .weight(1f), verticalArrangement = Arrangement.SpaceEvenly) {

                    ExposedDropdownMenuBox(
                        expanded = source_menu_open,
                        onExpandedChange = {
                            source_menu_open = !source_menu_open
                        }
                    ) {
                        TextField(
                            readOnly = true,
                            value = selected_source.readable,
                            onValueChange = {},
                            label = { Text("Lyrics source") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = source_menu_open
                                )
                            },
                            colors = text_field_colours
                        )
                        ExposedDropdownMenu(
                            expanded = source_menu_open,
                            onDismissRequest = {
                                source_menu_open = false
                            }
                        ) {
                            source_options.forEach { selected_option ->
                                DropdownMenuItem(
                                    onClick = {
                                        selected_source = selected_option
                                        source_menu_open = false
                                    },
                                    text = { Text(selected_option.readable) }
                                )
                            }
                        }
                    }

                    val focus = LocalFocusManager.current
                    var id_field_state by remember { mutableStateOf(TextFieldValue(lyrics_id ?: "")) }
                    TextField(
                        id_field_state,
                        { id_field_state = it },
                        singleLine = true,
                        label = {
                            Text("Lyrics ID")
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            focus.clearFocus()
                        }),
                        colors = text_field_colours
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    ToolButton({}, Modifier) {
                        Icon(Icons.Filled.Check, null, Modifier)
                    }

                    ToolButton(close, Modifier) {
                        Icon(Icons.Filled.Close, null, Modifier)
                    }

                    ToolButton({ show_finder = true }, Modifier) {
                        Text("Search for lyrics")
                    }
                }
            }
        }
    }
}

//@Composable
//private fun EditMenuToolbar(song: Song, close: () -> Unit, save: () -> Unit, modifier: Modifier = Modifier) {
//
//}