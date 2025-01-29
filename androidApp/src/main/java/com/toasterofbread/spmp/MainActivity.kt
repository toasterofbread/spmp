package com.toasterofbread.spmp

import ProgramArguments
import SpMp
import SpMp.isDebugBuild
import android.content.ComponentCallbacks2
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import com.toasterofbread.spmp.model.appaction.shortcut.ShortcutState
import com.toasterofbread.spmp.platform.AppContext
import dev.toastbits.composekit.context.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking

class MainActivity: ComponentActivity() {
    private val coroutine_scope = CoroutineScope(Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Thread.setDefaultUncaughtExceptionHandler { _: Thread, error: Throwable ->
            if (
                error is java.nio.channels.UnresolvedAddressException // Thrown by Kizzy
            ) {
                println("WARNING: Skipping error: ${error.stackTraceToString()}")
                return@setDefaultUncaughtExceptionHandler
            }

            error.printStackTrace()

            startActivity(Intent(this@MainActivity, ErrorReportActivity::class.java).apply {
                putExtra("message", error.message)
                putExtra("stack_trace", error.stackTraceToString())
            })
        }

        if (isDebugBuild()) {
            StrictMode.setVmPolicy(
                VmPolicy.Builder()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build()
            )
        }

        val shortcut_state: ShortcutState = ShortcutState()
        val context: AppContext = runBlocking { AppContext.create(this@MainActivity, coroutine_scope, ApplicationContext(this@MainActivity)) }
        SpMp.init(context)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (!context.isDisplayingAboveNavigationBar()) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }
        else {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        }

        val open_uri: Uri? =
            if (intent.action == Intent.ACTION_VIEW) intent.data
            else null

        val launch_arguments: ProgramArguments = ProgramArguments()

        setContent {
            val player_coroutine_scope: CoroutineScope = rememberCoroutineScope()
            var player_initialised: Boolean by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                SpMp.initPlayer(launch_arguments, player_coroutine_scope)
                player_initialised = true
            }

            if (player_initialised) {
                SpMp.App(launch_arguments, shortcut_state, open_uri = open_uri?.toString())
            }
        }
    }

    override fun onDestroy() {
        coroutine_scope.cancel()
        SpMp.release()
        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()
        SpMp.onStart()
    }

    override fun onStop() {
        super.onStop()
        SpMp.onStop()
    }

    // TODO
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
            }

            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
            -> {
                SpMp.onLowMemory()
            }

            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND,
            ComponentCallbacks2.TRIM_MEMORY_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE,
            -> {
            }

            else -> {
            }
        }
    }
}
