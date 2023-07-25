package com.toasterofbread.spmp.resources.uilocalisation

import com.toasterofbread.spmp.resources.uilocalisation.localised.getAmountSuffixes

const val UNLOCALISED_STRING_TYPE = "AMOUNT_SUFFIX"

fun parseYoutubeSubscribersString(string: String, hl: String): Int? {
    val suffixes = getAmountSuffixes(hl)
    if (suffixes != null) {
        if (string.last().isDigit()) {
            return string.toFloat().toInt()
        }

        val multiplier = suffixes[string.last()]
        if (multiplier == null) {
            SpMp.onUnlocalisedStringFound(UNLOCALISED_STRING_TYPE, string.last().toString(), hl)
            return null
        }

        return (string.substring(0, string.length - 1).toFloat() * multiplier).toInt()
    }

    SpMp.onUnlocalisedStringFound(UNLOCALISED_STRING_TYPE, null, hl)

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

    SpMp.onUnlocalisedStringFound(UNLOCALISED_STRING_TYPE, null, hl)

    return amount.toString()
}

