package com.toasterofbread.spmp.ui.layout

import SpMp
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.api.Api
import com.toasterofbread.spmp.api.YoutubeAccountCreationForm
import com.toasterofbread.spmp.api.cast
import com.toasterofbread.spmp.api.getYoutubeAccountCreationForm
import com.toasterofbread.spmp.api.model.BrowseEndpoint
import com.toasterofbread.spmp.api.model.MusicThumbnailRenderer
import com.toasterofbread.spmp.api.model.NavigationEndpoint
import com.toasterofbread.spmp.api.model.TextRuns
import com.toasterofbread.spmp.model.YoutubeChannelNotCreatedException
import com.toasterofbread.spmp.model.YoutubeMusicAuthInfo
import com.toasterofbread.spmp.model.mediaitem.Artist
import com.toasterofbread.spmp.model.mediaitem.ArtistData
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.platform.WebViewLogin
import com.toasterofbread.spmp.platform.composable.PlatformAlertDialog
import com.toasterofbread.spmp.platform.isWebViewLoginSupported
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.resources.getStringTODO
import com.toasterofbread.utils.composable.LinkifyText
import com.toasterofbread.utils.composable.SubtleLoadingIndicator
import okhttp3.Request
import java.net.URI

private const val MUSIC_URL = "https://music.youtube.com/"
private const val MUSIC_LOGIN_URL = "https://accounts.google.com/v3/signin/identifier?dsh=S1527412391%3A1678373417598386&continue=https%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26app%3Ddesktop%26hl%3Den-GB%26next%3Dhttps%253A%252F%252Fmusic.youtube.com%252F%253Fcbrd%253D1%26feature%3D__FEATURE__&hl=en-GB&ifkv=AWnogHfK4OXI8X1zVlVjzzjybvICXS4ojnbvzpE4Gn_Pfddw7fs3ERdfk-q3tRimJuoXjfofz6wuzg&ltmpl=music&passive=true&service=youtube&uilel=3&flowName=GlifWebSignIn&flowEntry=ServiceLogin"

@Composable
fun YoutubeMusicLoginConfirmation(info_only: Boolean = false, onFinished: (manual: Boolean?) -> Unit) {
    PlatformAlertDialog(
        { onFinished(null) },
        confirmButton = {
            FilledTonalButton({
                onFinished(if (info_only) null else false)
            }) {
                Text(getString("action_confirm_action"))
            }
        },
        dismissButton = if (info_only) null else ({
            TextButton({ onFinished(null) }) { Text(getString("action_deny_action")) }
        }),
        title = if (info_only) null else ({ Text(getString("prompt_confirm_action")) }),
        text = {
            Column {
                LinkifyText(getString(if (info_only) "info_ytm_login" else "warning_ytm_login"))
                if (!info_only) {
                    FilledTonalButton({ onFinished(true) }, Modifier.fillMaxWidth().padding(top = 5.dp).offset(y = 20.dp)) {
                        Text(getString("action_login_manually"))
                    }
                }
            }
        }
    )
}

