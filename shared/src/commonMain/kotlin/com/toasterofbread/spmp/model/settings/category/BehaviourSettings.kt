package com.toasterofbread.spmp.model.settings.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import dev.toastbits.composekit.platform.Platform
import dev.toastbits.composekit.platform.PreferencesProperty
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getBehaviourCategoryItems
import com.toasterofbread.spmp.platform.AppContext
import dev.toastbits.composekit.settings.ui.component.item.SettingsItem
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.s_key_open_np_on_song_played
import spmp.shared.generated.resources.s_sub_open_np_on_song_played
import spmp.shared.generated.resources.s_key_start_radio_on_song_press
import spmp.shared.generated.resources.s_sub_start_radio_on_song_press
import spmp.shared.generated.resources.s_key_multiselect_cancel_on_action
import spmp.shared.generated.resources.s_sub_multiselect_cancel_on_action
import spmp.shared.generated.resources.s_key_multiselect_cancel_on_none_selected
import spmp.shared.generated.resources.s_key_treat_singles_as_song
import spmp.shared.generated.resources.s_sub_treat_singles_as_song
import spmp.shared.generated.resources.s_key_treat_any_single_item_playlist_as_single
import spmp.shared.generated.resources.s_key_show_likes_playlist
import spmp.shared.generated.resources.s_key_search_show_suggestions
import spmp.shared.generated.resources.s_key_stop_player_on_app_close
import spmp.shared.generated.resources.s_sub_stop_player_on_app_close
import spmp.shared.generated.resources.s_key_lpm_close_on_action
import spmp.shared.generated.resources.s_key_lpm_increment_play_after
import spmp.shared.generated.resources.s_key_desktop_lpm_keep_on_background_scroll
import spmp.shared.generated.resources.s_sub_desktop_lpm_keep_on_background_scroll
import spmp.shared.generated.resources.s_cat_behaviour
import spmp.shared.generated.resources.s_cat_desc_behaviour

class BehaviourSettings(val context: AppContext): SettingsGroup("BEHAVIOUR", context.getPrefs()) {
    val OPEN_NP_ON_SONG_PLAYED: PreferencesProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_open_np_on_song_played) },
        getDescription = { stringResource(Res.string.s_sub_open_np_on_song_played) },
        getDefaultValue = { true }
    )
    val START_RADIO_ON_SONG_PRESS: PreferencesProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_start_radio_on_song_press) },
        getDescription = { stringResource(Res.string.s_sub_start_radio_on_song_press) },
        getDefaultValue = { true }
    )
    val MULTISELECT_CANCEL_ON_ACTION: PreferencesProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_multiselect_cancel_on_action) },
        getDescription = { stringResource(Res.string.s_sub_multiselect_cancel_on_action) },
        getDefaultValue = { true }
    )
    val MULTISELECT_CANCEL_ON_NONE_SELECTED: PreferencesProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_multiselect_cancel_on_none_selected) },
        getDescription = { null },
        getDefaultValue = { true }
    )
    val TREAT_SINGLES_AS_SONG: PreferencesProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_treat_singles_as_song) },
        getDescription = { stringResource(Res.string.s_sub_treat_singles_as_song) },
        getDefaultValue = { false }
    )
    val TREAT_ANY_SINGLE_ITEM_PLAYLIST_AS_SINGLE: PreferencesProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_treat_any_single_item_playlist_as_single) },
        getDescription = { null },
        getDefaultValue = { false }
    )
    val SHOW_LIKES_PLAYLIST: PreferencesProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_show_likes_playlist) },
        getDescription = { null },
        getDefaultValue = { true }
    )
    val SEARCH_SHOW_SUGGESTIONS: PreferencesProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_search_show_suggestions) },
        getDescription = { null },
        getDefaultValue = { true }
    )
    val STOP_PLAYER_ON_APP_CLOSE: PreferencesProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_stop_player_on_app_close) },
        getDescription = { stringResource(Res.string.s_sub_stop_player_on_app_close) },
        getDefaultValue = { false }
    )
    val LPM_CLOSE_ON_ACTION: PreferencesProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_lpm_close_on_action) },
        getDescription = { null },
        getDefaultValue = { true }
    )
    val LPM_INCREMENT_PLAY_AFTER: PreferencesProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_lpm_increment_play_after) },
        getDescription = { null },
        getDefaultValue = { true }
    )
    val DESKTOP_LPM_KEEP_ON_BACKGROUND_SCROLL: PreferencesProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_desktop_lpm_keep_on_background_scroll) },
        getDescription = { stringResource(Res.string.s_sub_desktop_lpm_keep_on_background_scroll) },
        getDefaultValue = { false },
        isHidden = {
            !Platform.DESKTOP.isCurrent()
        }
    )

    @Composable
    override fun getTitle(): String = stringResource(Res.string.s_cat_behaviour)

    @Composable
    override fun getDescription(): String = stringResource(Res.string.s_cat_desc_behaviour)

    @Composable
    override fun getIcon(): ImageVector = Icons.Outlined.TouchApp

    override fun getConfigurationItems(): List<SettingsItem> = getBehaviourCategoryItems(context)
}
