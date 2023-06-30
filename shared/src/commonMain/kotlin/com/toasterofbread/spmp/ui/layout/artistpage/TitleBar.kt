package com.toasterofbread.spmp.ui.layout.artistpage

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toasterofbread.spmp.model.mediaitem.Artist
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.platform.vibrateShort
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.resources.getStringTODO
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.utils.composable.WidthShrinkText

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TitleBar(item: MediaItem, modifier: Modifier = Modifier) {
    val horizontal_padding = 20.dp
    var editing_title by remember { mutableStateOf(false) }
    Crossfade(editing_title) { editing ->
        Column(
            modifier.padding(start = horizontal_padding).fillMaxHeight(), 
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Bottom)
        ) {
            if (editing) {
                var edited_title by remember(item) { mutableStateOf(item.title!!) }

                Column(Modifier.fillMaxWidth().padding(end = horizontal_padding), horizontalAlignment = Alignment.End) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
                        @Composable
                        fun Action(icon: ImageVector, action: () -> Unit) {
                            Box(
                                Modifier
                                    .background(Theme.current.accent, CircleShape)
                                    .size(42.dp)
                                    .padding(8.dp)
                                    .clickable(onClick = action),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, null, tint = Theme.current.on_accent)
                            }
                        }

                        Action(Icons.Filled.Close) { editing_title = false }
                        Action(Icons.Filled.Refresh) { edited_title = item.original_title!! }
                        Action(Icons.Filled.Done) {
                            item.editRegistry {
                                it.title = edited_title
                            }
                            editing_title = false
                        }
                    }

                    val field_colour = Theme.current.on_accent
                    OutlinedTextField(
                        edited_title,
                        onValueChange = { text ->
                            edited_title = text
                        },
                        label = { Text(getStringTODO("Edit title")) },
                        singleLine = true,
                        trailingIcon = {
                            Icon(Icons.Filled.Close, null, Modifier.clickable { edited_title = "" })
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            item.editRegistry {
                                it.title = edited_title
                            }
                            editing_title = false
                        }),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = field_colour,
                            focusedLabelColor = field_colour,
                            cursorColor = field_colour
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

            } else {
                WidthShrinkText(
                    item.title ?: "",
                    Modifier
                        .combinedClickable(
                            onClick = {},
                            onLongClick = {
                                SpMp.context.vibrateShort()
                                editing_title = true
                            }
                        )
                        .fillMaxWidth()
                        .padding(end = horizontal_padding),
                    style = LocalTextStyle.current.copy(
                        textAlign = TextAlign.Center,
                        fontSize = 35.sp,
                    )
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item is Artist && (item.subscriber_count ?: 0) > 0) {
                    Text(item.getReadableSubscriberCount(), style = MaterialTheme.typography.labelLarge )
                }

                Spacer(Modifier.fillMaxWidth().weight(1f))

                if (!editing_title) {
                    IconButton({ editing_title = true }) {
                        Icon(Icons.Default.Edit, getString("edit_\$x_title_dialog_title").replace("\$x", MediaItemType.ARTIST.getReadable()))
                    }
                }

                Crossfade(item.pinned_to_home) { pinned ->
                    IconButton({ item.setPinnedToHome(!pinned) }) {
                        Icon(if (pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin, null)
                    }
                }

                if (item is Artist) {
                    ArtistSubscribeButton(item, Modifier.padding(end = horizontal_padding - 10.dp))
                }
            }
        }
    }
}
