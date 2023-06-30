package com.toasterofbread.spmp.resources.uilocalisation

fun getYoutubeArtistPageLocalisations(getLanguage: (String) -> Int): YoutubeUILocalisation.LocalisationSet {
    val en = getLanguage("en")
    val ja = getLanguage("ja")

    return YoutubeUILocalisation.LocalisationSet().apply {
        add(
            en to "Songs",
            ja to "曲"
        )
        add(
            en to "Albums",
            ja to "アルバム"
        )
        add(
            en to "Videos",
            ja to "動画"
        )
        add(
            en to "Singles",
            ja to "シングル",
            id = YoutubeUILocalisation.StringID.ARTIST_PAGE_SINGLES
        )
        add(
            en to "Playlists",
            ja to "プレイリスト"
        )
        add(
            en to "From your library",
            ja to "ライブラリから"
        )
        add(
            en to "Fans might also like",
            ja to "おすすめのアーティスト"
        )
        add(
            en to "Featured on",
            ja to "収録プレイリスト"
        )
    }
}
