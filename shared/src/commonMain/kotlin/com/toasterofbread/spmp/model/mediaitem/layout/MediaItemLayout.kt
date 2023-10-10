package com.toasterofbread.spmp.model.mediaitem.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.resources.uilocalisation.LocalisedString
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MediaItemGrid
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MediaItemList
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.youtubeapi.EndpointNotImplementedException
import com.toasterofbread.spmp.youtubeapi.RadioBuilderModifier
import kotlinx.serialization.Serializable

fun getDefaultMediaItemPreviewSize(): DpSize = DpSize(100.dp, 120.dp)
@Composable
fun getMediaItemPreviewSquareAdditionalHeight(text_rows: Int?, line_height: TextUnit): Dp {
    return with(LocalDensity.current) { line_height.toDp() } * ((text_rows ?: 1) - 1)
}

data class MediaItemLayout(
    val items: List<MediaItem>,
    val title: LocalisedString?,
    val subtitle: LocalisedString?,
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
            title_modifier: Modifier = Modifier,
            multiselect_context: MediaItemMultiSelectContext? = null,
            apply_filter: Boolean = false,
            square_item_max_text_rows: Int? = null,
            show_download_indicators: Boolean = true
        ) {
            when (this) {
                GRID -> MediaItemGrid(layout, modifier, title_modifier, multiselect_context = multiselect_context, apply_filter = apply_filter, square_item_max_text_rows = square_item_max_text_rows, show_download_indicators = show_download_indicators)
                GRID_ALT -> MediaItemGrid(layout, modifier, title_modifier, alt_style = true, multiselect_context = multiselect_context, apply_filter = apply_filter, square_item_max_text_rows = square_item_max_text_rows, show_download_indicators = show_download_indicators)
                ROW -> MediaItemGrid(layout, modifier, title_modifier, 1, multiselect_context = multiselect_context, apply_filter = apply_filter, square_item_max_text_rows = square_item_max_text_rows, show_download_indicators = show_download_indicators)
                LIST -> MediaItemList(layout, modifier, title_modifier, false, multiselect_context = multiselect_context, apply_filter = apply_filter, show_download_indicators = show_download_indicators)
                NUMBERED_LIST -> MediaItemList(layout, modifier, title_modifier, true, multiselect_context = multiselect_context, apply_filter = apply_filter, show_download_indicators = show_download_indicators)
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
    fun Layout(modifier: Modifier = Modifier, title_Modifier: Modifier = Modifier, multiselect_context: MediaItemMultiSelectContext? = null, apply_filter: Boolean = false) {
        type!!.Layout(this, modifier, title_Modifier, multiselect_context, apply_filter)
    }

    @Serializable
    data class Continuation(var token: String, var type: Type, val song_id: String? = null, val playlist_skip_amount: Int = 0) {
        enum class Type {
            SONG,
            PLAYLIST,
            PLAYLIST_INITIAL
        }

        init {
            if (type == Type.SONG) {
                require(song_id != null)
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

            val result = radio_endpoint.getSongRadio(song_id!!, token, filters)
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

            return continuation_endpoint.getPlaylistContinuation(initial, token, if (initial) playlist_skip_amount else 0)
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
    title: LocalisedString?,
    subtitle: LocalisedString?,
    view_more: ViewMore? = null
): Boolean = title != null || subtitle != null || view_more != null
