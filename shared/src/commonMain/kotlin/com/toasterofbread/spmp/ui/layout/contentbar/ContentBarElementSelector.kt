package com.toasterofbread.spmp.ui.layout.contentbar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.platform.FormFactor
import com.toasterofbread.spmp.ui.layout.contentbar.element.ContentBarElement
import dev.toastbits.composekit.components.utils.composable.LargeDropdownMenu
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.content_bar_editor_add_element

@Composable
internal fun ContentBarElementSelector(
    button_colours: ButtonColors,
    modifier: Modifier = Modifier,
    onSelected: (ContentBarElement.Type) -> Unit
) {
    val show_element_buttons: Boolean = FormFactor.observe().value == FormFactor.LANDSCAPE
    var show_element_selector: Boolean by remember(show_element_buttons) { mutableStateOf(false) }

    val available_elements: List<ContentBarElement.Type> = ContentBarElement.Type.entries.filter { it.isAvailable() }

    LargeDropdownMenu(
        title = stringResource(Res.string.content_bar_editor_add_element),
        isOpen = show_element_selector,
        onDismissRequest = { show_element_selector = false },
        items = available_elements,
        selectedItem = null,
        onSelected = { _, item ->
            onSelected(item)
            show_element_selector = false
        }
    ) { element ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(element.getIcon(), null)
            Text(element.getName(), softWrap = false)
        }
    }

    if (!show_element_buttons) {
        Button(
            { show_element_selector = true },
            colors = button_colours
        ) {
            Icon(Icons.Default.Add, null)
            Text(stringResource(Res.string.content_bar_editor_add_element))
        }
    }
    else {
        Column(
            modifier,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Add, null)
                Text(stringResource(Res.string.content_bar_editor_add_element))
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                for (type in ContentBarElement.Type.entries.filter { it.isAvailable() }) {
                    Button(
                        { onSelected(type) },
                        colors = button_colours
                    ) {
                        Icon(type.getIcon(), null)
                        Text(type.getName(), softWrap = false)
                    }
                }
            }
        }
    }
}
