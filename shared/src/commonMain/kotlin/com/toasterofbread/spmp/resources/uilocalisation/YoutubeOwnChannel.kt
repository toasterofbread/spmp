package com.toasterofbread.spmp.resources.uilocalisation

fun getYoutubeOwnChannelLocalisations(getLanguage: (String) -> Int): YoutubeUILocalisation.LocalisationSet {
    val en = getLanguage("en")
    val ja = getLanguage("ja")

    return YoutubeUILocalisation.LocalisationSet().apply {
        add(
            en to "Songs on repeat",
            ja to "繰り返し再生されている曲"
        )
        add(
            en to "Artists on repeat",
            ja to "繰り返し再生するアーティスト"
        )
        add(
            en to "Videos on repeat",
            ja to "繰り返し再生されている動画"
        )
        add(
            en to "Playlists on repeat",
            ja to "繰り返し再生するプレイリスト"
        )
    }
}
