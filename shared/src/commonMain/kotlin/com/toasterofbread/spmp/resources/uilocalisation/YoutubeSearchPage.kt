package com.toasterofbread.spmp.resources.uilocalisation

fun getYoutubeSearchPageLocalisations(getLanguage: (String) -> Int): YoutubeUILocalisation.LocalisationSet {
    val en = getLanguage("en")
    val ja = getLanguage("ja")

    return YoutubeUILocalisation.LocalisationSet().apply {
        add(
            en to "Top result",
            ja to "上位の検索結果"
        )
        add(
            en to "Songs",
            ja to "曲"
        )
        add(
            en to "Videos",
            ja to "動画"
        )
        add(
            en to "Artists",
            ja to "アーティスト"
        )
        add(
            en to "Albums",
            ja to "アルバム"
        )
        add(
            en to "Community playlists",
            en to "Playlists",
            ja to "コミュニティの再生リスト",
            ja to "プレイリスト"
        )
        add(
            en to "Profiles",
            ja to "プロフィール"
        )
    }
}
