package com.toasterofbread.spmp.ui.layout.artistpage

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.toastbits.composekit.context.vibrateShort
import dev.toastbits.composekit.util.composable.WidthShrinkText
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.artist.toReadableSubscriberCount
import com.toasterofbread.spmp.model.mediaitem.db.observePinnedToHome
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.appTextField
import dev.toastbits.composekit.theme.core.onAccent
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.`edit_$x_title_dialog_title`
import spmp.shared.generated.resources.`edit_$x_title_dialog_title`

@Composable
fun ArtistPageTitleBar(item: MediaItem, modifier: Modifier = Modifier) {
    val player = LocalPlayerState.current

    val horizontal_padding = 20.dp
    var editing_title by remember { mutableStateOf(false) }
    Crossfade(editing_title) { editing ->
        Column(
            modifier.padding(start = horizontal_padding).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Bottom)
        ) {
            if (editing) {
                var edited_title: String by remember(item) {
                    mutableStateOf(item.getActiveTitle(player.database) ?: "")
                }

                Column(Modifier.fillMaxWidth().padding(end = horizontal_padding), horizontalAlignment = Alignment.End) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
                        @Composable
                        fun Action(icon: ImageVector, action: () -> Unit) {
                            Box(
                                Modifier
                                    .background(player.theme.accent, CircleShape)
                                    .size(42.dp)
                                    .padding(8.dp)
                                    .clickable(onClick = action),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, null, tint = player.theme.onAccent)
                            }
                        }

                        Action(Icons.Filled.Close) { editing_title = false }
                        Action(Icons.Filled.Refresh) {
                            edited_title = player.database.mediaItemQueries.customTitleById(item.id).executeAsOne().custom_title ?: ""
                        }
                        Action(Icons.Filled.Done) {
                            player.database.mediaItemQueries.updateTitleById(edited_title, item.id)
                            editing_title = false
                        }
                    }

                    val field_colour: Color = player.theme.onAccent
                    OutlinedTextField(
                        edited_title,
                        onValueChange = { text ->
                            edited_title = text
                        },
                        label = { Text(stringResource(Res.string.`edit_$x_title_dialog_title`).replace("\$x", stringResource(item.getType().getReadable()))) },
                        singleLine = true,
                        trailingIcon = {
                            Icon(Icons.Filled.Close, null, Modifier.clickable { edited_title = "" })
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            player.database.mediaItemQueries.updateTitleById(edited_title, item.id)
                            editing_title = false
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = field_colour,
                            focusedLabelColor = field_colour,
                            cursorColor = field_colour
                        ),
                        modifier = Modifier.fillMaxWidth().appTextField()
                    )
                }

            }
            else {
                val item_title: String? by item.observeActiveTitle()

                WidthShrinkText(
                    item_title ?: "",
                    Modifier
                        .combinedClickable(
                            onClick = {},
                            onLongClick = {
                                player.context.vibrateShort()
                                editing_title = true
                            }
                        )
                        .fillMaxWidth()
                        .padding(end = horizontal_padding),
                    style = LocalTextStyle.current.copy(
                        textAlign = TextAlign.Center,
                        fontSize = 35.sp
                    )
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item is Artist) {
                    val subscriber_count: Int = item.SubscriberCount.observe(player.database).value ?: 0
                    if (subscriber_count > 0) {
                        Text(subscriber_count.toReadableSubscriberCount(player.context), style = MaterialTheme.typography.labelLarge)
                    }
                }

                Spacer(Modifier.fillMaxWidth().weight(1f))

                if (item is Artist && !item.isForItem()) {
                    player.context.ytapi.user_auth_state?.also { auth_state ->
                        ArtistSubscribeButton(item, auth_state)
                    }
                }

                var item_pinned: Boolean by item.observePinnedToHome()
                Crossfade(item_pinned) { pinned ->
                    IconButton({ item_pinned = !pinned }) {
                        Icon(if (pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin, null)
                    }
                }

                if (!editing_title) {
                    IconButton({ editing_title = true }) {
                        Icon(Icons.Default.Edit, stringResource(Res.string.`edit_$x_title_dialog_title`).replace("\$x", stringResource(MediaItemType.ARTIST.getReadable())))
                    }
                }

                Spacer(Modifier.width(horizontal_padding - 10.dp))
            }
        }
    }
}
