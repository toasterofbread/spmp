package com.toasterofbread.spmp.ui.layout.apppage.settingspage

import com.toasterofbread.composesettings.ui.item.SettingsAccessibilityServiceItem
import com.toasterofbread.composesettings.ui.item.SettingsGroupItem
import com.toasterofbread.composesettings.ui.item.SettingsItem
import com.toasterofbread.composesettings.ui.item.SettingsSliderItem
import com.toasterofbread.composesettings.ui.item.SettingsToggleItem
import com.toasterofbread.composesettings.ui.item.SettingsValueState
import com.toasterofbread.spmp.PlayerAccessibilityService
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.resources.getString
import kotlin.math.roundToInt

private fun getMusicTopBarGroup(): List<SettingsItem> {
    return listOf(
        SettingsGroupItem(getString("s_group_topbar")),

        SettingsToggleItem(
            SettingsValueState(Settings.KEY_TOPBAR_LYRICS_LINGER.name),
            getString("s_key_topbar_lyrics_linger"), getString("s_sub_topbar_lyrics_linger")
        ),

        SettingsSliderItem(
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

private fun getAccessibilityServiceGroup(): List<SettingsItem> {
    if (!PlayerAccessibilityService.isSupported()) {
        return emptyList()
    }

    return listOf(
        SettingsGroupItem(getString("s_group_acc_service")),

        SettingsAccessibilityServiceItem(
            getString("s_acc_service_enabled"),
            getString("s_acc_service_disabled"),
            getString("s_acc_service_enable"),
            getString("s_acc_service_disable"),
            object : SettingsAccessibilityServiceItem.AccessibilityServiceBridge {
                override fun addEnabledListener(
                    listener: (Boolean) -> Unit,
                    context: PlatformContext
                ) {
                    PlayerAccessibilityService.addEnabledListener(listener, context)
                }

                override fun removeEnabledListener(
                    listener: (Boolean) -> Unit,
                    context: PlatformContext
                ) {
                    PlayerAccessibilityService.removeEnabledListener(listener, context)
                }

                override fun isEnabled(context: PlatformContext): Boolean {
                    return PlayerAccessibilityService.isEnabled(context)
                }

                override fun setEnabled(enabled: Boolean, context: PlatformContext) {
                    if (!enabled) {
                        PlayerAccessibilityService.disable()
                        return
                    }

                    if (PlayerAccessibilityService.isSettingsPermissionGranted(context)) {
                        PlayerAccessibilityService.enable(context, true)
                        return
                    }

                    PlayerAccessibilityService.enableInteractive(context)
                }
            }
        ),

        SettingsToggleItem(
            SettingsValueState(Settings.KEY_ACC_VOL_INTERCEPT_NOTIFICATION.name),
            getString("s_key_vol_intercept_notification"),
            getString("s_key_vol_intercept_notification")
        ) { checked, _, allowChange ->
            if (!checked) {
                allowChange(true)
                return@SettingsToggleItem
            }

            if (!PlayerAccessibilityService.isOverlayPermissionGranted(this)) {
                PlayerAccessibilityService.requestOverlayPermission(this) { success ->
                    allowChange(success)
                }
                return@SettingsToggleItem
            }

            allowChange(true)
        }
    )
}

internal fun getOtherCategory(): List<SettingsItem> {
    return listOf(
        SettingsSliderItem(
            SettingsValueState(Settings.KEY_NAVBAR_HEIGHT_MULTIPLIER.name),
            getString("s_key_navbar_height_multiplier"),
            getString("s_sub_navbar_height_multiplier")
        ),

        SettingsToggleItem(
            SettingsValueState(Settings.KEY_SEARCH_SHOW_SUGGESTIONS.name),
            getString("s_key_search_show_suggestions"), null
        )
    ) + getCachingGroup() + getAccessibilityServiceGroup() + getMusicTopBarGroup()
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
