package com.toasterofbread.spmp.resources.uilocalisation.localised

import com.toasterofbread.spmp.resources.uilocalisation.YoutubeUILocalisation

fun getYoutubeFilterChipsLocalisations(languages: UILanguages): YoutubeUILocalisation.LocalisationSet =
    with(languages) {
        YoutubeUILocalisation.LocalisationSet().apply {
            add(
                en to "Relax",
                ja to "リラックス",
                es to "Relax",
                id = YoutubeUILocalisation.StringID.SONG_FEED_RELAX
            )
            add(
                en to "Energize",
                ja to "エナジー",
                es to "Energía",
                id = YoutubeUILocalisation.StringID.SONG_FEED_ENERGISE
            )
            add(
                en to "Energise",
                ja to "エナジー",
                es to "Energía",
                id = YoutubeUILocalisation.StringID.SONG_FEED_ENERGISE
            )
            add(
                en to "Workout",
                ja to "ワークアウト",
                es to "Entretenimiento",
                id = YoutubeUILocalisation.StringID.SONG_FEED_WORKOUT
            )
            add(
                en to "Commute",
                ja to "通勤",
                es to "Para el camino",
                id = YoutubeUILocalisation.StringID.SONG_FEED_COMMUTE
            )
            add(
                en to "Focus",
                ja to "フォーカス",
                es to "Para concentrarse",
                id = YoutubeUILocalisation.StringID.SONG_FEED_FOCUS
            )
            add(
                en to "Podcasts",
                ja to "ポッドキャスト",
                es to "Podcast",
                id = YoutubeUILocalisation.StringID.SONG_FEED_PODCASTS
            )
            add(
                en to "Party",
                ja to "パーティー",
                id = YoutubeUILocalisation.StringID.SONG_FEED_PARTY
            )
            add(
                en to "Romance",
                ja to "ロマンス",
                id = YoutubeUILocalisation.StringID.SONG_FEED_ROMANCE
            )
            add(
                en to "Sad",
                ja to "悲しい",
                id = YoutubeUILocalisation.StringID.SONG_FEED_SAD
            )
            add(
                en to "Feel good",
                ja to "ポジティブ",
                id = YoutubeUILocalisation.StringID.SONG_FEED_FEEL_GOOD
            )
            add(
                en to "Sleep",
                ja to "睡眠",
                id = YoutubeUILocalisation.StringID.SONG_FEED_SLEEP
            )
        }
    }