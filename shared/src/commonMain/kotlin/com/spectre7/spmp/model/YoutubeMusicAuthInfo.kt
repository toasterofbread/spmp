package com.spectre7.spmp.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class YoutubeMusicAuthInfo: Set<String> {
    enum class ValueType { CHANNEL, COOKIE, HEADER }
    var initialised: Boolean = false

    lateinit var own_channel: Artist
        private set
    lateinit var cookie: String
        private set
    lateinit var headers: Map<String, String>
        private set

    fun initialisedOrNull(): YoutubeMusicAuthInfo? = if (initialised) this else null
    fun getOwnChannelOrNull(): Artist? = initialisedOrNull()?.own_channel

    constructor()

    constructor(own_channel: Artist, cookie: String, headers: Map<String, String>) {
        this.own_channel = own_channel

        runBlocking {
            with(own_channel) {
                withContext(Dispatchers.IO) {
                    is_own_channel = true
                }
            }
        }

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
            }
        }
        headers = set_headers

        initialised = true
    }

    override val size: Int get() = if (initialised) 2 + headers!!.size else 0
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
                else -> ValueType.HEADER.ordinal.toString()  + headerToString(headers.entries.elementAt(i - 3))
            }
        }
    }

    private fun headerToString(header: Map.Entry<String, String>): String {
        return "${header.key}=${header.value}"
    }
    private fun stringToHeader(header: String): Pair<String, String> {
        val split = header.split('=', limit = 2)
        return Pair(split[0], split[1])
    }
}
