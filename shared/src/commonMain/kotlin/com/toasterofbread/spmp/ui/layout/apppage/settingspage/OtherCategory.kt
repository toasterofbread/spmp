package com.toasterofbread.spmp.ui.layout.apppage.settingspage

import com.toasterofbread.composekit.settings.ui.item.SettingsGroupItem
import com.toasterofbread.composekit.settings.ui.item.SettingsItem
import com.toasterofbread.composekit.settings.ui.item.SettingsToggleItem
import com.toasterofbread.composekit.settings.ui.item.SettingsValueState
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.composekit.settings.ui.item.SettingsTextFieldItem
import kotlin.math.roundToInt

private fun getMusicTopBarGroup(): List<SettingsItem> {
    return listOf(
        SettingsGroupItem(getString("s_group_topbar")),

        SettingsToggleItem(
            SettingsValueState(Settings.KEY_TOPBAR_LYRICS_LINGER.name),
            getString("s_key_topbar_lyrics_linger"), getString("s_sub_topbar_lyrics_linger")
        ),

        AppSliderItem(
            SettingsValueState<Float>(Settings.KEY_TOPBAR_VISUALISER_WIDTH.name),
            getString("s_key_topbar_visualiser_width"), null,
            getValueText = { value ->
                "${(value.toFloat() * 100f).roundToInt()}%"
            }
        ),

        SettingsToggleItem(
            SettingsValueState(Settings.KEY_TOPBAR_SHOW_LYRICS_IN_QUEUE.name),
            getString("s_key_topbar_show_lyrics_in_queue"), null
        ),
        SettingsToggleItem(
            SettingsValueState(Settings.KEY_TOPBAR_SHOW_VISUALISER_IN_QUEUE.name),
            getString("s_key_topbar_show_visualiser_in_queue"), null
        ),

        SettingsToggleItem(
            SettingsValueState(Settings.KEY_TOPBAR_DISPLAY_OVER_ARTIST_IMAGE.name),
            getString("s_key_topbar_display_over_artist_image"), null
        )
    )
}

internal fun getOtherCategory(): List<SettingsItem> {
    return listOf(
        AppSliderItem(
            SettingsValueState(Settings.KEY_NAVBAR_HEIGHT_MULTIPLIER.name),
            getString("s_key_navbar_height_multiplier"),
            getString("s_sub_navbar_height_multiplier")
        ),

        SettingsToggleItem(
            SettingsValueState(Settings.KEY_SEARCH_SHOW_SUGGESTIONS.name),
            getString("s_key_search_show_suggestions"), null
        ),

        SettingsTextFieldItem(
            SettingsValueState(Settings.KEY_STATUS_WEBHOOK_URL.name),
            getString("s_key_status_webhook_url"),
            getString("s_sub_status_webhook_url")
        ),

        SettingsTextFieldItem(
            SettingsValueState(Settings.KEY_STATUS_WEBHOOK_PAYLOAD.name),
            getString("s_key_status_webhook_payload"),
            getString("s_sub_status_webhook_payload")
        )
    ) + getCachingGroup() + getMusicTopBarGroup()
}

private fun getCachingGroup(): List<SettingsItem> {
    return listOf(
        SettingsGroupItem(getString("s_group_caching")),
        SettingsToggleItem(
            SettingsValueState(Settings.KEY_THUMB_CACHE_ENABLED.name),
            getString("s_key_enable_thumbnail_cache"), null
        )
    )
}
