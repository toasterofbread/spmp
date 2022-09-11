package com.spectre7.utils

// TODO | Move to separate repository

import android.os.VibratorManager
import android.os.VibrationEffect
import android.content.Context
import com.spectre7.spmp.MainActivity

fun Boolean.toInt() = if (this) 1 else 0

fun vibrate(duration: Double) {
    val vibrator = (MainActivity.context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    vibrator.vibrate(VibrationEffect.createOneShot((duration * 1000.0).toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
}
