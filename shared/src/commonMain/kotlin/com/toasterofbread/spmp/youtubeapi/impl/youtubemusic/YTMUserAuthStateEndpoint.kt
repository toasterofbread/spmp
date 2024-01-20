package com.toasterofbread.spmp.youtubeapi.impl.youtubemusic

import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.ui.layout.youtubemusiclogin.YTAccountMenuResponse
import com.toasterofbread.spmp.youtubeapi.endpoint.UserAuthStateEndpoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.Request
import okhttp3.Response

class YTMUserAuthStateEndpoint(
    override val api: YoutubeMusicApi
): UserAuthStateEndpoint() {
    override suspend fun byHeaders(headers: Headers): Result<YoutubeMusicAuthInfo> {
        val names: Set<String> = headers.names()
        val missing_headers: MutableList<String> = mutableListOf()

        for (header in YoutubeMusicAuthInfo.REQUIRED_HEADERS) {
            if (names.none { it.equals(header, ignoreCase = true) }) {
                missing_headers.add(header)
            }
        }

        if (missing_headers.isNotEmpty()) {
            return Result.failure(IllegalArgumentException("Missing the following headers: $missing_headers"))
        }

        return withContext(Dispatchers.IO) {
            val request: Request = Request.Builder()
                .endpointUrl("/youtubei/v1/account/account_menu")
                .addAuthApiHeaders()
                .apply {
                    for (key in YoutubeMusicAuthInfo.REQUIRED_HEADERS) {
                        for (value in headers.values(key)) {
                            header(key, value)
                        }
                    }
                }
                .postWithBody()
                .build()

            println("HEADERS ${request.headers}")

            val result: Result<Response> = api.performRequest(request)
            val data: YTAccountMenuResponse = result.parseJsonResponse {
                return@withContext Result.failure(it)
            }

            val artist: Artist? = data.getAritst()
            if (artist == null) {
                return@withContext Result.failure(
                    YoutubeChannelNotCreatedException(headers, data.getChannelCreationToken())
                )
            }

            return@withContext Result.success(
                YoutubeMusicAuthInfo.create(api, artist, headers)
            )
        }
    }
}
