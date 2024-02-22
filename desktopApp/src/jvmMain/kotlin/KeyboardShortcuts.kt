import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.input.key.*
import androidx.compose.ui.window.WindowPlacement
import com.toasterofbread.composekit.platform.composable.onWindowBackPressed
import com.toasterofbread.spmp.model.settings.category.SystemSettings
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.apppage.AppPage
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.isTextFieldFocused
import com.toasterofbread.spmp.ui.layout.apppage.AppPageSidebarButton
import kotlin.math.roundToLong

// TODO | In-app list of shortcuts
// TODO | Configuration

private val NUMBER_KEYS: List<Key?> = listOf(
    Key.Zero, Key.One, Key.Two, Key.Three, Key.Four, Key.Five, Key.Six, Key.Seven, Key.Eight, Key.Nine, null
)
private const val SEEK_AMOUNT_MS: Long = 5000

internal fun PlayerState.processKeyEventShortcuts(
    event: KeyEvent,
    window: ComposeWindow,
    text_field_focus_state: Any
): Boolean {
    if (event.type == KeyEventType.KeyDown) {
        // System
        when (event.key) {
            Key.Escape -> return onWindowBackPressed()
            Key.F11 -> {
                if (window.placement == WindowPlacement.Fullscreen) {
                    window.placement = WindowPlacement.Floating
                }
                else {
                    window.placement = WindowPlacement.Fullscreen
                }
                return true
            }

            else -> {
                val number_index: Int = NUMBER_KEYS.indexOf(event.key)
                if (number_index != -1) {
                    if (event.isCtrlPressed) {
                        val page_index: Int = if (number_index == 0) 9 else number_index - 1
                        val page: AppPage? = AppPageSidebarButton.getShortcutButtonPage(page_index, this)
                        if (page != null) {
                            openAppPage(page)
                            return true
                        }
                    }
                    else {
                        withPlayer {
                            val seek_target: Long = (duration_ms * (number_index.toFloat() / NUMBER_KEYS.size)).roundToLong()
                            seekTo(seek_target)
                        }
                        return true
                    }
                }
            }
        }

        if (isTextFieldFocused(text_field_focus_state)) {
            return false
        }

        when (event.key) {
            // UI scale
            Key.Equals -> {
                if (event.isCtrlPressed) {
                    SystemSettings.Key.UI_SCALE.set(SystemSettings.Key.UI_SCALE.get<Float>() + 0.1f)
                    return true
                }
            }
            Key.Minus -> {
                if (event.isCtrlPressed) {
                    SystemSettings.Key.UI_SCALE.set((SystemSettings.Key.UI_SCALE.get<Float>() - 0.1f).coerceAtLeast(0.1f))
                    return true
                }
            }

            // Playback
            Key.Delete -> {
                if (event.isCtrlPressed) {
                    withPlayer {
                        if (current_song_index >= 0) {
                            removeFromQueue(current_song_index)
                        }
                    }
                    return true
                }
            }
            Key.Spacebar -> {
                withPlayer {
                    playPause()
                }
                return true
            }
            Key.DirectionLeft, Key.DirectionRight -> {
                withPlayer {
                    if (event.isCtrlPressed) {
                        if (event.key == Key.DirectionLeft) seekToPrevious() else seekToNext()
                    }
                    else {
                        seekBy(if (event.key == Key.DirectionLeft) -SEEK_AMOUNT_MS else SEEK_AMOUNT_MS)
                    }
                }
                return true
            }

            // Navigation
            Key.Tab -> {
                expansion.toggle()
                return true
            }
        }
    }

    return false
}
