package com.toasterofbread.spmp.ui.layout.nowplaying.overlay.songtheme

import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.createImageBitmapUtil
import dev.toastbits.composekit.util.generatePalette
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingPage
import com.toasterofbread.spmp.ui.layout.nowplaying.getNPBackground
import com.toasterofbread.spmp.ui.layout.nowplaying.getNPOnBackground
import com.toasterofbread.spmp.ui.layout.nowplaying.maintab.thumbnailrow.ColourpickCallback
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.NotifImagePlayerOverlayMenu
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.PlayerOverlayMenu
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.notifImagePlayerOverlayMenuButtonText
import dev.toastbits.composekit.components.platform.composable.ScrollBarLazyColumn
import dev.toastbits.composekit.util.thenIf
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

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

            palette_colours = thumb_image?.let {
                createImageBitmapUtil()?.generatePalette(it , 8)
            }
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
                        horizontalAlignment = Alignment.CenterHorizontally,
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
                                    .requiredSize(width = 48.dp, height = 40.dp)
                                    .padding(horizontal = 4.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colour
                                )
                            ) {}
                        }

                        item {
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
                        }
                    }

                    Column(
                        Modifier
                            .heightIn(min = this@BoxWithConstraints.maxHeight)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Spacer(Modifier.height(10.dp))

                        val sections: Map<StringResource, List<SongThemeOption>> = remember { SongThemeOption.getSections() }
                        for ((index, title, items) in sections.withIndex()) {
                            Text(
                                stringResource(title),
                                Modifier.padding(bottom = 5.dp),
                                style = MaterialTheme.typography.labelLarge
                            )

                            Column(
                                Modifier
                                    .thenIf(index + 1 != sections.size) {
                                        padding(bottom = 20.dp)
                                    }
                                    .border(1.dp, player.theme.onBackground, RoundedCornerShape(5.dp))
                                    .padding(5.dp)
                            ) {
                                for (item in items) {
                                    item.Content(song, Modifier)
                                }
                            }
                        }

                        val notif_image_menu_button_text: String? = notifImagePlayerOverlayMenuButtonText()
                        if (notif_image_menu_button_text != null) {
                            Button({ openMenu(NotifImagePlayerOverlayMenu()) }, colors = button_colours) {
                                Text(notif_image_menu_button_text)
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

fun <K, V> Map<K, V>.withIndex(): List<Triple<Int, K, V>> =
    entries.withIndex().map { Triple(it.index, it.value.key, it.value.value) }
