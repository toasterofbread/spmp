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
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.platform.BackHandler
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.utils.composable.WidthShrinkText
import com.toasterofbread.utils.modifier.background

internal enum class SpecialMode {
    ADJUST_SYNC, SELECT_SYNC_LINE
}

@Composable
internal fun SpecialModeMenu(special_mode: SpecialMode?, song: Song, setMode: (SpecialMode?) -> Unit) {
    Crossfade(
        special_mode,
        Modifier
            .fillMaxWidth()
            .padding(15.dp)
            .background(RoundedCornerShape(16.dp), Theme.accent_provider)
            .padding(horizontal = 10.dp)
    ) { mode ->
        val button_width = 35.dp

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompositionLocalProvider(LocalContentColor provides Theme.on_accent) {
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
                            setMode(SpecialMode.SELECT_SYNC_LINE)
                            },
                            Modifier.width(button_width)
                        ) {
                            Icon(Icons.Default.HourglassBottom, null)
                        }

                        IconButton(
                            {
                                sync_offset = null
                            },
                            Modifier.width(button_width)
                        ) {
                            Icon(Icons.Default.Refresh, null)
                        }
                    }

                    SpecialMode.SELECT_SYNC_LINE -> {
                        BackHandler {
                            setMode(SpecialMode.ADJUST_SYNC)
                        }

                        WidthShrinkText(
                            getString("lyrics_sync_long_press_line"),
                            Modifier.fillMaxWidth().weight(1f),
                            style = LocalTextStyle.current.copy(color = Theme.on_accent)
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
