package com.toasterofbread.spmp.resources.uilocalisation.localised

import com.toasterofbread.spmp.resources.uilocalisation.YoutubeUILocalisation

fun getYoutubeFilterChipsLocalisations(languages: UILanguages): YoutubeUILocalisation.LocalisationSet =
    with(languages) {
        YoutubeUILocalisation.LocalisationSet().apply {
            add(
                en to "Relax",
                ja to "リラックス",
                es to "Relax",
                zh to "放松",
                id = YoutubeUILocalisation.StringID.SONG_FEED_RELAX
            )
            add(
                en to "Energise",
                ja to "エナジー",
                es to "Energía",
                zh to "充电",
                id = YoutubeUILocalisation.StringID.SONG_FEED_ENERGISE
            )
            add(
                en to "Energize",
                ja to "エナジー",
                es to "Energía",
                zh to "活力",
                id = YoutubeUILocalisation.StringID.SONG_FEED_ENERGISE
            )
            add(
                en to "Workout",
                ja to "ワークアウト",
                es to "Entretenimiento",
                zh to "健身",
                id = YoutubeUILocalisation.StringID.SONG_FEED_WORKOUT
            )
            add(
                en to "Commute",
                ja to "通勤",
                es to "Para el camino",
                zh to "通勤",
                id = YoutubeUILocalisation.StringID.SONG_FEED_COMMUTE
            )
            add(
                en to "Focus",
                ja to "フォーカス",
                es to "Para concentrarse",
                zh to "专注",
                id = YoutubeUILocalisation.StringID.SONG_FEED_FOCUS
            )
            add(
                en to "Podcasts",
                ja to "ポッドキャスト",
                es to "Podcast",
                zh to "播客",
                id = YoutubeUILocalisation.StringID.SONG_FEED_PODCASTS
            )
            add(
                en to "Party",
                ja to "パーティー",
                zh to "派对",
                id = YoutubeUILocalisation.StringID.SONG_FEED_PARTY
            )
            add(
                en to "Romance",
                ja to "ロマンス",
                zh to "浪漫",
                id = YoutubeUILocalisation.StringID.SONG_FEED_ROMANCE
            )
            add(
                en to "Sad",
                ja to "悲しい",
                zh to "悲伤",
                id = YoutubeUILocalisation.StringID.SONG_FEED_SAD
            )
            add(
                en to "Feel good",
                ja to "ポジティブ",
                zh to "轻快",
                id = YoutubeUILocalisation.StringID.SONG_FEED_FEEL_GOOD
            )
            add(
                en to "Sleep",
                ja to "睡眠",
                zh to "睡眠",
                id = YoutubeUILocalisation.StringID.SONG_FEED_SLEEP
            )
        }
    }