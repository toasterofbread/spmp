package com.toasterofbread.spmp.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.toasterofbread.spmp.api.Api
import com.toasterofbread.spmp.api.Api.Companion.addYtHeaders
import com.toasterofbread.spmp.api.Api.Companion.getStream
import com.toasterofbread.spmp.api.Api.Companion.ytUrl
import com.toasterofbread.spmp.api.cast
import com.toasterofbread.spmp.api.getAccountPlaylists
import com.toasterofbread.spmp.model.mediaitem.AccountPlaylist
import com.toasterofbread.spmp.model.mediaitem.Artist
import com.toasterofbread.spmp.ui.layout.YTAccountMenuResponse
import com.toasterofbread.utils.printJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.Headers.Companion.headersOf
import okhttp3.Headers.Companion.toHeaders
import okhttp3.Request

class YoutubeMusicAuthInfo: Set<String> {
    enum class ValueType { CHANNEL, COOKIE, HEADER, PLAYLIST }
    var initialised: Boolean by mutableStateOf(false)

    lateinit var own_channel: Artist
        private set
    lateinit var cookie: String
        private set
    lateinit var headers: Map<String, String>
        private set

    var own_playlists_loaded: Boolean by mutableStateOf(false)
    var own_playlists: MutableList<String> = mutableStateListOf()
        private set
    fun onOwnPlaylistDeleted(playlist: AccountPlaylist) {
        own_playlists.remove(playlist.id)
    }

    fun initialisedOrNull(): YoutubeMusicAuthInfo? = if (initialised) this else null
    fun getOwnChannelOrNull(): Artist? = initialisedOrNull()?.own_channel

    constructor()

    constructor(own_channel: Artist, cookie: String, headers: Map<String, String>) {
        this.own_channel = own_channel
        own_channel.is_own_channel = true

        this.cookie = cookie
        this.headers = headers
        initialised = true
    }

    constructor(set: Set<String>) {
        if (set.isEmpty()) {
            return
        }

        require(set.size >= 2)
        val set_headers = mutableMapOf<String, String>()
        for (item in set) {
            val value = item.substring(1)
            when (ValueType.values()[item.take(1).toInt()]) {
                ValueType.CHANNEL -> own_channel = Artist.fromId(value).also {
                    runBlocking {
                        withContext(Dispatchers.IO) {
                            it.is_own_channel = true
                        }
                    }
                }
                ValueType.COOKIE -> cookie = value
                ValueType.HEADER -> stringToHeader(value).also { set_headers[it.first] = it.second }
                ValueType.PLAYLIST -> own_playlists.add(value)
            }
        }
        headers = set_headers

        initialised = true
    }

    override val size: Int get() = if (initialised) 2 + headers.size + own_playlists.size else 0
    override fun contains(element: String): Boolean = throw NotImplementedError()
    override fun containsAll(elements: Collection<String>): Boolean = throw NotImplementedError()
    override fun isEmpty(): Boolean = !initialised

    override fun iterator(): Iterator<String> = object : Iterator<String> {
        private var i = 0
        override fun hasNext(): Boolean = i < size
        override fun next(): String {
            return when (i++) {
                0 ->    ValueType.CHANNEL.ordinal.toString() + own_channel.id
                1 ->    ValueType.COOKIE.ordinal.toString()  + cookie
                else -> {
                    val index = i - 3
                    if (index >= headers.size)
                        ValueType.PLAYLIST.ordinal.toString() + own_playlists[index - headers.size]
                    else
                        ValueType.HEADER.ordinal.toString()  + headerToString(headers.entries.elementAt(index))
                }
            }
        }
    }

    suspend fun loadOwnPlaylists(): Result<Unit> {
        check(initialised)

        val result = getAccountPlaylists()
        return result.fold(
            { playlists ->
                synchronized(own_playlists) {
                    own_playlists.clear()
                    for (playlist in playlists) {
                        own_playlists.add(playlist.id)
                    }
                }
                own_playlists_loaded = true
                Result.success(Unit)
            },
            { Result.failure(it) }
        )
    }

    private fun headerToString(header: Map.Entry<String, String>): String {
        return "${header.key}=${header.value}"
    }
    private fun stringToHeader(header: String): Pair<String, String> {
        val split = header.split('=', limit = 2)
        return Pair(split[0], split[1])
    }

    companion object {
        val REQUIRED_KEYS = listOf("authorization", "cookie")

        suspend fun fromHeaders(headers: Map<String, String>): Result<YoutubeMusicAuthInfo> = withContext(Dispatchers.IO) {
            for (key in REQUIRED_KEYS) {
                check(headers.contains(key))
            }

            val request = Request.Builder()
                .ytUrl("/youtubei/v1/account/account_menu")
                .addYtHeaders(true)
                .apply {
                    for (key in REQUIRED_KEYS) {
                        header(key, headers[key]!!)
                    }
                }
                .post(Api.getYoutubeiRequestBody())
                .build()

            val result = Api.request(request)

            val response = result.getOrNull() ?: return@withContext result.cast()
            val stream = response.getStream()

            val parsed: YTAccountMenuResponse = try {
                Api.klaxon.parse(stream)!!
            }
            catch (e: Throwable) {
                return@withContext Result.failure(e)
            }
            finally {
                stream.close()
            }

            return@withContext Result.success(
                YoutubeMusicAuthInfo(
                    parsed.getAritst()!!,
                    headers["cookie"]!!,
                    headers
                )
            )
        }
    }
}

private class AccountMenuResponse
