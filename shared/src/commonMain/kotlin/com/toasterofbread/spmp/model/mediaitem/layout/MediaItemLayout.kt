package com.toasterofbread.spmp.model.mediaitem.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.resources.uilocalisation.LocalisedYoutubeString
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MediaItemGrid
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MediaItemList
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.youtubeapi.EndpointNotImplementedException
import com.toasterofbread.spmp.youtubeapi.RadioBuilderModifier

fun getDefaultMediaItemPreviewSize(): DpSize = DpSize(100.dp, 120.dp)

data class MediaItemLayout(
    val items: List<MediaItem>,
    val title: LocalisedYoutubeString?,
    val subtitle: LocalisedYoutubeString?,
    val type: Type? = null,
    var view_more: ViewMore? = null,
    val continuation: Continuation? = null
) {
    init {
        title?.getString()
        subtitle?.getString()
    }

    enum class Type {
        GRID,
        GRID_ALT,
        ROW,
        LIST,
        NUMBERED_LIST,
        CARD;

        @Composable
        fun Layout(
            layout: MediaItemLayout,
            modifier: Modifier = Modifier,
            multiselect_context: MediaItemMultiSelectContext? = null,
            apply_filter: Boolean = false
        ) {
            when (this) {
                GRID -> MediaItemGrid(layout, modifier, multiselect_context = multiselect_context, apply_filter = apply_filter)
                GRID_ALT -> MediaItemGrid(layout, modifier, alt_style = true, multiselect_context = multiselect_context, apply_filter = apply_filter)
                ROW -> MediaItemGrid(layout, modifier, 1, multiselect_context = multiselect_context, apply_filter = apply_filter)
                LIST -> MediaItemList(layout, modifier, false, multiselect_context = multiselect_context, apply_filter = apply_filter)
                NUMBERED_LIST -> MediaItemList(layout, modifier, true, multiselect_context = multiselect_context, apply_filter = apply_filter)
                CARD -> com.toasterofbread.spmp.ui.component.mediaitemlayout.MediaItemCard(
                    layout,
                    modifier,
                    multiselect_context = multiselect_context,
                    apply_filter = apply_filter
                )
            }
        }
    }

    @Composable
    fun Layout(modifier: Modifier = Modifier, multiselect_context: MediaItemMultiSelectContext? = null, apply_filter: Boolean = false) {
        type!!.Layout(this, modifier, multiselect_context, apply_filter)
    }

    data class Continuation(var token: String, var type: Type, val param: Any? = null) {
        enum class Type {
            SONG, // param is the song's ID
            PLAYLIST, // param unused
            PLAYLIST_INITIAL // param is the amount of songs to omit from the beginning
        }

        init {
            if (type == Type.SONG) {
                require(param is String)
            }
            else if (type == Type.PLAYLIST_INITIAL) {
                require(param is Int)
            }
        }

        suspend fun loadContinuation(context: PlatformContext, filters: List<RadioBuilderModifier> = emptyList()): Result<Pair<List<MediaItemData>, String?>> {
            return when (type) {
                Type.SONG -> loadSongContinuation(filters, context)
                Type.PLAYLIST -> loadPlaylistContinuation(false, context)
                Type.PLAYLIST_INITIAL -> loadPlaylistContinuation(true, context)
            }
        }

        fun update(token: String) {
            this.token = token
            if (type == Type.PLAYLIST_INITIAL) {
                type = Type.PLAYLIST
            }
        }

        private suspend fun loadSongContinuation(filters: List<RadioBuilderModifier>, context: PlatformContext): Result<Pair<List<MediaItemData>, String?>> {
            val radio_endpoint = context.ytapi.SongRadio
            if (!radio_endpoint.isImplemented()) {
                return Result.failure(EndpointNotImplementedException(radio_endpoint))
            }

            val result = radio_endpoint.getSongRadio(param as String, token, filters)
            return result.fold(
                { Result.success(Pair(it.items, it.continuation)) },
                { Result.failure(it) }
            )
        }

        private suspend fun loadPlaylistContinuation(initial: Boolean, context: PlatformContext): Result<Pair<List<MediaItemData>, String?>> {
            val continuation_endpoint = context.ytapi.PlaylistContinuation

            if (!continuation_endpoint.isImplemented()) {
                return Result.failure(EndpointNotImplementedException(continuation_endpoint))
            }

            return continuation_endpoint.getPlaylistContinuation(initial, token, if (initial) param as Int else 0)
        }
    }

    @Composable
    fun TitleBar(
        modifier: Modifier = Modifier,
        font_size: TextUnit? = null,
        multiselect_context: MediaItemMultiSelectContext? = null
    ) {
        com.toasterofbread.spmp.ui.component.mediaitemlayout.TitleBar(items, title, subtitle, modifier, view_more, font_size, multiselect_context)
    }
}

internal fun shouldShowTitleBar(
    title: LocalisedYoutubeString?,
    subtitle: LocalisedYoutubeString?,
    view_more: ViewMore? = null
): Boolean = title != null || subtitle != null || view_more != null
