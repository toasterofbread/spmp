package com.spectre7.spmp.model

import androidx.compose.runtime.*
import com.beust.klaxon.Json
import com.beust.klaxon.JsonObject
import com.spectre7.spmp.api.DataApi
import com.spectre7.spmp.platform.ProjectPreferences
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class MediaItemDataRegistry {
    private val entries: MutableMap<String, Entry> = mutableMapOf()
    val size: Int get() = entries.size

    open class Entry {
        @Json(ignored = true)
        var item: MediaItem? = null

        @Json(ignored = true)
        val title_state: MutableState<String?> = mutableStateOf(null)

        var title: String?
            get() = title_state.value
            set(value) {
                title_state.value = value
                item?.also { i ->
                    i.title_listeners.call(i.title)
                }
            }

        @Suppress("UNCHECKED_CAST")
        var play_counts: MutableMap<Int, Int> = mutableStateMapOf()
            set(value) {
                value as LinkedHashMap<String, Int>
                for (entry in value) {
                    field[entry.key.toInt()] = entry.value
                }
            }

        private fun getCurrentDay(): Int = ChronoUnit.DAYS.between(LocalDate.ofEpochDay(0), LocalDate.now()).toInt()

        fun incrementPlayCount(by: Int = 1) {
            val key = getCurrentDay()
            val current = play_counts[key] ?: 0
            play_counts[key] = current + by
        }

        fun getPlayCount(range: ChronoUnit?): Int {
            var play_count = 0
            for (entry in play_counts) {
                val key_days = entry.key.toLong()
                val key_day = LocalDate.ofEpochDay(key_days)

                if (
                    range != null &&
                    range.between(key_day, LocalDate.now()) > 1
                ) {
                    continue
                }
                play_count += entry.value
            }
            return play_count
        }
    }

    @Synchronized
    fun getEntry(item: MediaItem): Entry {
        return entries.getOrPut(item.uid) {
            item.getDefaultRegistryEntry()
        }.also { it.item = item }
    }

    @Synchronized
    fun load(prefs: ProjectPreferences = Settings.prefs) {
        val data = prefs.getString("data_registry", null)
        if (data == null) {
            return
        }

        entries.clear()

        val parsed = DataApi.klaxon.parseJsonObject(data.reader())
        runBlocking {
            parsed.entries.map { item ->
                GlobalScope.async {
                    try {
                        val type = MediaItemType.values()[item.key.take(1).toInt()]
                        entries[item.key] = type.parseRegistryEntry(item.value as JsonObject)
                    }
                    catch (e: Throwable) {
                        e.printStackTrace()
                    }
                }
            }.joinAll()
        }
    }

    @Synchronized
    fun save(prefs: ProjectPreferences = Settings.prefs) {
        prefs.edit {
            putString("data_registry", DataApi.klaxon.toJsonString(entries))
        }
    }
}
