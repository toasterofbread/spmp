package com.spectre7.spmp.model

import com.spectre7.spmp.platform.PlatformContext
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.Reader
import java.time.Duration
import java.time.Instant
import kotlin.concurrent.thread
import kotlin.io.path.relativeTo

class Cache {
    companion object {
        private lateinit var cache_dir: File

        fun init(context: PlatformContext) {
            cache_dir = File(context.getCacheDir(), "spmp")

            if (!cache_dir.exists()) {
                cache_dir.mkdirs()
                if (!cache_dir.exists()) {
                    throw FileNotFoundException("Could not create cache dir")
                }
            }

            thread {
                clean()
            }
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

            try {
                file.createNewFile()
            }
            catch (e: IOException) {
                throw RuntimeException(file.absolutePath, e)
            }

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

            val meta_line = reader.readLine()
            if (meta_line == null) {
                reader.close()
                return null
            }

            val expiry = parseCacheMetadata(meta_line)
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