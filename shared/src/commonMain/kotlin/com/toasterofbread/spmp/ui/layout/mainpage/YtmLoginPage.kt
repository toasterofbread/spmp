package com.toasterofbread.spmp.ui.layout.mainpage

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.youtubeapi.composable.LoginPage

data class YtmLoginPage(val page: LoginPage, private val confirm_param: Any? = null): PlayerOverlayPage {
    @Composable
    override fun Page(previous_item: MediaItem?, close: () -> Unit) {
        page.LoginPage(
            Modifier.fillMaxSize(),
            confirm_param = confirm_param
        ) { result ->
            result?.fold(
                { Settings.KEY_YTM_AUTH.set(it) },
                { TODO(it.toString()) }
            )
            close()
        }
    }
}
