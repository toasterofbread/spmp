package com.toasterofbread.spmp.resources.uilocalisation

import SpMp
import com.toasterofbread.spmp.resources.uilocalisation.localised.HMSData
import com.toasterofbread.spmp.resources.uilocalisation.localised.getHoursMinutesSecondsSuffixes
import org.apache.commons.lang3.time.DurationFormatUtils
import java.time.Duration

private const val HOUR_MS: Long = 3600000L

fun durationToString(duration_ms: Long, short: Boolean = false, hl: String = SpMp.ui_language): String {
    if (short) {
        return DurationFormatUtils.formatDuration(
            duration_ms,
            if (duration_ms >= HOUR_MS) "H:mm:ss" else "mm:ss",
            true
        )
    }

    var hms = getHoursMinutesSecondsSuffixes(hl)
    if (hms == null) {
        SpMp.Log.warning("HMS duration strings not implemented for language '$hl'")
        hms = getHoursMinutesSecondsSuffixes("en")
    }

    checkNotNull(hms)

    val string = StringBuilder()

    val duration = Duration.ofMillis(duration_ms)
    duration.toHours().also {
        if (it != 0L) {
            string.append("$it${hms.splitter}${hms.hours}")
        }
    }
    (duration.toMinutes() % 60L).toInt().also {
        if (it != 0) {
            string.append("${hms.splitter}$it${hms.splitter}${hms.minutes}")
        }
    }
    (duration.seconds % 60L).toInt().also {
        if (it != 0) {
            string.append("${hms.splitter}$it${hms.splitter}${hms.seconds}")
        }
    }

    return string.toString()
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

    var hms = getHoursMinutesSecondsSuffixes(hl)
    if (hms == null) {
        SpMp.Log.warning("HMS duration strings not implemented for language '$hl'")
        hms = getHoursMinutesSecondsSuffixes(null)
    }

    return parseHhMmSsDurationString(string, hms!!)
}

private fun parseHhMmSsDurationString(string: String, hms: HMSData): Long? {
    try {
        val parts = string.split(' ').map { it.removeSuffix("+") }

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
    catch (e: Throwable) {
        SpMp.Log.warning("Parsing duration string $string $hms failed\n${e.stackTraceToString()}")
        return null
    }
}
