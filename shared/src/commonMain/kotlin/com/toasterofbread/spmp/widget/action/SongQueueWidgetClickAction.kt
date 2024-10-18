package com.toasterofbread.spmp.widget.action

import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource

@Serializable
enum class SongQueueWidgetClickAction(val nameResource: StringResource): TypeWidgetClickAction
