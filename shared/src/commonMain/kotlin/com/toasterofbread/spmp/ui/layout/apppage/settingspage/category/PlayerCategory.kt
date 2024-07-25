package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import dev.toastbits.composekit.settings.ui.item.GroupSettingsItem
import dev.toastbits.composekit.settings.ui.item.SettingsItem
import dev.toastbits.composekit.settings.ui.item.MultipleChoiceSettingsItem
import dev.toastbits.composekit.settings.ui.item.ToggleSettingsItem
import dev.toastbits.composekit.platform.PreferencesProperty
import com.toasterofbread.spmp.model.settings.category.NowPlayingQueueRadioInfoPosition
import com.toasterofbread.spmp.model.settings.category.NowPlayingQueueWaveBorderMode
import com.toasterofbread.spmp.model.settings.category.OverscrollClearMode
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.AppSliderItem
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.PlayerOverlayMenuAction
import com.toasterofbread.spmp.platform.AppContext
import dev.toastbits.composekit.utils.common.toCustomResource
import kotlin.enums.enumEntries
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.s_option_wave_border_mode_time
import spmp.shared.generated.resources.s_option_wave_border_mode_time_sync
import spmp.shared.generated.resources.s_option_wave_border_mode_scroll
import spmp.shared.generated.resources.s_option_wave_border_mode_none
import spmp.shared.generated.resources.s_option_wave_border_mode_line

internal fun getPlayerCategoryItems(context: AppContext): List<SettingsItem> {
    return listOf(
        AppSliderItem(
            context.settings.state.EXPAND_SWIPE_SENSITIVITY,
            range = 0.1f .. 10f
        ),

        ToggleSettingsItem(
            context.settings.state.MINI_SHOW_PREV_BUTTON
        ),

        ToggleSettingsItem(
            context.settings.state.MINI_OVERSCROLL_CLEAR_ENABLED
        ),

        AppSliderItem(
            context.settings.state.MINI_OVERSCROLL_CLEAR_TIME,
            range = 0f .. 1f
        ),

        MultipleChoiceSettingsItem(
            context.settings.state.MINI_OVERSCROLL_CLEAR_MODE
        ) { mode ->
            mode.getReadable()
        },

        GroupSettingsItem(null),

        ToggleSettingsItem(
            context.settings.state.SHOW_REPEAT_SHUFFLE_BUTTONS,
            title_max_lines = 2
        ),

        ToggleSettingsItem(
            context.settings.state.SHOW_SEEK_BAR_GRADIENT,
            title_max_lines = 2
        ),

        ToggleSettingsItem(
            context.settings.state.LANDSCAPE_SWAP_CONTROLS_AND_IMAGE,
            title_max_lines = 2
        ),

        MultipleChoiceSettingsItem(
            context.settings.state.OVERLAY_CUSTOM_ACTION,
        ) { action ->
            action.getReadable()
        },

        ToggleSettingsItem(
            context.settings.state.OVERLAY_SWAP_LONG_SHORT_PRESS_ACTIONS,
            title_max_lines = 2
        ),

        GroupSettingsItem(null),

        AppSliderItem(
            context.settings.state.QUEUE_ITEM_SWIPE_SENSITIVITY,
            range = 0.1f .. 2f
        ),

        AppSliderItem(
            context.settings.state.QUEUE_EXTRA_SIDE_PADDING,
            range = 0f .. 1f,
            min_label = "0%".toCustomResource(),
            max_label = "100%".toCustomResource(),
            getValueText = {
                (it as Float * 100).roundToInt().toString() + "%"
            }
        ),

        MultipleChoiceSettingsItem(
            context.settings.state.QUEUE_WAVE_BORDER_MODE,
        ) { mode ->
            when (mode) {
                NowPlayingQueueWaveBorderMode.TIME -> stringResource(Res.string.s_option_wave_border_mode_time)
                NowPlayingQueueWaveBorderMode.TIME_SYNC -> stringResource(Res.string.s_option_wave_border_mode_time_sync)
                NowPlayingQueueWaveBorderMode.SCROLL -> stringResource(Res.string.s_option_wave_border_mode_scroll)
                NowPlayingQueueWaveBorderMode.NONE -> stringResource(Res.string.s_option_wave_border_mode_none)
                NowPlayingQueueWaveBorderMode.LINE -> stringResource(Res.string.s_option_wave_border_mode_line)
            }
        },

        MultipleChoiceSettingsItem(
            context.settings.state.QUEUE_RADIO_INFO_POSITION
        ) { position ->
            position.getReadable()
        },

        ToggleSettingsItem(
            context.settings.state.PAUSE_ON_BT_DISCONNECT
        ),
        ToggleSettingsItem(
            context.settings.state.RESUME_ON_BT_CONNECT
        ),
        ToggleSettingsItem(
            context.settings.state.PAUSE_ON_WIRED_DISCONNECT
        ),
        ToggleSettingsItem(
            context.settings.state.RESUME_ON_WIRED_CONNECT
        )
    )
}
