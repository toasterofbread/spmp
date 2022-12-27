package com.spectre7.spmp

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.media.AudioManager
import android.media.AudioManager.STREAM_MUSIC
import android.media.AudioManager.STREAM_VOICE_CALL
import android.view.KeyEvent
import android.view.KeyEvent.*
import android.view.ViewConfiguration.getLongPressTimeout
import android.view.accessibility.AccessibilityEvent


class PlayerAccessibilityService: AccessibilityService() {

    private var custom_volume_enabled = false

    private val pressed_time = mutableMapOf<Int, Long>(
        KEYCODE_VOLUME_UP to -1,
        KEYCODE_VOLUME_DOWN to -1
    )

    private val prefs_change_listener = OnSharedPreferenceChangeListener { prefs, key ->
        if (key == "custom_volume_enabled") {
            custom_volume_enabled = prefs.getBoolean("custom_volume_enabled", false)
            println(if (custom_volume_enabled) "ENABLED" else "DISABLED")
        }
    }

    override fun onCreate() {
        super.onCreate()
        getSharedPreferences("com.spectre7.spmp.PREFERENCES", Context.MODE_PRIVATE).registerOnSharedPreferenceChangeListener(prefs_change_listener)
    }

    override fun onDestroy() {
        super.onDestroy()
        getSharedPreferences("com.spectre7.spmp.PREFERENCES", Context.MODE_PRIVATE).unregisterOnSharedPreferenceChangeListener(prefs_change_listener)
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null) {
            return false
        }

        if (event.keyCode == KEYCODE_VOLUME_UP || event.keyCode == KEYCODE_VOLUME_DOWN) {
            if (event.action == ACTION_DOWN) {
                pressed_time[event.keyCode] = System.currentTimeMillis()
            }
            else if (event.action == ACTION_UP) {
                val intent = Intent(this, PlayerHost.PlayerService::class.java)
                intent.putExtra("action", SERVICE_INTENT_ACTIONS.BUTTON_VOLUME.ordinal)
                intent.putExtra("key_code", event.keyCode)
                intent.putExtra("long", (System.currentTimeMillis() - pressed_time[event.keyCode]!!) >= getLongPressTimeout())
                startService(intent)
            }
            return true
        }

        return false
    }

    override fun onAccessibilityEvent(p0: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    override fun onServiceConnected() {
        val info = AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK
        info.notificationTimeout = 50
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        info.flags = AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        serviceInfo = info
    }
}