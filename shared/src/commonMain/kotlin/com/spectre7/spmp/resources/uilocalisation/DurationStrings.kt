package com.toasterofbread.spmp.resources.uilocalisation

import org.apache.commons.lang3.time.DurationFormatUtils
import java.time.Duration

private const val HOUR: Long = 3600000L

fun durationToString(duration: Long, short: Boolean = false, hl: String = SpMp.ui_language): String {
    if (short) {
        return DurationFormatUtils.formatDuration(
            duration,
            if (duration >= HOUR) "H:mm:ss" else "mm:ss",
            true
        )
    }
    else {
        val hms = getHMS(hl)
        if (hms != null) {
            val f = StringBuilder()

            val dur = Duration.ofMillis(duration)
            dur.toHours().also {
                if (it != 0L) {
                    f.append("$it${hms.splitter}${hms.hours}")
                }
            }
            dur.toMinutesPart().also {
                if (it != 0) {
                    f.append("${hms.splitter}$it${hms.splitter}${hms.minutes}")
                }
            }
            dur.toSecondsPart().also {
                if (it != 0) {
                    f.append("${hms.splitter}$it${hms.splitter}${hms.seconds}")
                }
            }

            return f.toString()
        }
    }

    throw NotImplementedError(hl.split('-', limit = 2).first())
}

fun parseYoutubeDurationString(string: String, hl: String): Long? {
    if (string.contains(':')) {
        val parts = string.split(':')

        if (parts.size !in 2..3) {
            return null
        }

        val seconds = parts.last().toLong()
        val minutes = parts[parts.size - 2].toLong()
        val hours = if (parts.size == 3) parts.first().toLong() else 0L

        return ((hours * 60 + minutes) * 60 + seconds) * 1000
    }
    else {
        val hms = getHMS(hl)
        if (hms != null) {
            return parseHhMmSsDurationString(string, hms)
        }
    }

    throw NotImplementedError(hl.split('-', limit = 2).first())
}

private data class HMSData(val hours: String, val minutes: String, val seconds: String, val splitter: String = "")

private fun getHMS(hl: String): HMSData? =
    when (hl.split('-', limit = 2).first()) {
        "en" -> HMSData("hours", "minutes", "seconds", " ")
        "ja" -> HMSData("時間", "分", "秒")
        else -> null
    }

private fun parseHhMmSsDurationString(string: String, hms: HMSData): Long? {
    val parts = string.split(' ')

    val h = parts.indexOf(hms.hours)
    val hours =
        if (h != -1) parts[h - 1].toLong()
        else null

    val m = parts.indexOf(hms.minutes)
    val minutes =
        if (m != -1) parts[m - 1].toLong()
        else null

    val s = parts.indexOf(hms.seconds)
    val seconds =
        if (s != -1) parts[s - 1].toLong()
        else null

    if (hours == null && minutes == null && seconds == null) {
        return null
    }

    return (((hours ?: 0) * 60 + (minutes ?: 0)) * 60 + (seconds ?: 0)) * 1000
}
