package com.spectre7.utils

// TODO | Move to separate repository

import android.os.VibratorManager
import android.os.VibrationEffect
import android.content.Context
import android.widget.Toast
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.spectre7.spmp.MainActivity

fun Boolean.toInt() = if (this) 1 else 0

fun vibrate(duration: Double) {
    val vibrator = (MainActivity.context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    vibrator.vibrate(VibrationEffect.createOneShot((duration * 1000.0).toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
}

fun sendToast(text: String) {
    try {
        Toast.makeText(MainActivity.context, text, Toast.LENGTH_SHORT).show()
    }
    catch (e: NullPointerException) {
        Looper.prepare()
        Toast.makeText(MainActivity.context, text, Toast.LENGTH_SHORT).show()
    }
}

fun getString(id: Int): String {
    return MainActivity.resources.getString(id)
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
