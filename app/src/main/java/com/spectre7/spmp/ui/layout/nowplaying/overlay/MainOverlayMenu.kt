import com.spectre7.spmp.R
import android.content.Intent
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.palette.graphics.Palette
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.PlayerServiceHost
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.ui.layout.nowplaying.NOW_PLAYING_MAIN_PADDING
import com.spectre7.spmp.ui.layout.nowplaying.overlay.DownloadOverlayMenu
import com.spectre7.spmp.ui.layout.nowplaying.overlay.OverlayMenu
import com.spectre7.spmp.ui.layout.nowplaying.overlay.PaletteSelectorOverlayMenu
import com.spectre7.spmp.ui.layout.nowplaying.overlay.lyrics.LyricsOverlayMenu
import com.spectre7.utils.Marquee
import com.spectre7.utils.sendToast
import com.spectre7.utils.vibrateShort

class MainOverlayMenu(
    val setOverlayMenu: (OverlayMenu?) -> Unit,
    val theme_palette: Palette?,
    val requestColourPicker: ((Color?) -> Unit) -> Unit,
    val onColourSelected: (Color) -> Unit,
    val screen_width_dp: Dp
): OverlayMenu() {

    override fun closeOnTap(): Boolean = true

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun Menu(
        song: Song,
        expansion: Float,
        openShutterMenu: (@Composable () -> Unit) -> Unit,
        close: () -> Unit,
        seek_state: Any
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            @Composable
            fun InfoField(name: String, value: String, shareable: Boolean) {
                OutlinedTextField(
                    TextFieldValue(value),
                    {},
                    enabled = false,
                    singleLine = true,
                    label = {
                        Text(name)
                    },
                    shape = RoundedCornerShape(30.dp),
                    colors = TextFieldDefaults.textFieldColors(
                        disabledIndicatorColor = MainActivity.theme.getAccent(),
                        disabledLabelColor = MainActivity.theme.getAccent(),
                        disabledTextColor = MainActivity.theme.getAccent(),
                        disabledTrailingIconColor = MainActivity.theme.getAccent(),
                        containerColor = Color.Transparent
                    ),
                    trailingIcon = {
                        if (shareable) {
                            Row(horizontalArrangement = Arrangement.End) {
                                val clipboard = LocalClipboardManager.current
                                IconButton(onClick = {
                                    clipboard.setText(AnnotatedString(value))
                                    sendToast("Copied ${name.lowercase()} to clipboard")
                                }) {
                                    Icon(Icons.Filled.ContentCopy, null, Modifier.size(20.dp))
                                }

                                val share_intent = Intent.createChooser(Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, value)
                                    type = "text/plain"
                                }, null)
                                IconButton(onClick = {
                                    MainActivity.context.startActivity(share_intent)
                                }) {
                                    Icon(Icons.Filled.Share, null, Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                )
            }

            if (PlayerServiceHost.status.m_song != null) {
                song.artist.PreviewLong(
                    content_colour = Color.White,
                    onClick = null,
                    onLongClick = null,
                    modifier = Modifier
                )

                val song = PlayerServiceHost.status.m_song!!
                InfoField("Original title", song.original_title, false)
                InfoField("Video id", song.id, true)
            }

            Spacer(
                Modifier
                    .fillMaxHeight()
                    .weight(1f))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {

                val button_modifier = Modifier
                    .background(
                        MainActivity.theme.getAccent(),
                        CircleShape
                    )
                    .size(42.dp)
                    .padding(8.dp)
                val button_colour = ColorFilter.tint(MainActivity.theme.getOnAccent())

                Box(
                    button_modifier.clickable {
                        setOverlayMenu(
                            PaletteSelectorOverlayMenu(theme_palette, requestColourPicker, onColourSelected)
                        )
                    }
                ) {
                    Image(
                        painterResource(R.drawable.ic_palette), null,
                        colorFilter = button_colour
                    )
                }

                Box(
                    button_modifier.combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { setOverlayMenu(
                            LyricsOverlayMenu(
                                (screen_width_dp - (NOW_PLAYING_MAIN_PADDING * 2) - (15.dp * expansion * 2)).value * 0.9.dp
                            )
                        ) },
                        onLongClick = { 
                            vibrateShort()
                            setOverlayMenu(
                                LyricsOverlayMenu(
                                    (screen_width_dp - (NOW_PLAYING_MAIN_PADDING * 2) - (15.dp * expansion * 2)).value * 0.9.dp
                                )
                            )
                        }
                    )
                ) {
                    Image(
                        painterResource(R.drawable.ic_music_note), null,
                        colorFilter = button_colour
                    )
                }

                Box(
                    button_modifier.clickable {
                        setOverlayMenu(DownloadOverlayMenu())
                    }
                ) {
                    Image(
                        painterResource(R.drawable.ic_download), null,
                        colorFilter = button_colour
                    )
                }
            }
        }
    }
}