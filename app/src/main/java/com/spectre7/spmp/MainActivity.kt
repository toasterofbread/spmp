package com.spectre7.spmp

import android.util.Log
import android.content.res.Resources
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.spectre7.spmp.ui.layout.PlayerView
import com.spectre7.spmp.ui.theme.MyApplicationTheme
import androidx.compose.animation.core.Animatable
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.AnimationVector4D

abstract class Theme {
    abstract var background_colour: Animatable<Color, AnimationVector4D>// = Animatable(MaterialTheme.colorScheme.background)
    abstract var on_background_colour: Animatable<Color, AnimationVector4D>// = Animatable(MaterialTheme.colorScheme.onBackground)
    abstract var accent_colour: Animatable<Color, AnimationVector4D>// = Animatable(MaterialTheme.colorScheme.primary)
}

class MainActivity : ComponentActivity() {

    lateinit var player: PlayerHost

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        PlayerHost(this)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PlayerView()
                }
            }
        }
    }

    override fun onDestroy() {
        PlayerHost.release()
        super.onDestroy()
    }

    companion object {

        val context: MainActivity get() = instance!!
        val resources: Resources get() = context.resources

        @JvmStatic
        private var instance: MainActivity? = null

        fun runInMainThread(code: () -> Unit) {
            Handler(Looper.getMainLooper()).post {
                code()
            };
        }
    }
}
