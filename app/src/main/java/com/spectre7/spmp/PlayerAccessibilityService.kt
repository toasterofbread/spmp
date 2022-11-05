package com.spectre7.spmp

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.view.KeyEvent
import android.view.KeyEvent.*
import android.view.ViewConfiguration.getLongPressTimeout
import android.view.accessibility.AccessibilityEvent
import com.spectre7.utils.getString as getResString

class PlayerAccessibilityService: AccessibilityService() {

    private var custom_volume_enabled = false
    private lateinit var KEY_CUSTOM_VOLUME_ENABLED: String

    private val pressed_time = mutableMapOf<Int, Long>(
        KEYCODE_VOLUME_UP to -1,
        KEYCODE_VOLUME_DOWN to -1
    )

    private val prefs_change_listener = OnSharedPreferenceChangeListener { prefs, key ->
        if (key == KEY_CUSTOM_VOLUME_ENABLED) {
            custom_volume_enabled = prefs.getBoolean(KEY_CUSTOM_VOLUME_ENABLED, false)
            println(if (custom_volume_enabled) "ENABLED" else "DISABLED")
        }
    }

    override fun onCreate() {
        super.onCreate()
        KEY_CUSTOM_VOLUME_ENABLED = getResString(R.string.s_key_custom_volume_enabled, this)
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
                val serviceIntent = Intent(this, PlayerHost.PlayerService::class.java)
                serviceIntent.putExtra("action", SERVICE_INTENT_ACTIONS.BUTTON_VOLUME.ordinal)
                serviceIntent.putExtra("key_code", event.keyCode)
                serviceIntent.putExtra("long", (System.currentTimeMillis() - pressed_time[event.keyCode]!!) >= getLongPressTimeout())
                startService(serviceIntent)
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