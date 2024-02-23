package com.toasterofbread.spmp.youtubeapi.composable

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.toasterofbread.spmp.youtubeapi.YoutubeApi

abstract class LoginPage: YoutubeApi.Implementable {
    @Composable
    abstract fun LoginPage(
        modifier: Modifier,
        confirm_param: Any?,
        content_padding: PaddingValues,
        onFinished: (Result<YoutubeApi.UserAuthState>?) -> Unit
    )
    @Composable
    abstract fun LoginConfirmationDialog(info_only: Boolean, manual_only: Boolean, onFinished: (param: Any?) -> Unit)

    abstract fun getTitle(confirm_param: Any?): String?
    abstract fun getIcon(confirm_param: Any?): ImageVector?

    open fun targetsDisabledPadding(confirm_param: Any?): Boolean = false
}
