package com.spectre7.spmp.model

import java.time.Instant
import java.time.Duration
import android.content.SharedPreferences
import android.content.Context

class Cache {
    companion object {
        private lateinit var cache: SharedPreferences
        private const val epoch_length = 10

        fun init(context: Context) {
            cache = context.getSharedPreferences("com.spectre7.spmp.cache", Context.MODE_PRIVATE)
            clean()
        }

        fun clean() {
            with (cache.edit()) {
                val now = Instant.now()
                for (item in cache.getAll()) {
                    val parsed = parseCacheValue(item.value)
                    if (parsed.first.isBefore(now)) {
                        remove(item.key)
                    }
                }
                apply()
            }
        }

        fun reset() {
            with (cache.edit()) {
                for (item in cache.getAll()) {
                    remove(item.key)
                }
                apply()
            }
        }

        fun set(key: String, value: String, lifetime: Duration?) {
            val expiry: String = if (lifetime != null) Instant.now().plusSeconds(lifetime.toSeconds()).getEpochSecond().toString() else ""
            with (cache.edit()) {
                putString(expiry.padStart(epoch_length, '0') + value)
                apply()
            }
        }

        fun get(key: String, default: String? = null) {
            val value: String = cache.getString(key, null) ?: return default
            val parsed = parseCacheValue(value)

            if (parsed.first.isBefore(Instant.now())) {
                // Cache has expired
                with (cache.edit()) {
                    remove(key)
                    apply()
                }
                return default
            }

            return parsed.second
        }

        private fun parseCacheValue(value: String): Pair<Instant, String> {
            return Pair(Instant.ofEpochSecond(value.substring(0, epoch_length).toLong()), value.substring(epoch_length))
        }
    }
}