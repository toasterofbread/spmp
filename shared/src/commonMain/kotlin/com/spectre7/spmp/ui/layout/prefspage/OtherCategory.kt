package com.spectre7.spmp.ui.layout.prefspage

import SpMp
import com.spectre7.settings.model.*
import com.spectre7.spmp.PlayerAccessibilityService
import com.spectre7.spmp.model.MusicTopBarMode
import com.spectre7.spmp.model.Settings
import com.spectre7.spmp.platform.PlatformContext
import com.spectre7.spmp.resources.getString
import com.spectre7.spmp.resources.getStringTODO
import kotlin.math.roundToInt

private fun getMusicTopBarGroup(): List<SettingsItem> {
    fun MusicTopBarMode.getString(): String = when (this) {
        MusicTopBarMode.VISUALISER -> getString("s_option_topbar_mode_visualiser")
        MusicTopBarMode.LYRICS -> getString("s_option_topbar_mode_lyrics")
        MusicTopBarMode.NONE -> getString("s_option_topbar_mode_none")
    }

    return listOf(
        SettingsGroup(getString("s_group_topbar")),

        SettingsItemToggle(
            SettingsValueState(Settings.KEY_TOPBAR_LYRICS_LINGER.name),
            getString("s_key_topbar_lyrics_linger"), getString("s_sub_topbar_lyrics_linger")
        ),

        SettingsItemSlider(
            SettingsValueState<Float>(Settings.KEY_TOPBAR_VISUALISER_WIDTH.name),
            getStringTODO("Visualiser width"), null,
            getValueText = { value ->
                "${(value * 100f).roundToInt()}%"
            }
        ),

        SettingsItemMultipleChoice(
            SettingsValueState(Settings.KEY_TOPBAR_DEFAULT_MODE_HOME.name),
            getString("s_key_topbar_default_mode_home"), null,
            MusicTopBarMode.values().size,
            true
        ) {
            MusicTopBarMode.values()[it].getString()
        },
        SettingsItemMultipleChoice(
            SettingsValueState(Settings.KEY_TOPBAR_DEFAULT_MODE_NOWPLAYING.name),
            getString("s_key_topbar_default_mode_nowplaying"), null,
            MusicTopBarMode.values().size,
            true
        ) {
            MusicTopBarMode.values()[it].getString()
        },

        SettingsItemToggle(
            SettingsValueState(Settings.KEY_TOPBAR_SHOW_IN_QUEUE.name),
            getString("s_key_topbar_show_in_queue"), null
        ),
        SettingsItemMultipleChoice(
            SettingsValueState(Settings.KEY_TOPBAR_DEFAULT_MODE_QUEUE.name),
            getString("s_key_topbar_default_mode_queue"), null,
            MusicTopBarMode.values().size,
            true
        ) {
            MusicTopBarMode.values()[it].getString()
        }
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

                    TODO()
//                    val dialog = AlertDialog.Builder(MainActivity.context)
//                    dialog.setCancelable(true)
//                    dialog.setTitle(getString("acc_ser_enable_dialog_title"))
//                    dialog.setMessage(getString("acc_ser_enable_dialog_body"))
//                    dialog.setPositiveButton(getString("acc_ser_enable_dialog_btn_root")) { _, _ ->
//                        PlayerAccessibilityService.enable(MainActivity.context, true)
//                    }
//                    dialog.setNeutralButton(getString("acc_ser_enable_dialog_btn_manual")) { _, _ ->
//                        PlayerAccessibilityService.enable(MainActivity.context, false)
//                    }
//                    dialog.setNegativeButton(getString("action_cancel")) { _, _ -> }
//                    dialog.create().show()
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
