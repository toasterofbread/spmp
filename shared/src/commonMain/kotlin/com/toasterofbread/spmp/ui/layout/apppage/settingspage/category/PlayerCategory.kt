package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import dev.toastbits.composekit.settingsitem.presentation.ui.component.item.GroupSettingsItem
import dev.toastbits.composekit.settingsitem.domain.SettingsItem
import dev.toastbits.composekit.settingsitem.presentation.ui.component.item.MultipleChoiceSettingsItem
import dev.toastbits.composekit.settingsitem.presentation.ui.component.item.ToggleSettingsItem
import com.toasterofbread.spmp.model.settings.category.NowPlayingQueueWaveBorderMode
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.AppSliderItem
import com.toasterofbread.spmp.platform.AppContext
import dev.toastbits.composekit.util.toCustomResource
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
            context.settings.Player.EXPAND_SWIPE_SENSITIVITY,
            range = 0.1f .. 10f
        ),

        ToggleSettingsItem(
            context.settings.Player.MINI_SHOW_PREV_BUTTON
        ),

        ToggleSettingsItem(
            context.settings.Player.MINI_OVERSCROLL_CLEAR_ENABLED
        ),

        AppSliderItem(
            context.settings.Player.MINI_OVERSCROLL_CLEAR_TIME,
            range = 0f .. 1f
        ),

        MultipleChoiceSettingsItem(
            context.settings.Player.MINI_OVERSCROLL_CLEAR_MODE
        ) { mode ->
            mode.getReadable()
        },

        GroupSettingsItem(null),

        ToggleSettingsItem(
            context.settings.Player.SHOW_REPEAT_SHUFFLE_BUTTONS
        ),

        ToggleSettingsItem(
            context.settings.Player.SHOW_SEEK_BAR_GRADIENT
        ),

        ToggleSettingsItem(
            context.settings.Player.LANDSCAPE_SWAP_CONTROLS_AND_IMAGE
        ),

        MultipleChoiceSettingsItem(
            context.settings.Player.OVERLAY_CUSTOM_ACTION,
        ) { action ->
            action.getReadable()
        },

        ToggleSettingsItem(
            context.settings.Player.OVERLAY_SWAP_LONG_SHORT_PRESS_ACTIONS
        ),

        GroupSettingsItem(null),

        AppSliderItem(
            context.settings.Player.QUEUE_ITEM_SWIPE_SENSITIVITY,
            range = 0.1f .. 2f
        ),

        AppSliderItem(
            context.settings.Player.QUEUE_EXTRA_SIDE_PADDING,
            range = 0f .. 1f,
            min_label = "0%".toCustomResource(),
            max_label = "100%".toCustomResource(),
            getValueText = {
                (it as Float * 100).roundToInt().toString() + "%"
            }
        ),

        MultipleChoiceSettingsItem(
            context.settings.Player.QUEUE_WAVE_BORDER_MODE,
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
            context.settings.Player.QUEUE_RADIO_INFO_POSITION
        ) { position ->
            position.getReadable()
        },

        ToggleSettingsItem(
            context.settings.Player.PAUSE_ON_BT_DISCONNECT
        ),
        ToggleSettingsItem(
            context.settings.Player.RESUME_ON_BT_CONNECT
        ),
        ToggleSettingsItem(
            context.settings.Player.PAUSE_ON_WIRED_DISCONNECT
        ),
        ToggleSettingsItem(
            context.settings.Player.RESUME_ON_WIRED_CONNECT
        )
    )
}
