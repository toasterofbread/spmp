package com.toasterofbread.spmp.ui.layout.apppage.library.pageselector

import LocalPlayerState
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.toasterofbread.composekit.utils.common.getContrasted
import com.toasterofbread.composekit.utils.composable.ResizableOutlinedTextField
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.apppage.library.LibraryAppPage
import com.toasterofbread.spmp.ui.layout.contentbar.LayoutSlot

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibraryAppPage.LibraryTextButtonPageSelector(
    slot: LayoutSlot,
    content_padding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val player: PlayerState = LocalPlayerState.current

    Column(modifier.padding(content_padding)) {
        Row(verticalAlignment = Alignment.CenterVertically) {

            AnimatedVisibility(current_tab.enableSearching()) {
                SearchButton()
            }

            Row(Modifier.fillMaxWidth().weight(1f)) {
                AnimatedVisibility(
                    showing_search_field && current_tab.enableSearching(),
                    enter = fadeIn() + expandHorizontally(clip = false)
                ) {
                    ResizableOutlinedTextField(
                        search_filter ?: "",
                        { search_filter = it },
                        Modifier.height(45.dp).fillMaxWidth().weight(1f),
                        singleLine = true
                    )
                }

                Row(Modifier.fillMaxWidth().weight(1f)) {
                    val shown_tabs = tabs.filter { !it.isHidden() }

                    for (tab in shown_tabs.withIndex()) {
                        Crossfade(tab.value == current_tab) { selected ->
                            Box(
                                Modifier
                                    .fillMaxWidth(
                                        1f / (shown_tabs.size - tab.index)
                                    )
                                    .padding(horizontal = 5.dp)
                            ) {
                                ElevatedFilterChip(
                                    selected,
                                    {
                                        setCurrentTab(tab.value)
                                    },
                                    {
                                        Box(Modifier.fillMaxWidth().padding(end = 8.dp), contentAlignment = Alignment.Center) {
                                            Icon(tab.value.getIcon(), null, Modifier.requiredSizeIn(minWidth = 20.dp, minHeight = 20.dp))
                                        }
                                    },
                                    colors = with(player.theme) {
                                        FilterChipDefaults.elevatedFilterChipColors(
                                            containerColor = background,
                                            labelColor = on_background,
                                            selectedContainerColor = accent,
                                            selectedLabelColor = on_accent
                                        )
                                    },
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = player.theme.on_background
                                    )
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(current_tab.enableSorting()) {
                SortButton()
            }
        }

        AnimatedVisibility(!current_tab.canShowAltContent(), Modifier.align(Alignment.End)) {
            current_tab.SideContent(showing_alt_content)
        }

        AnimatedVisibility(current_tab.canShowAltContent()) {
            Row(Modifier.padding(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                @Composable
                fun getButtonColours(current: Boolean) =
                    ButtonDefaults.buttonColors(
                        containerColor = if (current) player.theme.vibrant_accent else player.theme.vibrant_accent.copy(alpha = 0.1f),
                        contentColor = if (current) player.theme.vibrant_accent.getContrasted() else player.theme.on_background
                    )

                val (main_button, alt_button) = remember(current_tab) { current_tab.getAltContentButtons() }

                Button(
                    { showing_alt_content = false },
                    Modifier.fillMaxWidth(0.5f).weight(1f),
                    colors = getButtonColours(!showing_alt_content)
                ) {
                    Text(main_button.first, textAlign = TextAlign.Center)
                }

                Button(
                    { showing_alt_content = true },
                    Modifier.fillMaxWidth().weight(1f),
                    colors = getButtonColours(showing_alt_content)
                ) {
                    Text(alt_button.first, textAlign = TextAlign.Center)
                }

                current_tab.SideContent(showing_alt_content)
            }
        }
    }
}