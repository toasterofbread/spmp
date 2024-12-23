package com.toasterofbread.spmp.ui.layout.nowplaying.overlay.songtheme

import LocalPlayerState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toasterofbread.spmp.model.mediaitem.db.Property
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.settings.Settings
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import dev.toastbits.composekit.settingsitem.domain.PlatformSettingsProperty
import dev.toastbits.composekit.util.composable.OnChangedEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import kotlin.math.roundToInt

internal abstract class SliderOption(
    override val titleResource: StringResource,
    override val icon: ImageVector,
    val getProperty: Settings.() -> PlatformSettingsProperty<Float>,
    val getSongProperty: Song.() -> Property<Float?>
): SongThemeOption() {

    @Composable
    override fun Content(song: Song, modifier: Modifier) {
        val player: PlayerState = LocalPlayerState.current
        val coroutine_scope: CoroutineScope = rememberCoroutineScope()

        var song_value: Float? by getSongProperty(song).observe(player.database)
        val global_value: Float by getProperty(player.settings).observe()

        val current_value: Float = song_value ?: global_value
        var slider_value: Float by remember { mutableStateOf(current_value) }

        var value_changed: Boolean by remember { mutableStateOf(false) }
        val anim_state: Animatable<Float, AnimationVector1D> = remember(song_value) { Animatable(current_value) }

        OnChangedEffect(anim_state.value) {
            song_value = anim_state.value
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FlowRow(
                Modifier.offset(y = 10.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TitleBar()

                Spacer(Modifier.fillMaxWidth().weight(1f))

                Text(
                    (current_value * 100).roundToInt().toString().padStart(3, ' '),
                    fontSize = 15.sp,
                    softWrap = false
                )
            }

            val slider_colour: Color = player.theme.onBackground

            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Slider(
                    value = anim_state.value,
                    onValueChange = {
                        slider_value = it
                        if (value_changed) {
                            coroutine_scope.launch {
                                anim_state.snapTo(it)
                            }
                        }
                        else {
                            value_changed = true
                        }
                    },
                    onValueChangeFinished = {
                        value_changed = false
                        coroutine_scope.launch {
                            anim_state.animateTo(slider_value)
                        }
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = slider_colour,
                        activeTrackColor = slider_colour,
                        inactiveTrackColor = slider_colour.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.weight(1f)
                )

                IconButton({
                    value_changed = false
                    coroutine_scope.launch {
                        anim_state.animateTo(global_value)
                    }
                }) {
                    Icon(Icons.Filled.Refresh, null)
                }
            }
        }
    }
}
