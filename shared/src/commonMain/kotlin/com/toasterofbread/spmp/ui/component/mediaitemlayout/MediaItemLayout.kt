package com.toasterofbread.spmp.ui.component.mediaitemlayout

import SpMp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.beust.klaxon.Json
import com.toasterofbread.spmp.api.Api
import com.toasterofbread.spmp.api.Api.Companion.addYtHeaders
import com.toasterofbread.spmp.api.Api.Companion.getStream
import com.toasterofbread.spmp.api.Api.Companion.ytUrl
import com.toasterofbread.spmp.api.RadioModifier
import com.toasterofbread.spmp.api.YoutubeiBrowseResponse
import com.toasterofbread.spmp.api.cast
import com.toasterofbread.spmp.api.radio.getSongRadio
import com.toasterofbread.spmp.model.mediaitem.AccountPlaylist
import com.toasterofbread.spmp.model.mediaitem.BrowseParamsPlaylist
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.platform.composable.rememberImagePainter
import com.toasterofbread.spmp.resources.uilocalisation.LocalisedYoutubeString
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.mainpage.PlayerState
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.utils.composable.WidthShrinkText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

fun getDefaultMediaItemPreviewSize(): DpSize = DpSize(100.dp, 130.dp)

data class MediaItemLayout(
    val title: LocalisedYoutubeString?,
    val subtitle: LocalisedYoutubeString?,
    val type: Type? = null,
    val items: MutableList<MediaItem> = mutableListOf(),
    val thumbnail_source: ThumbnailSource? = null,
    val thumbnail_item_type: MediaItemType? = null,
    var view_more: ViewMore? = null,
    var continuation: Continuation? = null,
    var square_item_max_text_rows: Int? = null,
    @Json(ignored = true)
    var itemSizeProvider: @Composable () -> DpSize = { getDefaultMediaItemPreviewSize() }
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
                CARD -> MediaItemCard(layout, modifier, multiselect_context = multiselect_context, apply_filter = apply_filter)
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

        suspend fun loadContinuation(filters: List<RadioModifier> = emptyList()): Result<Pair<List<MediaItem>, String?>> {
            return when (type) {
                Type.SONG -> loadSongContinuation(filters)
                Type.PLAYLIST -> loadPlaylistContinuation(false)
                Type.PLAYLIST_INITIAL -> loadPlaylistContinuation(true)
            }
        }

        fun update(token: String) {
            this.token = token
            if (type == Type.PLAYLIST_INITIAL) {
                type = Type.PLAYLIST
            }
        }

        private suspend fun loadSongContinuation(filters: List<RadioModifier>): Result<Pair<List<MediaItem>, String?>> {
            val result = getSongRadio(param as String, token, filters)
            return result.fold(
                { Result.success(Pair(it.items, it.continuation)) },
                { Result.failure(it) }
            )
        }

        private suspend fun loadPlaylistContinuation(initial: Boolean): Result<Pair<List<MediaItem>, String?>> = withContext(Dispatchers.IO) {
            if (initial) {
                val playlist = AccountPlaylist.fromId(token)
                playlist.data.items?.also { items ->
                    return@withContext Result.success(Pair(
                        items.subList(param as Int, items.size - 1),
                        playlist.data.continuation?.token
                    ))
                }
            }

            val hl = SpMp.data_language
            val request = Request.Builder()
                .ytUrl(
                    if (initial) "/youtubei/v1/browse"
                    else "/youtubei/v1/browse?ctoken=$token&continuation=$token&type=next"
                )
                .addYtHeaders()
                .post(Api.getYoutubeiRequestBody(
                    if (initial) mapOf("browseId" to token)
                    else null
                ))
                .build()

            val result = Api.request(request)
            if (result.isFailure) {
                return@withContext result.cast()
            }

            val stream = result.getOrThrow().getStream()
            val parsed: YoutubeiBrowseResponse = try {
                Api.klaxon.parse(stream)!!
            }
            catch (e: Throwable) {
                return@withContext Result.failure(e)
            }
            finally {
                stream.close()
            }

            val shelf =
                if (initial) parsed.contents!!.singleColumnBrowseResultsRenderer!!.tabs.first().tabRenderer.content!!.sectionListRenderer.contents!!.single().musicPlaylistShelfRenderer!!
                else parsed.continuationContents!!.musicPlaylistShelfContinuation!!

            return@withContext Result.success(Pair(
                shelf.contents!!.withIndex().mapNotNull { item ->
                    if (item.index < param as Int) {
                        return@mapNotNull null
                    }
                    item.value.toMediaItem(hl)?.first
                },
                shelf.continuations?.firstOrNull()?.nextContinuationData?.continuation
            ))
        }
    }

    data class ViewMore(
        val list_page_browse_id: String? = null,
        val media_item: MediaItem? = null,
        val action: (() -> Unit)? = null,

        var layout_type: Type? = null,
        val browse_params: String? = null
    ) {
        var layout: MediaItemLayout? by mutableStateOf(null)
        
        init {
            check(list_page_browse_id != null || media_item != null || action != null) 
            check(browse_params == null || list_page_browse_id != null)
        }

        fun openViewMore(player: PlayerState, title: LocalisedYoutubeString?) {
            if (media_item != null) {
                player.openMediaItem(media_item, true)
            }
            else if (list_page_browse_id != null) {
                if (browse_params != null) {
                    player.openMediaItem(
                        BrowseParamsPlaylist
                            .fromId(list_page_browse_id, browse_params)
                            .apply { editData {
                                supplyTitle(title?.getString() ?: "")
                            }}
                    )
                }
                else {
                    player.openViewMorePage(list_page_browse_id)
                }
            }
            else if (action != null) {
                action.invoke()
            }
            else {
                throw NotImplementedError(toString())
            }
        }
    }

    class ThumbnailSource(val media_item: MediaItem? = null, val url: String? = null) {
        init {
            check(media_item != null || url != null)
        }

        fun getThumbUrl(quality: MediaItemThumbnailProvider.Quality): String? {
            return url ?: media_item?.getThumbUrl(quality)
        }
    }

    @Composable
    fun TitleBar(
        modifier: Modifier = Modifier,
        font_size: TextUnit? = null,
        multiselect_context: MediaItemMultiSelectContext? = null
    ) {
        TitleBar(title, subtitle, modifier, view_more, thumbnail_source, thumbnail_item_type, font_size, multiselect_context)
    }
}

