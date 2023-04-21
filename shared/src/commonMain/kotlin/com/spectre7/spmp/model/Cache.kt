package com.spectre7.spmp.model

import com.spectre7.spmp.platform.PlatformContext
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.Reader
import java.time.Duration
import java.time.Instant
import kotlin.io.path.relativeTo
import com.spectre7.utils.getString

class Cache {
    companion object {
        private lateinit var cache_dir: File

        fun init(context: PlatformContext) {
            cache_dir = File(context.getCacheDir(), getString("app_name"))

            if (!cache_dir.exists()) {
                cache_dir.mkdirs()
                if (!cache_dir.exists()) {
                    throw FileNotFoundException("Could not create cache dir")
                }
            }
            clean()
        }

        fun clean() {
            val now = Instant.now()
            for (file in cache_dir.walkTopDown()) {
                if (file.isDirectory) {
                    continue
                }

                val reader = file.bufferedReader()
                val metadata = reader.readLine()
                reader.close()

                if (parseCacheMetadata(metadata).isBefore(now)) {
                    file.delete()
                    println("Deleted expired cache file at ${file.toPath().relativeTo(cache_dir.toPath())}")
                }
            }
        }

        fun reset() {
            cache_dir.deleteRecursively()
        }

        fun set(path: String, value: Reader?, lifetime: Duration?) {
            val file = cache_dir.resolve(path)
            if (value == null) {
                file.delete()
                return
            }

            file.parentFile?.mkdirs()
            file.createNewFile()

            val expiry: String = if (lifetime != null) Instant.now().plusSeconds(lifetime.toSeconds()).epochSecond.toString() else ""

            val writer = file.writer()
            writer.write(expiry + "\n")
            value.copyTo(writer)
            writer.write("\n")
            writer.flush()
            writer.close()
        }

        fun setString(path: String, value: String, lifetime: Duration?) {
            val reader = BufferedReader(value.reader())
            set(path, reader, lifetime)
            reader.close()
        }

        fun get(path: String): BufferedReader? {
            val file = cache_dir.resolve(path)
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