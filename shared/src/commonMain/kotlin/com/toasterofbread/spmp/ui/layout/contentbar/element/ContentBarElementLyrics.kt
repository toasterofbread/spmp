package com.toasterofbread.spmp.ui.layout.contentbar.element

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import dev.toastbits.composekit.util.composable.getValue
import dev.toastbits.composekit.components.utils.composable.*
import com.toasterofbread.spmp.model.mediaitem.loader.SongLyricsLoader
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.component.*
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.LayoutSlot
import dev.toastbits.composekit.util.composable.AlignableCrossfade
import kotlin.math.sign
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.s_option_lyrics_text_alignment_start
import spmp.shared.generated.resources.s_option_lyrics_text_alignment_center
import spmp.shared.generated.resources.s_option_lyrics_text_alignment_end
import spmp.shared.generated.resources.content_bar_element_lyrics_config_alignment
import spmp.shared.generated.resources.s_option_lyrics_text_alignment_start
import spmp.shared.generated.resources.s_option_lyrics_text_alignment_center
import spmp.shared.generated.resources.s_option_lyrics_text_alignment_end
import spmp.shared.generated.resources.content_bar_element_lyrics_config_linger
import spmp.shared.generated.resources.content_bar_element_lyrics_config_show_furigana
import spmp.shared.generated.resources.content_bar_element_lyrics_config_max_lines
import spmp.shared.generated.resources.content_bar_element_lyrics_config_preallocate_max_space

