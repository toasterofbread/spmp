package com.toasterofbread.spmp.resources.uilocalisation.localised

import com.toasterofbread.spmp.resources.uilocalisation.YoutubeUILocalisation

fun getYoutubeSearchPageLocalisations(languages: UILanguages): YoutubeUILocalisation.LocalisationSet =
    with(languages) {
        YoutubeUILocalisation.LocalisationSet().apply {
            add(
                en to "Top result",
                ja to "上位の検索結果" ,
                es to "Mejor resultado"
            )
            add(
                en to "Songs",
                ja to "曲" ,
                es to "Canciónes"
            )
            add(
                en to "Videos",
                ja to "動画" ,
                es to "Videos" 
            )
            add(
                en to "Artists",
                ja to "アーティスト" ,
                es to "Artistas"
            )
            add(
                en to "Albums",
                ja to "アルバム" ,
                es to "Artistas"
            )
            add(
                en to "Community playlists",
                en to "Playlists",
                ja to "コミュニティの再生リスト",
                ja to "プレイリスト" ,
                es to "Playlist de la comunidad"
            )
            add(
                en to "Profiles",
                ja to "プロフィール" ,
                es to "Perfiles"
            )
        }
    }