internal fun shouldShowTitleBar(
    title: LocalisedYoutubeString?,
    subtitle: LocalisedYoutubeString?,
    view_more: MediaItemLayout.ViewMore? = null,
    thumbnail_source: MediaItemLayout.ThumbnailSource? = null
): Boolean = thumbnail_source != null || title != null || subtitle != null || view_more != null

@Composable
internal fun TitleBar(
    title: LocalisedYoutubeString?,
    subtitle: LocalisedYoutubeString?,
    modifier: Modifier = Modifier,
    view_more: MediaItemLayout.ViewMore? = null,
    thumbnail_source: MediaItemLayout.ThumbnailSource? = null,
    thumbnail_item_type: MediaItemType? = null,
    font_size: TextUnit? = null,
    multiselect_context: MediaItemMultiSelectContext? = null
) {
    AnimatedVisibility(shouldShowTitleBar(title, subtitle, view_more, thumbnail_source), modifier) {
        val title_string: String? = remember { title?.getString() }
        val subtitle_string: String? = remember { subtitle?.getString() }

        Row(
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val thumbnail_url = thumbnail_source?.getThumbUrl(MediaItemThumbnailProvider.Quality.LOW)
            if (thumbnail_url != null) {
                Image(
                    rememberImagePainter(thumbnail_url),
                    null,
                    Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .clip(if (thumbnail_item_type == MediaItemType.ARTIST) CircleShape else RectangleShape)
                )
            }

            Column(verticalArrangement = Arrangement.Center, modifier = Modifier.weight(1f)) {
                if (subtitle_string != null) {
                    WidthShrinkText(subtitle_string, style = MaterialTheme.typography.titleSmall.copy(color = Theme.on_background))
                }

                if (title_string != null) {
                    WidthShrinkText(
                        title_string,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.headlineMedium.let { style ->
                            style.copy(
                                color = Theme.on_background,
                                fontSize = font_size ?: style.fontSize
                            )
                        }
                    )
                }
            }

//            Row {
//                view_more?.also { view_more ->
//                    val player = LocalPlayerState.current
//                    IconButton({ view_more.openViewMore(player, title) }, Modifier.height(20.dp)) {
//                        Icon(Icons.Default.MoreHoriz, null)
//                    }
//                }
//
//                multiselect_context?.CollectionToggleButton(items)
//            }
        }
    }
}
