package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import dev.toastbits.composekit.settings.ui.item.GroupSettingsItem
import dev.toastbits.composekit.settings.ui.item.SettingsItem
import dev.toastbits.composekit.settings.ui.item.ToggleSettingsItem
import dev.toastbits.composekit.platform.PreferencesProperty
import com.toasterofbread.spmp.platform.AppContext
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.s_group_long_press_menu

internal fun getBehaviourCategoryItems(context: AppContext): List<SettingsItem> {
    return listOf(
        ToggleSettingsItem(
            context.settings.behaviour.OPEN_NP_ON_SONG_PLAYED
        ),

        ToggleSettingsItem(
            context.settings.behaviour.START_RADIO_ON_SONG_PRESS
        ),

        ToggleSettingsItem(
            context.settings.behaviour.MULTISELECT_CANCEL_ON_ACTION
        ),

        ToggleSettingsItem(
            context.settings.behaviour.MULTISELECT_CANCEL_ON_NONE_SELECTED
        ),

        ToggleSettingsItem(
            context.settings.behaviour.TREAT_SINGLES_AS_SONG
        ),

        ToggleSettingsItem(
            context.settings.behaviour.TREAT_ANY_SINGLE_ITEM_PLAYLIST_AS_SINGLE
        ),

        ToggleSettingsItem(
            context.settings.behaviour.SHOW_LIKES_PLAYLIST
        ),

        ToggleSettingsItem(
            context.settings.behaviour.SEARCH_SHOW_SUGGESTIONS
        ),

        ToggleSettingsItem(
            context.settings.behaviour.STOP_PLAYER_ON_APP_CLOSE
        ),

        GroupSettingsItem(Res.string.s_group_long_press_menu),

        ToggleSettingsItem(
            context.settings.behaviour.LPM_CLOSE_ON_ACTION
        ),

        ToggleSettingsItem(
            context.settings.behaviour.LPM_INCREMENT_PLAY_AFTER
        ),

        ToggleSettingsItem(
            context.settings.behaviour.DESKTOP_LPM_KEEP_ON_BACKGROUND_SCROLL
        )
    )
}
