package com.toasterofbread.spmp.ui.layout.artistpage.lff

import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.toastbits.composekit.components.platform.composable.PlatformTextField
import dev.toastbits.composekit.components.platform.composable.platformClickable
import dev.toastbits.composekit.util.blendWith
import dev.toastbits.composekit.util.getContrasted
import dev.toastbits.composekit.components.utils.composable.LinkifyText
import dev.toastbits.composekit.components.utils.composable.ShapedIconButton
import dev.toastbits.ytmkt.model.external.ThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistLayout
import com.toasterofbread.spmp.model.mediaitem.artist.toReadableSubscriberCount
import com.toasterofbread.spmp.model.mediaitem.db.observePinnedToHome
import com.toasterofbread.spmp.model.mediaitem.layout.Layout
import com.toasterofbread.spmp.model.mediaitem.layout.AppMediaItemLayout
import com.toasterofbread.spmp.model.MediaItemLayoutParams
import com.toasterofbread.spmp.model.MediaItemGridParams
import com.toasterofbread.spmp.model.mediaitem.observeUrl
import com.toasterofbread.spmp.ui.component.Thumbnail
import com.toasterofbread.spmp.ui.component.WAVE_BORDER_HEIGHT_DP
import com.toasterofbread.spmp.ui.component.WaveBorder
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.appTextField
import com.toasterofbread.spmp.ui.layout.artistpage.ArtistInfoDialog
import com.toasterofbread.spmp.ui.layout.artistpage.ArtistSubscribeButton
import dev.toastbits.ytmkt.model.external.ItemLayoutType
import dev.toastbits.ytmkt.uistrings.YoutubeUILocalisation
import dev.toastbits.ytmkt.uistrings.YoutubeUiString

