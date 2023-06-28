package com.spectre7.spmp.ui.layout.mainpage

import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.spectre7.spmp.model.*
import com.spectre7.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.spectre7.spmp.resources.uilocalisation.LocalisedYoutubeString
import com.spectre7.spmp.ui.component.MusicTopBarWithVisualiser
import com.spectre7.spmp.ui.layout.RadioBuilderIcon
import com.spectre7.spmp.ui.layout.YoutubeMusicLoginConfirmation
import com.spectre7.spmp.ui.theme.Theme

@Composable
fun MainPageTopBar(
    auth_info: YoutubeMusicAuthInfo,
    getFilterChips: () -> List<Pair<Int, String>>?,
    getSelectedFilterChip: () -> Int?,
    onFilterChipSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val player = LocalPlayerState.current

    Column(modifier.animateContentSize()) {
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

            IconButton({
                if (auth_info.initialised) {
                    player.onMediaItemClicked(auth_info.own_channel)
                } else {
                    show_login_confirmation = true
                }
            }) {
                Crossfade(auth_info) { info ->
                    if (auth_info.initialised) {
                        info.own_channel.Thumbnail(
                            MediaItemThumbnailProvider.Quality.LOW,
                            Modifier.clip(CircleShape).size(27.dp),
                            failure_icon = Icons.Default.Person
                        )
                    } else {
                        Icon(Icons.Default.Person, null)
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
                FilterChipsRow(getFilterChips, getSelectedFilterChip, onFilterChipSelected)
            }
        }
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
private fun ColumnScope.FilterChipsRow(getFilterChips: () -> List<Pair<Int, String>>?, getSelectedFilterChip: () -> Int?, onFilterChipSelected: (Int?) -> Unit) {
    val enabled: Boolean by mutableSettingsState(Settings.KEY_FEED_SHOW_FILTERS)
    val filter_chips = getFilterChips()

    AnimatedVisibility(enabled && filter_chips?.isNotEmpty() == true) {
        LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val selected_filter_chip = getSelectedFilterChip()

            items(filter_chips?.size ?: 0) { i ->
                val chip_index = filter_chips!![i].first
                ElevatedFilterChip(
                    i == selected_filter_chip,
                    {
                        onFilterChipSelected(if (i == selected_filter_chip) null else i)
                    },
                    { Text(LocalisedYoutubeString.filterChip(chip_index)) },
                    colors = with(Theme.current) {
                        FilterChipDefaults.elevatedFilterChipColors(
                            containerColor = background,
                            labelColor = on_background,
                            selectedContainerColor = accent,
                            selectedLabelColor = on_accent
                        )
                    },
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = Theme.current.on_background
                    )
                )
            }
        }
    }
}
