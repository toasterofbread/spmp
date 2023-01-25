package com.spectre7.spmp.ui.layout.nowplaying.overlay.lyrics

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.R
import com.spectre7.spmp.api.DataApi
import com.spectre7.spmp.api.LyricsSearchResult
import com.spectre7.spmp.model.Song
import com.spectre7.utils.getContrasted
import com.spectre7.utils.getString
import com.spectre7.utils.setAlpha

@Composable
internal fun ColumnScope.LyricsSearchResults(results: List<LyricsSearchResult>, modifier: Modifier = Modifier, onFinished: (Int?) -> Unit) {

    BackHandler {
        onFinished(null)
    }

    if (results.isNotEmpty()) {
        LazyColumn(
            Modifier.fillMaxSize().weight(1f),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(results.size + 1, { if (it == results.size) 0 else results[it].id }) {

                if (it == results.size) {
                    Text("No more results", color = MainActivity.theme.getAccent())
                }
                else {
                    val result = results[it]
                    Box(
                        Modifier
                            .background(MainActivity.theme.getAccent(), RoundedCornerShape(16))
                            .clickable {
                                onFinished(it)
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

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(result.name, color = MainActivity.theme.getOnAccent())

                                @Composable
                                fun text(text: String, colour: Color) {
                                    Text(
                                        text,
                                        Modifier.padding(5.dp),
                                        color = colour,
                                        fontSize = 10.sp,
                                        softWrap = false
                                    )
                                }

                                Spacer(Modifier.fillMaxWidth().weight(1f))

                                val sync_colour = if (result.sync_type == Song.Lyrics.SyncType.NONE) Color.LightGray else Color.Magenta
                                Box(Modifier.background(sync_colour, CircleShape)) {
                                    text(result.sync_type.readable, sync_colour.getContrasted())
                                }
                                Box(Modifier.background(result.source.colour, CircleShape)) {
                                    text(result.source.readable, result.source.colour.getContrasted())
                                }
                            }

                            Column(
                                Modifier
                                    .border(Dp.Hairline, MainActivity.theme.getOnAccent(), shape)
                                    .background(
                                        MainActivity.theme
                                            .getOnAccent()
                                            .setAlpha(0.1), shape
                                    )
                                    .padding(2.dp)
                                    .fillMaxWidth()
                            ) {
                                if (result.artist_name != null) {
                                    Item(getString(R.string.artist), result.artist_name!!, MainActivity.theme.getOnAccent())
                                }
                                if (result.album_name != null) {
                                    Item(getString(R.string.album), result.album_name!!, MainActivity.theme.getOnAccent())
                                }
                            }
                        }
                    }
                }

            }
        }
    }
    else {
        Text("No results found", color = MainActivity.theme.getAccent())
    }
}
