package com.toasterofbread.spmp.ui.layout.apppage.library.pageselector

import LocalPlayerState
import androidx.compose.animation.core.tween
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.*
import com.toasterofbread.composekit.utils.common.amplify
import com.toasterofbread.composekit.utils.common.toInt
import com.toasterofbread.composekit.utils.composable.*
import com.toasterofbread.composekit.utils.composable.RowOrColumn
import com.toasterofbread.composekit.utils.modifier.scrollWithoutClip
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.LayoutSlot
import com.toasterofbread.spmp.ui.layout.apppage.library.LibraryAppPage

@Composable
fun LibraryAppPage.LibraryIconButtonPageSelector(
    slot: LayoutSlot,
    content_padding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val player: PlayerState = LocalPlayerState.current

    BoxWithConstraints {
        RowOrColumn(
            !slot.is_vertical,
            modifier = modifier
                .then(
                    if (slot.is_vertical) Modifier.heightIn(min = this@BoxWithConstraints.maxHeight).fillMaxHeight()
                    else Modifier.widthIn(min = this@BoxWithConstraints.maxWidth).fillMaxWidth()
                )
                .zIndex(1f)
                .drawWithContent {
                    drawContent()
                }
                .padding(content_padding)
                .width(50.dp)
                .scrollWithoutClip(
                    rememberScrollState(),
                    is_vertical = true
                )
        ) {
            SidebarButtonSelector(
                vertical = slot.is_vertical,
                selected_button = tabs.indexOf(current_tab).takeIf { it != -1 },
                buttons = tabs,
                indicator_colour = player.theme.vibrant_accent,
                scrolling = false,
                showButton = { tab ->
                    !tab.isHidden()
                }
            ) { _, tab ->
                val colour: Color =
                    if (tab == current_tab) player.theme.on_accent
                    else player.theme.on_background

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

            Spacer(
                Modifier.then(
                    if (slot.is_vertical) Modifier.fillMaxHeight()
                    else Modifier.fillMaxWidth()
                ).weight(1f)
            )

            val (main_button, alt_button) = remember(current_tab) { current_tab.getAltContentButtons() }

            SidebarButtonSelector(
                vertical = slot.is_vertical,
                selected_button = showing_alt_content.toInt(),
                buttons = listOf(false, true),
                indicator_colour = player.theme.vibrant_accent,
                scrolling = false,
                showButton = {
                    current_tab.canShowAltContent()
                },
                extraContent = { _, it ->
                    if (it) {
                        return@SidebarButtonSelector
                    }

                    current_tab.SideContent(showing_alt_content)

                    AnimatedVisibility(current_tab.enableSearching()) {
                        SearchButton(Icons.Default.FilterAlt)
                    }

                    BoxWithConstraints(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f, false)
                            .requiredHeight(0.dp)
                            .zIndex(-10f)
                    ) {
                        val field_size: DpSize = DpSize(250.dp, 65.dp)
                        val focus_requester: FocusRequester = remember { FocusRequester() }

                        this@SidebarButtonSelector.AnimatedVisibility(
                            showing_search_field && current_tab.enableSearching(),
                            Modifier
                                .requiredSize(field_size)
                                .offset(x = (field_size.width + maxWidth) / 2, y = (-45f / 2f).dp),
                            enter = slideInHorizontally() { -it * 2 },
                            exit = slideOutHorizontally() { -it * 2 }
                        ) {
                            LaunchedEffect(Unit) {
                                focus_requester.requestFocus()
                            }

                            Row(
                                Modifier
                                    .background(player.theme.background.amplify(0.025f), MaterialTheme.shapes.small)
                                    .requiredSize(field_size)
                                    .padding(10.dp)
                                    .padding(end = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                AnimatedVisibility(current_tab.enableSorting()) {
                                    SortButton()
                                }

                                ResizableOutlinedTextField(
                                    search_filter ?: "",
                                    { search_filter = it },
                                    Modifier.fillMaxWidth().weight(1f).focusRequester(focus_requester),
                                    singleLine = true
                                )
                            }
                        }
                    }

                    AnimatedVisibility(current_tab.canShowAltContent()) {
                        Spacer(Modifier.height(20.dp))
                    }
                }
            ) { _, show ->
                val colour: Color =
                    if (show == showing_alt_content) player.theme.on_accent
                    else player.theme.on_background

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
