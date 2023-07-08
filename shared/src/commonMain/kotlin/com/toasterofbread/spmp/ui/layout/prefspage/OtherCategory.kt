package com.toasterofbread.spmp.ui.layout.prefspage

import SpMp
import com.toasterofbread.settings.model.SettingsGroup
import com.toasterofbread.settings.model.SettingsItem
import com.toasterofbread.settings.model.SettingsItemAccessibilityService
import com.toasterofbread.settings.model.SettingsItemSlider
import com.toasterofbread.settings.model.SettingsItemToggle
import com.toasterofbread.settings.model.SettingsValueState
import com.toasterofbread.spmp.PlayerAccessibilityService
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.resources.getString
import kotlin.math.roundToInt

private fun getMusicTopBarGroup(): List<SettingsItem> {
    return listOf(
        SettingsGroup(getString("s_group_topbar")),

        SettingsItemToggle(
            SettingsValueState(Settings.KEY_TOPBAR_LYRICS_LINGER.name),
            getString("s_key_topbar_lyrics_linger"), getString("s_sub_topbar_lyrics_linger")
        ),

        SettingsItemSlider(
            SettingsValueState<Float>(Settings.KEY_TOPBAR_VISUALISER_WIDTH.name),
            getString("s_key_topbar_visualiser_width"), null,
            getValueText = { value ->
                "${(value * 100f).roundToInt()}%"
            }
        ),

        SettingsItemToggle(
            SettingsValueState(Settings.KEY_TOPBAR_SHOW_LYRICS_IN_QUEUE.name),
            getString("s_key_topbar_show_lyrics_in_queue"), null
        ),
        SettingsItemToggle(
            SettingsValueState(Settings.KEY_TOPBAR_SHOW_VISUALISER_IN_QUEUE.name),
            getString("s_key_topbar_show_visualiser_in_queue"), null
        ),

        SettingsItemToggle(
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
        SettingsGroup(getString("s_group_acc_service")),

        SettingsItemAccessibilityService(
            getString("s_acc_service_enabled"),
            getString("s_acc_service_disabled"),
            getString("s_acc_service_enable"),
            getString("s_acc_service_disable"),
            object : SettingsItemAccessibilityService.AccessibilityServiceBridge {
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

                override fun setEnabled(enabled: Boolean) {
                    if (!enabled) {
                        PlayerAccessibilityService.disable()
                        return
                    }

                    val context = SpMp.context
                    if (PlayerAccessibilityService.isSettingsPermissionGranted(context)) {
                        PlayerAccessibilityService.enable(context, true)
                        return
                    }

                    PlayerAccessibilityService.enableInteractive(context)
                }
            }
        ),

        SettingsItemToggle(
            SettingsValueState(Settings.KEY_ACC_VOL_INTERCEPT_NOTIFICATION.name),
            getString("s_key_vol_intercept_notification"),
            getString("s_key_vol_intercept_notification")
        ) { checked, _, allowChange ->
            if (!checked) {
                allowChange(true)
                return@SettingsItemToggle
            }

            if (!PlayerAccessibilityService.isOverlayPermissionGranted(SpMp.context)) {
                PlayerAccessibilityService.requestOverlayPermission(SpMp.context) { success ->
                    allowChange(success)
                }
                return@SettingsItemToggle
            }

            allowChange(true)
        }
    )
}

internal fun getOtherCategory(): List<SettingsItem> {
    return getCachingGroup() + getAccessibilityServiceGroup() + getMusicTopBarGroup()
}

private fun getCachingGroup(): List<SettingsItem> {
    return listOf(
        SettingsGroup(getString("s_group_caching")),
        SettingsItemToggle(
            SettingsValueState(Settings.KEY_THUMB_CACHE_ENABLED.name),
            getString("s_key_enable_thumbnail_cache"), null
        )
    )
}
