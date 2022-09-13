package com.spectre7.utils

// TODO | Move to separate repository

import android.os.VibratorManager
import android.os.VibrationEffect
import android.content.Context
import android.widget.Toast
import android.os.Looper
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
