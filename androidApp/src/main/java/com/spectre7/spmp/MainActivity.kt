package com.spectre7.spmp

import android.content.ComponentCallbacks2
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.platform.PlatformContext
import android.content.Intent
import android.net.Uri

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        StrictMode.setVmPolicy(VmPolicy.Builder()
            .detectLeakedClosableObjects()
            .penaltyLog()
            .build()
        )

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        PlatformContext.main_activity = MainActivity::class.java

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
