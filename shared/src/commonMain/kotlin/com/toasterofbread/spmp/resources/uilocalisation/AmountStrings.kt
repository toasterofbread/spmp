package com.toasterofbread.spmp.resources.uilocalisation

import com.toasterofbread.spmp.resources.uilocalisation.localised.getAmountSuffixes

fun parseYoutubeSubscribersString(string: String, hl: String): Int? {
    val suffixes = getAmountSuffixes(hl)
    if (suffixes != null) {
        if (string.last().isDigit()) {
            return string.toFloat().toInt()
        }

        val multiplier = suffixes[string.last()]
        if (multiplier == null) {
            SpMp.Log.warning("Amount suffix '${string.last()}' not implemented for language '$hl'")
            return null
        }

        return (string.substring(0, string.length - 1).toFloat() * multiplier).toInt()
    }

    SpMp.Log.warning("Amount suffixes not implemented for language '$hl'")

    return null
}

fun amountToString(amount: Int, hl: String): String {
    val suffixes = getAmountSuffixes(hl)
    if (suffixes != null) {
        for (suffix in suffixes) {
            if (amount >= suffix.value) {
                return "${amount / suffix.value}${suffix.key}"
            }
        }

        return amount.toString()
    }

    SpMp.Log.warning("Amount suffixes not implemented for language '$hl'")
    return amount.toString()
}

