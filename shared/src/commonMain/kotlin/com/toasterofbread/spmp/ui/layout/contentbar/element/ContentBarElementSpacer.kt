package com.toasterofbread.spmp.ui.layout.contentbar.element

import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import com.toasterofbread.composekit.utils.common.thenIf
import com.toasterofbread.composekit.utils.composable.RowOrColumnScope
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import kotlin.math.roundToInt
import kotlinx.serialization.json.*

private const val SIZE_DP_STEP: Float = 10f
private const val MIN_SIZE_DP: Float = 10f

class ContentBarElementSpacer(data: JsonObject?): ContentBarElement {
    // If negative, spacer will fill width/height
    private var size_dp: Float by mutableStateOf(
        data?.get("size_dp")?.jsonPrimitive?.float ?: MIN_SIZE_DP
    )

    private fun getJsonData(): JsonObject = Json.encodeToJsonElement(
        mapOf("size_dp" to size_dp)
    ).jsonObject

    override fun getData(): ContentBarElementData =
        ContentBarElementData(type = ContentBarElement.Type.SPACER, data = getJsonData())

    @Composable
    override fun Element(vertical: Boolean, modifier: Modifier) {}

    @Composable
    fun RowOrColumnScope.SpacerElement(
        vertical: Boolean,
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit = {}
    ) {
        val player: PlayerState = LocalPlayerState.current

        val spacer_modifier: Modifier
        if (size_dp >= 0) {
            // Static size
            spacer_modifier =
                if (vertical) Modifier.height(size_dp.dp)
                else Modifier.width(size_dp.dp)
        }

        else {
            // Fill width/height
            spacer_modifier = Modifier
                .weight(1f)
                .then(
                    if (vertical) Modifier.fillMaxHeight().padding(vertical = 10.dp)
                    else Modifier.fillMaxWidth().padding(horizontal = 10.dp)
                )
        }

        val size: Dp = 30.dp
        Box(
            modifier
                .then(spacer_modifier)
                .then(
                    if (vertical) Modifier.width(size)
                    else Modifier.height(size)
                ),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }

    @Composable
    override fun Configuration(modifier: Modifier, onModification: () -> Unit) {
        var previous_static_size: Float by remember { mutableStateOf(0f) }

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(getString("content_bar_element_spacer_config_fill"))

                Spacer(Modifier.fillMaxWidth().weight(1f))

                Switch(
                    checked = size_dp < 0,
                    onCheckedChange = { checked ->
                        if (checked) {
                            previous_static_size = size_dp.coerceAtLeast(0f)
                        }
                        size_dp = if (checked) -1f else previous_static_size
                        onModification()
                    }
                )
            }

            AnimatedVisibility(size_dp >= 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(getString("content_bar_element_spacer_config_size"))

                    Spacer(Modifier.fillMaxWidth().weight(1f))

                    IconButton({
                        size_dp = (size_dp - SIZE_DP_STEP).coerceAtLeast(MIN_SIZE_DP)
                        onModification()
                    }) {
                        Icon(Icons.Default.Remove, null)
                    }

                    Text(size_dp.roundToInt().toString() + "dp")

                    IconButton({
                        size_dp += SIZE_DP_STEP
                        onModification()
                    }) {
                        Icon(Icons.Default.Add, null)
                    }
                }
            }
        }
    }
}
