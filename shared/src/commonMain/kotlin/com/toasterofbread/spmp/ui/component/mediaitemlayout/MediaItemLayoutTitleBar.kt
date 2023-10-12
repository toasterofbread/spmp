package com.toasterofbread.spmp.ui.component.mediaitemlayout

import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.mediaitem.MediaItemHolder
import com.toasterofbread.spmp.model.mediaitem.layout.ViewMore
import com.toasterofbread.spmp.model.mediaitem.layout.shouldShowTitleBar
import com.toasterofbread.spmp.resources.uilocalisation.LocalisedString
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.utils.composable.WidthShrinkText

@Composable
fun TitleBar(
    items: List<MediaItemHolder>,
    title: LocalisedString?,
    subtitle: LocalisedString?,
    modifier: Modifier = Modifier,
    view_more: ViewMore? = null,
    font_size: TextUnit? = null,
    multiselect_context: MediaItemMultiSelectContext? = null
) {
    val player = LocalPlayerState.current

    AnimatedVisibility(shouldShowTitleBar(title, subtitle, view_more), modifier) {
        val title_string: String? = remember { title?.getString(player.context) }
        val subtitle_string: String? = remember { subtitle?.getString(player.context) }

        Row(
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.Center, modifier = Modifier.weight(1f)) {
                if (subtitle_string != null) {
                    WidthShrinkText(subtitle_string, style = MaterialTheme.typography.titleSmall.copy(color = player.theme.on_background))
                }

                if (title_string != null) {
                    WidthShrinkText(
                        title_string,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.headlineMedium.let { style ->
                            style.copy(
                                color = player.theme.on_background,
                                fontSize = font_size ?: style.fontSize
                            )
                        }
                    )
                }
            }

            Row {
                view_more?.also { view_more ->
                    IconButton({ view_more.execute(player, title) }) {
                        Icon(Icons.Default.MoreHoriz, null)
                    }
                }

                multiselect_context?.CollectionToggleButton(items)
            }
        }
    }
}
