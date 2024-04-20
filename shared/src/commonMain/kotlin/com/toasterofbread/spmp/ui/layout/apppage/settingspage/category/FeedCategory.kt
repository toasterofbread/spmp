package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import LocalPlayerState
import androidx.compose.runtime.remember
import dev.toastbits.composekit.settings.ui.item.SettingsItem
import dev.toastbits.composekit.settings.ui.item.ToggleSettingsItem
import dev.toastbits.composekit.platform.PreferencesProperty
import com.toasterofbread.spmp.model.deserialise
import com.toasterofbread.spmp.model.getString
import com.toasterofbread.spmp.model.serialise
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.AppSliderItem
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.AppStringSetItem
import com.toasterofbread.spmp.platform.AppContext
import dev.toastbits.ytmkt.uistrings.RawUiString
import dev.toastbits.ytmkt.uistrings.UiString

internal fun getFeedCategoryItems(context: AppContext): List<SettingsItem> {
    return listOf(
        ToggleSettingsItem(
            context.settings.feed.SHOW_ARTISTS_ROW
        ),

        ToggleSettingsItem(
            context.settings.feed.SHOW_SONG_DOWNLOAD_INDICATORS
        ),

        AppSliderItem(
            context.settings.feed.INITIAL_ROWS,
            "1",
            "10",
            range = 1f..10f,
            steps = 10
        ),

        AppSliderItem(
            context.settings.feed.SQUARE_PREVIEW_TEXT_LINES,
            "1",
            "5",
            range = 1f..5f
        ),

        AppSliderItem(
            context.settings.feed.GRID_ROW_COUNT,
            "1",
            "10",
            range = 1f..10f
        ),

        AppSliderItem(
            context.settings.feed.GRID_ROW_COUNT_EXPANDED,
            "1",
            "10",
            range = 1f..10f
        ),

        AppSliderItem(
            context.settings.feed.LANDSCAPE_GRID_ROW_COUNT,
            "1",
            "10",
            range = 1f..10f
        ),

        AppSliderItem(
            context.settings.feed.LANDSCAPE_GRID_ROW_COUNT_EXPANDED,
            "1",
            "10",
            range = 1f..10f
        ),

        ToggleSettingsItem(
            context.settings.feed.SHOW_RADIOS
        ),

        AppStringSetItem(
            context.settings.feed.HIDDEN_ROWS,
            getString("s_hidden_feed_rows_dialog_title"),
            itemToText = {
                val player = LocalPlayerState.current
                remember(it) {
                    UiString.deserialise(it).getString(player.context)
                }
            },
            textToItem = {
                RawUiString(it).serialise()
            }
        )
    )
}
