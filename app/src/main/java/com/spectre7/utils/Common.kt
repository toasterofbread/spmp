package com.spectre7.utils

// TODO | Move to separate repository

import android.content.Context
import android.os.Looper
import android.os.VibrationEffect
import android.os.VibratorManager
import android.widget.Toast
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.spectre7.spmp.MainActivity

fun Boolean.toInt() = if (this) 1 else 0

fun vibrate(duration: Double) {
    val vibrator = (MainActivity.context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    vibrator.vibrate(VibrationEffect.createOneShot((duration * 1000.0).toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
}

fun sendToast(text: String, length: Int = Toast.LENGTH_SHORT, context: Context = MainActivity.context) {
    try {
        Toast.makeText(context, text, length).show()
    }
    catch (_: NullPointerException) {
        Looper.prepare()
        Toast.makeText(context, text, length).show()
    }
}

fun getString(id: Int, context: Context = MainActivity.context): String {
    return context.resources.getString(id)
}

@Composable
fun NoRipple(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalRippleTheme provides object : RippleTheme {
        @Composable
        override fun defaultColor() = Color.Unspecified

        @Composable
        override fun rippleAlpha(): RippleAlpha = RippleAlpha(0.0f,0.0f,0.0f,0.0f)
    }) {
        content()
    }
}

@Composable
fun OnChangedEffect(key: Any, block: suspend () -> Unit) {
    var launched by remember { mutableStateOf(false) }
    LaunchedEffect(key) {
        if (!launched) {
            launched = true
        }
        else {
            block()
        }
    }
}

fun getAppName(context: Context): String {
    val info = context.applicationInfo
    val string_id = info.labelRes
    return if (string_id == 0) info.nonLocalizedLabel.toString() else context.getString(string_id)
}