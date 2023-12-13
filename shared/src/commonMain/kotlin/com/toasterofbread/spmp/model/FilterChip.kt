package com.toasterofbread.spmp.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.toasterofbread.spmp.resources.uilocalisation.LocalisedString
import com.toasterofbread.spmp.resources.uilocalisation.YoutubeLocalisedString
import com.toasterofbread.spmp.resources.uilocalisation.YoutubeUILocalisation
import com.toasterofbread.spmp.resources.uilocalisation.YoutubeUILocalisation.StringID.*

data class FilterChip(
    val text: LocalisedString,
    val params: String
) {
    fun getId(): YoutubeUILocalisation.StringID? =
        (text as? YoutubeLocalisedString)?.getYoutubeStringId()

    fun getIcon(): ImageVector? =
        when (getId()) {
            SONG_FEED_RELAX -> Icons.Outlined.Weekend
            SONG_FEED_ENERGISE -> Icons.Outlined.ElectricBolt
            SONG_FEED_WORKOUT -> Icons.Outlined.DirectionsRun
            SONG_FEED_COMMUTE -> Icons.Outlined.Train
            SONG_FEED_FOCUS -> Icons.Outlined.Keyboard
            SONG_FEED_PODCASTS -> Icons.Outlined.Podcasts
            SONG_FEED_PARTY -> Icons.Outlined.Celebration
            SONG_FEED_ROMANCE -> Icons.Outlined.Fireplace
            SONG_FEED_SAD -> Icons.Outlined.WaterDrop
            SONG_FEED_FEEL_GOOD -> Icons.Outlined.WbSunny
            SONG_FEED_SLEEP -> Icons.Outlined.Bedtime
            else -> null
        }

    companion object {
        val ORDER: List<YoutubeUILocalisation.StringID> = listOf(
            SONG_FEED_RELAX,
            SONG_FEED_ENERGISE,
            SONG_FEED_WORKOUT,
            SONG_FEED_COMMUTE,
            SONG_FEED_FOCUS,
            SONG_FEED_FEEL_GOOD,
            SONG_FEED_PARTY,
            SONG_FEED_ROMANCE,
            SONG_FEED_SAD,
            SONG_FEED_SLEEP,
            SONG_FEED_PODCASTS
        )
    }
}

fun List<FilterChip>.sortFilterChips(): List<FilterChip> {
    return sortedBy { FilterChip.ORDER.indexOf(it.getId()) }
}
