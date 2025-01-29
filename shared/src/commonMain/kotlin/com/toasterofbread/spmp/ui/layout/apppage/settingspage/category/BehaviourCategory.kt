package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.AppSliderItem
import dev.toastbits.composekit.settingsitem.presentation.ui.component.item.GroupSettingsItem
import dev.toastbits.composekit.settingsitem.domain.SettingsItem
import dev.toastbits.composekit.settingsitem.presentation.ui.component.item.ToggleSettingsItem
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.s_group_long_press_menu

internal fun getBehaviourCategoryItems(context: AppContext): List<SettingsItem> {
    return listOf(
        ToggleSettingsItem(
            context.settings.Behaviour.OPEN_NP_ON_SONG_PLAYED
        ),

        ToggleSettingsItem(
            context.settings.Behaviour.START_RADIO_ON_SONG_PRESS
        ),

        ToggleSettingsItem(
            context.settings.Behaviour.MULTISELECT_CANCEL_ON_ACTION
        ),

        ToggleSettingsItem(
            context.settings.Behaviour.MULTISELECT_CANCEL_ON_NONE_SELECTED
        ),

        ToggleSettingsItem(
            context.settings.Behaviour.TREAT_SINGLES_AS_SONG
        ),

        ToggleSettingsItem(
            context.settings.Behaviour.TREAT_ANY_SINGLE_ITEM_PLAYLIST_AS_SINGLE
        ),

        ToggleSettingsItem(
            context.settings.Behaviour.SHOW_LIKES_PLAYLIST
        ),

        ToggleSettingsItem(
            context.settings.Behaviour.SEARCH_SHOW_SUGGESTIONS
        ),

        ToggleSettingsItem(
            context.settings.Behaviour.STOP_PLAYER_ON_APP_CLOSE
        ),

        ToggleSettingsItem(
            context.settings.Behaviour.INCLUDE_PLAYBACK_POSITION_IN_SHARE_URL
        ),

        AppSliderItem(
            context.settings.Behaviour.REPEAT_SONG_ON_PREVIOUS_THRESHOLD_S,
            range = -1f .. 10f
        ),

        GroupSettingsItem(Res.string.s_group_long_press_menu),

        ToggleSettingsItem(
            context.settings.Behaviour.LPM_CLOSE_ON_ACTION
        ),

        ToggleSettingsItem(
            context.settings.Behaviour.LPM_INCREMENT_PLAY_AFTER
        ),

        ToggleSettingsItem(
            context.settings.Behaviour.DESKTOP_LPM_KEEP_ON_BACKGROUND_SCROLL
        )
    )
}
