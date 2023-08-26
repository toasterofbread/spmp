package com.toasterofbread.spmp

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.os.Handler
import android.view.KeyEvent
import android.view.KeyEvent.ACTION_DOWN
import android.view.KeyEvent.KEYCODE_VOLUME_DOWN
import android.view.KeyEvent.KEYCODE_VOLUME_UP
import android.view.ViewConfiguration.getLongPressTimeout
import android.view.accessibility.AccessibilityEvent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.platform.PlatformPreferences
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.service.playerservice.PlayerService
import com.toasterofbread.utils.Permissions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.Timer
import java.util.TimerTask
import kotlin.collections.set
import android.provider.Settings as AndroidSettings

enum class SERVICE_INTENT_ACTIONS { BUTTON_VOLUME }

actual class PlayerAccessibilityService : AccessibilityService(), LifecycleOwner {
    private lateinit var context: PlatformContext
    private val coroutine_scope = CoroutineScope(Job())

    private lateinit var volume_intercept_mode: PlayerAccessibilityServiceVolumeInterceptMode
    private var listen_while_screen_off: Boolean = false

    private val prefs_change_listener = object : PlatformPreferences.Listener {
        override fun onChanged(prefs: PlatformPreferences, key: String) {
            when (key) {
//                Settings.KEY_ACC_VOL_INTERCEPT_MODE.name -> {
//                    volume_intercept_mode = VOLUME_INTERCEPT_MODE.values()[Settings.get(Settings.KEY_ACC_VOL_INTERCEPT_MODE, prefs)]
//                }
                Settings.KEY_ACC_SCREEN_OFF.name -> {
                    listen_while_screen_off = Settings.get(Settings.KEY_ACC_SCREEN_OFF, prefs)
                    if (!listen_while_screen_off) {
                        screen_off_listener.stopListening()
                    }
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

    private val long_press_timer = Timer()
    private var long_presssed: Boolean = false
    private var current_long_press_task: TimerTask? = null

    override fun onCreate() {
        super.onCreate()
        instance = WeakReference(this)
        context = PlatformContext(this, coroutine_scope).init()

        lifecycle_registry = LifecycleRegistry(this)
        lifecycle_registry.currentState = Lifecycle.State.CREATED

        val prefs = context.getPrefs()
        prefs.addListener(prefs_change_listener)
//        volume_intercept_mode = VOLUME_INTERCEPT_MODE.values()[Settings.get(Settings.KEY_ACC_VOL_INTERCEPT_MODE, prefs)]
        listen_while_screen_off = Settings.get(Settings.KEY_ACC_SCREEN_OFF, prefs)

        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_SCREEN_ON)
        filter.addAction(Intent.ACTION_SCREEN_OFF)
        registerReceiver(broadcast_receiver, filter)
    }

    override fun onDestroy() {
        coroutine_scope.cancel()
        context.getPrefs().removeListener(prefs_change_listener)
        unregisterReceiver(broadcast_receiver)
        long_press_timer.cancel()
        instance = null

        super.onDestroy()
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null || volume_intercept_mode == PlayerAccessibilityServiceVolumeInterceptMode.NEVER) {
            return false
        }

        if (event.keyCode == KEYCODE_VOLUME_UP || event.keyCode == KEYCODE_VOLUME_DOWN) {
            when (volume_intercept_mode) {
                PlayerAccessibilityServiceVolumeInterceptMode.ALWAYS -> {
                    // TODO
//                    !context.player_state.isRunningAndFocused()
                }
                PlayerAccessibilityServiceVolumeInterceptMode.APP_OPEN -> {
                    if (!context.isAppInForeground()) {
                        return false
                    }
                }
                PlayerAccessibilityServiceVolumeInterceptMode.NEVER -> return false
            }

            onVolumeKeyPressed(event.keyCode == KEYCODE_VOLUME_UP, event.action == ACTION_DOWN)
            return true
        }

        return false
    }

    private fun onVolumeKeyPressed(volume_up: Boolean, key_down: Boolean) {
        if (key_down) {
            current_long_press_task = object : TimerTask() {
                override fun run() {
                    long_presssed = true

                    val intent = Intent(PlayerService::class.java.canonicalName)
                    intent.putExtra("action", SERVICE_INTENT_ACTIONS.BUTTON_VOLUME.ordinal)
                    intent.putExtra("up", volume_up)
                    intent.putExtra("long", true)
                    LocalBroadcastManager.getInstance(this@PlayerAccessibilityService).sendBroadcast(intent)
                }
            }
            long_press_timer.schedule(current_long_press_task, getLongPressTimeout().toLong())
        }
        else {
            if (long_presssed) {
                long_presssed = false
                return
            }
            current_long_press_task?.cancel()

            val intent = Intent(PlayerService::class.java.canonicalName)
            intent.putExtra("action", SERVICE_INTENT_ACTIONS.BUTTON_VOLUME.ordinal)
            intent.putExtra("up", volume_up)
            intent.putExtra("long", false)
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

    actual companion object {
        private var enabled_listeners: MutableMap<(Boolean) -> Unit, ContentObserver> = mutableMapOf()
        private var instance: WeakReference<PlayerAccessibilityService>? = null

        actual fun isSupported(): Boolean = true

        actual fun addEnabledListener(listener: (Boolean) -> Unit, context: PlatformContext) {
            val observer: ContentObserver = object : ContentObserver(Handler(context.ctx.mainLooper)) {
                override fun onChange(selfChange: Boolean) {
                    super.onChange(selfChange)
                    listener(isEnabled(context))
                }
            }

            val uri = AndroidSettings.Secure.getUriFor(AndroidSettings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            context.ctx.contentResolver.registerContentObserver(uri, false, observer)

            enabled_listeners[listener] = observer
        }

        actual fun removeEnabledListener(listener: (Boolean) -> Unit, context: PlatformContext) {
            val observer = enabled_listeners.remove(listener) ?: return
            context.ctx.contentResolver.unregisterContentObserver(observer)
        }

        actual fun isEnabled(context: PlatformContext): Boolean {
            val enabled_services = AndroidSettings.Secure.getString(context.ctx.contentResolver, AndroidSettings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            return enabled_services?.contains("${context.ctx.packageName}/${PlayerAccessibilityService::class.java.canonicalName}") == true
        }

        actual fun enableInteractive(context: PlatformContext) {
            val dialog = AlertDialog.Builder(context.ctx)
            dialog.setCancelable(true)
            dialog.setTitle(getString("acc_ser_enable_dialog_title"))
            dialog.setMessage(getString("acc_ser_enable_dialog_body"))
            dialog.setPositiveButton(getString("acc_ser_enable_dialog_btn_root")) { _, _ ->
                enable(context, true)
            }
            dialog.setNeutralButton(getString("acc_ser_enable_dialog_btn_manual")) { _, _ ->
                enable(context, false)
            }
            dialog.setNegativeButton(getString("action_cancel")) { _, _ -> }
            dialog.create().show()
        }

        actual fun enable(context: PlatformContext, root: Boolean) {
            if (!root) {
                val intent = Intent(AndroidSettings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                )
                context.ctx.startActivity(intent)
                return
            }

            fun enableSecurely() {
                val resolver = context.ctx.contentResolver
                val enabled_services = AndroidSettings.Secure.getString(resolver, AndroidSettings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
                val service_entry = "${context.ctx.packageName}/${PlayerAccessibilityService::class.java.canonicalName}"

                AndroidSettings.Secure.putString(resolver, AndroidSettings.Secure.ENABLED_ACCESSIBILITY_SERVICES, when {
                    enabled_services.isNullOrBlank() -> service_entry
                    enabled_services.contains(service_entry) -> enabled_services
                    else -> "$service_entry:$enabled_services"
                })
                AndroidSettings.Secure.putInt(resolver, AndroidSettings.Secure.ACCESSIBILITY_ENABLED, 1)
            }

            if (Permissions.hasPermission(Manifest.permission.WRITE_SECURE_SETTINGS, context.ctx)) {
                enableSecurely()
                return
            }

            Permissions.requestPermission(Manifest.permission.WRITE_SECURE_SETTINGS, context.ctx) { result, error ->
                when (result) {
                    Permissions.GrantError.OK -> enableSecurely()
                    Permissions.GrantError.ROOT_NOT_GRANTED -> context.sendToast(getString("err_root_not_granted"))
                    else -> {
                        val dialog = AlertDialog.Builder(context.ctx)
                        dialog.setCancelable(false)
                        dialog.setTitle(getString("err_secure_settings_grant_failed"))
                        dialog.setMessage(error)
                        dialog.setPositiveButton("Ok") { _, _ -> }
                        dialog.create().show()
                    }
                }
            }
        }

        actual fun disable() {
            if (instance != null && instance!!.get() != null) {
                instance!!.get()!!.disableSelf()
            }
        }

        actual fun isSettingsPermissionGranted(context: PlatformContext): Boolean =
            Permissions.hasPermission(Manifest.permission.WRITE_SECURE_SETTINGS, context.ctx)

        actual fun requestRootPermission(callback: (granted: Boolean) -> Unit) =
            Permissions.requestRootPermission(callback)

        actual fun isOverlayPermissionGranted(context: PlatformContext): Boolean = android.provider.Settings.canDrawOverlays(context.ctx)
        actual fun requestOverlayPermission(context: PlatformContext, callback: (success: Boolean) -> Unit) {
            Permissions.requestPermission(Manifest.permission.SYSTEM_ALERT_WINDOW, context.ctx) { result, error ->
                callback(result == Permissions.GrantError.OK)
            }
        }
    }

    override val lifecycle: Lifecycle
        get() = lifecycle_registry
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
