package com.toasterofbread.spmp

import SpMp
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
import androidx.core.view.WindowCompat
import com.toasterofbread.spmp.platform.ApplicationContext
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.utils.common.isDebugBuild
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

class MainActivity : ComponentActivity() {
    private val coroutine_scope = CoroutineScope(Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        PlatformContext.main_activity = MainActivity::class.java

        Thread.setDefaultUncaughtExceptionHandler { _: Thread, error: Throwable ->
            if (
                error is java.nio.channels.UnresolvedAddressException // Thrown by Kizzy
                || error::class.simpleName == "KordInitializationException" // Thrown by Kord
            ) {
                SpMp.Log.warning("Skipping error: ${error.stackTraceToString()}")
                return@setDefaultUncaughtExceptionHandler
            }

            error.printStackTrace()

            startActivity(Intent(this@MainActivity, ErrorReportActivity::class.java).apply {
                putExtra("message", error.message)
                putExtra("stack_trace", error.stackTraceToString())
            })
        }

        if (isDebugBuild()) {
            StrictMode.setVmPolicy(VmPolicy.Builder()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .build()
            )
        }

        val context = PlatformContext(this, coroutine_scope, ApplicationContext(this))
        SpMp.init(context)
        context.init()

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

        setContent {
            context.setStatusBarColour(context.theme.background)
            context.setNavigationBarColour(context.theme.background)

            SpMp.App(open_uri = open_uri?.toString())
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
