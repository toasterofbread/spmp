package com.toasterofbread.spmp.resources.uilocalisation.localised

fun getAmountSuffixes(hl: String): Map<Char, Int>? =
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
