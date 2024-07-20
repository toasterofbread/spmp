package com.toasterofbread.spmp.model.settings.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.runtime.Composable
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getPlayerCategoryItems
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.PlayerOverlayMenuAction
import com.toasterofbread.spmp.platform.AppContext
import dev.toastbits.composekit.platform.PlatformPreferences
import dev.toastbits.composekit.platform.PreferencesProperty
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.s_key_mini_player_show_prev_button
import spmp.shared.generated.resources.s_key_mini_player_overscroll_clear_enabled
import spmp.shared.generated.resources.s_key_mini_player_overscroll_clear_time
import spmp.shared.generated.resources.s_key_mini_player_overscroll_clear_mode
import spmp.shared.generated.resources.s_key_player_show_repeat_shuffle_buttons
import spmp.shared.generated.resources.s_sub_player_show_repeat_shuffle_buttons
import spmp.shared.generated.resources.s_key_player_show_progress_bar_gradient
import spmp.shared.generated.resources.s_key_player_landscape_swap_controls_and_image
import spmp.shared.generated.resources.s_key_player_overlay_menu_custom_action
import spmp.shared.generated.resources.s_sub_player_overlay_menu_custom_action
import spmp.shared.generated.resources.s_key_player_overlay_menu_swap_long_short_press_actions
import spmp.shared.generated.resources.s_key_np_queue_item_swipe_sensitivity
import spmp.shared.generated.resources.s_sub_np_queue_item_swipe_sensitivity
import spmp.shared.generated.resources.s_key_np_queue_extra_side_padding
import spmp.shared.generated.resources.s_sub_np_queue_extra_side_padding
import spmp.shared.generated.resources.s_key_np_queue_wave_border_mode
import spmp.shared.generated.resources.s_sub_np_queue_wave_border_mode
import spmp.shared.generated.resources.s_key_np_queue_radio_info_position
import spmp.shared.generated.resources.s_key_resume_on_bt_connect
import spmp.shared.generated.resources.s_sub_resume_on_bt_connect
import spmp.shared.generated.resources.s_key_pause_on_bt_disconnect
import spmp.shared.generated.resources.s_key_resume_on_wired_connect
import spmp.shared.generated.resources.s_sub_resume_on_wired_connect
import spmp.shared.generated.resources.s_key_pause_on_wired_disconnect
import spmp.shared.generated.resources.s_key_player_expand_swipe_sensitivity
import spmp.shared.generated.resources.s_cat_player
import spmp.shared.generated.resources.s_cat_desc_player
import spmp.shared.generated.resources.s_option_np_queue_radio_info_top_bar
import spmp.shared.generated.resources.s_option_np_queue_radio_info_above_items
import spmp.shared.generated.resources.s_option_mini_player_overscroll_clear_mode_always_hide
import spmp.shared.generated.resources.s_option_mini_player_overscroll_clear_mode_hide_if_queue_empty
import spmp.shared.generated.resources.s_option_mini_player_overscroll_clear_mode_none_if_queue_empty

