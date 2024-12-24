@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package com.toasterofbread.spmp.ui.layout.apppage.mainpage

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import dev.toastbits.composekit.util.addUnique
import dev.toastbits.composekit.util.thenIf
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.nowplaying.maintab.getMinimisedPlayerHeight
import com.toasterofbread.spmp.ui.layout.nowplaying.maintab.getMinimisedPlayerVPadding
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlaying
import com.toasterofbread.spmp.platform.FormFactor

val MINIMISED_NOW_PLAYING_HEIGHT_DP: Float
    @Composable get() = FormFactor.observe().value.getMinimisedPlayerHeight().value
val MINIMISED_NOW_PLAYING_V_PADDING_DP: Float
    @Composable get() = FormFactor.observe().value.getMinimisedPlayerVPadding().value

private val LocalFocusedTextFieldOwners: ProvidableCompositionLocal<MutableList<Any>> = staticCompositionLocalOf { mutableStateListOf() }

@Composable
fun RootView(player: PlayerState) {
    val density: Density = LocalDensity.current
    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                with(density) {
                    player.screen_size = DpSize(
                        size.width.toDp(),
                        size.height.toDp()
                    )
                }
            }
    )

    var window_transparency_enabled: Boolean by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        window_transparency_enabled = player.settings.Theme.ENABLE_WINDOW_TRANSPARENCY.get()
    }

    Canvas(Modifier.fillMaxSize()) {
        drawRect(
            player.theme.background,
            blendMode = BlendMode.SrcIn
        )
    }

    Box(Modifier.fillMaxSize()) {
        player.HomePage()
        NowPlaying(Modifier.fillMaxSize())
    }

    player.PersistentContent()
}

@Composable
fun isTextFieldFocused(): Boolean =
    LocalFocusedTextFieldOwners.current.isNotEmpty()

@Composable
fun getTextFieldFocusState(): Any =
    LocalFocusedTextFieldOwners.current

fun isTextFieldFocused(state: Any): Boolean =
    (state as List<*>).isNotEmpty()

@Composable
fun Modifier.appTextField(focus_requester: FocusRequester = remember { FocusRequester() }): Modifier {
    val hotkey_disablers: MutableList<Any> = LocalFocusedTextFieldOwners.current

    DisposableEffect(Unit) {
        onDispose {
            hotkey_disablers.remove(focus_requester)
        }
    }

    return focusRequester(focus_requester)
        .onFocusChanged {
            if (it.isFocused) {
                hotkey_disablers.addUnique(focus_requester)
            }
            else {
                hotkey_disablers.remove(focus_requester)
            }
        }
}
