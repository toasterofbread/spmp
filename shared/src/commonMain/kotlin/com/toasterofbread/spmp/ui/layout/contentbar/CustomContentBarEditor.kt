package com.toasterofbread.spmp.ui.layout.contentbar

import LocalPlayerState
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.*
import com.toasterofbread.composekit.platform.composable.platformClickable
import com.toasterofbread.composekit.utils.composable.*
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.contentbar.element.*
import kotlin.math.roundToInt

private const val BAR_SIZE_DP_STEP: Float = 10f

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CustomContentBarEditor(editing_bar: CustomContentBar, commit: (CustomContentBar) -> Unit) {
    val player: PlayerState = LocalPlayerState.current

    val vertical_bar: Boolean = false
    val delete_button_offset: Dp = 40.dp

    var bar: CustomContentBar by remember { mutableStateOf(editing_bar) }
    var selected_element: Int? by remember { mutableStateOf(null) }

    OnChangedEffect(editing_bar) {
        bar = editing_bar
    }

    fun deleteElement(index: Int) {
        bar = bar.copy(
            element_data = bar.element_data.toMutableList().apply { removeAt(index) }
        )

        if (selected_element == index) {
            selected_element = null
        }
        else {
            selected_element?.also { element ->
                if (element > index) {
                    selected_element = element - 1
                }
            }
        }

        commit(bar)
    }

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(getString("content_bar_editor_bar_size"))
                Spacer(Modifier.fillMaxWidth().weight(1f))

                IconButton({
                    bar = bar.copy(size_dp = (bar.size_dp - BAR_SIZE_DP_STEP).coerceAtLeast(BAR_SIZE_DP_STEP))
                    commit(bar)
                }) {
                    Icon(Icons.Default.Remove, null)
                }

                Text(bar.size_dp.roundToInt().toString() + "dp")

                IconButton({
                    bar = bar.copy(size_dp = bar.size_dp + BAR_SIZE_DP_STEP)
                    commit(bar)
                }) {
                    Icon(Icons.Default.Add, null)
                }

                IconButton(
                    {
                        bar = bar.copy(size_dp = CUSTOM_CONTENT_BAR_DEFAULT_SIZE_DP)
                        commit(bar)
                    },
                    Modifier.padding(start = 10.dp)
                    ) {
                    Icon(Icons.Default.Refresh, null)
                }
            }
        }

        ElementSelector { element_type ->
            bar = bar.copy(
                element_data = listOf(ContentBarElementData(element_type)) + bar.element_data
            )
            selected_element = 0

            commit(bar)
        }

        bar.CustomBarContent(
            vertical = vertical_bar,
            content_padding = PaddingValues(),
            selected_element_override = selected_element?.takeIf { bar.elements[it] !is ContentBarElementSpacer } ?: -1,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp)
                .border(2.dp, player.theme.vibrant_accent, RoundedCornerShape(16.dp))
                .padding(5.dp),
            spacerElementHandler = { index, spacer -> with(spacer) {
                SpacerElement(
                    vertical_bar,
                    Modifier
                        .clickable { selected_element = index }
                        .border(2.dp, player.theme.vibrant_accent)
                ) {
                    Box(Modifier.requiredSize(0.dp)) {
                        IconButton(
                            { deleteElement(index) },
                            Modifier.wrapContentSize(unbounded = true).offset(y = delete_button_offset)
                        ) {
                            Icon(Icons.Default.Delete, null, tint = player.theme.on_background)
                        }
                    }
                }
            }},
            buttonContent = { index, element ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.platformClickable(onClick = { selected_element = index })
                    ) {
                        element.Element(vertical_bar, Modifier)
                    }
                }

                IconButton(
                    { deleteElement(index) },
                    Modifier.offset(y = delete_button_offset)
                ) {
                    Icon(Icons.Default.Delete, null, tint = player.theme.on_background)
                }
            }
        )

        NullableValueAnimatedVisibility(selected_element) { element_index ->
            val element: ContentBarElement =
                element_index?.let { bar.elements.getOrNull(it) } ?: return@NullableValueAnimatedVisibility

            ElementEditor(element, element_index) {
                val data: MutableList<ContentBarElementData> = bar.element_data.toMutableList()
                data[element_index] = element.getData()
                bar = bar.copy(element_data = data)

                commit(bar)
            }
        }
    }
}

@Composable
private fun ElementSelector(
    modifier: Modifier = Modifier,
    onSelected: (ContentBarElement.Type) -> Unit
) {
    val player: PlayerState = LocalPlayerState.current

    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Add, null)
            Text(getString("content_bar_editor_add_element"))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (type in ContentBarElement.Type.entries) {
                Button({ onSelected(type) }) {
                    Icon(type.getIcon(), null)
                    Text(type.getName())
                }
            }
        }
    }
}

@Composable
private fun ElementEditor(
    element: ContentBarElement,
    index: Int,
    modifier: Modifier = Modifier,
    commit: () -> Unit
) {
    Column(modifier) {
        Text(
            getString("content_bar_editor_configure_element_\$x").replace("\$x", (index + 1).toString()),
            style = MaterialTheme.typography.headlineSmall
        )

        element.Configuration(modifier) {
            commit()
        }
    }
}
