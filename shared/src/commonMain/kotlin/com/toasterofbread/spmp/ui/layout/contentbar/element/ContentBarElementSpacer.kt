package com.toasterofbread.spmp.ui.layout.contentbar.element

import LocalPlayerState
import androidx.compose.animation.*
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

class ContentBarElementSpacer(data: ContentBarElementData): ContentBarElement(data) {
    @Composable
    override fun ElementContent(vertical: Boolean, modifier: Modifier) {}
}
