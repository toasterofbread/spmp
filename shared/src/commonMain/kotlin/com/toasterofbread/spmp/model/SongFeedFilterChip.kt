package com.toasterofbread.spmp.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import dev.toastbits.ytmkt.uistrings.YoutubeUILocalisation
import dev.toastbits.ytmkt.uistrings.YoutubeUiString
import dev.toastbits.ytmkt.endpoint.SongFeedFilterChip

fun SongFeedFilterChip.getId(): YoutubeUILocalisation.StringID? =
    (text as? YoutubeUiString)?.getYoutubeStringId()

fun SongFeedFilterChip.getIcon(): ImageVector? =
    when (getId()) {
        YoutubeUILocalisation.StringID.SONG_FEED_RELAX -> Icons.Outlined.Weekend
        YoutubeUILocalisation.StringID.SONG_FEED_ENERGISE -> Icons.Outlined.ElectricBolt
        YoutubeUILocalisation.StringID.SONG_FEED_WORKOUT -> Icons.AutoMirrored.Outlined.DirectionsRun
        YoutubeUILocalisation.StringID.SONG_FEED_COMMUTE -> Icons.Outlined.Train
        YoutubeUILocalisation.StringID.SONG_FEED_FOCUS -> Icons.Outlined.Keyboard
        YoutubeUILocalisation.StringID.SONG_FEED_PODCASTS -> Icons.Outlined.Podcasts
        YoutubeUILocalisation.StringID.SONG_FEED_PARTY -> Icons.Outlined.Celebration
        YoutubeUILocalisation.StringID.SONG_FEED_ROMANCE -> Icons.Outlined.Fireplace
        YoutubeUILocalisation.StringID.SONG_FEED_SAD -> Icons.Outlined.WaterDrop
        YoutubeUILocalisation.StringID.SONG_FEED_FEEL_GOOD -> Icons.Outlined.WbSunny
        YoutubeUILocalisation.StringID.SONG_FEED_SLEEP -> Icons.Outlined.Bedtime
        else -> null
    }

fun List<SongFeedFilterChip>.sortFilterChips(): List<SongFeedFilterChip> =
    sortedBy { FILTER_CHIP_ORDER.indexOf(it.getId()) }

private val FILTER_CHIP_ORDER: List<YoutubeUILocalisation.StringID> =
    listOf(
        YoutubeUILocalisation.StringID.SONG_FEED_RELAX,
        YoutubeUILocalisation.StringID.SONG_FEED_ENERGISE,
        YoutubeUILocalisation.StringID.SONG_FEED_WORKOUT,
        YoutubeUILocalisation.StringID.SONG_FEED_COMMUTE,
        YoutubeUILocalisation.StringID.SONG_FEED_FOCUS,
        YoutubeUILocalisation.StringID.SONG_FEED_FEEL_GOOD,
        YoutubeUILocalisation.StringID.SONG_FEED_PARTY,
        YoutubeUILocalisation.StringID.SONG_FEED_ROMANCE,
        YoutubeUILocalisation.StringID.SONG_FEED_SAD,
        YoutubeUILocalisation.StringID.SONG_FEED_SLEEP,
        YoutubeUILocalisation.StringID.SONG_FEED_PODCASTS
    )