@Composable
fun YoutubeMusicLogin(modifier: Modifier = Modifier, manual: Boolean = false, onFinished: (Result<YoutubeMusicAuthInfo>?) -> Unit) {
    var channel_not_created_error: YoutubeChannelNotCreatedException? by remember { mutableStateOf(null) }
    var channel_creation_form: Result<YoutubeAccountCreationForm.ChannelCreationForm>? by remember { mutableStateOf(null) }

    LaunchedEffect(channel_not_created_error) {
        val error = channel_not_created_error ?: return@LaunchedEffect
        if (error.channel_creation_token != null) {
            channel_creation_form = null
            channel_creation_form = getYoutubeAccountCreationForm(error.cookie, error.headers, error.channel_creation_token)
        }
        else {
            channel_creation_form = Result.failure(RuntimeException(getStringTODO("No channel creation token")))
        }
    }

    channel_creation_form?.also {
        it.onSuccess { form ->
            val chanel_error = channel_not_created_error!!

            YoutubeChannelCreateDialog(
                chanel_error.cookie,
                chanel_error.headers,
                form
            ) { result ->
                if (result == null) {
                    onFinished(null)
                    return@YoutubeChannelCreateDialog
                }

                result.fold(
                    { channel ->
                        onFinished(
                            Result.success(
                                YoutubeMusicAuthInfo(
                                    channel,
                                    chanel_error.cookie,
                                    chanel_error.headers
                                )
                            )
                        )
                    },
                    { error ->
                        onFinished(Result.failure(error))
                    }
                )
            }
        }
    }

    Crossfade(channel_creation_form ?: if (channel_not_created_error != null) null else true, modifier) { state ->
        if (state is Result<*>) {
            val error = state.exceptionOrNull()
            if (error != null) {
                // TODO
            }
        }
        else if (state == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                SubtleLoadingIndicator(message = getString("youtube_channel_creation_load_message"))
            }
        }
        else if (manual) {
            YoutubeMusicManualLogin(Modifier.fillMaxSize(), onFinished)
        }
        else if (isWebViewLoginSupported()) {
            var finished: Boolean by remember { mutableStateOf(false) }
            val lock = remember { Object() }

            WebViewLogin(
                MUSIC_URL,
                Modifier.fillMaxSize(),
                loading_message = getString("youtube_login_load_message"),
                shouldShowPage = { !it.startsWith(MUSIC_URL) },
                onClosed = { onFinished(null) }
            ) { request, openUrl, getCookie ->
                synchronized(lock) {
                    if (finished) {
                        return@WebViewLogin
                    }

                    val url = URI(request.url)
                    if (url.host == "music.youtube.com" && url.path?.startsWith("/youtubei/v1/") == true) {
                        if (!request.requestHeaders.containsKey("Authorization")) {
                            openUrl(MUSIC_LOGIN_URL)
                            return@WebViewLogin
                        }

                        finished = true

                        val cookie = getCookie(MUSIC_URL)
                        val account_request = Request.Builder()
                            .url("https://music.youtube.com/youtubei/v1/account/account_menu")
                            .addHeader("cookie", cookie)
                            .apply {
                                for (header in request.requestHeaders) {
                                    addHeader(header.key, header.value)
                                }
                            }
                            .post(Api.getYoutubeiRequestBody(null))
                            .build()

                        val result = Api.request(account_request)
                        result.fold(
                            { response ->
                                val parsed: YTAccountMenuResponse = Api.klaxon.parse(response.body!!.charStream())!!
                                response.close()

                                val auth_result = YoutubeMusicAuthInfo.fromYTAccountMenuResponse(parsed, cookie, request.requestHeaders)
                                val auth_error = auth_result.exceptionOrNull()
                                if (auth_error is YoutubeChannelNotCreatedException) {
                                    channel_not_created_error = auth_error
                                    return@fold
                                }

                                onFinished(auth_result)
                            },
                            {
                                onFinished(result.cast())
                            }
                        )
                    }
                }
            }
        }
        else {
            // TODO
            LaunchedEffect(Unit) {
                SpMp.context.openUrl(MUSIC_LOGIN_URL)
            }
            YoutubeMusicManualLogin(Modifier.fillMaxSize(), onFinished)
        }
    }
}

data class CreateChannelResponse(val navigationEndpoint: ChannelNavigationEndpoint) {
    data class ChannelNavigationEndpoint(val browseEndpoint: BrowseEndpoint)
}

data class YTAccountMenuResponse(val actions: List<Action>) {
    data class Action(val openPopupAction: OpenPopupAction)
    data class OpenPopupAction(val popup: Popup)
    data class Popup(val multiPageMenuRenderer: MultiPageMenuRenderer)
    data class MultiPageMenuRenderer(val sections: List<Section>, val header: Header? = null)

    data class Section(val multiPageMenuSectionRenderer: MultiPageMenuSectionRenderer)
    data class MultiPageMenuSectionRenderer(val items: List<Item>)
    data class Item(val compactLinkRenderer: CompactLinkRenderer)
    data class CompactLinkRenderer(val navigationEndpoint: NavigationEndpoint? = null)

    data class Header(val activeAccountHeaderRenderer: ActiveAccountHeaderRenderer)
    data class ActiveAccountHeaderRenderer(val accountName: TextRuns, val accountPhoto: MusicThumbnailRenderer.Thumbnail)

    fun getAritst(): Artist? {
        val account = actions.first().openPopupAction.popup.multiPageMenuRenderer.header!!.activeAccountHeaderRenderer
        return ArtistData(getChannelId() ?: return null).apply {
            title = account.accountName.first_text
            thumbnail_provider = MediaItemThumbnailProvider.fromThumbnails(account.accountPhoto.thumbnails)
        }
    }

    private fun getSections() = actions.first().openPopupAction.popup.multiPageMenuRenderer.sections

    private fun getChannelId(): String? {
        for (section in getSections()) {
            for (item in section.multiPageMenuSectionRenderer.items) {
                val browse_endpoint = item.compactLinkRenderer.navigationEndpoint?.browseEndpoint
                if (browse_endpoint?.getMediaItemType() == MediaItemType.ARTIST) {
                    return browse_endpoint.browseId
                }
            }
        }
        return null
    }

    fun getChannelCreationToken(): String? {
        for (section in getSections()) {
            for (item in section.multiPageMenuSectionRenderer.items) {
                val endpoint = item.compactLinkRenderer.navigationEndpoint?.channelCreationFormEndpoint
                if (endpoint != null) {
                    return endpoint.channelCreationToken
                }
            }
        }
        return null
    }
}
