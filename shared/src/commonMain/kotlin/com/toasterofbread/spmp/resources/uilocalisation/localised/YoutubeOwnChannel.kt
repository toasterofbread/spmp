package com.toasterofbread.spmp.resources.uilocalisation.localised

import com.toasterofbread.spmp.resources.uilocalisation.YoutubeUILocalisation

fun getYoutubeOwnChannelLocalisations(languages: UILanguages): YoutubeUILocalisation.LocalisationSet =
    with(languages) {
        YoutubeUILocalisation.LocalisationSet().apply {
            add(
                en to "Songs on repeat",
                ja to "繰り返し再生されている曲" ,
                zh to "反复聆听的歌曲" ,
                es to "Canciones que más escuchastes"
            )
            add(
                en to "Artists on repeat",
                zh to "反复聆听的歌手" ,
                ja to "繰り返し再生するアーティスト" ,
                es to "Artistas más escuchados" ,
            )
            add(
                en to "Videos on repeat",
                zh to "反复收看的视频" ,
                ja to "繰り返し再生されている動画"
            )
            add(
                en to "Playlists on repeat",
                zh to "反复聆听的歌单" ,
                ja to "繰り返し再生するプレイリスト" ,
                es to "Playlist mas escuchadas" 
            )
            add(
                en to "Playlists",
                zh to "播放列表" ,
                ja to "再生リスト" ,
                es to "Playlists"
            )
        }
    }