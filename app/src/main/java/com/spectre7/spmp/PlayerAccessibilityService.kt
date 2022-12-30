package com.spectre7.spmp

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.database.ContentObserver
import android.os.Handler
import android.view.KeyEvent
import android.view.KeyEvent.*
import android.view.ViewConfiguration.getLongPressTimeout
import android.view.accessibility.AccessibilityEvent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.spectre7.spmp.model.Settings
import com.spectre7.utils.Permissions
import com.spectre7.utils.getString
import com.spectre7.utils.sendToast
import java.lang.ref.WeakReference
import kotlin.collections.set
import android.provider.Settings as AndroidSettings

class PlayerAccessibilityService : AccessibilityService() {

    enum class VOLUME_INTERCEPT_MODE {
        ALWAYS,
        APP_OPEN,
        NEVER
    }
    private lateinit var volume_intercept_mode: VOLUME_INTERCEPT_MODE

    private val prefs_change_listener = OnSharedPreferenceChangeListener { prefs, key ->
        when (key) {
            Settings.KEY_ACC_VOL_INTERCEPT_MODE.name -> {
                volume_intercept_mode = VOLUME_INTERCEPT_MODE.values()[Settings.get(Settings.KEY_ACC_VOL_INTERCEPT_MODE, preferences = prefs)]
            }
        }
    }

    private val pressed_time = mutableMapOf<Int, Long>(
        KEYCODE_VOLUME_UP to -1,
        KEYCODE_VOLUME_DOWN to -1
    )

    override fun onCreate() {
        super.onCreate()

        val prefs = MainActivity.getSharedPreferences(this)
        volume_intercept_mode = VOLUME_INTERCEPT_MODE.values()[Settings.get(Settings.KEY_ACC_VOL_INTERCEPT_MODE, preferences = prefs)]
        prefs.registerOnSharedPreferenceChangeListener(prefs_change_listener)
        
        instance = WeakReference(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        MainActivity.getSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(prefs_change_listener)
        instance = null
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null || volume_intercept_mode == VOLUME_INTERCEPT_MODE.NEVER) {
            return false
        }

        if (volume_intercept_mode == VOLUME_INTERCEPT_MODE.APP_OPEN) {

        }
        // VOLUME_INTERCEPT_MODE.ALWAYS
        else {

        }

        if (event.keyCode == KEYCODE_VOLUME_UP || event.keyCode == KEYCODE_VOLUME_DOWN) {
            if (event.action == ACTION_DOWN) {
                pressed_time[event.keyCode] = System.currentTimeMillis()
            }
            else if (event.action == ACTION_UP) {
                onVolumePressIntercepted(event.keyCode == KEYCODE_VOLUME_UP, (System.currentTimeMillis() - pressed_time[event.keyCode]!!) >= getLongPressTimeout())
            }
            return true
        }

        return false
    }

    private fun onVolumePressIntercepted(up: Boolean, long: Boolean) {
        val intent = Intent(PlayerService::class.java.canonicalName)
        intent.putExtra("action", SERVICE_INTENT_ACTIONS.BUTTON_VOLUME.ordinal)
        intent.putExtra("up", up)
        intent.putExtra("long", long)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
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

    companion object: ListenableAccessibilityService(PlayerAccessibilityService::class.java.canonicalName as String) {
        private var instance: WeakReference<PlayerAccessibilityService>? = null
        fun disable() {
            if (instance != null && instance!!.get() != null) {
                instance!!.get()!!.disableSelf()
            }
        }

        fun enable(context: Context, root: Boolean) {
            if (!root) {
                val intent = Intent(AndroidSettings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                )
                context.startActivity(intent)
                return
            }

            fun enableSecurely() {
                val resolver = MainActivity.context.contentResolver
                val enabled_services = AndroidSettings.Secure.getString(resolver, AndroidSettings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
                val service_entry = "${context.packageName}/${PlayerAccessibilityService::class.java.canonicalName}"

                AndroidSettings.Secure.putString(resolver, AndroidSettings.Secure.ENABLED_ACCESSIBILITY_SERVICES, when {
                    enabled_services.isNullOrBlank() -> service_entry
                    enabled_services.contains(service_entry) -> enabled_services
                    else -> "$service_entry:$enabled_services"
                })
                AndroidSettings.Secure.putInt(resolver, AndroidSettings.Secure.ACCESSIBILITY_ENABLED, 1)
            }

            if (Permissions.hasPermission(Manifest.permission.WRITE_SECURE_SETTINGS, context)) {
                enableSecurely()
                return
            }

            Permissions.requestPermission(Manifest.permission.WRITE_SECURE_SETTINGS, context) { result, error ->
                when (result) {
                    Permissions.GrantError.OK -> enableSecurely()
                    Permissions.GrantError.ROOT_NOT_GRANTED -> sendToast(getString(R.string.err_root_not_granted))
                    else -> {
                        val dialog = AlertDialog.Builder(context)
                        dialog.setCancelable(false)
                        dialog.setTitle(getString(R.string.err_secure_settings_grant_failed))
                        dialog.setMessage(error)
                        dialog.setPositiveButton("Ok") { _, _ -> }
                        dialog.create().show()
                    }
                }
            }
        }
    }
}

open class ListenableAccessibilityService(private val class_name: String) {
    private var enabled_listeners: MutableMap<(Boolean) -> Unit, ContentObserver> = mutableMapOf()

    fun addEnabledListener(listener: (Boolean) -> Unit, context: Context) {
        val observer: ContentObserver = object : ContentObserver(Handler(context.mainLooper)) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                listener(isEnabled(context))
            }
        }

        val uri = AndroidSettings.Secure.getUriFor(AndroidSettings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        context.contentResolver.registerContentObserver(uri, false, observer)

        enabled_listeners[listener] = observer
    }

    fun removeEnabledListener(listener: (Boolean) -> Unit, context: Context) {
        val observer = enabled_listeners.remove(listener) ?: return
        context.contentResolver.unregisterContentObserver(observer)
    }

    fun isEnabled(context: Context): Boolean {
        val enabled_services = AndroidSettings.Secure.getString(context.contentResolver, AndroidSettings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return enabled_services.contains("${context.packageName}/$class_name")
    }
}
