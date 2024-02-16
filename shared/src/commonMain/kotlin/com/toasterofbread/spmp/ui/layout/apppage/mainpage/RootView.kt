@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package com.toasterofbread.spmp.ui.layout.apppage.mainpage

import LocalPlayerState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.toasterofbread.composekit.utils.common.addUnique
import com.toasterofbread.composekit.utils.common.thenIf
import com.toasterofbread.spmp.model.settings.category.ThemeSettings
import com.toasterofbread.spmp.platform.form_factor
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.nowplaying.maintab.getMinimisedPlayerHeight
import com.toasterofbread.spmp.ui.layout.nowplaying.maintab.getMinimisedPlayerVPadding

val MINIMISED_NOW_PLAYING_HEIGHT_DP: Float
    @Composable get() = LocalPlayerState.current.form_factor.getMinimisedPlayerHeight().value
val MINIMISED_NOW_PLAYING_V_PADDING_DP: Float
    @Composable get() = LocalPlayerState.current.form_factor.getMinimisedPlayerVPadding().value

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

    val window_transparency_enabled: Boolean = ThemeSettings.Key.ENABLE_WINDOW_TRANSPARENCY.get()
    val background_opacity: Float by ThemeSettings.Key.WINDOW_BACKGROUND_OPACITY.rememberMutableState()

    Canvas(Modifier.fillMaxSize()) {
        drawRect(
            player.theme.background.thenIf(window_transparency_enabled) { copy(alpha = background_opacity) },
            blendMode = BlendMode.SrcIn
        )
    }

    Column(Modifier.fillMaxSize()) {
        player.HomePage()
        player.NowPlaying()
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
