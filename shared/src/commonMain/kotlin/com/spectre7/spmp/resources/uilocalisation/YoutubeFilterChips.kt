package com.toasterofbread.spmp.resources.uilocalisation

fun getYoutubeFilterChipsLocalisations(getLanguage: (String) -> Int): YoutubeUILocalisation.LocalisationSet {
    val en = getLanguage("en")
    val ja = getLanguage("ja")

    return YoutubeUILocalisation.LocalisationSet().apply {
        add(
            en to "Relax",
            ja to "リラックス"
        )
        add(
            en to "Energize",
            ja to "エナジー"
        )
        add(
            en to "Workout",
            ja to "ワークアウト"
        )
        add(
            en to "Commute",
            ja to "通勤・通学"
        )
        add(
            en to "Focus",
            ja to "フォーカス"
        )
    }
}
