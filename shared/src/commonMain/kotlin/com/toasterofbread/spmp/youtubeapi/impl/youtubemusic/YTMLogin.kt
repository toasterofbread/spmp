package com.toasterofbread.spmp.youtubeapi.impl.youtubemusic
import com.toasterofbread.spmp.ui.layout.youtubemusiclogin.AccountSwitcherEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.YoutubeMusicApi
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import okhttp3.Headers
import okhttp3.Request
import okhttp3.Response
import okhttp3.OkHttpClient
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.YoutubeMusicAuthInfo
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistRef
import com.toasterofbread.spmp.ui.layout.youtubemusiclogin.YTAccountMenuResponse
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.YoutubeChannelNotCreatedException
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.DataParseException
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.youtubeapi.executeResult
import com.toasterofbread.spmp.youtubeapi.fromJson

internal object YTMLogin {
    fun replaceCookiesInString(base_cookies: String, new_cookies: List<String>): String {
        var cookie_string: String = base_cookies

        for (cookie in new_cookies) {
            val split: List<String> = cookie.split('=', limit = 2)

            val name: String = split[0]
            val new_value: String = split[1].split(';', limit = 2)[0]

            val cookie_start: Int = cookie_string.indexOf("$name=") + name.length + 1
            if (cookie_start != -1) {
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
        headers: Headers,
        account: AccountSwitcherEndpoint.AccountItem,
        api: YoutubeMusicApi
    ): Result<YoutubeMusicAuthInfo> = withContext(Dispatchers.IO) {
        val account_headers: Headers

        if (!account.isSelected) {
            val sign_in_url: String =
                account.serviceEndpoint.selectActiveIdentityEndpoint.supportedTokens.first { it.accountSigninToken != null }.accountSigninToken!!.signinUrl

            val sign_in_request: Request = with(api) {
                Request.Builder()
                    .endpointUrl(sign_in_url)
                    .headers(headers)
                    .get()
                    .build()
            }

            val result: Result<Response> = OkHttpClient().executeResult(sign_in_request)

            val new_cookies: List<String> = result.fold(
                {
                    it.use { response ->
                        response.headers.mapNotNull { header ->
                            if (header.first == "Set-Cookie") header.second
                            else null
                        }
                    }
                },
                {
                    return@withContext Result.failure(it)
                }
            )

            val cookie_string: String = replaceCookiesInString(
                headers["Cookie"]!!,
                new_cookies
            )

            account_headers = headers
                .newBuilder()
                .set("Cookie", cookie_string)
                .build()

            val channel_id: String? =
                account.serviceEndpoint.selectActiveIdentityEndpoint.supportedTokens.firstOrNull { it.offlineCacheKeyToken != null }?.offlineCacheKeyToken?.clientCacheKey

            if (channel_id != null) {
                return@withContext Result.success(
                    YoutubeMusicAuthInfo.create(api, ArtistRef("UC$channel_id"), account_headers)
                )
            }
        }
        else {
            account_headers = headers
        }

        return@withContext completeLogin(account_headers, api)
    }

    suspend fun completeLogin(
        headers: Headers,
        api: YoutubeMusicApi
    ): Result<YoutubeMusicAuthInfo> = withContext(Dispatchers.IO) {
        with(api) {
            val account_request: Request = Request.Builder()
                .endpointUrl("/youtubei/v1/account/account_menu")
                .headers(headers)
                .addAuthlessApiHeaders()
                .postWithBody()
                .build()

            val result: Result<Response> = api.performRequest(account_request)
            result.fold(
                { response ->
                    try {
                        val parsed: YTAccountMenuResponse = response.body!!.charStream().use { stream ->
                            api.gson.fromJson(stream)
                        }

                        val headers_builder: Headers.Builder = headers.newBuilder()
                        val new_cookies: List<String> = response.headers.mapNotNull { header ->
                            if (header.first == "Set-Cookie") header.second
                            else null
                        }
                        headers_builder["Cookie"] = replaceCookiesInString(headers_builder["Cookie"]!!, new_cookies)

                        val channel: Artist? = parsed.getAritst()
                        if (channel == null) {
                            return@withContext Result.failure(YoutubeChannelNotCreatedException(headers_builder.build(), parsed.getChannelCreationToken()))
                        }

                        return@withContext Result.success(YoutubeMusicAuthInfo.create(api, channel, headers_builder.build()))
                    }
                    catch (e: Throwable) {
                        return@withContext Result.failure(
                            DataParseException(e, account_request) {
                                runCatching {
                                    api.performRequest(account_request).getOrThrow().body!!.string()
                                }
                            }
                        )
                    }
                },
                {
                    return@withContext Result.failure(it)
                }
            )
        }
    }
}