@Serializable
data class ContentBarElementLyrics(
    override val config: ContentBarElementConfig = ContentBarElementConfig(),
    val alignment: Int = 0,
    val linger: Boolean = true,
    val show_furigana: Boolean = true
    // val max_lines: Int = 1,
    // val preallocate_max_space: Boolean = false,
): ContentBarElement() {
    override fun getType(): ContentBarElement.Type = ContentBarElement.Type.LYRICS

    override fun copyWithConfig(config: ContentBarElementConfig): ContentBarElement =
        copy(config = config)

    private var lyrics_state: SongLyricsLoader.ItemState? by mutableStateOf(null)

    @Composable
    override fun isDisplaying(): Boolean {
        val player: PlayerState = LocalPlayerState.current
        val current_song: Song? by player.status.song_state
        lyrics_state = current_song?.let { SongLyricsLoader.rememberItemState(it, player.context) }

        LaunchedEffect(current_song) {
            current_song?.also { song ->
                SongLyricsLoader.loadBySong(song, player.context)
            }
        }

        return lyrics_state?.lyrics?.synced == true
    }

    @Composable
    override fun ElementContent(vertical: Boolean, slot: LayoutSlot?, bar_size: DpSize, onPreviewClick: (() -> Unit)?, modifier: Modifier) {
        val player: PlayerState = LocalPlayerState.current
        val current_song: Song? by player.status.song_state
        val lyrics_sync_offset: Long? by current_song?.getLyricsSyncOffset(player.database, true)

        if (onPreviewClick != null) {
            IconButton(onPreviewClick, modifier) {
                Icon(Icons.Default.MusicNote, null)
            }
            return
        }

        AlignableCrossfade(
            lyrics_state?.lyrics,
            modifier,
            contentAlignment =
                if (alignment == 0) Alignment.Center
                else if (vertical) {
                    if (alignment < 0) Alignment.TopCenter
                    else Alignment.BottomCenter
                }
                else {
                    if (alignment < 0) Alignment.CenterStart
                    else Alignment.CenterEnd
                }
        ) { lyrics ->
            if (lyrics?.synced != true) {
                return@AlignableCrossfade
            }

            val getTime: () -> Long = {
                (player.controller?.current_position_ms ?: 0) + (lyrics_sync_offset ?: 0)
            }

            if (vertical) {
                VerticalLyricsLineDisplay(
                    lyrics = lyrics,
                    getTime = getTime,
                    lyrics_linger = linger,
                    show_furigana = show_furigana
                )
            }
            else {
                HorizontalLyricsLineDisplay(
                    lyrics = lyrics,
                    getTime = getTime,
                    lyrics_linger = linger,
                    show_furigana = show_furigana,
                    text_align =
                        if (alignment < 0) TextAlign.Start
                        else if (alignment == 0) TextAlign.Center
                        else TextAlign.End
                    // max_lines = max_lines,
                    // preallocate_max_space = preallocate_max_space
                )
            }
        }
    }

    @Composable
    override fun SubConfigurationItems(item_modifier: Modifier, onModification: (ContentBarElement) -> Unit) {
        var show_alignment_selector: Boolean by remember { mutableStateOf(false) }

        LargeDropdownMenu(
            title = stringResource(Res.string.content_bar_element_lyrics_config_alignment),
            isOpen = show_alignment_selector,
            onDismissRequest = { show_alignment_selector = false },
            items = (0 until 3).toList(),
            selectedItem = alignment.sign + 1,
            itemContent = {
                Text(
                    if (it == 0) stringResource(Res.string.s_option_lyrics_text_alignment_start)
                    else if (it == 1) stringResource(Res.string.s_option_lyrics_text_alignment_center)
                    else stringResource(Res.string.s_option_lyrics_text_alignment_end)
                )
            },
            onSelected = { _, index ->
                onModification(copy(alignment = index - 1))
                show_alignment_selector = false
            }
        )

        FlowRow(
            item_modifier,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                stringResource(Res.string.content_bar_element_lyrics_config_alignment),
                Modifier.align(Alignment.CenterVertically),
                softWrap = false
            )

            Button({ show_alignment_selector = !show_alignment_selector }) {
                Text(
                    if (alignment < 0) stringResource(Res.string.s_option_lyrics_text_alignment_start)
                    else if (alignment == 0) stringResource(Res.string.s_option_lyrics_text_alignment_center)
                    else stringResource(Res.string.s_option_lyrics_text_alignment_end)
                )
            }
        }

        FlowRow(
            item_modifier,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                stringResource(Res.string.content_bar_element_lyrics_config_linger),
                Modifier.align(Alignment.CenterVertically),
                softWrap = false
            )

            Switch(
                linger,
                { onModification(copy(linger = it)) }
            )
        }

        FlowRow(
            item_modifier,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                stringResource(Res.string.content_bar_element_lyrics_config_show_furigana),
                Modifier.align(Alignment.CenterVertically),
                softWrap = false
            )

            Switch(
                show_furigana,
                { onModification(copy(show_furigana = it)) }
            )
        }

        // FlowRow(
        //     item_modifier,
        //     horizontalArrangement = Arrangement.SpaceBetween
        // ) {
        //     Text(
        //         stringResource(Res.string.content_bar_element_lyrics_config_max_lines),
        //         Modifier.align(Alignment.CenterVertically),
        //         softWrap = false
        //     )

        //     Row(
        //         verticalAlignment = Alignment.CenterVertically
        //     ) {
        //         IconButton({ onModification(copy(max_lines = (max_lines - 1).coerceAtLeast(1))) }) {
        //             Icon(Icons.Default.Remove, null)
        //         }

        //         Text((max_lines.coerceAtLeast(1)).toString())

        //         IconButton({ onModification(copy(max_lines = max_lines + 1)) }) {
        //             Icon(Icons.Default.Add, null)
        //         }
        //     }
        // }

        // FlowRow(
        //     item_modifier,
        //     horizontalArrangement = Arrangement.SpaceBetween
        // ) {
        //     Text(
        //         stringResource(Res.string.content_bar_element_lyrics_config_preallocate_max_space),
        //         Modifier.align(Alignment.CenterVertically),
        //         softWrap = false
        //     )

        //     Switch(
        //         preallocate_max_space,
        //         { onModification(copy(preallocate_max_space = it)) }
        //     )
        // }
    }
}
