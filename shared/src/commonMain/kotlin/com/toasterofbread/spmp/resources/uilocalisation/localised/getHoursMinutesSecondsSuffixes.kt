package com.toasterofbread.spmp.resources.uilocalisation.localised

data class HMSData(val hours: String, val minutes: String, val seconds: String, val splitter: String = "")

fun getHoursMinutesSecondsSuffixes(hl: String?): HMSData? =
    when (hl?.split('-', limit = 2)?.firstOrNull()) {
        "en-GB", null -> HMSData("hours", "minutes", "seconds", " ")
        "ja-JP" -> HMSData("時間", "分", "秒")
        else -> null
    }
