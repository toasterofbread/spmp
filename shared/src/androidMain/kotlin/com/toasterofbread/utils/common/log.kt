package com.toasterofbread.utils.common

import android.util.Log

actual fun log(message: Any?) {
    val content = message.toString()
    if (content.length > 3000) {
        Log.d("SpMp", content.substring(0, 3000))
        log(content.substring(3000))
    } else {
        Log.d("SpMp", content)
    }
}