package com.toasterofbread.spmp.resources.uilocalisation.localised

import com.toasterofbread.spmp.resources.uilocalisation.YoutubeUILocalisation

fun getYoutubeOwnChannelLocalisations(languages: UILanguages): YoutubeUILocalisation.LocalisationSet =
    with(languages) {
        YoutubeUILocalisation.LocalisationSet().apply {
            add(
                en to "Songs on repeat",
                ja to "繰り返し再生されている曲" ,
                es to "Canciones que más escuchastes"
            )
            add(
                en to "Artists on repeat",
                ja to "繰り返し再生するアーティスト" ,
                es to "Artistas más escuchados" ,
            )
            add(
                en to "Videos on repeat",
                ja to "繰り返し再生されている動画"
            )
            add(
                en to "Playlists on repeat",
                ja to "繰り返し再生するプレイリスト" ,
                es to "Playlist mas escuchadas" 
            )
            add(
                en to "Playlists",
                ja to "再生リスト" ,
                es to "Playlists"
            )
        }
    }