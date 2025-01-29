package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import LocalPlayerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.toastbits.composekit.settingsitem.domain.SettingsItem
import dev.toastbits.composekit.settingsitem.presentation.ui.component.item.ToggleSettingsItem
import com.toasterofbread.spmp.model.deserialise
import com.toasterofbread.spmp.model.getString
import com.toasterofbread.spmp.model.serialise
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.AppSliderItem
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.AppStringSetItem
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import dev.toastbits.composekit.util.toCustomResource
import dev.toastbits.ytmkt.uistrings.RawUiString
import dev.toastbits.ytmkt.uistrings.UiString
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.s_hidden_feed_rows_dialog_title

internal fun getFeedCategoryItems(context: AppContext): List<SettingsItem> {
    return listOf(
        ToggleSettingsItem(
            context.settings.Feed.SHOW_ARTISTS_ROW
        ),

        ToggleSettingsItem(
            context.settings.Feed.SHOW_SONG_DOWNLOAD_INDICATORS
        ),

        AppSliderItem(
            context.settings.Feed.INITIAL_ROWS,
            "1".toCustomResource(),
            "10".toCustomResource(),
            range = 1f..10f,
            steps = 10
        ),

        AppSliderItem(
            context.settings.Feed.SQUARE_PREVIEW_TEXT_LINES,
            "1".toCustomResource(),
            "5".toCustomResource(),
            range = 1f..5f
        ),

        AppSliderItem(
            context.settings.Feed.GRID_ROW_COUNT,
            "1".toCustomResource(),
            "10".toCustomResource(),
            range = 1f..10f
        ),

        AppSliderItem(
            context.settings.Feed.GRID_ROW_COUNT_EXPANDED,
            "1".toCustomResource(),
            "10".toCustomResource(),
            range = 1f..10f
        ),

        AppSliderItem(
            context.settings.Feed.LANDSCAPE_GRID_ROW_COUNT,
            "1".toCustomResource(),
            "10".toCustomResource(),
            range = 1f..10f
        ),

        AppSliderItem(
            context.settings.Feed.LANDSCAPE_GRID_ROW_COUNT_EXPANDED,
            "1".toCustomResource(),
            "10".toCustomResource(),
            range = 1f..10f
        ),

        ToggleSettingsItem(
            context.settings.Feed.SHOW_RADIOS
        ),

        AppStringSetItem(
            context.settings.Feed.HIDDEN_ROWS,
            Res.string.s_hidden_feed_rows_dialog_title,
            itemToText = {
                val player: PlayerState = LocalPlayerState.current
                var text: String by remember { mutableStateOf("") }

                LaunchedEffect(it) {
                    text = UiString.deserialise(it).getString(player.context)
                }

                return@AppStringSetItem text
            },
            textToItem = {
                RawUiString(it).serialise()
            }
        )
    )
}
