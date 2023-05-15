package com.spectre7.spmp.ui.layout

import SpMp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.spectre7.spmp.api.*
import com.spectre7.spmp.model.Artist
import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.model.YoutubeMusicAuthInfo
import com.spectre7.spmp.platform.WebViewLogin
import com.spectre7.spmp.platform.isWebViewLoginSupported
import okhttp3.Request
import java.net.URI

private const val MUSIC_URL = "https://music.youtube.com/"
private const val MUSIC_LOGIN_URL = "https://accounts.google.com/v3/signin/identifier?dsh=S1527412391%3A1678373417598386&continue=https%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26app%3Ddesktop%26hl%3Den-GB%26next%3Dhttps%253A%252F%252Fmusic.youtube.com%252F%253Fcbrd%253D1%26feature%3D__FEATURE__&hl=en-GB&ifkv=AWnogHfK4OXI8X1zVlVjzzjybvICXS4ojnbvzpE4Gn_Pfddw7fs3ERdfk-q3tRimJuoXjfofz6wuzg&ltmpl=music&passive=true&service=youtube&uilel=3&flowName=GlifWebSignIn&flowEntry=ServiceLogin"

@Composable
fun YoutubeMusicLogin(modifier: Modifier = Modifier, onFinished: (Result<YoutubeMusicAuthInfo>?) -> Unit) {
    if (isWebViewLoginSupported()) {
        WebViewLogin(MUSIC_URL, modifier, shouldShowPage = { !it.startsWith(MUSIC_URL) }) { request, openUrl, getCookie ->
            val url = URI(request.url)
            if (url.host == "music.youtube.com" && url.path?.startsWith("/youtubei/v1/") == true) {
                if (!request.requestHeaders.containsKey("Authorization")) {
                    openUrl(MUSIC_LOGIN_URL)
                    return@WebViewLogin
                }

                val cookie = getCookie(MUSIC_URL)
                val account_request = Request.Builder()
                    .url("https://music.youtube.com/youtubei/v1/account/account_menu")
                    .addHeader("cookie", cookie)
                    .apply {
                        for (header in request.requestHeaders) {
                            addHeader(header.key, header.value)
                        }
                    }
                    .post(DataApi.getYoutubeiRequestBody())
                    .build()

                val result = DataApi.request(account_request)
                result.fold(
                    { response ->
                        val parsed: AccountMenuResponse = DataApi.klaxon.parse(response.body!!.charStream())!!
                        response.close()

                        onFinished(Result.success(
                            YoutubeMusicAuthInfo(
                                parsed.getAritst()!!,
                                cookie,
                                request.requestHeaders
                            )
                        ))
                    },
                    {
                        onFinished(result.cast())
                    }
                )
            }
        }
    }
    else {
        // TODO
        SpMp.context.openUrl(MUSIC_LOGIN_URL)
        YoutubeMusicManualLogin(modifier, onFinished)
    }
}

@Composable 
fun YoutubeMusicManualLogin(modifier: Modifier = Modifier, onFinished: (Result<YoutubeMusicAuthInfo>?) -> Unit) {
    Column(modifier) {
        Text(getStringTODO("TODO"))

        var headers_value by remember { mutableStateOf("") }
        TextField(
            headers_value, 
            { headers_value = it }, 
            Modifier.fillMaxWidth(), 
            label = {
                Text("Headers")
            }
        )

        Button({
            onFinished(Result.success(
                TODO(headers_value)
            ))
        }) {
            Text(getStringTODO("Done"))
        }
    }
}

private class AccountMenuResponse(val actions: List<Action>) {
    class Action(val openPopupAction: OpenPopupAction)
    class OpenPopupAction(val popup: Popup)
    class Popup(val multiPageMenuRenderer: MultiPageMenuRenderer)
    class MultiPageMenuRenderer(val sections: List<Section>, val header: Header? = null)

    class Section(val multiPageMenuSectionRenderer: MultiPageMenuSectionRenderer)
    class MultiPageMenuSectionRenderer(val items: List<Item>)
    class Item(val compactLinkRenderer: CompactLinkRenderer)
    class CompactLinkRenderer(val navigationEndpoint: NavigationEndpoint? = null)

    class Header(val activeAccountHeaderRenderer: ActiveAccountHeaderRenderer)
    class ActiveAccountHeaderRenderer(val accountName: TextRuns, val accountPhoto: MusicThumbnailRenderer.Thumbnail)

    fun getAritst(): Artist? {
        val account = actions.first().openPopupAction.popup.multiPageMenuRenderer.header!!.activeAccountHeaderRenderer
        return Artist.fromId(getChannelId() ?: return null).editArtistData {
            supplyTitle(account.accountName.first_text)
            supplyThumbnailProvider(MediaItem.ThumbnailProvider.fromThumbnails(account.accountPhoto.thumbnails))
        }
    }

    fun getChannelId(): String? {
        for (section in actions.first().openPopupAction.popup.multiPageMenuRenderer.sections) {
            for (item in section.multiPageMenuSectionRenderer.items) {
                val browse_endpoint = item.compactLinkRenderer.navigationEndpoint?.browseEndpoint
                if (browse_endpoint?.getPageType() == "MUSIC_PAGE_TYPE_USER_CHANNEL") {
                    return browse_endpoint.browseId
                }
            }
        }
        return null
    }
}
