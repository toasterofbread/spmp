package com.toasterofbread.spmp.ui.layout.nowplaying.overlay

import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.toastbits.composekit.platform.composable.ScrollBarLazyColumn
import dev.toastbits.composekit.utils.composable.OnChangedEffect
import dev.toastbits.composekit.utils.composable.LargeDropdownMenu
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.settings.category.ThemeSettings
import com.toasterofbread.spmp.platform.FormFactor
import com.toasterofbread.spmp.platform.generatePalette
import com.toasterofbread.spmp.platform.isVideoPlaybackSupported
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingPage
import com.toasterofbread.spmp.ui.layout.nowplaying.getNPBackground
import com.toasterofbread.spmp.ui.layout.nowplaying.getNPOnBackground
import com.toasterofbread.spmp.ui.layout.nowplaying.maintab.thumbnailrow.ColourpickCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

val DEFAULT_THUMBNAIL_ROUNDING: Float
    @Composable get() =
        if (NowPlayingPage.observeFormFactor().value.is_large) 0f
        else 0.05f

class SongThemePlayerOverlayMenu(
    val requestColourPicker: (ColourpickCallback) -> Unit,
    val onColourSelected: (Color) -> Unit
): PlayerOverlayMenu() {

    override fun closeOnTap(): Boolean = true

    @Composable
    override fun Menu(
        getSong: () -> Song?,
        getExpansion: () -> Float,
        openMenu: (PlayerOverlayMenu?) -> Unit,
        getSeekState: () -> Any,
        getCurrentSongThumb: () -> ImageBitmap?
    ) {
        val song: Song = getSong() ?: return
        val player: PlayerState = LocalPlayerState.current

        val thumb_image: ImageBitmap? = getCurrentSongThumb()
        var palette_colours: List<Color>? by remember { mutableStateOf(null) }
        var colourpick_requested: Boolean by remember { mutableStateOf(false) }
        val button_colours: ButtonColors = ButtonDefaults.buttonColors(
            containerColor = player.getNPBackground(),
            contentColor = player.getNPOnBackground()
        )

        LaunchedEffect(thumb_image) {
            palette_colours = null
            palette_colours = thumb_image?.generatePalette(8)
        }

        AnimatedVisibility(
            !colourpick_requested,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            BoxWithConstraints {
                Row(Modifier.fillMaxSize().padding(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ScrollBarLazyColumn(
                        Modifier.fillMaxHeight(),
                        contentPadding = PaddingValues(top = 10.dp, bottom = 10.dp, start = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
                        reverseScrollBarLayout = true
                    ) {
                        items(palette_colours ?: emptyList()) { colour ->
                            Button(
                                onClick = {
                                    onColourSelected(colour)
                                },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .requiredSize(40.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colour
                                )
                            ) {}
                        }
                    }

                    Column(
                        Modifier
                            .padding(vertical = 10.dp)
                            .heightIn(min = this@BoxWithConstraints.maxHeight)
                            .verticalScroll(rememberScrollState())
                    ) {
                        ValueSlider(
                            song.ThumbnailRounding.observe(player.database),
                            player.settings.theme.NOWPLAYING_DEFAULT_IMAGE_CORNER_ROUNDING.get(),
                            getString("song_theme_menu_corner_radius")
                        )

                        ValueSlider(
                            song.PlayerGradientDepth.observe(player.database),
                            player.settings.theme.NOWPLAYING_DEFAULT_GRADIENT_DEPTH.get(),
                            getString("song_theme_menu_gradient_depth")
                        )

                        ValueSlider(
                            song.BackgroundWaveSpeed.observe(player.database),
                            player.settings.theme.NOWPLAYING_DEFAULT_WAVE_SPEED.get(),
                            getString("song_theme_menu_wave_speed")
                        )

                        ValueSlider(
                            song.BackgroundWaveOpacity.observe(player.database),
                            player.settings.theme.NOWPLAYING_DEFAULT_WAVE_OPACITY.get(),
                            getString("song_theme_menu_wave_opacity")
                        )

                        if (isVideoPlaybackSupported()) {
                            val default_video_position: ThemeSettings.VideoPosition by player.settings.theme.NOWPLAYING_DEFAULT_VIDEO_POSITION.observe()
                            var song_video_position: ThemeSettings.VideoPosition? by song.VideoPosition.observe(player.database)

                            var show_position_selector: Boolean by remember { mutableStateOf(false) }
                            LargeDropdownMenu(
                                show_position_selector,
                                { show_position_selector = false },
                                ThemeSettings.VideoPosition.entries.size,
                                (song_video_position ?: default_video_position).ordinal,
                                {
                                    Text(ThemeSettings.VideoPosition.entries[it].getReadable())
                                }
                            ) {
                                song_video_position = ThemeSettings.VideoPosition.entries[it]
                                show_position_selector = false
                            }

                            FlowRow(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    getString("song_theme_menu_video_position"),
                                    fontSize = 15.sp,
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                )

                                Button({ show_position_selector = !show_position_selector }) {
                                    Text((song_video_position ?: default_video_position).getReadable())
                                }
                            }
                        }

                        ValueSlider(
                            song.BackgroundImageOpacity.observe(player.database),
                            player.settings.theme.NOWPLAYING_DEFAULT_BACKGROUND_IMAGE_OPACITY.get(),
                            getString("song_theme_menu_background_image_opacity")
                        )

                        if (player.form_factor == FormFactor.LANDSCAPE) {
                            ValueSlider(
                                song.LandscapeQueueOpacity.observe(player.database),
                                player.settings.theme.NOWPLAYING_DEFAULT_LANDSCAPE_QUEUE_OPACITY.get(),
                                getString("song_theme_menu_queue_opacity")
                            )
                        }

                        ValueSlider(
                            song.ShadowRadius.observe(player.database),
                            player.settings.theme.NOWPLAYING_DEFAULT_SHADOW_RADIUS.get(),
                            getString("song_theme_menu_image_shadow_radius")
                        )

                        Spacer(Modifier.fillMaxHeight().weight(1f))

                        Row(Modifier.fillMaxWidth()) {
                            IconButton(
                                {
                                    colourpick_requested = true
                                    requestColourPicker {
                                        colourpick_requested = false
                                        if (it != null) {
                                            onColourSelected(it)
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Colorize, null)
                            }

                            Spacer(Modifier.fillMaxWidth().weight(1f))

                            val notif_image_menu_button_text: String? = notifImagePlayerOverlayMenuButtonText()
                            if (notif_image_menu_button_text != null) {
                                Button({ openMenu(NotifImagePlayerOverlayMenu()) }, colors = button_colours) {
                                    Text(notif_image_menu_button_text)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ValueSlider(value_state: MutableState<Float?>, default_value: Float, title: String) {
    val player: PlayerState = LocalPlayerState.current
    val coroutine_scope: CoroutineScope = rememberCoroutineScope()

    val current_value: Float = value_state.value ?: default_value
    var slider_value: Float by remember { mutableStateOf(current_value) }

    var value_changed: Boolean by remember { mutableStateOf(false) }
    val anim_state: Animatable<Float, AnimationVector1D> = remember(value_state) { Animatable(current_value) }

    OnChangedEffect(anim_state.value) {
        value_state.value = anim_state.value
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.offset(y = 10.dp)) {
            Text(title, fontSize = 15.sp)

            Spacer(Modifier.fillMaxWidth().weight(1f))

            Text((current_value * 100).roundToInt().toString().padStart(3, ' '), fontSize = 15.sp)
        }

        val slider_colour: Color = player.theme.on_background

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
                    anim_state.animateTo(default_value)
                }
            }) {
                Icon(Icons.Filled.Refresh, null)
            }
        }
    }
}
