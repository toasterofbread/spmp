package com.toasterofbread.spmp.model.settings

import dev.toastbits.ytmkt.model.ApiAuthenticationState
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistRef
import io.ktor.http.Headers
import io.ktor.http.HeadersBuilder
import io.ktor.util.flattenEntries
import com.toasterofbread.spmp.platform.AppContext

private enum class ValueType { CHANNEL, HEADER }

fun ApiAuthenticationState.Companion.packSetData(
    own_channel_id: String?,
    headers: Headers
): Set<String> {
    val set: MutableSet<String> = mutableSetOf()

    if (own_channel_id != null) {
        set.add(ValueType.CHANNEL.ordinal.toString() + own_channel_id)
    }

    for ((key, value) in headers.flattenEntries()) {
        set.add(ValueType.HEADER.ordinal.toString() + "$key=$value")
    }

    return set
}

fun ApiAuthenticationState.Companion.unpackSetData(
    set: Set<String>,
    context: AppContext
): Pair<String?, Headers>? {
    if (set.isEmpty()) {
        return null
    }

    var own_channel_id: String? = null
    val headers_builder: HeadersBuilder = HeadersBuilder()

    for (item in set) {
        val value = item.substring(1)
        when (ValueType.entries[item.take(1).toInt()]) {
            ValueType.CHANNEL -> {
                if (own_channel_id != null) {
                    continue
                }

                val own_channel: Artist = ArtistRef(value)
                own_channel_id = own_channel.id
                own_channel.createDbEntry(context.database)
            }
            ValueType.HEADER -> {
                val split: List<String> = value.split('=', limit = 2)
                headers_builder.append(split[0], split[1])
            }
        }
    }

    return Pair(own_channel_id, headers_builder.build())
}
