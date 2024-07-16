package com.toasterofbread.spmp.ui.layout.contentbar

import androidx.compose.runtime.*
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import com.toasterofbread.spmp.ui.layout.contentbar.element.ContentBarElement
import com.toasterofbread.spmp.ui.component.shortcut.trigger.getName
import com.toasterofbread.spmp.platform.FormFactor
import com.toasterofbread.spmp.model.getIcon
import com.toasterofbread.spmp.resources.getString
import dev.toastbits.composekit.utils.composable.LargeDropdownMenu

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
        show_element_selector,
        { show_element_selector = false },
        available_elements.size,
        null,
        {
            val element: ContentBarElement.Type = available_elements[it]
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(element.getIcon(), null)
                Text(element.getName(), softWrap = false)
            }
        }
    ) {
        onSelected(available_elements[it])
        show_element_selector = false
    }

    if (!show_element_buttons) {
        Button(
            { show_element_selector = true },
            colors = button_colours
        ) {
            Icon(Icons.Default.Add, null)
            Text(getString("content_bar_editor_add_element"))
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
                Text(getString("content_bar_editor_add_element"))
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
