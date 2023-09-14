package com.toasterofbread.spmp.resources.uilocalisation.localised

import com.toasterofbread.spmp.resources.uilocalisation.YoutubeUILocalisation

fun getYoutubeFilterChipsLocalisations(languages: UILanguages): YoutubeUILocalisation.LocalisationSet =
    with(languages) {
        YoutubeUILocalisation.LocalisationSet().apply {
            add(
                en to "Relax",
                ja to "リラックス",
                es to "Relax"
            )
            add(
                en to "Energize",
                ja to "エナジー" ,
                es to "Energía"
            )
            add(
                en to "Energise",
                ja to "エナジー" ,
                es to "Energía"
            )
            add(
                en to "Workout",
                ja to "ワークアウト" ,
                es to "Entretenimiento"
            )
            add(
                en to "Commute",
                ja to "通勤・通学" ,
                es to "Para el camino"
            )
            add(
                en to "Focus",
                ja to "フォーカス" ,
                es to "Para concentrarse"
            )
            add(
                en to "Podcasts",
                ja to "ポッドキャスト" ,
                es to "Podcast" 
            )
        }
    }