package com.toasterofbread.spmp.ui.layout.nowplaying.overlay.lyrics

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.toastbits.composekit.components.platform.composable.BackHandler
import dev.toastbits.composekit.util.composable.WidthShrinkText
import dev.toastbits.composekit.components.utils.modifier.background
import com.toasterofbread.spmp.model.mediaitem.song.Song
import dev.toastbits.composekit.theme.core.onAccent
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.lyrics_sync_long_press_line

internal enum class SpecialMode {
    ADJUST_SYNC, SELECT_SYNC_LINE
}

@Composable
internal fun SpecialModeMenu(special_mode: SpecialMode?, song: Song, setMode: (SpecialMode?) -> Unit) {
    val player = LocalPlayerState.current

    Crossfade(
        special_mode,
        Modifier
            .fillMaxWidth()
            .padding(15.dp)
            .background(RoundedCornerShape(16.dp)) { player.theme.accent }
            .padding(horizontal = 10.dp)
    ) { mode ->
        val button_width = 40.dp

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompositionLocalProvider(LocalContentColor provides player.theme.onAccent) {
                when (mode) {
                    SpecialMode.ADJUST_SYNC -> {
                        var sync_offset: Long? by song.LyricsSyncOffset.observe(LocalPlayerState.current.database)

                        IconButton({
                            sync_offset = (sync_offset ?: 0) - 100
                        }) {
                            Icon(Icons.Default.Remove, null)
                        }

                        Text(((sync_offset ?: 0) / 1000f).toString())

                        IconButton({
                            sync_offset = (sync_offset ?: 0) + 100
                        }) {
                            Icon(Icons.Default.Add, null)
                        }

                        Spacer(Modifier.fillMaxWidth().weight(1f))

                        IconButton(
                            {
                                sync_offset = null
                            },
                            Modifier.width(button_width)
                        ) {
                            Icon(Icons.Default.Refresh, null)
                        }

                        IconButton(
                            {
                            setMode(SpecialMode.SELECT_SYNC_LINE)
                            },
                            Modifier.width(button_width)
                        ) {
                            Icon(Icons.Default.HourglassBottom, null)
                        }
                    }

                    SpecialMode.SELECT_SYNC_LINE -> {
                        BackHandler {
                            setMode(SpecialMode.ADJUST_SYNC)
                        }

                        WidthShrinkText(
                            stringResource(Res.string.lyrics_sync_long_press_line),
                            Modifier.fillMaxWidth().weight(1f),
                            style = LocalTextStyle.current.copy(color = player.theme.onAccent)
                        )
                    }

                    else -> {}
                }

                IconButton(
                    {
                        if (mode == SpecialMode.SELECT_SYNC_LINE) setMode(SpecialMode.ADJUST_SYNC)
                        else setMode(null)
                    },
                    Modifier.width(button_width)
                ) {
                    Icon(Icons.Default.Close, null)
                }
            }
        }
    }
}