class PlayerSettings(val context: AppContext): SettingsGroup("PLAYER", context.getPrefs()) {
    val MINI_SHOW_PREV_BUTTON: PreferencesProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_mini_player_show_prev_button) },
        getDescription = { null },
        getDefaultValue = { false }
    )
    val MINI_OVERSCROLL_CLEAR_ENABLED: PreferencesProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_mini_player_overscroll_clear_enabled) },
        getDescription = { null },
        getDefaultValue = { false }
    )
    val MINI_OVERSCROLL_CLEAR_TIME: PreferencesProperty<Float> by property(
        getName = { stringResource(Res.string.s_key_mini_player_overscroll_clear_time) },
        getDescription = { null },
        getDefaultValue = { 0.2f }
    )
    val MINI_OVERSCROLL_CLEAR_MODE: PreferencesProperty<OverscrollClearMode> by enumProperty(
        getName = { stringResource(Res.string.s_key_mini_player_overscroll_clear_mode) },
        getDescription = { null },
        getDefaultValue = { OverscrollClearMode.HIDE_IF_QUEUE_EMPTY }
    )
    val SHOW_REPEAT_SHUFFLE_BUTTONS: PreferencesProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_player_show_repeat_shuffle_buttons) },
        getDescription = { stringResource(Res.string.s_sub_player_show_repeat_shuffle_buttons) },
        getDefaultValue = { false }
    )
    val SHOW_SEEK_BAR_GRADIENT: PreferencesProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_player_show_progress_bar_gradient) },
        getDescription = { null },
        getDefaultValue = { true }
    )
    val LANDSCAPE_SWAP_CONTROLS_AND_IMAGE: PreferencesProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_player_landscape_swap_controls_and_image) },
        getDescription = { null },
        getDefaultValue = { false }
    )
    val OVERLAY_CUSTOM_ACTION: PreferencesProperty<PlayerOverlayMenuAction> by enumProperty(
        getName = { stringResource(Res.string.s_key_player_overlay_menu_custom_action) },
        getDescription = { stringResource(Res.string.s_sub_player_overlay_menu_custom_action) },
        getDefaultValue = { PlayerOverlayMenuAction.DEFAULT_CUSTOM }
    )
    val OVERLAY_SWAP_LONG_SHORT_PRESS_ACTIONS: PreferencesProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_player_overlay_menu_swap_long_short_press_actions) },
        getDescription = { null },
        getDefaultValue = { false }
    )
    val QUEUE_ITEM_SWIPE_SENSITIVITY: PreferencesProperty<Float> by property(
        getName = { stringResource(Res.string.s_key_np_queue_item_swipe_sensitivity) },
        getDescription = { stringResource(Res.string.s_sub_np_queue_item_swipe_sensitivity) },
        getDefaultValue = { 1f }
    )
    val QUEUE_EXTRA_SIDE_PADDING: PreferencesProperty<Float> by property(
        getName = { stringResource(Res.string.s_key_np_queue_extra_side_padding) },
        getDescription = { stringResource(Res.string.s_sub_np_queue_extra_side_padding) },
        getDefaultValue = { 0f }
    )
    val QUEUE_WAVE_BORDER_MODE: PreferencesProperty<NowPlayingQueueWaveBorderMode> by enumProperty(
        getName = { stringResource(Res.string.s_key_np_queue_wave_border_mode) },
        getDescription = { stringResource(Res.string.s_sub_np_queue_wave_border_mode) },
        getDefaultValue = { NowPlayingQueueWaveBorderMode.TIME }
    )
    val QUEUE_RADIO_INFO_POSITION: PreferencesProperty<NowPlayingQueueRadioInfoPosition> by enumProperty(
        getName = { stringResource(Res.string.s_key_np_queue_radio_info_position) },
        getDescription = { null },
        getDefaultValue = { NowPlayingQueueRadioInfoPosition.TOP_BAR }
    )
    val RESUME_ON_BT_CONNECT: PreferencesProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_resume_on_bt_connect) },
        getDescription = { stringResource(Res.string.s_sub_resume_on_bt_connect) },
        getDefaultValue = { true }
    )
    val PAUSE_ON_BT_DISCONNECT: PreferencesProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_pause_on_bt_disconnect) },
        getDescription = { null },
        getDefaultValue = { true }
    )
    val RESUME_ON_WIRED_CONNECT: PreferencesProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_resume_on_wired_connect) },
        getDescription = { stringResource(Res.string.s_sub_resume_on_wired_connect) },
        getDefaultValue = { true }
    )
    val PAUSE_ON_WIRED_DISCONNECT: PreferencesProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_pause_on_wired_disconnect) },
        getDescription = { null },
        getDefaultValue = { true }
    )
    val EXPAND_SWIPE_SENSITIVITY: PreferencesProperty<Float> by property(
        getName = { stringResource(Res.string.s_key_player_expand_swipe_sensitivity) },
        getDescription = { null },
        getDefaultValue = { 3.5f }
    )

    override val page: CategoryPage =
        SimplePage(
            { stringResource(Res.string.s_cat_player) },
            { stringResource(Res.string.s_cat_desc_player) },
            { getPlayerCategoryItems(context) },
            { Icons.Outlined.PlayArrow }
        )
}

enum class NowPlayingQueueRadioInfoPosition {
    TOP_BAR, ABOVE_ITEMS;

    @Composable
    fun getReadable(): String =
        when (this) {
            TOP_BAR -> stringResource(Res.string.s_option_np_queue_radio_info_top_bar)
            ABOVE_ITEMS -> stringResource(Res.string.s_option_np_queue_radio_info_above_items)
        }
}

enum class OverscrollClearMode {
    ALWAYS_HIDE,
    HIDE_IF_QUEUE_EMPTY,
    NONE_IF_QUEUE_EMPTY;

    @Composable
    fun getReadable(): String =
        when (this) {
            ALWAYS_HIDE -> stringResource(Res.string.s_option_mini_player_overscroll_clear_mode_always_hide)
            HIDE_IF_QUEUE_EMPTY -> stringResource(Res.string.s_option_mini_player_overscroll_clear_mode_hide_if_queue_empty)
            NONE_IF_QUEUE_EMPTY -> stringResource(Res.string.s_option_mini_player_overscroll_clear_mode_none_if_queue_empty)
        }
}

enum class NowPlayingQueueWaveBorderMode {
    TIME, TIME_SYNC, SCROLL, NONE, LINE
}