@Composable
fun LFFArtistStartPane(
    modifier: Modifier,
    artist: Artist,
    multiselect_context: MediaItemMultiSelectContext?,
    content_padding: PaddingValues,
    current_accent_colour: Color,
    item_layouts: List<ArtistLayout>?,
    apply_filter: Boolean
) {
    val player: PlayerState = LocalPlayerState.current

    val start_padding: Dp = content_padding.calculateStartPadding(LocalLayoutDirection.current)
    val primary_shape: Shape = RoundedCornerShape(15.dp)

    Column(modifier) {
        BoxWithConstraints(
            Modifier
                .fillMaxHeight()
                .weight(1f)
        ) {
            val scroll_state: ScrollState = rememberScrollState()

            Column(
                Modifier
                    .heightIn(min = maxHeight)
                    .verticalScroll(scroll_state)
            ) {
                Spacer(Modifier.height(content_padding.calculateTopPadding()))

                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = start_padding)
                        .aspectRatio(1f)
                        .clip(primary_shape),
                    contentAlignment = Alignment.Center
                ) {
                    artist.Thumbnail(
                        ThumbnailProvider.Quality.HIGH,
                        Modifier.fillMaxWidth().aspectRatio(1f)
                    )

                    Row(
                        Modifier.align(Alignment.BottomEnd).padding(5.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val icon_button_colours: IconButtonColors = IconButtonDefaults.iconButtonColors(
                            containerColor = current_accent_colour.copy(alpha = 0.5f),
                            contentColor = current_accent_colour.getContrasted()
                        )
                        val icon_button_shape: Shape = CircleShape

                        Spacer(Modifier.fillMaxWidth().weight(1f))

                        val shuffle_playlist_id: String? by artist.ShufflePlaylistId.observe(player.database)
                        if (shuffle_playlist_id != null) {
                            ShapedIconButton(
                                { player.playMediaItem(artist, shuffle = true) },
                                icon_button_colours,
                                shape = icon_button_shape
                            ) {
                                Icon(Icons.Default.Shuffle, null)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                val title: String? by artist.observeActiveTitle()
                var editing_title: Boolean by remember { mutableStateOf(false) }
                var edited_title: String by remember { mutableStateOf(title ?: "") }

                val title_style: TextStyle = MaterialTheme.typography.displaySmall.copy(textAlign = TextAlign.Center)
                val title_padding: PaddingValues = PaddingValues(10.dp)

                LaunchedEffect(editing_title) {
                    if (editing_title) {
                        edited_title = title ?: ""
                    }
                }

                LaunchedEffect(edited_title) {
                    if (editing_title) {
                        artist.setActiveTitle(edited_title, player.context)
                    }
                }

                title?.also {
                    Box(Modifier.padding(start = start_padding).align(Alignment.CenterHorizontally), contentAlignment = Alignment.Center) {
                        Text(
                            it,
                            Modifier
                                .padding(title_padding)
                                .platformClickable(
                                    onAltClick = { editing_title = true }
                                ),
                            style = title_style
                        )

                        this@Column.AnimatedVisibility(editing_title, enter = fadeIn(), exit = fadeOut()) {
                            PlatformTextField(
                                edited_title,
                                { edited ->
                                    if (edited.contains('\n')) {
                                        editing_title = false
                                        return@PlatformTextField
                                    }

                                    edited_title = edited
                                },
                                Modifier.fillMaxWidth().appTextField(),
                                text_style = title_style,
                                background_colour = current_accent_colour.blendWith(player.theme.background, 0.2f),
                                shape = primary_shape,
                                content_padding = title_padding
                            )
                        }
                    }
                }

                val subscriber_count: Int? = artist.SubscriberCount.observe(player.database).value
                Row(Modifier.padding(start = start_padding), verticalAlignment = Alignment.CenterVertically) {
                    if (subscriber_count != null) {
                        Text(subscriber_count.toReadableSubscriberCount(player.context), style = MaterialTheme.typography.bodyLarge)
                    }
                    else {
                        InfoButtons(artist)
                    }

                    Spacer(Modifier.fillMaxWidth().weight(1f))

                    player.context.ytapi.user_auth_state?.also { auth_state ->
                        ArtistSubscribeButton(artist, auth_state)
                    }

                    var item_pinned: Boolean by artist.observePinnedToHome()
                    Crossfade(item_pinned) { pinned ->
                        IconButton({ item_pinned = !pinned }) {
                            Icon(if (pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin, null)
                        }
                    }

                    Crossfade(editing_title) { editing ->
                        IconButton({ editing_title = !editing }) {
                            Icon(
                                if (editing) Icons.Default.Done
                                else Icons.Default.Edit,
                                null
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp).fillMaxWidth())

                for (item_layout in item_layouts ?: emptyList()) {
                    val layout: AppMediaItemLayout = item_layout.rememberMediaItemLayout(player.database).layout
                    if ((layout.title as? YoutubeUiString)?.getYoutubeStringId() != YoutubeUILocalisation.StringID.ARTIST_ROW_ARTISTS) {
                        continue
                    }

                    ItemLayoutType.ROW.Layout(
                        layout,
                        MediaItemLayoutParams(
                            modifier = Modifier.height(200.dp),
                            multiselect_context = multiselect_context,
                            apply_filter = apply_filter,
                            content_padding = PaddingValues(start = start_padding),
                            title_modifier = Modifier.alpha(0.75f),
                            getTitleTextStyle = { style -> style.copy(fontSize = 20.sp )}
                        ),
                        grid_params = MediaItemGridParams(
                            itemSizeProvider = {
                                DpSize(100.dp, 120.dp)
                            }
                        )
                    )
                }

                Spacer(Modifier.height(5.dp).fillMaxWidth())

                val long_description: Boolean = scroll_state.canScrollForward || scroll_state.canScrollBackward
                val description: String? by artist.Description.observe(player.database)

                if (!long_description) {
                    description?.also { desc ->
                        if (desc.isNotBlank()) {
                            LinkifyText(desc, current_accent_colour, Modifier.padding(start = start_padding), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }

                if (subscriber_count != null) {
                    Row(
                        Modifier.padding(start = start_padding).align(Alignment.End),
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        InfoButtons(artist)
                    }
                }

                if (long_description) {
                    description?.also { desc ->
                        Spacer(Modifier.height(WAVE_BORDER_HEIGHT_DP.dp + 10.dp))

                        WaveBorder(
                            Modifier.fillMaxWidth(),
                            waves = 6,
                            border_colour = LocalContentColor.current.copy(alpha = 0.5f),
                            border_thickness = 2.dp,
                            getOffset = { -it }
                        )

                        if (desc.isNotBlank()) {
                            LinkifyText(desc, current_accent_colour, Modifier.padding(start = start_padding), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }

                Spacer(Modifier.height(content_padding.calculateBottomPadding()))
            }
        }
    }
}

@Composable
private fun InfoButtons(artist: Artist) {
    val player: PlayerState = LocalPlayerState.current

    var show_info: Boolean by remember { mutableStateOf(false) }
    if (show_info) {
        ArtistInfoDialog(artist) { show_info = false }
    }

    val artist_url: String = artist.observeUrl()

    if (player.context.canShare()) {
        IconButton({
            player.context.shareText(
                artist_url,
                artist.getActiveTitle(player.database) ?: ""
            )
        }) {
            Icon(Icons.Outlined.Share, null)
        }
    }

    if (player.context.canOpenUrl()) {
        IconButton({
            player.context.openUrl(artist_url)
        }) {
            Icon(Icons.Outlined.OpenInNew, null)
        }
    }

    IconButton({ show_info = !show_info }) {
        Icon(Icons.Default.Info, null)
    }
}
