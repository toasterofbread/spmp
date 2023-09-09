package com.toasterofbread.spmp.resources.uilocalisation.localised

import com.toasterofbread.spmp.resources.uilocalisation.YoutubeUILocalisation

fun getYoutubeArtistPageLocalisations(languages: UILanguages): YoutubeUILocalisation.LocalisationSet =
    with(languages) {
        YoutubeUILocalisation.LocalisationSet().apply {
            add(
                en to "Songs",
                ja to "曲",
                id = YoutubeUILocalisation.StringID.ARTIST_ROW_OTHER
            )
            add(
                en to "Albums",
                ja to "アルバム",
                id = YoutubeUILocalisation.StringID.ARTIST_ROW_OTHER
            )
            add(
                en to "Videos",
                ja to "動画",
                id = YoutubeUILocalisation.StringID.ARTIST_ROW_OTHER
            )
            add(
                en to "Singles",
                ja to "シングル",
                id = YoutubeUILocalisation.StringID.ARTIST_ROW_SINGLES
            )
            add(
                en to "Playlists",
                ja to "プレイリスト",
                id = YoutubeUILocalisation.StringID.ARTIST_ROW_OTHER
            )
            add(
                en to "From your library",
                ja to "ライブラリから",
                id = YoutubeUILocalisation.StringID.ARTIST_ROW_OTHER
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
