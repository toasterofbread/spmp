package com.toasterofbread.spmp.resources.uilocalisation.localised

import com.toasterofbread.spmp.resources.uilocalisation.YoutubeUILocalisation

fun getYoutubeFilterChipsLocalisations(languages: UILanguages): YoutubeUILocalisation.LocalisationSet =
    with(languages) {
        YoutubeUILocalisation.LocalisationSet().apply {
            add(
                en to "Relax",
                ja to "リラックス"
            )
            add(
                en to "Energize",
                ja to "エナジー"
            )
            add(
                en to "Energise",
                ja to "エナジー"
            )
            add(
                en to "Workout",
                ja to "ワークアウト"
            )
            add(
                en to "Commute",
                ja to "通勤・通学"
            )
            add(
                en to "Focus",
                ja to "フォーカス"
            )
            add(
                en to "Podcasts",
                ja to "ポッドキャスト"
            )
        }
    }
