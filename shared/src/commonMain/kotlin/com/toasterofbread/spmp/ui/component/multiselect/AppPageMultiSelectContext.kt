package com.toasterofbread.spmp.ui.component.multiselect

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.model.state.UiState
import com.toasterofbread.spmp.platform.FormFactor
import com.toasterofbread.spmp.platform.AppContext

class AppPageMultiSelectContext(
    private val ui_state: UiState,
    context: AppContext
): MediaItemMultiSelectContext(context) {
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
                ui_state.addMultiselectInfoAllItemsGetter(getAllItems)
            }

            onDispose {
                if (getAllItems != null) {
                    ui_state.removeMultiselectInfoAllItemsGetter(getAllItems)
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
