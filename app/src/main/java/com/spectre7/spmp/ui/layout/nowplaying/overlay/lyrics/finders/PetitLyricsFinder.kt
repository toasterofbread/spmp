package com.spectre7.spmp.ui.layout.nowplaying.overlay.lyrics.finders

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import com.spectre7.spmp.model.Song
import kotlinx.coroutines.sync.Mutex

@Composable
fun PetitLyricsFinder(
    song: Song,
    modifier: Modifier = Modifier,
    ToolButton: @Composable (onClick: () -> Unit, icon: Boolean, modifier: Modifier, content: @Composable () -> Unit) -> Unit,
    onFinished: (lyrics_id: String?) -> Unit
) {
    val check_mutex = remember { Mutex() }

    var title by remember { mutableStateOf(TextFieldValue(song.title)) }
    var artist by remember { mutableStateOf(TextFieldValue(song.artist.name)) }

    Column(modifier, verticalArrangement = Arrangement.SpaceEvenly) {
        Column(Modifier.fillMaxHeight().weight(1f)) {
            Text("Search on PetitLyrics")
        }
        Row {
            ToolButton({ onFinished(null) }, true, Modifier) {
                Icon(Icons.Filled.Close, null)
            }
            ToolButton({  }, true, Modifier) {
                Icon(Icons.Filled.Search, null)
            }
        }
    }
}