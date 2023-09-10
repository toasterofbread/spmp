package com.toasterofbread.spmp.youtubeapi.endpoint

import com.toasterofbread.spmp.youtubeapi.YoutubeApi
import okhttp3.Headers

abstract class UserAuthStateEndpoint: YoutubeApi.Endpoint() {
    abstract suspend fun byHeaders(headers: Headers): Result<YoutubeApi.UserAuthState>
}
