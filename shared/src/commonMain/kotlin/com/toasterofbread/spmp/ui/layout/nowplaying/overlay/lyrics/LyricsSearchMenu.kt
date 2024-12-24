package com.toasterofbread.spmp.ui.layout.nowplaying.overlay.lyrics

import LocalPlayerState
import PlatformIO
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.db.Database
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.appTextField
import com.toasterofbread.spmp.youtubeapi.lyrics.LyricsReference
import com.toasterofbread.spmp.youtubeapi.lyrics.LyricsSource
import dev.toastbits.composekit.components.utils.composable.LargeDropdownMenu
import dev.toastbits.composekit.theme.core.onAccent
import dev.toastbits.composekit.util.composable.OnChangedEffect
import dev.toastbits.composekit.util.composable.getValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.action_cancel
import spmp.shared.generated.resources.action_close
import spmp.shared.generated.resources.action_confirm_action
import spmp.shared.generated.resources.artist
import spmp.shared.generated.resources.lyrics_no_lyrics_set_confirmation_title
import spmp.shared.generated.resources.lyrics_none_found
import spmp.shared.generated.resources.`lyrics_search_on_$source`
import spmp.shared.generated.resources.lyrics_source_cannot_search
import spmp.shared.generated.resources.lyrics_source_dialog_title
import spmp.shared.generated.resources.prompt_confirm_action
import spmp.shared.generated.resources.song_name
import java.util.concurrent.locks.ReentrantLock
import kotlin.time.Duration.Companion.milliseconds

private const val LYRICS_SEARCH_RETRY_COUNT = 3

@Composable
fun LyricsSearchMenu(
    song: Song,
    modifier: Modifier = Modifier,
    close: (changed: Boolean) -> Unit,
) {
    val player: PlayerState = LocalPlayerState.current
    val db: Database = player.context.database

    val song_title: String? by song.observeActiveTitle()
    val song_artists: List<Artist>? by song.Artists.observe(db)
    val song_artist_title: String? by song_artists?.firstOrNull()?.observeActiveTitle()

    val onAccent: Color = player.theme.onAccent
    val accent: Color = player.theme.accent

    val load_lock: ReentrantLock = remember { ReentrantLock() }
    var loading by remember { mutableStateOf(false) }

    val text_field_colours: TextFieldColors =
        TextFieldDefaults.colors(
            focusedContainerColor = accent.copy(alpha = 0.75f),
            unfocusedContainerColor = accent.copy(alpha = 0.75f),
            focusedTextColor = onAccent,
            unfocusedTextColor = onAccent,
            focusedLabelColor = onAccent,
            unfocusedLabelColor = onAccent,
            focusedTrailingIconColor = onAccent,
            unfocusedTrailingIconColor = onAccent,
            cursorColor = onAccent,
            focusedIndicatorColor = accent,
            unfocusedIndicatorColor = accent.copy(alpha = 0.5f)
        )

    val focus = LocalFocusManager.current
    val keyboard_controller = LocalSoftwareKeyboardController.current

    val title = remember (song_title) { mutableStateOf(TextFieldValue(song_title ?: "")) }
    val artist = remember (song_artist_title) { mutableStateOf(TextFieldValue(song_artist_title ?: "")) }
    var search_state: Boolean by remember { mutableStateOf(false) }

    val default_source: Int by player.settings.Lyrics.DEFAULT_SOURCE.observe()
    var selected_source: LyricsSource by remember { mutableStateOf(LyricsSource.fromIdx(default_source)) }

    var search_results: Pair<List<LyricsSource.SearchResult>, Int>? by remember { mutableStateOf(null) }
    var edit_page_open by remember { mutableStateOf(true) }

    OnChangedEffect(search_state) {
        keyboard_controller?.hide()

        withContext(Dispatchers.PlatformIO) {
            synchronized(load_lock) {
                check(!loading)
                loading = true
            }

            if (selected_source.supportsLyricsBySearching()) {
                var result: Result<List<LyricsSource.SearchResult>>? = null
                var retry_count = LYRICS_SEARCH_RETRY_COUNT

                while (retry_count-- > 0) {
                    result = selected_source.searchForLyrics(
                        title = title.value.text,
                        artist_name = if (artist.value.text.trim().isEmpty()) null else artist.value.text,
                        album_name = song.Album.get(db)?.getActiveTitle(db),
                        duration = song.Duration.get(db)?.milliseconds
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
                            player.context.sendToast(getString(Res.string.lyrics_none_found))
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
                            Icon(Icons.Default.ArrowDropDown, null, tint = onAccent)

                            Text(
                                stringResource(Res.string.`lyrics_search_on_$source`)
                                    .replace("\$source", selected_source.getReadable()),
                                color = onAccent
                            )

                            LargeDropdownMenu(
                                title = stringResource(Res.string.lyrics_source_dialog_title),
                                isOpen = source_selector_open,
                                onDismissRequest = { source_selector_open = false },
                                items = (0 until LyricsSource.SOURCE_AMOUNT).toList(),
                                selectedItem = selected_source.source_index,
                                onSelected = { _, source_idx ->
                                    selected_source = LyricsSource.fromIdx(source_idx)
                                    source_selector_open = false
                                }
                            ) { source_idx ->
                                Text(LyricsSource.fromIdx(source_idx).getReadable())
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
                                tint = onAccent
                            )
                        }

                        if (confirming_no_lyrics) {
                            AlertDialog(
                                onDismissRequest = { confirming_no_lyrics = false },
                                confirmButton = {
                                    Button({ song.Lyrics.set(LyricsReference.NONE, player.database) }) {
                                        Text(stringResource(Res.string.action_confirm_action))
                                    }
                                },
                                dismissButton = {
                                    Button({ confirming_no_lyrics = false }) {
                                        Text(stringResource(Res.string.action_cancel))
                                    }
                                },
                                title = {
                                    Text(stringResource(Res.string.prompt_confirm_action))
                                },
                                text = {
                                    Text(stringResource(Res.string.lyrics_no_lyrics_set_confirmation_title))
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
                            modifier = Modifier.clickable {}.appTextField(),
                            enabled = enabled
                        )
                    }

                    Crossfade(selected_source.supportsLyricsBySearching()) { search_supported ->
                        Column {
                            Field(title, stringResource(Res.string.song_name), search_supported)
                            Spacer(Modifier.height(10.dp))
                            Field(artist, stringResource(Res.string.artist), search_supported)
                        }
                    }

                    AnimatedVisibility(!selected_source.supportsLyricsBySearching()) {
                        Text(
                            stringResource(Res.string.lyrics_source_cannot_search),
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
                        contentColor = onAccent
                    )
                ) {
                    Text(stringResource(Res.string.action_close))
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
                            0 -> CircularProgressIndicator(Modifier.requiredSize(22.dp), color = onAccent, strokeWidth = 3.dp)
                            else -> {
                                Icon(
                                    when (icon) {
                                        1 -> Icons.Default.Search
                                        2 -> Icons.Default.Download
                                        else -> Icons.Default.Edit
                                    },
                                    null,
                                    tint = onAccent
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
