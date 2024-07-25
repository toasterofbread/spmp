package com.toasterofbread.spmp.ui.layout.nowplaying.overlay.lyrics

import LocalAppState
import LocalDataase
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
import dev.toastbits.composekit.platform.composable.BackHandler
import dev.toastbits.composekit.utils.composable.WidthShrinkText
import dev.toastbits.composekit.utils.modifier.background
import com.toasterofbread.spmp.model.mediaitem.song.Song
import dev.toastbits.composekit.settings.ui.on_accent
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.lyrics_sync_long_press_line

internal enum class SpecialMode {
    ADJUST_SYNC, SELECT_SYNC_LINE
}

@Composable
internal fun SpecialModeMenu(special_mode: SpecialMode?, song: Song, setMode: (SpecialMode?) -> Unit) {
    val state: SpMp.State = LocalAppState.current

    Crossfade(
        special_mode,
        Modifier
            .fillMaxWidth()
            .padding(15.dp)
            .background(RoundedCornerShape(16.dp)) { state.theme.accent }
            .padding(horizontal = 10.dp)
    ) { mode ->
        val button_width = 40.dp

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompositionLocalProvider(LocalContentColor provides state.theme.on_accent) {
                when (mode) {
                    SpecialMode.ADJUST_SYNC -> {
                        var sync_offset: Long? by song.LyricsSyncOffset.observe(LocalDataase.current)

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
                            style = LocalTextStyle.current.copy(color = state.theme.on_accent)
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
