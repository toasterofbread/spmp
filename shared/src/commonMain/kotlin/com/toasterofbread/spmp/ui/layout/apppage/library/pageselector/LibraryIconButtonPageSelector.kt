package com.toasterofbread.spmp.ui.layout.apppage.library.pageselector

import LocalPlayerState
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.apppage.library.LibraryAppPage
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.LayoutSlot
import dev.toastbits.composekit.theme.core.onAccent
import dev.toastbits.composekit.theme.core.vibrantAccent
import dev.toastbits.composekit.util.*
import dev.toastbits.composekit.components.utils.composable.*
import dev.toastbits.composekit.components.utils.modifier.*
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.roundToInt

@Composable
fun LibraryAppPage.LibraryIconButtonPageSelector(
    slot: LayoutSlot,
    content_padding: PaddingValues,
    lazy: Boolean,
    modifier: Modifier = Modifier,
    show_page_buttons: Boolean = true,
    show_contextual_buttons: Boolean = true,
    show_source_buttons: Boolean = true,
    separate_source_and_contextual: Boolean = false
) {
    val player: PlayerState = LocalPlayerState.current
    val density: Density = LocalDensity.current

    var bar_height: Int by remember { mutableStateOf(0) }
    val filter_bar_height: Dp = 65.dp
    val getFilterBarAnimationOffset: (Int) -> Int = remember(slot) {{
        (if (slot.is_start) -it else it) * 2
    }}

    BoxWithOptionalConstraints(lazy) { constraints ->
        if (!slot.is_vertical && show_contextual_buttons) {
            Box(Modifier.requiredHeight(0.dp)) {
                AnimatedVisibility(
                    showing_search_field && current_tab.enableSearching(),
                    Modifier
                        .fillMaxWidth()
                        .requiredHeight(filter_bar_height)
                        .run {
                            offset {
                                IntOffset(
                                    x = 0,
                                    y = -bar_height / 2
                                )
                            }
                        }
                        ,
                    enter = slideInVertically() { getFilterBarAnimationOffset(it) },
                    exit = slideOutVertically() { getFilterBarAnimationOffset(it) }
                ) {
                    FilterBar(Modifier.padding(content_padding.horizontal).padding(horizontal = 20.dp))
                }
            }
        }

        RowOrColumn(
            !slot.is_vertical,
            modifier = modifier
                .thenIf(!slot.is_vertical) {
                    onSizeChanged {
                        bar_height = it.height
                    }
                }
                .thenWith(
                    constraints,
                    nullAction = {
                        if (slot.is_vertical) fillMaxHeight()
                        else fillMaxWidth()
                    },
                    action = {
                        if (slot.is_vertical) heightIn(min = it.maxHeight).fillMaxHeight()
                        else widthIn(min = it.maxWidth).fillMaxWidth()
                    }
                )
                .zIndex(1f)
                .drawWithContent {
                    drawContent()
                }
                .padding(content_padding)
                .run {
                    if (slot.is_vertical) width(50.dp)
                    else height(50.dp)
                }
                .scrollWithoutClip(
                    rememberScrollState(),
                    is_vertical = slot.is_vertical
                ),
            alignment = 0
        ) {
            if (show_page_buttons) {
                SidebarButtonSelector(
                    vertical = slot.is_vertical,
                    selected_button = tabs.indexOf(current_tab).takeIf { it != -1 },
                    buttons = tabs,
                    indicator_colour = player.theme.vibrantAccent,
                    scrolling = false,
                    showButton = { tab ->
                        !tab.isHidden()
                    }
                ) { _, tab ->
                    val colour: Color =
                        if (tab == current_tab) player.theme.onAccent
                        else player.theme.onBackground

                    CompositionLocalProvider(LocalContentColor provides colour) {
                        IconButton({
                            if (tab != current_tab) {
                                // val increasing: Boolean = tabs.indexOf(tab) > tabs.indexOf(current_tab)
                                current_tab = tab
                                // coroutine_scope.launchSingle {
                                    // wave_border_offset.animateTo(
                                    //     if (increasing) wave_border_offset.value + 20f
                                    //     else wave_border_offset.value - 20f,
                                    //     tween(500)
                                    // )
                                // }
                            }
                        }) {
                            Icon(tab.getIcon(), null, Modifier.requiredSizeIn(minWidth = 20.dp, minHeight = 20.dp))
                        }
                    }
                }
            }

            if (separate_source_and_contextual && (show_contextual_buttons || show_source_buttons)) {
                Spacer(
                    Modifier.then(
                        if (slot.is_vertical) Modifier.fillMaxHeight()
                        else Modifier.fillMaxWidth()
                    ).weight(1f)
                )
            }

            val (main_button, alt_button) = remember(current_tab) { current_tab.getAltContentButtons() }

            SidebarButtonSelector(
                vertical = slot.is_vertical,
                selected_button = showing_alt_content.toInt(),
                buttons = if (show_source_buttons) listOf(false, true) else emptyList(),
                indicator_colour = player.theme.vibrantAccent,
                scrolling = false,
                alignment = 0,
                showButton = {
                    current_tab.canShowAltContent()
                },
                extraContent = { _, it ->
                    if (it || !show_contextual_buttons) {
                        return@SidebarButtonSelector
                    }

                    with (current_tab) {
                        SideContent(showing_alt_content)
                    }

                    AnimatedVisibility(current_tab.enableSearching()) {
                        SearchButton(Icons.Default.FilterAlt)
                    }

                    if (slot.is_vertical) {
                        BoxWithConstraints(
                            Modifier
                                .fillMaxWidth()
                                .weight(1f, false)
                                .requiredHeight(0.dp)
                                .zIndex(-10f)
                        ) {
                            val filter_bar_size: DpSize = DpSize(250.dp, filter_bar_height)

                            this@SidebarButtonSelector.AnimatedVisibility(
                                showing_search_field && current_tab.enableSearching(),
                                Modifier
                                    .requiredSize(filter_bar_size)
                                    .run {
                                        if (slot.is_vertical) offset(x = (filter_bar_size.width + maxWidth) / 2, y = (-45f / 2f).dp)
                                        else offset(y = -(filter_bar_size.height))
                                    },
                                enter = slideInHorizontally() { getFilterBarAnimationOffset(it) },
                                exit = slideOutHorizontally() { getFilterBarAnimationOffset(it) }
                            ) {
                                FilterBar(Modifier.requiredSize(filter_bar_size))
                            }
                        }
                    }

                    AnimatedVisibility(current_tab.canShowAltContent()) {
                        Spacer(Modifier.height(20.dp))
                    }
                }
            ) { _, show ->
                val colour: Color =
                    if (show == showing_alt_content) player.theme.onAccent
                    else player.theme.onBackground

                CompositionLocalProvider(LocalContentColor provides colour) {
                    IconButton({ showing_alt_content = show }) {
                        Icon(
                            if (show) alt_button.second
                            else main_button.second,
                            null,
                            Modifier.requiredSizeIn(minWidth = 20.dp, minHeight = 20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryAppPage.FilterBar(modifier: Modifier = Modifier) {
    val player: PlayerState = LocalPlayerState.current
    val focus_requester: FocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focus_requester.requestFocus()
    }

    Row(
        modifier
            .background(player.theme.background.amplify(0.025f), MaterialTheme.shapes.small)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AnimatedVisibility(current_tab.enableSorting()) {
            SortButton()
        }

        val state: TextFieldState = remember { TextFieldState(search_filter ?: "") }

        LaunchedEffect(state) {
            snapshotFlow { state.text }
                .collectLatest {
                    search_filter = it.toString()
                }
        }

        OutlinedTextField(
            state,
            Modifier.fillMaxWidth().weight(1f).focusRequester(focus_requester),
            lineLimits = TextFieldLineLimits.SingleLine,
            contentPadding = OutlinedTextFieldDefaults.contentPadding().horizontal
        )
    }
}

// private fun ContentDrawScope.leftBorderContent(colour: Color, background_colour: Color, getOffset: () -> Float = { 0f }) {
//     val shape: Shape =
//         WaveShape(
//             (size.height / 10.dp.toPx()).toInt(),
//             getOffset() + (size.height / 2),
//             width_multiplier = 2f
//         )
//     val outline: Outline =
//         shape.createOutline(Size(size.height, 5.dp.toPx()), LayoutDirection.Ltr, this)

//     scale(2f, Offset.Zero) {
//         rotate(90f, Offset.Zero) {
//             translate(top = (-4).dp.toPx()) {
//                 drawOutline(outline, colour)
//                 translate(top = (-1).dp.toPx()) {
//                     drawOutline(outline, background_colour)
//                 }
//             }
//         }
//     }
// }
