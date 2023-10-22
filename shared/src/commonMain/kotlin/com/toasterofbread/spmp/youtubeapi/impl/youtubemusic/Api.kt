package com.toasterofbread.spmp.youtubeapi.impl.youtubemusic

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.youtubeapi.YoutubeApi
import com.toasterofbread.spmp.youtubeapi.fromJson
import com.toasterofbread.spmp.youtubeapi.getReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import java.io.Reader
import java.util.zip.ZipException

const val DEFAULT_CONNECT_TIMEOUT = 10000

fun <T> Result.Companion.failure(request: Request, response: Response, api: YoutubeApi?): Result<T> {
    var body: String
    if (api != null) {
        try {
            val reader = response.getReader(api)
            body = reader.readText()
            reader.close()
        }
        catch (e: ZipException) {
            body = response.body!!.string()
        }
    }
    else {
        body = response.body!!.string()
    }

    response.close()

    val request_body = request.body?.let {
        val buffer = Buffer()
        it.writeTo(buffer)
        buffer.readUtf8()
    }

    return failure(RuntimeException("Request failed\nRequest=$request\nRequest body=$request_body\nResponse=$body"))
}

inline fun <I, O> Result<I>.cast(transform: (I) -> O = { it as O }): Result<O> {
    return fold(
        { runCatching { transform(it) } },
        { Result.failure(it) }
    )
}

fun <T> Result<T>.unit(): Result<Unit> {
    return fold(
        { Result.success(Unit) },
        { Result.failure(it) }
    )
}

fun <T> Result<T>.getOrThrowHere(): T =
    fold(
        { it },
        { throw Exception(it) }
    )

// TODO remove
fun <T> Result<T>.getOrReport(context: PlatformContext, error_key: String): T? {
    return fold(
        {
            it
        },
        {
            context.sendNotification(it)
            null
        }
    )
}

fun Artist.isOwnChannel(api: YoutubeApi): Boolean {
    return id == api.user_auth_state?.own_channel?.id
}
