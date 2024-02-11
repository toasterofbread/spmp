package com.toasterofbread.spmp.resources.uilocalisation.localised

import com.toasterofbread.spmp.resources.uilocalisation.YoutubeUILocalisation

fun getYoutubeArtistPageLocalisations(languages: UILanguages): YoutubeUILocalisation.LocalisationSet =
    with(languages) {
        YoutubeUILocalisation.LocalisationSet().apply {
            add(
                en to "Songs",
                ja to "曲",
                es to "Canciónes",
                zh to "歌曲" ,
                id = YoutubeUILocalisation.StringID.ARTIST_ROW_SONGS
            )
            add(
                en to "Albums",
                ja to "アルバム",
                es to "Álbumes",
                zh to "专辑" ,
                id = YoutubeUILocalisation.StringID.ARTIST_ROW_OTHER
            )
            add(
                en to "Videos",
                ja to "動画",
                es to "Videos",
                zh to "视频" ,
                id = YoutubeUILocalisation.StringID.ARTIST_ROW_VIDEOS
            )
            add(
                en to "Singles",
                ja to "シングル",
                es to "Sencillos",
                zh to "单曲" ,
                id = YoutubeUILocalisation.StringID.ARTIST_ROW_SINGLES
            )
            add(
                en to "Playlists",
                ja to "プレイリスト",
                es to "Playlists",
                zh to "播放列表" ,
                id = YoutubeUILocalisation.StringID.ARTIST_ROW_OTHER
            )
            add(
                en to "From your library",
                ja to "ライブラリから",
                es to "De tu biblioteca",
                zh to "来自你的库" ,
                id = YoutubeUILocalisation.StringID.ARTIST_ROW_OTHER
            )
            add(
                en to "Fans might also like",
                en to "Similar artists",
                ja to "おすすめのアーティスト" ,
                ja to "似てるかも",
                es to "A los fans también podrían gustarles",
                zh to "粉丝可能还会喜欢",
                id = YoutubeUILocalisation.StringID.ARTIST_ROW_ARTISTS
            )
            add(
                en to "Featured on",
                ja to "収録プレイリスト" ,
                zh to "精选" ,
                es to "Aparece en"
            )
        }
    }