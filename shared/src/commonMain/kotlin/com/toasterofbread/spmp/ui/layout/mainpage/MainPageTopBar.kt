package com.toasterofbread.spmp.ui.layout.mainpage

import LocalPlayerState
import SpMp
import androidx.compose.animation.Crossfade
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.*
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.ui.component.MusicTopBarWithVisualiser
import com.toasterofbread.spmp.ui.component.WaveBorder
import com.toasterofbread.spmp.ui.layout.YoutubeMusicLoginConfirmation
import com.toasterofbread.spmp.ui.layout.radiobuilder.RadioBuilderIcon
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.spmp.ui.layout.SearchPage

@Composable
fun MainPageTopBar(
    auth_info: YoutubeMusicAuthInfo,
    getFilterChips: () -> List<FilterChip>?,
    getSelectedFilterChip: () -> Int?,
    onFilterChipSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val player = LocalPlayerState.current

    Column(modifier) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            RadioBuilderButton()

            MusicTopBarWithVisualiser(
                Settings.INTERNAL_TOPBAR_MODE_HOME,
                Modifier.fillMaxSize().weight(1f),
                hide_while_inactive = false,
                can_show_visualiser = true
            )

            var show_login_confirmation by remember { mutableStateOf(false) }
            if (show_login_confirmation) {
                YoutubeMusicLoginConfirmation { manual ->
                    show_login_confirmation = false
                    if (manual == true) player.setOverlayPage(PlayerOverlayPage.YtmLoginPage(true))
                    else if (manual == false) player.setOverlayPage(PlayerOverlayPage.YtmLoginPage())
                }
            }

            Crossfade(player.main_page == MainPage.Library) { library_open ->
                IconButton({
                    if (library_open) {
                        player.navigateBack()
                    }
                    else {
                        player.setMainPage(MainPage.Library)
                    }
                }) {
                    if (library_open) {
                        Icon(Icons.Default.List, null)
                    }
                    else {
                        Icon(Icons.Default.MusicNote, null)
                    }
                }
            }
        }

        val multiselect_context = player.main_multiselect_context
        Crossfade(multiselect_context.is_active) { multiselect_active ->
            if (multiselect_active) {
                multiselect_context.InfoDisplay(Modifier.fillMaxWidth())
            }
            else {
                AnimatedVisibility(player.main_page != MainPage.Library) {
                    Crossfade(player.main_page) { page ->
                        when (page) {
                            is SearchPage -> {
                                Row {
                                    IconButton({ player.navigateBack() }) {
                                        Icon(Icons.Default.Close, null)
                                    }
                                    page.FilterBar(Modifier.fillMaxWidth().weight(1f))
                                }
                            }
                            else -> {
                                Row {
                                    IconButton({ player.setMainPage(MainPage.Search) }) {
                                        Icon(Icons.Default.Search, null)
                                    }
                                    FilterChipsRow(
                                        getFilterChips, getSelectedFilterChip, onFilterChipSelected, 
                                        Modifier.fillMaxWidth().weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        WaveBorder(Modifier.requiredWidth(SpMp.context.getScreenWidth()))
    }
}

@Composable
private fun RadioBuilderButton() {
    val player = LocalPlayerState.current
    IconButton({ player.setOverlayPage(PlayerOverlayPage.RadioBuilderPage) }) {
        RadioBuilderIcon()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChipsRow(
    getFilterChips: () -> List<FilterChip>?,
    getSelectedFilterChip: () -> Int?,
    onFilterChipSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val enabled: Boolean by mutableSettingsState(Settings.KEY_FEED_SHOW_FILTERS)
    val filter_chips = getFilterChips()

    Crossfade(if (enabled) filter_chips else null, modifier) { chips ->
        if (chips?.isNotEmpty() == true) {
            LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val selected_filter_chip = getSelectedFilterChip()

                items(chips.size) { i ->
                    ElevatedFilterChip(
                        i == selected_filter_chip,
                        {
                            onFilterChipSelected(if (i == selected_filter_chip) null else i)
                        },
                        { Text(chips[i].text.getString()) },
                        colors = with(Theme) {
                            FilterChipDefaults.elevatedFilterChipColors(
                                containerColor = background,
                                labelColor = on_background,
                                selectedContainerColor = accent,
                                selectedLabelColor = on_accent
                            )
                        },
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = Theme.on_background
                        )
                    )
                }
            }
        }
    }
}
