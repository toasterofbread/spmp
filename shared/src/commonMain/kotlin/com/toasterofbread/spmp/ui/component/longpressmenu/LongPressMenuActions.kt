package com.toasterofbread.spmp.ui.component.longpressmenu

import LocalPlayerState
import SpMp.isDebugBuild
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.toastbits.composekit.utils.common.getContrasted
import dev.toastbits.composekit.utils.composable.WidthShrinkText
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.toInfoString
import LocalAppState
import com.toasterofbread.spmp.ui.component.longpressmenu.artist.ArtistLongPressMenuInfo
import com.toasterofbread.spmp.ui.component.longpressmenu.playlist.PlaylistLongPressMenuInfo
import com.toasterofbread.spmp.ui.component.longpressmenu.song.SongLongPressMenuInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.lpm_action_share
import spmp.shared.generated.resources.lpm_action_open_external
import spmp.shared.generated.resources.lpm_action_print_info
import spmp.shared.generated.resources.lpm_action_hide

const val MENU_ITEM_SPACING: Int = 20

@Composable
internal fun ColumnScope.LongPressMenuInfoActions(
    data: LongPressMenuData,
    spacing: Dp,
    getAccentColour: () -> Color,
    onAction: () -> Unit,
) {
    val state: SpMp.State = LocalAppState.current
    val coroutine_scope: CoroutineScope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxHeight().weight(1f),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        when (data.item) {
            is Song -> SongLongPressMenuInfo(data.item, data.multiselect_key, getAccentColour)
            is Artist -> ArtistLongPressMenuInfo(data.item, getAccentColour)
            is Playlist -> PlaylistLongPressMenuInfo(data.item, getAccentColour)
        }
    }

    // Share
    if (state.context.canShare()) {
        LongPressMenuActionProvider.ActionButton(
            Icons.Filled.Share,
            stringResource(Res.string.lpm_action_share),
            getAccentColour,
            onClick = {
                coroutine_scope.launch {
                    state.context.shareText(
                        data.item.getUrl(state.context),
                        if (data.item is Song) data.item.getActiveTitle(state.database) else null
                    )
                }
            },
            onAction = onAction
        )
    }

    // Open
    if (state.context.canOpenUrl()) {
        LongPressMenuActionProvider.ActionButton(
            Icons.Filled.OpenWith,
            stringResource(Res.string.lpm_action_open_external),
            getAccentColour,
            onClick = {
                coroutine_scope.launch {
                    state.context.openUrl(data.item.getUrl(state.context))
                }
            },
            onAction = onAction
        )
    }

    if (isDebugBuild()) {
        Row(
            Modifier.clickable {
                val item_data = data.item.getEmptyData()
                item_data.populateData(item_data, state.database)
                println(item_data.toInfoString())
            },
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Print, null, tint = getAccentColour())
            WidthShrinkText(stringResource(Res.string.lpm_action_print_info), fontSize = 15.sp)
        }
    }
}

@Composable
internal fun ColumnScope.LongPressMenuActions(data: LongPressMenuData, background_colour: Color, getAccentColour: () -> Color, onAction: () -> Unit) {
    val state: SpMp.State = LocalAppState.current

    // Data-provided actions
    data.Actions(
        LongPressMenuActionProvider(
            { background_colour.getContrasted() },
            getAccentColour,
            { background_colour },
            onAction
        ),
        MENU_ITEM_SPACING.dp
    )

    // Hide
    LongPressMenuActionProvider.ActionButton(
        Icons.Filled.VisibilityOff,
        stringResource(Res.string.lpm_action_hide),
        getAccentColour,
        onClick = {
            data.item.Hidden.set(true, state.database)
        },
        onAction = onAction
    )
}
