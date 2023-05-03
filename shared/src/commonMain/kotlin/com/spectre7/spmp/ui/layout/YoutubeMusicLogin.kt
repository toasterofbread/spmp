package com.spectre7.spmp.ui.layout

private const val MUSIC_URL = "https://music.youtube.com/"
private const val MUSIC_LOGIN_URL = "https://accounts.google.com/v3/signin/identifier?dsh=S1527412391%3A1678373417598386&continue=https%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26app%3Ddesktop%26hl%3Den-GB%26next%3Dhttps%253A%252F%252Fmusic.youtube.com%252F%253Fcbrd%253D1%26feature%3D__FEATURE__&hl=en-GB&ifkv=AWnogHfK4OXI8X1zVlVjzzjybvICXS4ojnbvzpE4Gn_Pfddw7fs3ERdfk-q3tRimJuoXjfofz6wuzg&ltmpl=music&passive=true&service=youtube&uilel=3&flowName=GlifWebSignIn&flowEntry=ServiceLogin"

@Composable
fun YoutubeMusicLogin(modifier: Modifier = Modifier, onFinished: (Result<YoutubeMusicAuthInfo>) -> Unit) {
    var showing_warning: Boolean? = remember { mutableStateOf(!Settings.CHOICE_ACCEPT_YTM_LOGIN_WARNING.get<Boolean>()) }
    when (showing_warning) {
        true -> {
            PlatformAlertDialog(
                { 
                    show_reset_confirmation = false
                    Settings.CHOICE_ACCEPT_YTM_LOGIN_WARNING.set(true)
                },
                confirmButton = {
                    FilledTonalButton({ showing_warning = false }) {
                        Text(getString("action_confirm_action"))
                    }
                },
                dismissButton = { TextButton({ showing_warning = null }) { Text(getString("action_deny_action")) } },
                title = { Text(getString("prompt_confirm_action")) },
                text = {
                    Text(getString("warning_ytm_login"))
                }
            )
        }
        false -> {
            if (isWebViewLoginSupported()) {
                WebViewLogin(MUSIC_URL, modifier, { !it.startsWith(MUSIC_URL) }) { request, openUrl ->
                    if (request.url.host == "music.youtube.com" && request.url.path?.startsWith("/youtubei/v1/") == true) {
                        if (!request.requestHeaders.containsKey("Authorization")) {
                            openUrl(MUSIC_LOGIN_URL)
                            return@WebViewLogin false
                        }

                        val cookie = CookieManager.getInstance().getCookie(YOUTUBE_MUSIC_URL)
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

                        return@WebViewLogin true
                    }

                    return@WebViewLogin false
                }
            }
            else {
                // TODO
                SpMp.context.openUrl(MUSIC_LOGIN_URL)
            }
        }
        null -> {

        }
    }
}
