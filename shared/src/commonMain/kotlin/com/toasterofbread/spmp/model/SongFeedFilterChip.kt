package com.toasterofbread.spmp.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import dev.toastbits.ytmkt.uistrings.YoutubeUILocalisation
import dev.toastbits.ytmkt.uistrings.YoutubeUiString
import dev.toastbits.ytmkt.endpoint.SongFeedFilterChip
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.error_message_generic
import spmp.shared.generated.resources.home_feed_filter_commute
import spmp.shared.generated.resources.home_feed_filter_energise
import spmp.shared.generated.resources.home_feed_filter_feel_good
import spmp.shared.generated.resources.home_feed_filter_focus
import spmp.shared.generated.resources.home_feed_filter_party
import spmp.shared.generated.resources.home_feed_filter_podcasts
import spmp.shared.generated.resources.home_feed_filter_relax
import spmp.shared.generated.resources.home_feed_filter_romance
import spmp.shared.generated.resources.home_feed_filter_sad
import spmp.shared.generated.resources.home_feed_filter_sleep
import spmp.shared.generated.resources.home_feed_filter_workout

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

fun SongFeedFilterChip.getDisplayStringResource(): StringResource =
    when (getId()) {
        YoutubeUILocalisation.StringID.SONG_FEED_RELAX -> Res.string.home_feed_filter_relax
        YoutubeUILocalisation.StringID.SONG_FEED_ENERGISE -> Res.string.home_feed_filter_energise
        YoutubeUILocalisation.StringID.SONG_FEED_WORKOUT -> Res.string.home_feed_filter_workout
        YoutubeUILocalisation.StringID.SONG_FEED_COMMUTE -> Res.string.home_feed_filter_commute
        YoutubeUILocalisation.StringID.SONG_FEED_FOCUS -> Res.string.home_feed_filter_focus
        YoutubeUILocalisation.StringID.SONG_FEED_PODCASTS -> Res.string.home_feed_filter_podcasts
        YoutubeUILocalisation.StringID.SONG_FEED_PARTY -> Res.string.home_feed_filter_party
        YoutubeUILocalisation.StringID.SONG_FEED_ROMANCE -> Res.string.home_feed_filter_romance
        YoutubeUILocalisation.StringID.SONG_FEED_SAD -> Res.string.home_feed_filter_sad
        YoutubeUILocalisation.StringID.SONG_FEED_FEEL_GOOD -> Res.string.home_feed_filter_feel_good
        YoutubeUILocalisation.StringID.SONG_FEED_SLEEP -> Res.string.home_feed_filter_sleep
        else -> Res.string.error_message_generic
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
