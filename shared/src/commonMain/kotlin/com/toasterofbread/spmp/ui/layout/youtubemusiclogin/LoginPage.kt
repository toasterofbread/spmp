package com.toasterofbread.spmp.ui.layout.youtubemusiclogin

import dev.toastbits.ytmkt.model.ApiAuthenticationState
import dev.toastbits.ytmkt.model.ApiImplementable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import dev.toastbits.ytmkt.model.YtmApi
import dev.toastbits.ytmkt.impl.unimplemented.UnimplementedYtmApi
import dev.toastbits.ytmkt.impl.youtubei.YoutubeiApi

abstract class LoginPage: ApiImplementable {
    @Composable
    abstract fun LoginPage(
        modifier: Modifier,
        confirm_param: Any?,
        content_padding: PaddingValues,
        onFinished: (Result<ApiAuthenticationState>?) -> Unit
    )
    @Composable
    abstract fun LoginConfirmationDialog(info_only: Boolean, manual_only: Boolean, onFinished: (param: Any?) -> Unit)

    @Composable
    abstract fun getTitle(confirm_param: Any?): String?
    abstract fun getIcon(confirm_param: Any?): ImageVector?

    open fun targetsDisabledPadding(confirm_param: Any?): Boolean = false
}

val YtmApi.LoginPage: LoginPage
    get() = when (this) {
        is UnimplementedYtmApi -> UnimplementedLoginPage()
        is YoutubeiApi -> YoutubeMusicLoginPage(this)
        else -> throw NotImplementedError(this::class.toString())
    }

private class UnimplementedLoginPage: LoginPage() {
    override fun isImplemented(): Boolean = false
    @Composable
    override fun LoginPage(
        modifier: Modifier,
        confirm_param: Any?,
        content_padding: PaddingValues,
        onFinished: (Result<ApiAuthenticationState>?) -> Unit
    ) {
        throw NotImplementedError()
    }
    @Composable
    override fun LoginConfirmationDialog(info_only: Boolean, manual_only: Boolean, onFinished: (param: Any?) -> Unit) {
        throw NotImplementedError()
    }
    @Composable
    override fun getTitle(confirm_param: Any?): String? {
        throw NotImplementedError()
    }
    override fun getIcon(confirm_param: Any?): ImageVector? {
        throw NotImplementedError()
    }
}
