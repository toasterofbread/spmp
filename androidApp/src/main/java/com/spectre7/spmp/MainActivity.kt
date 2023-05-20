package com.spectre7.spmp

import SpMp
import android.content.ComponentCallbacks2
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.spectre7.spmp.platform.PlatformContext

class MainActivity : ComponentActivity() {
    companion object {
        init {
            PlatformContext.main_activity = MainActivity::class.java
            PlatformContext.ic_spmp = R.drawable.ic_spmp
            PlatformContext.ic_thumb_up = R.drawable.ic_thumb_up
            PlatformContext.ic_thumb_up_off = R.drawable.ic_thumb_up_off
            PlatformContext.ic_skip_next = R.drawable.ic_skip_next
            PlatformContext.ic_skip_previous = R.drawable.ic_skip_previous
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!ProjectBuildConfig.IS_DEBUG) {
            Thread.setDefaultUncaughtExceptionHandler { _: Thread, error: Throwable ->
                error.printStackTrace()

                startActivity(Intent(this@MainActivity, ErrorReportActivity::class.java).apply {
                    putExtra("message", error.message)
                    putExtra("stack_trace", error.stackTraceToString())
                })
            }
        }

        StrictMode.setVmPolicy(VmPolicy.Builder()
            .detectLeakedClosableObjects()
            .penaltyLog()
            .build()
        )

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val context = PlatformContext(this)
        SpMp.init(context)

        val open_uri: Uri? =
            if (intent.action == Intent.ACTION_VIEW) intent.data
            else null

        setContent {
            SpMp.App(open_uri?.toString())
        }
    }

    override fun onDestroy() {
        SpMp.release()
        super.onDestroy()
    }

    // TODO
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
            }

            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                SpMp.onLowMemory()
            }

            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND,
            ComponentCallbacks2.TRIM_MEMORY_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
            }

            else -> {
            }
        }
    }
}
