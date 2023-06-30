package com.toasterofbread.spmp.resources.uilocalisation

fun parseYoutubeSubscribersString(string: String, hl: String): Int? {
    val suffixes = getAmountSuffixes(hl)
    if (suffixes != null) {
        if (string.last().isDigit()) {
            return string.toFloat().toInt()
        }

        val multiplier = suffixes[string.last()] ?: throw NotImplementedError(string.last().toString())
        return (string.substring(0, string.length - 1).toFloat() * multiplier).toInt()
    }

    throw NotImplementedError(hl)
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

    throw NotImplementedError(hl)
}

private fun getAmountSuffixes(hl: String): Map<Char, Int>? =
    when (hl) {
        "en" -> mapOf(
            'B' to 1000000000,
            'M' to 1000000,
            'K' to 1000
        )
        "ja" -> mapOf(
            '億' to 100000000,
            '万' to 10000,
            '千' to 1000,
            '百' to 100
        )
        else -> null
    }
