package com.spectre7.spmp.platform

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import android.os.VibrationEffect
import android.os.VibratorManager
import android.widget.Toast
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.unit.Dp
import java.io.InputStream

actual class ProjectContext(private val context: Context) {
    actual fun getPrefs(): ProjectPreferences = ProjectPreferences.getInstance(context)

    @SuppressLint("InternalInsetResource", "DiscouragedApi")
    @Composable
    actual fun getStatusBarHeight(): Dp {
        var height: Dp? by remember { mutableStateOf(null) }
        if (height != null) {
            return height!!
        }

        val resource_id: Int = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resource_id > 0) {
            with(LocalDensity.current) {
                height = context.resources.getDimensionPixelSize(resource_id).toDp()
                return height!!
            }
        }

        throw RuntimeException()
    }
    actual fun getLightColorScheme(): ColorScheme = dynamicLightColorScheme(context)
    actual fun getDarkColorScheme(): ColorScheme = dynamicDarkColorScheme(context)

    actual fun sendToast(text: String, length: Int) {
        try {
            Toast.makeText(context, text, length).show()
        }
        catch (_: NullPointerException) {
            Looper.prepare()
            Toast.makeText(context, text, length).show()
        }
    }

    actual fun vibrate(duration: Double) {
        val vibrator = (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        vibrator.vibrate(VibrationEffect.createOneShot((duration * 1000.0).toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
    }

    actual fun openResourceFile(path: String): InputStream = context.resources.assets.open(path)
    actual fun listResourceFiles(path: String): List<String>? = context.resources.assets.list(path)?.toList()

    actual fun loadFontFromFile(path: String): Font = Font(path, context.resources.assets)
}
