package com.toasterofbread.spmp.ui.component.multiselect

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.platform.FormFactor

class AppPageMultiSelectContext(private val player: PlayerState): MediaItemMultiSelectContext(player.context) {
    @Composable
    override fun InfoDisplayContent(
        modifier: Modifier,
        content_modifier: Modifier,
        getAllItems: (() -> List<List<MultiSelectItem>>)?,
        wrapContent: @Composable (@Composable () -> Unit) -> Unit,
        show_alt_content: Boolean,
        altContent: (@Composable () -> Unit)?
    ): Boolean {
        val form_factor: FormFactor by FormFactor.observe()
        if (!form_factor.is_large) {
            return super.InfoDisplayContent(modifier, content_modifier, getAllItems, wrapContent, show_alt_content, altContent)
        }

        // Displayed in PlayerState.PersistentContent()
        DisposableEffect(is_active, getAllItems) {
            if (!is_active) {
                return@DisposableEffect onDispose {}
            }

            if (getAllItems != null) {
                player.multiselect_info_all_items_getters.add(getAllItems)
            }

            onDispose {
                if (getAllItems != null) {
                    player.multiselect_info_all_items_getters.remove(getAllItems)
                }
            }
        }

        if (show_alt_content) {
            Box(modifier) {
                altContent?.invoke()
            }
        }

        return false
    }
}
