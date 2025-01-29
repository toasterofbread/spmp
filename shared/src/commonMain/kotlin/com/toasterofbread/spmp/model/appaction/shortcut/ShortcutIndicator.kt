package com.toasterofbread.spmp.model.appaction.shortcut

import androidx.compose.runtime.Composable

@Composable
fun ShortcutIndicator() {
    TODO()
    // val shortcut_index: Int? = remember(button_type) { getButtonShortcutButton(button_type, player) }
    // if (shortcut_index == null) {
    //     return@IconButton
    // }

    // val show_shortcut_indicator: Boolean by remember(shortcut_index) { derivedStateOf {
    //     showing_shortcut_indices > shortcut_index
    // } }

    // AnimatedVisibility(
    //     show_shortcut_indicator,
    //     Modifier.offset(17.dp, 17.dp).zIndex(1f),
    //     enter = fadeIn(),
    //     exit = fadeOut()
    // ) {
    //     val indicator_colour: Color =
    //         if (button == current_button) player.theme.onAccent
    //         else player.theme.accent

    //     Box(
    //         Modifier.size(20.dp).background(indicator_colour, SHORTCUT_INDICATOR_SHAPE),
    //         contentAlignment = Alignment.Center
    //     ) {
    //         Text(
    //             (shortcut_index + 1).toString(),
    //             Modifier.offset(y = (-5).dp),
    //             fontSize = 10.sp,
    //             color = indicator_colour.getContrasted()
    //         )
    //     }
    // }
}
