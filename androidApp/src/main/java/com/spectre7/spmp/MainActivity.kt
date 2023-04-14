package com.spectre7.spmp

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.spectre7.spmp.platform.PlatformContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        PlatformContext.main_activity = MainActivity::class.java

        val context = PlatformContext(this)
        SpMp.init(context)

        setContent {
            SpMp.App()
        }
    }

    override fun onDestroy() {
        SpMp.release()
        super.onDestroy()
    }
}
