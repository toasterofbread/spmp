package com.spectre7.spmp.ui.layout.nowplaying.overlay.lyrics

import androidx.compose.animation.Crossfade
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.api.DataApi
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.ui.layout.nowplaying.overlay.lyrics.finders.PetitLyricsFinder
import com.spectre7.utils.MeasureUnconstrainedView
import kotlin.concurrent.thread

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsEditMenu(song: Song, lyrics: Song.Lyrics?, close: () -> Unit) {

    fun testLyricsId(id: String, callback: (lyrics: Song.Lyrics?) -> Unit) {
        thread {
            callback(DataApi.getLyrics(id))
        }
    }

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

    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Column(
            Modifier
                .fillMaxHeight()
                .weight(1f), verticalArrangement = Arrangement.SpaceEvenly) {
            val source_options = listOf("ptl")
            var source_menu_open by remember { mutableStateOf(false) }
            var selected_source by remember { mutableStateOf(source_options[0]) }

            fun getReadableSource(source: String): String {
                return when (source) {
                    "ptl" -> "PetitLyrics"
                    else -> throw NotImplementedError()
                }
            }

            ExposedDropdownMenuBox(
                expanded = source_menu_open,
                onExpandedChange = {
                    source_menu_open = !source_menu_open
                }
            ) {
                TextField(
                    readOnly = true,
                    value = getReadableSource(selected_source),
                    onValueChange = {},
                    label = { Text("Lyrics source") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = source_menu_open
                        )
                    },
                    colors = ExposedDropdownMenuDefaults.textFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = source_menu_open,
                    onDismissRequest = {
                        source_menu_open = false
                    }
                ) {
                    source_options.forEach { selectionOption ->
                        DropdownMenuItem(
                            onClick = {
                                selected_source = selectionOption
                                source_menu_open = false
                            },
                            text = { Text(getReadableSource(selectionOption)) }
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
                })
            )
        }

        EditMenuToolbar(song, close, {})
    }
}

@Composable
private fun EditMenuToolbar(song: Song, close: () -> Unit, save: () -> Unit) {
    var show_finder by remember { mutableStateOf(false) }
    val bar_height = 45.dp

    val ToolButton = @Composable { onClick: () -> Unit, icon: Boolean, modifier: Modifier, content: @Composable () -> Unit ->
        if (icon) {
            IconButton(
                onClick,
                modifier.clip(CircleShape),
                content = content,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MainActivity.theme.getAccent(),
                    contentColor = MainActivity.theme.getOnAccent()
                ),
            )
        }
        else{
            Button(
                onClick,
                modifier,
                content = { content() },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MainActivity.theme.getAccent(),
                    contentColor = MainActivity.theme.getOnAccent()
                )
            )
        }
    }

    Crossfade(show_finder) { finder ->
        if (finder) {
            PetitLyricsFinder(song, Modifier.requiredHeight(50.dp), ToolButton) {
                show_finder = false
            }
        }
        else {
            Row(Modifier, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                ToolButton(save, true, Modifier) {
                    Icon(Icons.Filled.Check, null, Modifier)
                }

                ToolButton(close, true, Modifier) {
                    Icon(Icons.Filled.Close, null, Modifier)
                }

                ToolButton({ show_finder = true }, false, Modifier) {
                    Text("Search for lyrics")
                }
            }

        }
    }
}