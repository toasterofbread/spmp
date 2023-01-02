package com.spectre7.spmp.ui.layout.nowplaying.overlay.lyrics

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.R
import com.spectre7.spmp.api.DataApi
import com.spectre7.spmp.model.Song
import com.spectre7.utils.getContrasted
import com.spectre7.utils.getString
import com.spectre7.utils.setAlpha
import kotlinx.coroutines.sync.Mutex

@Composable
fun ColumnScope.LyricsFinder(
    song: Song,
    text_field_colours: TextFieldColors,
    modifier: Modifier = Modifier,
    ToolButton: @Composable (onClick: () -> Unit, modifier: Modifier, content: @Composable () -> Unit) -> Unit,
    onFinished: (lyrics_id: String?) -> Unit
) {
    val check_mutex = remember { Mutex() }
    val focus = LocalFocusManager.current

    var title = remember { mutableStateOf(TextFieldValue(song.title)) }
    var artist = remember { mutableStateOf(TextFieldValue(song.artist.name)) }

    var search_requested by remember { mutableStateOf(false) }
    var search_results: List<DataApi.LyricsSearchResult>? by remember { mutableStateOf(null) }

    suspend fun performSearch() {
        if (check_mutex.isLocked) {
            return
        }

        check_mutex.lock()

        DataApi.searchForLyrics(
            title.value.text,
            if (artist.value.text.trim().isEmpty()) null else artist.value.text
        ) { results ->
            search_results = results ?: listOf()
            check_mutex.unlock()
        }
    }

    LaunchedEffect(search_requested) {
        if (search_requested) {
            search_requested = false
            performSearch()
        }
    }

    BackHandler {
        onFinished(null)
    }

    Crossfade(search_results) { results ->
        if (results == null) {
            Column(
                modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {

                    Box(
                        Modifier
                            .background(MainActivity.theme.getAccent(), CircleShape)
                            .padding(10.dp)) {
                        Text("Search for lyrics", color = MainActivity.theme.getOnAccent())
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

                    Field(title, getString(R.string.song_name))
                    Field(artist, getString(R.string.artist))
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ToolButton({ search_requested = true }, Modifier) {
                        Crossfade(check_mutex.isLocked) { searching ->
                            if (searching) {
                                CircularProgressIndicator(Modifier.requiredSize(25.dp), color = MainActivity.theme.getOnAccent(), strokeWidth = 3.dp)
                            }
                            else {
                                Icon(Icons.Filled.Search, null)
                            }
                        }
                    }
                    ToolButton({ onFinished(null) }, Modifier) {
                        Icon(Icons.Filled.Close, null)
                    }
                }
            }
        }
        else {
            ResultsDisplay(results, close = { search_results = null }) { index ->
                val selected = search_results!![index]
                search_results = null
            }
        }
    }
}

@Composable
private fun ResultsDisplay(results: List<DataApi.LyricsSearchResult>, close: () -> Unit, select: (Int) -> Unit) {

    BackHandler(onBack = close)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LazyColumn(
            Modifier.fillMaxSize().weight(1f),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            items(results.size, { results[it].id }) {
                val result = results[it]
                Box(
                    Modifier
                        .background(MainActivity.theme.getAccent(), RoundedCornerShape(16))
                        .clickable {

                        }
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

                        @Composable
                        fun Item(name: String, value: String, colour: Color) {
                            Row(Modifier.padding(5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(name.uppercase(), fontWeight = FontWeight.Light, color = colour)
                                Text("-", fontWeight = FontWeight.Light, color = colour)
                                Text(value, color = colour)
                            }
                        }

                        val shape = RoundedCornerShape(16)

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(result.name, color = MainActivity.theme.getOnAccent())
                            Box(Modifier.background(result.source.colour, CircleShape)) {
                                Text(result.source.readable, Modifier.padding(5.dp), color = result.source.colour.getContrasted(), fontSize = 12.sp)
                            }
                        }

                        Column(
                            Modifier
                                .border(Dp.Hairline, MainActivity.theme.getOnAccent(), shape)
                                .background(
                                    MainActivity.theme
                                        .getBackground(true)
                                        .setAlpha(0.2), shape
                                )
                                .padding(2.dp)
                                .fillMaxWidth()
                        ) {
                            if (result.artist_name != null) {
                                Item(getString(R.string.artist), result.artist_name, MainActivity.theme.getBackground(true))
                            }
                            if (result.album_name != null) {
                                Item(getString(R.string.album), result.album_name, MainActivity.theme.getBackground(true))
                            }
                        }
                    }
                }
            }
        }

        Button(
            close,
            Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MainActivity.theme.getAccent(),
                contentColor = MainActivity.theme.getOnAccent()
            )
        ) {
            Text(getString(R.string.action_back))
        }
    }


}
