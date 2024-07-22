package com.toasterofbread.spmp.youtubeapi

import dev.toastbits.ytmkt.impl.youtubei.YoutubeiApi
import dev.toastbits.ytmkt.model.external.mediaitem.YtmArtist
import dev.toastbits.ytmkt.model.internal.YoutubeAccountMenuResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Headers
import io.ktor.util.flattenEntries
import com.toasterofbread.spmp.platform.AppContext

object YTMLogin {
    fun replaceCookiesInString(base_cookies: String, new_cookies: List<String>): String {
        var cookie_string: String = base_cookies

        for (cookie in new_cookies) {
            val split: List<String> = cookie.split('=', limit = 2)

            val name: String = split[0]
            val new_value: String = split[1].split(';', limit = 2)[0]

            val cookie_start: Int = cookie_string.indexOf("$name=") + name.length + 1
            if (cookie_start != name.length) {
                val cookie_end: Int = cookie_string.indexOf(';', cookie_start)
                cookie_string = (
                    cookie_string.substring(0, cookie_start)
                        + new_value
                        + if (cookie_end != -1) cookie_string.substring(cookie_end, cookie_string.length) else ""
                )
            }
            else {
                cookie_string += "; $name=$new_value"
            }
        }

        return cookie_string
    }

    suspend fun completeLoginWithAccount(
        context: AppContext,
        headers: Headers,
        account: AccountSwitcherEndpoint.AccountItem,
        api: YoutubeiApi
    ): Result<SpMpYoutubeiAuthenticationState> = runCatching {
        val account_headers: Headers

        if (!account.isSelected) {
            val sign_in_url_path: String =
                account.serviceEndpoint.selectActiveIdentityEndpoint!!.supportedTokens.first { it.accountSigninToken != null }.accountSigninToken!!.signinUrl

            val sign_in_response: HttpResponse =
                HttpClient().get("https://music.youtube.com/" + sign_in_url_path) {
                    expectSuccess = true
                    headers {
                        appendAll(headers)
                    }
                }

            val new_cookies: List<String> = sign_in_response.headers.flattenEntries().mapNotNull { header ->
                if (header.first.lowercase() == "set-cookie") header.second
                else null
            }

            val cookie_string: String = replaceCookiesInString(
                headers["Cookie"]!!,
                new_cookies
            )

            account_headers = Headers.build {
                appendAll(headers)
                set("Cookie", cookie_string)
            }

            val channel_id: String? =
                account.serviceEndpoint.selectActiveIdentityEndpoint.supportedTokens.firstOrNull { it.offlineCacheKeyToken != null }?.offlineCacheKeyToken?.clientCacheKey

            if (channel_id != null) {
                return@runCatching SpMpYoutubeiAuthenticationState(
                    context.database,
                    api,
                    "UC$channel_id",
                    account_headers
                )
            }
        }
        else {
            account_headers = headers
        }

        return@runCatching completeLogin(context, account_headers, api).getOrThrow()
    }

    suspend fun completeLogin(
        context: AppContext,
        headers: Headers,
        api: YoutubeiApi
    ): Result<SpMpYoutubeiAuthenticationState> = runCatching {
        with(api) {
            val account_response: HttpResponse =
                api.client.request {
                    endpointPath("account/account_menu")
                    headers {
                        appendAll(headers)
                    }
                    addUnauthenticatedApiHeaders()
                    postWithBody()
                }

            val parsed: YoutubeAccountMenuResponse = account_response.body()

            val new_headers: Headers = Headers.build {
                appendAll(headers)

                val new_cookies: List<String> = account_response.headers.flattenEntries().mapNotNull { header ->
                    if (header.first.lowercase() == "set-cookie") header.second
                    else null
                }

                set("Cookie", replaceCookiesInString(headers["Cookie"]!!, new_cookies))
            }

            val channel: YtmArtist? = parsed.getAritst()

            return@runCatching SpMpYoutubeiAuthenticationState(
                context.database,
                api,
                channel?.id,
                new_headers
            )
        }
    }
}
