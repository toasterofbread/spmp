package com.spectre7.spmp.model

import android.content.Context
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.time.Duration
import java.time.Instant

class Cache {
    companion object {
        private lateinit var cache_dir: File

        fun init(context: Context) {
            cache_dir = File(context.cacheDir, "cache")

            if (!cache_dir.exists()) {
                cache_dir.mkdirs()
                if (!cache_dir.exists()) {
                    throw FileNotFoundException("Could not create cache dir")
                }
            }
            clean()
        }

        fun clean() {
            val files = cache_dir.listFiles() ?: return
            val now = Instant.now()
            for (file in files) {
                val reader = file.bufferedReader()
                val metadata = reader.readLine()
                reader.close()

                if (parseCacheMetadata(metadata).isBefore(now)) {
                    file.delete()
                }
            }
        }

        fun reset() {
            val files = cache_dir.listFiles() ?: return
            for (file in files) {
                file.delete()
            }
        }

        fun set(key: String, value: BufferedReader, lifetime: Duration?) {
            val file = File(cache_dir, key)
            file.createNewFile()

            val expiry: String = if (lifetime != null) Instant.now().plusSeconds(lifetime.toSeconds()).epochSecond.toString() else ""

            val writer = file.writer()
            writer.write(expiry + "\n")
            value.copyTo(writer)
            writer.flush()
            writer.close()
        }

        fun setString(key: String, value: String, lifetime: Duration?) {
            val reader = BufferedReader(value.reader())
            set(key, reader, lifetime)
            reader.close()
        }

        fun get(key: String): BufferedReader? {
            val file = File(cache_dir, key)
            if (!file.exists()) {
                return null
            }

            val reader = file.bufferedReader()
            val expiry = parseCacheMetadata(reader.readLine())

            if (expiry.isBefore(Instant.now())) {
                reader.close()
                file.delete()
                return null
            }

            return reader
        }

        fun getString(key: String, default: () -> String?): String? {
            val reader = get(key) ?: return default()
            val ret = reader.readText()
            reader.close()
            return ret
        }

        private fun parseCacheMetadata(value: String): Instant {
            if (value.isBlank()) {
                return Instant.MAX
            }
            return Instant.ofEpochSecond(value.toLong())
        }
    }
}