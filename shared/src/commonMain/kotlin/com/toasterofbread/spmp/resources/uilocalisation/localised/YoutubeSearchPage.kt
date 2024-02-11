package com.toasterofbread.spmp.resources.uilocalisation.localised

import com.toasterofbread.spmp.resources.uilocalisation.YoutubeUILocalisation

fun getYoutubeSearchPageLocalisations(languages: UILanguages): YoutubeUILocalisation.LocalisationSet =
    with(languages) {
        YoutubeUILocalisation.LocalisationSet().apply {
            add(
                en to "Top result",
                ja to "上位の検索結果" ,
                zh to "最佳结果" ,
                es to "Mejor resultado"
            )
            add(
                en to "Songs",
                ja to "曲" ,
                zh to "歌曲" ,
                es to "Canciónes"
            )
            add(
                en to "Videos",
                ja to "動画" ,
                zh to "视频" ,
                es to "Videos" 
            )
            add(
                en to "Artists",
                ja to "アーティスト" ,
                zh to "歌手" ,
                es to "Artistas"
            )
            add(
                en to "Albums",
                ja to "アルバム" ,
                zh to "专辑" ,
                es to "Artistas"
            )
            add(
                en to "Community playlists",
                en to "Playlists",
                ja to "コミュニティの再生リスト",
                ja to "プレイリスト" ,
                zh to "社区播放列表" ,
                es to "Playlist de la comunidad"
            )
            add(
                en to "Profiles",
                ja to "プロフィール" ,
                zh to "个人资料" ,
                es to "Perfiles"
            )
        }
    }