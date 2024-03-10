package com.toasterofbread.spmp.ui.layout.contentbar.element

import LocalPlayerState
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clipToBounds
import com.toasterofbread.composekit.utils.common.*
import com.toasterofbread.composekit.utils.composable.*
import com.toasterofbread.spmp.model.mediaitem.loader.SongLyricsLoader
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.component.LyricsLineDisplay
import kotlinx.serialization.json.*

class ContentBarElementLyrics(size_mode: SizeMode? = null, size: Int? = null): ContentBarElement {
    private var size_mode: SizeMode by mutableStateOf(SizeMode.DEFAULT)
    private var size: Int by mutableStateOf(50)

    init {
        if (size_mode != null) {
            this.size_mode = size_mode
        }
        if (size != null) {
            this.size = size
        }
    }

    constructor(data: JsonObject?): this(
        data?.get("size_mode")?.jsonPrimitive?.int?.let { SizeMode.entries.getOrNull(it) },
        data?.get("size")?.jsonPrimitive?.int
    )

    private fun getJsonData(): JsonObject = buildJsonObject {
        put("size", size)
        put("size_mode", size_mode.ordinal)
    }

    override fun getData(): ContentBarElementData =
        ContentBarElementData(type = ContentBarElement.Type.LYRICS, data = getJsonData())

    override fun shouldFillLength(): Boolean =
        size_mode == SizeMode.FILL

    @Composable
    override fun Element(vertical: Boolean, bar_width: Dp, modifier: Modifier) {
        val player: PlayerState = LocalPlayerState.current
        val current_song: Song? by player.status.song_state

        val lyrics_state: SongLyricsLoader.ItemState? = remember(current_song?.id) { current_song?.let { SongLyricsLoader.getItemState(it, player.context) } }
        val lyrics_sync_offset: Long? by current_song?.getLyricsSyncOffset(player.database, true)

        val size_dp: Dp? =
            when (size_mode) {
                SizeMode.FILL -> null
                SizeMode.STATIC -> size.dp
                SizeMode.PERCENTAGE -> bar_width * size * 0.01f
            }

        Crossfade(
            lyrics_state?.lyrics,
            modifier
                .thenWith(size_dp) {
                    if (vertical) height(it)
                    else width(it)
                }
                .clipToBounds()
        ) { lyrics ->
            if (lyrics?.synced != true) {
                return@Crossfade
            }

            LyricsLineDisplay(
                lyrics = lyrics,
                getTime = {
                    (player.controller?.current_position_ms ?: 0) + (lyrics_sync_offset ?: 0)
                }
            )
        }
    }

    @Composable
    override fun ConfigurationItems(modifier: Modifier, onModification: () -> Unit) {
        var show_mode_selector: Boolean by remember { mutableStateOf(false) }

        LargeDropdownMenu(
            show_mode_selector,
            onDismissRequest = {
                show_mode_selector = false
            },
            SizeMode.entries.size,
            size_mode.ordinal,
            { SizeMode.entries[it].getName() }
        ) {
            show_mode_selector = false
            size_mode = SizeMode.entries[it]
            size = 50
            onModification()
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(getString("content_bar_element_lyrics_config_size_mode"))

            Spacer(Modifier.fillMaxWidth().weight(1f))

            Button({ show_mode_selector = !show_mode_selector }) {
                Text(size_mode.getName())
            }
        }

        AnimatedVisibility(size_mode != SizeMode.FILL) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(getString("content_bar_element_lyrics_config_size"))

                Spacer(Modifier.fillMaxWidth().weight(1f))

                IconButton({
                    size = (size - size_mode.getStep()).coerceAtLeast(10)
                    onModification()
                }) {
                    Icon(Icons.Default.Remove, null)
                }

                Text(
                    if (size_mode == SizeMode.STATIC) "${size}dp"
                    else "${size}%"
                )

                IconButton({
                    size = size + size_mode.getStep()
                    if (size_mode == SizeMode.PERCENTAGE) {
                        size = size.coerceAtMost(100)
                    }

                    onModification()
                }) {
                    Icon(Icons.Default.Add, null)
                }
            }
        }
    }

    enum class SizeMode {
        FILL,
        STATIC,
        PERCENTAGE;

        fun getName(): String =
            when (this) {
                FILL -> getString("content_bar_element_lyrics_config_size_mode_fill")
                STATIC -> getString("content_bar_element_lyrics_config_size_mode_static")
                PERCENTAGE -> getString("content_bar_element_lyrics_config_size_mode_percentage")
            }

        fun getStep(): Int =
            when (this) {
                FILL -> 0
                STATIC -> 10
                PERCENTAGE -> 10
            }

        companion object {
            val DEFAULT: SizeMode = FILL
        }
    }
}
