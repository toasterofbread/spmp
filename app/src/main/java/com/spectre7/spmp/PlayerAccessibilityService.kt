package com.spectre7.spmp

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AlertDialog
import android.content.*
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.database.ContentObserver
import android.media.session.MediaSessionManager
import android.os.Handler
import android.service.notification.NotificationListenerService
import android.view.KeyEvent
import android.view.KeyEvent.*
import android.view.ViewConfiguration.getLongPressTimeout
import android.view.accessibility.AccessibilityEvent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.spectre7.spmp.model.Settings
import com.spectre7.utils.Permissions
import com.spectre7.utils.getString
import com.spectre7.utils.sendToast
import kotlinx.coroutines.*
import java.lang.ref.WeakReference
import kotlin.collections.set
import android.provider.Settings as AndroidSettings


class PlayerAccessibilityService : AccessibilityService(), LifecycleOwner {

    enum class VOLUME_INTERCEPT_MODE {
        ALWAYS,
        APP_OPEN,
        NEVER
    }
    private lateinit var volume_intercept_mode: VOLUME_INTERCEPT_MODE
    private var listen_while_screen_off: Boolean = false

    private val prefs_change_listener = OnSharedPreferenceChangeListener { prefs, key ->
        when (key) {
            Settings.KEY_ACC_VOL_INTERCEPT_MODE.name -> {
                volume_intercept_mode = VOLUME_INTERCEPT_MODE.values()[Settings.get(Settings.KEY_ACC_VOL_INTERCEPT_MODE, prefs)]
            }
            Settings.KEY_ACC_SCREEN_OFF.name -> {
                listen_while_screen_off = Settings.get(Settings.KEY_ACC_SCREEN_OFF, prefs)
                if (!listen_while_screen_off) {
                    screen_off_listener.stopListening()
                }
            }
        }
    }

    private lateinit var lifecycle_registry: LifecycleRegistry
    private val screen_off_listener = ScreenOffListener { volume_up, key_down ->
        onVolumeKeyPressed(volume_up, key_down)
    }
    private val broadcast_receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> screen_off_listener.stopListening()
                Intent.ACTION_SCREEN_OFF -> if (listen_while_screen_off) screen_off_listener.startListening(lifecycleScope)
            }
        }
    }

    private val pressed_time = mutableMapOf<Boolean, Long?>(
        true to null,
        false to null
    )

    override fun onCreate() {
        super.onCreate()
        instance = WeakReference(this)

        lifecycle_registry = LifecycleRegistry(this)
        lifecycle_registry.currentState = Lifecycle.State.CREATED

        val prefs = MainActivity.getSharedPreferences(this)
        prefs.registerOnSharedPreferenceChangeListener(prefs_change_listener)
        volume_intercept_mode = VOLUME_INTERCEPT_MODE.values()[Settings.get(Settings.KEY_ACC_VOL_INTERCEPT_MODE, prefs)]
        listen_while_screen_off = Settings.get(Settings.KEY_ACC_SCREEN_OFF, prefs)

        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_SCREEN_ON)
        filter.addAction(Intent.ACTION_SCREEN_OFF)
        registerReceiver(broadcast_receiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null

        MainActivity.getSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(prefs_change_listener)
        unregisterReceiver(broadcast_receiver)
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null || volume_intercept_mode == VOLUME_INTERCEPT_MODE.NEVER) {
            return false
        }


        if (event.keyCode == KEYCODE_VOLUME_UP || event.keyCode == KEYCODE_VOLUME_DOWN) {
            if (volume_intercept_mode == VOLUME_INTERCEPT_MODE.APP_OPEN) {
                if (!MainActivity.isInForeground()) {
                    return false
                }
            }
            // VOLUME_INTERCEPT_MODE.ALWAYS
            else if (!PlayerServiceHost.isRunningAndFocused()) {
                return false
            }

            onVolumeKeyPressed(event.keyCode == KEYCODE_VOLUME_UP, event.action == ACTION_DOWN)
            return true
        }

        return false
    }

    private fun onVolumeKeyPressed(volume_up: Boolean, key_down: Boolean) {
        if (key_down) {
            pressed_time[volume_up] = System.currentTimeMillis()
        }
        else {
            val intent = Intent(PlayerService::class.java.canonicalName)
            intent.putExtra("action", SERVICE_INTENT_ACTIONS.BUTTON_VOLUME.ordinal)
            intent.putExtra("up", volume_up)

            val pressed = pressed_time[volume_up]
            val long = if (pressed == null) false else (System.currentTimeMillis() - pressed) >= getLongPressTimeout()
            intent.putExtra("long", long)
            pressed_time[volume_up] = null

            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        }
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

    companion object {
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
            return enabled_services.contains("${context.packageName}/${PlayerAccessibilityService::class.java.canonicalName}")
        }

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

    override fun getLifecycle(): Lifecycle {
        return lifecycle_registry
    }
}

class ScreenOffListener(private val onVolumeKeyPressed: (volume_up: Boolean, key_down: Boolean) -> Unit) {
    private var job: Job? = null

    fun startListening(scope: CoroutineScope): Boolean {
        try {
            job = scope.launch(Dispatchers.IO) {
                val stream = Permissions.runAsRoot("getevent -lq").inputStream
                var line: String? = null

                fun isKeyDown(): Boolean {
                    if (line == null) {
                        return false
                    }
                    return line!!.substring(53, line!!.indexOf(' ', 54)) != "UP"
                }

                while (stream.bufferedReader().readLine().also { line = it } != null && isActive) {
                    if (line == null) {
                        continue
                    }

                    val start = 32
                    val end = line!!.indexOf(' ', start + 1)

                    when (line!!.subSequence(start, end)) {
                        "KEY_VOLUMEUP" -> {
                            onVolumeKeyPressed(true, isKeyDown())
                        }
                        "KEY_VOLUMEDOWN" -> {
                            onVolumeKeyPressed(false, isKeyDown())
                        }
                    }
                }

                stream.close()
            }

        } catch (e: Exception) {
            job?.cancel()
            return false
        }

        return true
    }

    fun stopListening() {
        job?.cancel()
        job = null
    }
}
