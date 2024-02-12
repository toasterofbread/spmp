package com.toasterofbread.spmp.resources.uilocalisation.localised

private val amount_suffixes: Map<String, Map<Char, Int>> = mapOf(
    UILanguages.en to mapOf(
        'B' to 1000000000,
        'M' to 1000000,
        'K' to 1000
    ),
    UILanguages.ja to mapOf(
        '億' to 100000000,
        '万' to 10000,
        '千' to 1000,
        '百' to 100
    ),
    UILanguages.zh to mapOf(
        '亿' to 100000000,
        '万' to 10000,
        '千' to 1000,
        '百' to 100
    )
)

fun getAmountSuffixes(language: String): Map<Char, Int>? =
    amount_suffixes.getByLanguage(language)?.value?.value
